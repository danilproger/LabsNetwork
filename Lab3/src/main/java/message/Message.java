package message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Message {
	private final static int MAX_ATTEMPTS = 10;
	private final static long TIME_TO_RESEND = 300;

	private final UUID messageID;
	private MessageType messageType;
	private final String message;

	private String destinationIp;
	private String destinationPort;

	private int attempts;
	private long sentTime;

	public Message(MessageType type, String message) {
		this.messageID = UUID.randomUUID();
		this.message = message;
		this.messageType = type;
	}

	public Message(Message message) {
		this.messageID = message.messageID;
		this.message = message.message;
		this.messageType = message.messageType;
	}

	private Message(UUID messageID, String message, MessageType type, String destinationIp, String destinationPort) {
		this.messageID = messageID;
		this.message = message;
		this.messageType = type;
		this.destinationIp = destinationIp;
		this.destinationPort = destinationPort;
	}

	public void incAttempts() {
		++attempts;
		sentTime = System.currentTimeMillis();
	}

	public int checkTime() {
		if ((System.currentTimeMillis() - sentTime) > TIME_TO_RESEND) {
			if (attempts < MAX_ATTEMPTS) {
				return 1;
			} else {
				return -1;
			}
		} else {
			return 0;
		}
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public boolean needToConfirm() {
		return messageType != MessageType.CONFIRM;
	}

	public UUID getID() {
		return messageID;
	}

	public String getDestinationIp() {
		return destinationIp;
	}

	public void setDestinationIp(String destinationIp) {
		this.destinationIp = destinationIp;
	}

	public String getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(String destinationPort) {
		this.destinationPort = destinationPort;
	}

	public String getText() {
		return message;
	}

	public MessageType getType() {
		return messageType;
	}

	public DatagramPacket encodeToPacket() {
		byte[] data = (
						messageID.toString() + ":" +
						messageType.toString() + ":" +
						destinationIp + ":" +
						destinationPort + ":" +
						message
		).getBytes(StandardCharsets.UTF_16);

		InetAddress destinationInetAddress = null;

		try {
			destinationInetAddress = InetAddress.getByName(destinationIp.substring(1));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		if (destinationInetAddress == null) {
			return null;
		}

		return new DatagramPacket(
				data,
				data.length,
				destinationInetAddress,
				Integer.parseInt(destinationPort)
		);
	}

	public static Message decodeToMessage(DatagramPacket packet) {
		String[] data = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_16).split(":");

		UUID id = UUID.fromString(data[0]);
		MessageType type = MessageType.valueOf(data[1]);
		String destinationIp = data[2];
		String destinationPort = data[3];

		StringBuilder text = new StringBuilder();

		for (int i = 4; i < data.length; ++i) {
			text.append(data[i]);
			if (i != data.length - 1) {
				text.append(":");
			}
		}

		return new Message(id, text.toString(), type, destinationIp, destinationPort);
	}
}
