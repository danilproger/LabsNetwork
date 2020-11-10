package node;

import message.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class NodeReceiver extends Thread {
	private final List<NodeInfo> neighbours;
	private final Map<UUID, Message> messages;
	private final TreeNode self;
	private final int loss;
	private final DatagramSocket socket;

	private final byte[] buffer;
	private final DatagramPacket packet;

	public NodeReceiver(List<NodeInfo> neighbours, Map<UUID, Message> messages, TreeNode self, int loss, DatagramSocket socket) {
		this.neighbours = neighbours;
		this.messages = messages;
		this.self = self;
		this.loss = loss;
		this.socket = socket;

		this.buffer = new byte[2048];
		this.packet = new DatagramPacket(buffer, buffer.length);
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				socket.receive(packet);

				Message message = Message.decodeToMessage(packet);

				switch (message.getType()) {
					case MESSAGE:
						NodeInfo info = new NodeInfo(packet.getAddress(), packet.getPort());
						if (!neighbours.contains(info)) {
							neighbours.add(info);
						}
						if ((int) (Math.random() * (100)) >= loss) {
							if (!messages.containsKey(message.getID())) {
								messages.put(message.getID(), message);
								System.out.println(message.getText());
								self.sendMessageToAll(message.getID());
							}
						}
						break;
					case ACCEPT:
						//TODO
						break;
					case DISCONNECT:
						//TODO
						break;

				}


			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
