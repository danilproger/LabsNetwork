package proxy;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Proxy {
    private static final int BUF_SIZE = 8096;

    private final int port;
    private Selector selector;
    private final HashMap<SocketChannel, Stage> connectionStage;
    private final HashMap<Integer, HostInfo> DNSConnections;
    private final HashMap<SocketChannel, SocketChannel> proxyConnection;

    public Proxy(int port) {
        this.port = port;
        this.connectionStage = new HashMap<>();
        this.DNSConnections = new HashMap<>();
        this.proxyConnection = new HashMap<>();
    }

    public void run() throws IOException {
        selector = Selector.open();
        String[] dnsServers = ResolverConfig.getCurrentConfig().servers();
        SelectionKey key;
        ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);
        ServerSocketChannel server = ChannelUtils.createServerSocket(selector,
                new InetSocketAddress("localhost", port), SelectionKey.OP_ACCEPT);
        DatagramChannel dnsChannel = ChannelUtils.createDatagramSocket(selector,
                new InetSocketAddress(dnsServers[0], 53), SelectionKey.OP_READ);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                key = iter.next();
                iter.remove();
                if (key.isValid()) {
                    if (key.isConnectable()) {
                        System.out.println("Finishing connection for " + key.channel());
                        ((SocketChannel) key.channel()).finishConnect();
                    }
                    if (key.isAcceptable()) {
                        System.out.println("Accepting new channel: " + key.channel());
                        SocketChannel newChannel = ChannelUtils.createSocket(server, selector,
                                SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
                        connectionStage.put(newChannel, Stage.FIRST);
                    }
                    if (key.isReadable()) {
                        boolean dnsMessage = !(key.channel() instanceof SocketChannel);
                        buffer.clear();
                        if (!dnsMessage) {
                            if (connectionStage.get((SocketChannel) key.channel()) == null) {
                                connectionStage.put(((SocketChannel) key.channel()), Stage.FIRST);
                            }
                            SocketChannel channelFrom = (SocketChannel) key.channel();
                            switch (connectionStage.get((SocketChannel) key.channel())) {
                                case FIRST:
                                    boolean correctFirst = MessageUtils.getFirstMessage(channelFrom);
                                    if (correctFirst) {
                                        System.out.println("connection stage 1 -> stage 2: " + key.channel());
                                        MessageUtils.sendFirstConfirmation(channelFrom);
                                        connectionStage.replace(channelFrom, Stage.SECOND);
                                    } else {
                                        connectionStage.remove((SocketChannel) key.channel());
                                        killChannelsOnKey(key);
                                    }
                                    buffer.clear();
                                    break;
                                case SECOND:
                                    SecondParseResult secondMessage = MessageUtils.getSecondMessage(channelFrom);
                                    if (secondMessage.isCorrect()) {
                                        if (secondMessage.isDns()) {
                                            System.out.println("connection stage 2 -> stage 3, resolving DNS: " + key.channel());
                                            Name name = Name.fromString(new String(secondMessage.getAddress()), Name.root);
                                            Record rec = Record.newRecord(name, Type.A, DClass.IN);
                                            Message dns = Message.newQuery(rec);
                                            dnsChannel.write(ByteBuffer.wrap(dns.toWire()));
                                            int port = secondMessage.getPort();
                                            DNSConnections.put(dns.getHeader().getID(), new HostInfo(channelFrom, port));
                                        } else {
                                            System.out.println("connection stage 2 -> stage 3, connecting: " + key.channel());
                                            InetAddress address = InetAddress.getByAddress(secondMessage.getAddress());
                                            int port = secondMessage.getPort();
                                            if (establishConnection(channelFrom, new InetSocketAddress(address, port), key))
                                                connectionStage.replace(channelFrom, Stage.THIRD);
                                        }
                                    }
                                    buffer.clear();
                                    break;
                                case THIRD:
                                    SocketChannel channelTo = proxyConnection.get(channelFrom);
                                    System.out.println("Connection from " + channelFrom.toString() + " to " + channelTo.toString());
                                    if (channelTo.isConnected()) {
                                        int amount;
                                        try {
                                            amount = channelFrom.read(buffer);
                                            if (amount == -1) {
                                                killChannelsOnKey(key);
                                            } else {
                                                System.out.println(amount);
                                                System.out.println(Arrays.toString(buffer.array()));
                                                channelTo.write(ByteBuffer.wrap(buffer.array(), 0, amount));
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            System.out.println("Closing connection");
                                            killChannelsOnKey(key);
                                        }
                                    }
                                    buffer.clear();
                                    break;
                            }
                        } else {
                            System.out.println("dns message from: " + key.channel());
                            ByteBuffer dnsBuf = ByteBuffer.allocate(1024);
                            int len = dnsChannel.read(dnsBuf);
                            if (len <= 0) continue;
                            Message msg = new Message(dnsBuf.array());
                            Record[] recs = msg.getSectionArray(1);
                            for (Record rec : recs) {
                                if (rec instanceof ARecord) {
                                    ARecord arec = (ARecord) rec;
                                    InetAddress adr = arec.getAddress();
                                    int id = msg.getHeader().getID();
                                    HostInfo myConnection = DNSConnections.get(id);
                                    int port = myConnection.getPort();
                                    SocketChannel channel = DNSConnections.get(id).getChannel();
                                    if (establishConnection(channel, new InetSocketAddress(adr, port), key)) {
                                        connectionStage.replace(channel, Stage.THIRD);
                                    } else {
                                        connectionStage.remove(channel);
                                        killChannelsOnKey(key);
                                    }
                                    DNSConnections.remove(id);
                                    break;
                                }
                            }
                            buffer.clear();
                        }
                    }
                }
            }
        }

    }

    private boolean establishConnection(SocketChannel channel, InetSocketAddress serverAddress, SelectionKey key) throws IOException {
        SocketChannel serverChannel = SocketChannel.open(serverAddress);
        System.out.println("establishing connection: " + key.channel());

        if (!serverChannel.isConnected()) {
            return false;
        }

        try {
            MessageUtils.sendSecondConfirmationMessage(channel, (short)serverAddress.getPort(), serverChannel.isConnected());
        } catch (IOException e) {
            return false;
        }

        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
        proxyConnection.put(channel, serverChannel);
        proxyConnection.put(serverChannel, channel);
        connectionStage.put(serverChannel, Stage.THIRD);
        return serverChannel.isConnected();
    }

    private void killChannelsOnKey(SelectionKey key) throws IOException {
        SocketChannel channel = proxyConnection.get((SocketChannel) key.channel());
        if (channel != null) {
            channel.close();
            proxyConnection.remove(proxyConnection.get((SocketChannel) key.channel()));
            proxyConnection.remove((SocketChannel) key.channel());
        }
        key.channel().close();
    }

}
