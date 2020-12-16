package proxy;

import java.nio.channels.SocketChannel;

public class HostInfo {
    private SocketChannel channel;
    private int port;

    public HostInfo(SocketChannel channel, int port){
        this.channel = channel;
        this.port = port;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
