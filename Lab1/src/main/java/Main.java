import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		MulticastDetection multicast = null;
		try {
			multicast = new MulticastDetection("224.0.0.11", 8080);
			multicast.start();
		} catch (IOException e) {
			if (multicast != null) {
				multicast.stop();
			}
			e.printStackTrace();
		}
	}
}