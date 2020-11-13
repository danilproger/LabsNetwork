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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;

public class TreeNode implements Closeable {
	private final String name;
	private final int loss;

	private final DatagramSocket socket;
	private String delegator;

	private final List<String> neighbours;
	private final Map<String, String> delegators;

	private final Map<UUID, Message> messagesToConfirm;
	private final Queue<Message> messagesToSend;
	private final Queue<UUID> receivedMessages;

	private final Thread receiver;
	private final Thread sender;
	private final Thread resender;

	public TreeNode(String name, int port, int loss) throws IOException {
		this.name = name;
		this.loss = loss;

		this.socket = new DatagramSocket(port);
		this.delegator = null;

		this.neighbours = Collections.synchronizedList(new ArrayList<>());
		this.delegators = new ConcurrentHashMap<>();

		this.messagesToConfirm = new ConcurrentHashMap<>();
		this.messagesToSend = new ConcurrentLinkedDeque<>();
		this.receivedMessages = new ConcurrentLinkedDeque<>();

		this.receiver = new Thread(this::receivingMessages);
		this.sender = new Thread(this::sendingMessage);
		this.resender = new Thread(this::resendingMessages);
	}

	public TreeNode(String name, int port, int loss, InetAddress connectionIp, int connectionPort) throws IOException {
		this(name, port, loss);
		neighbours.add(connectionIp.toString() + ":" + connectionPort);
	}

	public void start() {
		receiver.start();
		sender.start();
		resender.start();

		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Scanner in = new Scanner(System.in);

		while (true) {
			String line = in.nextLine();
			if (line.equals("exit")) {
				break;
			}
			if (line.equals("nei")) {
				System.out.println("-------neighbours-----");
				neighbours.forEach(System.out::println);
				System.out.println("----------------------");
				continue;
			}
			if (line.equals("del")) {
				System.out.println("-----delegator-----");
				if (delegator != null) {
					System.out.println(delegator);
				}
				System.out.println("-------------------");
				continue;
			}
			if (line.equals("con")) {
				System.out.println("---messages co confirm---");
				messagesToConfirm.forEach((id, message) -> System.out.println(id));
				System.out.println("-------------------------");
				continue;
			}
			if (line.equals("dels")) {
				System.out.println("---delegators---");
				delegators.forEach((id, delegator_) -> System.out.println(id + " has delegator: " + delegator_));
				System.out.println("----------------");
				continue;
			}
			if (line.equals("recv")) {
				System.out.println("---received messages---");
				receivedMessages.forEach(System.out::println);
				System.out.println("-----------------------");
				continue;
			}
			String text =
					name + "\n" +
							formatter.format(Calendar.getInstance().getTime()) + "\n" +
							line;

			Message message = new Message(
					MessageType.MESSAGE,
					text
			);

			neighbours.forEach((id) -> {
				String[] neighbourInfo = id.split(":");
				Message messageForNeighbour = new Message(message);
				messageForNeighbour.setDestinationIp(neighbourInfo[0]);
				messageForNeighbour.setDestinationPort(neighbourInfo[1]);

				messagesToSend.add(messageForNeighbour);
			});
		}

		in.close();
		this.close();
	}


	@Override
	public void close() {
		sender.interrupt();
		receiver.interrupt();
		resender.interrupt();

		socket.close();
	}

	private void sendingMessage() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (messagesToSend.size() > 0) {
					Message messageToSend = messagesToSend.remove();
					DatagramPacket packet = messageToSend.encodeToPacket();
					socket.send(packet);

					if (messageToSend.needToConfirm()) {
						messageToSend.incAttempts();
						if (!messagesToConfirm.containsKey(messageToSend.getID())) {
							messagesToConfirm.put(messageToSend.getID(), messageToSend);
						}
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void receivingMessages() {
		byte[] buffer = new byte[16384];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		while (!Thread.currentThread().isInterrupted()) {
			try {
				socket.receive(packet);
				if (!lostPacket()) {
					handleReceivedPacket(packet);
				}
			} catch (IOException ignored) {

			}
		}
	}

	private void resendingMessages() {
		while (!Thread.currentThread().isInterrupted()) {
			for (UUID id : messagesToConfirm.keySet()) {
				Message messageToConfirm = messagesToConfirm.getOrDefault(id, null);
				if (messageToConfirm != null) {
					switch (messageToConfirm.checkTime()) {
						case 1 -> {
							messagesToSend.add(messageToConfirm);
						}

						case -1 -> {
							String expectedReceiver = messageToConfirm.getDestinationIp() + ":" + messageToConfirm.getDestinationPort();
							String[] expectedRecieverInfo = expectedReceiver.split(":");

							messagesToConfirm.values().removeIf((message) ->
									(message.getDestinationIp().equals(expectedRecieverInfo[0])) &&
									(message.getDestinationPort().equals(expectedRecieverInfo[1]))
							);

							neighbours.remove(expectedReceiver);

							String expectedReceiverDelegator = delegators.getOrDefault(expectedReceiver, null);
							if (expectedReceiverDelegator != null) {
								delegators.remove(expectedReceiver);
								neighbours.add(expectedReceiverDelegator);
							}



							if (delegator != null && delegator.equals(expectedReceiver)) {
								delegator = neighbours.get(0);

								Message newDelegatorMessage = new Message(
										MessageType.DELEGATOR,
										delegator
								);

								neighbours.forEach((neighbour) -> {
									if (!neighbour.equals(delegator)) {
										String[] neighbourInfo = neighbour.split(":");
										Message message = new Message(newDelegatorMessage);
										message.setDestinationIp(neighbourInfo[0]);
										message.setDestinationPort(neighbourInfo[1]);

										messagesToSend.add(message);
									}
								});
							}
						}
					}
				}
			}
		}
	}

	private synchronized void handleReceivedPacket(DatagramPacket packet) {
		Message receivedMessage = Message.decodeToMessage(packet);

		String sender = packet.getAddress().toString() + ":" + packet.getPort();
		String[] senderInfo = sender.split(":");

		if (!neighbours.contains(sender)) {
			neighbours.add(sender);
			if (delegator == null) {
				delegator = sender;

				Message newDelegatorMessage = new Message(
						MessageType.DELEGATOR,
						delegator
				);

				neighbours.forEach((neighbour) -> {
					if (!neighbour.equals(sender)) {
						String[] neighbourInfo = neighbour.split(":");
						Message message = new Message(newDelegatorMessage);
						message.setDestinationIp(neighbourInfo[0]);
						message.setDestinationPort(neighbourInfo[1]);

						messagesToSend.add(message);
					}
				});
			} else {
				Message delegatorMessage = new Message(
						MessageType.DELEGATOR,
						delegator
				);

				delegatorMessage.setDestinationIp(senderInfo[0]);
				delegatorMessage.setDestinationPort(senderInfo[1]);
				messagesToSend.add(delegatorMessage);
			}
		}

		switch (receivedMessage.getType()) {
			case CONFIRM -> {
				messagesToConfirm.remove(receivedMessage.getID());
			}
			case MESSAGE -> {
				if (!receivedMessages.contains(receivedMessage.getID())) {
					receivedMessages.add(receivedMessage.getID());

					System.out.println(receivedMessage.getText());

					neighbours.forEach((neighbour) -> {
						if (!neighbour.equals(sender)) {
							String[] neighbourInfo = neighbour.split(":");
							Message message = new Message(receivedMessage);
							message.setDestinationIp(neighbourInfo[0]);
							message.setDestinationPort(neighbourInfo[1]);

							messagesToSend.add(message);
						}
					});
				}
				Message confirm = new Message(receivedMessage);
				confirm.setMessageType(MessageType.CONFIRM);
				confirm.setDestinationIp(senderInfo[0]);
				confirm.setDestinationPort(senderInfo[1]);

				messagesToSend.add(confirm);
			}
			case DELEGATOR -> {
				if (!receivedMessages.contains(receivedMessage.getID())) {
					receivedMessages.add(receivedMessage.getID());
					delegators.put(sender, receivedMessage.getText());
				}
				Message confirm = new Message(receivedMessage);
				confirm.setMessageType(MessageType.CONFIRM);
				confirm.setDestinationIp(senderInfo[0]);
				confirm.setDestinationPort(senderInfo[1]);

				messagesToSend.add(confirm);
			}
		}

	}

	private boolean lostPacket() {
		return ThreadLocalRandom.current().nextInt(0, 100) < loss;
	}
}
