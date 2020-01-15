package nio;

public class ServerMain {
	public static void main(String[] args) {
		NioSocketServer server = new NioSocketServer();
		server.start();
	}

}
