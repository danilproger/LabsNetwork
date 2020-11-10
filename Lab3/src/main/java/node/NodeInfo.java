package node;

import java.net.InetAddress;

public class NodeInfo {
	private final InetAddress ip;
	private final int port;

	public NodeInfo(InetAddress ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public InetAddress getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

}
