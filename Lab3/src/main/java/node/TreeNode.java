package node;

import message.Message;
import message.MessageType;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

public class TreeNode implements Closeable {
	private final String name;
	private final int port;
	private final int loss;

	private NodeReceiver receiver;

	private final DatagramSocket socket;

	private final List<NodeInfo> neighbours;
	private final Map<UUID, Message> messages;

	public TreeNode(String name, int port, int loss) throws IOException {
		this.name = name;
		this.port = port;
		this.loss = loss;

		this.socket = new DatagramSocket(port);

		this.neighbours = Collections.synchronizedList(new ArrayList<>());
		this.messages = Collections.synchronizedMap(new HashMap<>());

		this.receiver = new NodeReceiver(
				neighbours,
				messages,
				this,
				loss,
				socket
		);

	}

	public TreeNode(String name, int port, int loss, InetAddress connectionIp, int connectionPort) throws IOException {
		this(name, port, loss);
		neighbours.add(new NodeInfo(connectionIp, connectionPort));
	}

	public void start() throws IOException {
		receiver.start();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

		try (Scanner in = new Scanner(System.in)) {
			while (0 != 1) {

				String line = in.nextLine();

				if (line.equals("-exit")) {
					sendExitMessage();
					break;
				}

				String text =
						name + "\n" +
								formatter.format(Calendar.getInstance().getTime()) + "\n" +
								line;

				Message message = new Message(text, MessageType.MESSAGE);

				messages.put(message.getID(), message);
				sendMessageToAll(message.getID());

			}
		} finally {
			System.out.println("R.I.P.");
		}
	}

	@Override
	public void close() {
		receiver.interrupt();
		socket.close();
	}

	private void sendExitMessage() {
		//TODO
	}

	public void sendMessageToAll(UUID messageId) throws IOException {
		for (NodeInfo neighbour : neighbours) {
			InetAddress neighbourIp = neighbour.getIp();
			int neighbourPort = neighbour.getPort();

			DatagramPacket packet = messages.get(messageId).encodeToPacket(neighbourIp, neighbourPort);

			socket.send(packet);
			messages.get(messageId).inc();
		}
	}
}
