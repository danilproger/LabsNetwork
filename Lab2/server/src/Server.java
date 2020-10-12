import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements AutoCloseable {
	private final ServerSocket serverSocket;
	private final ArrayList<Socket> clientSockets;
	private final ArrayList<Thread> clientThreads;

	public Server(int serverPort) throws IOException {
		this.serverSocket = new ServerSocket(serverPort);
		this.clientSockets = new ArrayList<>();
		this.clientThreads = new ArrayList<>();
	}

	public void start() throws IOException {
		System.out.println("Server is listening: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());
		System.out.println("|   Address     |   Speed   |   Ins speed   |   Percents   |   File name   ");
		while (true) {
			Socket client = serverSocket.accept();
			Thread clientThread = new Thread(new ClientHandler(client));

			clientSockets.add(client);
			clientThreads.add(clientThread);

			clientThread.start();
		}
	}

	@Override
	public void close() throws IOException {

		for (Socket s : clientSockets) {
			s.close();
		}

		for (Thread t : clientThreads) {
			t.interrupt();
		}

		serverSocket.close();
	}
}