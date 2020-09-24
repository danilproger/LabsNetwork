import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		try (MulticastDetection multicast = new MulticastDetection("224.0.0.11", 8080)) {
			multicast.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}