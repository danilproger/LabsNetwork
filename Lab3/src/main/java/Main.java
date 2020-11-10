import node.TreeNode;

import java.io.IOException;
import java.net.InetAddress;

public class Main {
	public static void main(String[] args) {
		if (args.length == 3) {
			String nodeName = args[0];
			int port = Integer.parseInt(args[1]);
			int loss = Integer.parseInt(args[2]);

			try (TreeNode node = new TreeNode(nodeName, port, loss)) {
				node.start();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		} else if (args.length == 5) {
			String nodeName = args[0];
			int port = Integer.parseInt(args[1]);
			int loss = Integer.parseInt(args[2]);

			try {
				InetAddress connectionIp = InetAddress.getByName(args[3]);
				int connectionPort = Integer.parseInt(args[4]);

				try (TreeNode node = new TreeNode(
						nodeName,
						port,
						loss,
						connectionIp,
						connectionPort
				)) {
					node.start();
				} catch (IOException ex) {
					ex.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("invalid amount of args");
		}
	}
}
