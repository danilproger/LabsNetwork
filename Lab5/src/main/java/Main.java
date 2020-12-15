import proxy.Proxy;

import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		try {
			Proxy proxy = new Proxy(1488);
			proxy.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
