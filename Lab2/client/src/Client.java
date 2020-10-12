import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client implements AutoCloseable {
	private final File file;
	private final Socket clientSocket;
	private final DataOutputStream outputStream;

	public Client(int serverPort, String serverIp, String filePath) throws IOException {
		this.file = new File(filePath);
		this.clientSocket = new Socket(InetAddress.getByName(serverIp), serverPort);
		this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
	}

	public void start() throws IOException {
		sendFileName();
		sendFileSize();
		sendFile();
	}

	@Override
	public void close() throws IOException {
		outputStream.close();
		clientSocket.close();
	}

	private void sendFileName() throws IOException {
		byte[] fileName = file.getName().getBytes(StandardCharsets.UTF_8);
		outputStream.writeInt(fileName.length);
		outputStream.write(fileName);
	}

	private void sendFileSize() throws IOException {
		outputStream.writeLong(file.length());
	}

	private void sendFile() throws IOException {
		FileInputStream fileStream = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		int bytes;
		while ((bytes = fileStream.read(buffer, 0, 1024)) > 0) {
			outputStream.write(buffer, 0, bytes);
		}
		fileStream.close();
	}
}
