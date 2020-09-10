import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class MulticastDetection {
	private final long RECEIVE_TIMEOUT = 3000;
	private final int SOCKET_TIMEOUT = 300;

	private final MulticastSocket receiver;
	private final DatagramSocket publisher;

	private final Map<String, Long> clientsTable = new HashMap<>();

	private final InetAddress groupIP;
	private final int groupPort;

	public MulticastDetection(String group, int port) throws IOException {
		receiver = new MulticastSocket(port);
		publisher = new DatagramSocket();
		groupIP = InetAddress.getByName(group);
		groupPort = port;

		receiver.joinGroup(groupIP);
		receiver.setSoTimeout(SOCKET_TIMEOUT);
	}

	public void start() throws IOException {
		String message = "empty message";
		DatagramPacket toSend = new DatagramPacket(
				message.getBytes(),
				message.length(),
				groupIP,
				groupPort
		);

		while (true) {
			publisher.send(toSend);

			long start = System.currentTimeMillis();
			while (System.currentTimeMillis() - start < RECEIVE_TIMEOUT) {
				DatagramPacket rdp = new DatagramPacket(new byte[1024], 1024);
				try {
					receiver.receive(rdp);
				} catch (SocketTimeoutException e) {
					continue;
				}
				updateTable(rdp);
			}
			checkTable();
			printTable();
		}
	}

	private void updateTable(DatagramPacket dp) {
		if (dp != null) {
			clientsTable.put(dp.getSocketAddress().toString(), System.currentTimeMillis());
		}
	}

	private void checkTable() {
		HashMap<String, Long> feedbackMap = new HashMap<>(clientsTable);

		long time = System.currentTimeMillis();
		for (Map.Entry<String, Long> v : feedbackMap.entrySet()) {
			if (time - v.getValue() > RECEIVE_TIMEOUT * 3) {
				clientsTable.remove(v.getKey());
			}
		}
	}

	private void printTable() {
		long time = System.currentTimeMillis();

		System.out.println("_________________________________");
		System.out.println("|                                |");

		for (Map.Entry<String, Long> v : clientsTable.entrySet()) {
			String output = String.format("|%-32s|", v.getKey() + ":" + (time - v.getValue()));
			System.out.println(output);
		}

		System.out.println("|                                |");
		System.out.println("_________________________________\n");
	}
}
