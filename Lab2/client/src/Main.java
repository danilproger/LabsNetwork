import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		String filePath = args[0];
		String serverIp = args[1];
		int serverPort = Integer.parseInt(args[2]);

		try (Client client = new Client(serverPort, serverIp, filePath)) {
			client.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
