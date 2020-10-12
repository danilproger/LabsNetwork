import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		int serverPort = Integer.parseInt(args[0]);
		try (Server server = new Server(serverPort)) {
			server.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
