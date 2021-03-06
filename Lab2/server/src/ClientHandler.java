import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
	private String fileName;
	private File file;
	private long fileSize;

	private final Socket socket;

	public ClientHandler(Socket client) {
		this.socket = client;
	}

	@Override
	public void run() {
		try(DataInputStream stream = new DataInputStream(socket.getInputStream())) {
			receiveFileName(stream);
			receiveFileSize(stream);
			createFile();
			receiveFile(stream);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void receiveFileName(DataInputStream stream) throws IOException {
		int fileNameSize = stream.readInt();
		byte[] fileNameBytes = new byte[fileNameSize];

		stream.readFully(fileNameBytes, 0, fileNameSize);
		fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
	}

	private void receiveFileSize(DataInputStream stream) throws IOException {
		fileSize = stream.readLong();
	}

	private void createFile() throws IOException {
		String uploads = "uploads/";
		file = new File(uploads + fileName);

		if (file.exists()) {
			int i = 0;
			do {
				i++;
				file = new File(uploads + i + fileName);
			} while (file.exists());
			fileName = i + fileName;
		}

		if (!file.createNewFile()) {
			throw new IOException();
		}
	}

	private void receiveFile(DataInputStream stream) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(file);
		int read = 0, bytes;
		byte[] buffer = new byte[1024];
		long start = System.currentTimeMillis();

		while (read < fileSize) {
			long instantStart = System.currentTimeMillis();
			bytes = stream.read(buffer, 0, 1024);
			long instantEnd = System.currentTimeMillis();

			double instantSpeed = 1. * bytes / (1. * (instantEnd - instantStart));

			outputStream.write(buffer, 0, bytes);
			read += bytes;

			printMessage(read, start, instantSpeed);
		}
		outputStream.close();
	}

	private void printMessage(int read, long start, double instantSpeed) {
		long end = System.currentTimeMillis();
		double speed = 1. * read / (1. * end - 1. * start);
		double percents = 1. * read / (1. * fileSize) * 100.0;
		System.out.printf("|%s|%11.1f|%15.1f|%14.2f|   %s\n",
				socket.getInetAddress().getHostAddress() + ":" + socket.getPort(),
				speed,
				instantSpeed,
				percents,
				fileName
		);
	}
}
