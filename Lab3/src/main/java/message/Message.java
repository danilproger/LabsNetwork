package message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Message {
	private final UUID messageID;
	private final MessageType messageType;
	private final String message;
	private int attempts;

	public Message(String message, MessageType type) {
		this.messageID = UUID.randomUUID();
		this.message = message;
		this.messageType = type;
		this.attempts = 0;
	}

	private Message(UUID messageID, String message, MessageType type) {
		this.messageID = messageID;
		this.message = message;
		this.messageType = type;
		this.attempts = 0;
	}

	public UUID getID() {
		return messageID;
	}

	public String getText() {
		return message;
	}

	public MessageType getType() {
		return messageType;
	}

	public void inc() {
		++attempts;
	}

	public void dec() {
		--attempts;
	}

	public DatagramPacket encodeToPacket(InetAddress ip, int port) {
		byte[] data = (
				messageID.toString() + ":" +
						messageType.toString() + ":" +
						message
		).getBytes(StandardCharsets.UTF_16);

		return new DatagramPacket(
				data,
				data.length,
				ip,
				port
		);
	}

	public static Message decodeToMessage(DatagramPacket packet) {
		String[] data = new String(packet.getData(), StandardCharsets.UTF_16).split(":");
		UUID id = UUID.fromString(data[0]);
		MessageType type = MessageType.valueOf(data[1]);
		StringBuilder text = new StringBuilder();

		for (int i = 2; i < data.length; ++i) {
			text.append(data[i]);
			if (i != data.length - 1) {
				text.append(":");
			}
		}

		return new Message(id, text.toString(), type);
	}
}
