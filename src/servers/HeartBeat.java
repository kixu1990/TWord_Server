package servers;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HeartBeat {
	private static final int PORT = 11003;
	
	public void init() throws Exception {
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("�������շ�������...");
		while(true) {
			final Socket client = serverSocket.accept();
			InputStream is = client.getInputStream();
			byte[] b = is.readAllBytes();
			String[] s = new String(b).split("\n");
			
			System.out.println(new String(b));
			
		}
	}

}
