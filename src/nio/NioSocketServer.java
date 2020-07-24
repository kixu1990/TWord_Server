package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import message.MyMessage;
import servers.User;

public class NioSocketServer {
	private volatile byte flag = 1;
		
	public void setFlag(byte flag) {
		this.flag = flag;
	}
	
	public void start() {
		try(ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){
			serverSocketChannel.socket().bind(new InetSocketAddress(11001));
			serverSocketChannel.configureBlocking(false);
			Selector selector = Selector.open();			
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			System.out.println("NIO服务器启动...");
			
			ServerHandlerBs handler = new ServerHandlerImpl(1024 * 100000,null);
			
			while(flag == 1) {
				selector.select();
							
				Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
				System.out.println("开始处理请求：... 总通道数量： "+ selector.keys().size());
				while(keyIterator.hasNext()) {
					SelectionKey key = keyIterator.next();
					try {
						if(key.isAcceptable()) {
							handler.handleAccept(key);
						}
						if(key.isReadable()) {
							handler.handleRead(key);
						}
					}catch (IOException e) {
						// TODO: handle exception
						//e.printStackTrace();
						System.out.println();
						SocketChannel channel = (SocketChannel)key.channel();
						channel.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					keyIterator.remove();
				}
				
				System.out.println("完成请求处理。");
			}
		}catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public static boolean sendMessage(MyMessage message,User user) {
		byte[] messageBytes = ObjectFlidByte.objectToByteArray(message);
				
		SocketChannel socketChannel = null;
		ByteBuffer buffer = null;		
		HashMap<SocketChannel, Buffer> m = UsersSocketChannel.getInstance().getSocketChannel(user.getUserId());
		if(m != null) {
		Set keySet = m.keySet();
		Iterator it = keySet.iterator();
		if(it.hasNext()) {
			try {
			socketChannel = (SocketChannel) it.next();
			buffer = ByteBuffer.allocate(messageBytes.length+4);
			buffer.clear();
			buffer.put(getHeadByte(messageBytes.length));
			System.out.println(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
			int sendSize = 0;
			while(buffer.remaining() > 0) {
				sendSize += socketChannel.write(buffer);
//				System.out.println(sendSize+"    "+buffer.remaining());
				buffer.position(sendSize);
				buffer.mark();
			}
			
			buffer.clear();
			}catch (IOException e) {
				// TODO: handle exception
//				e.printStackTrace();
				return false;
			}
			System.out.println("发送信息成功！ 通道： "+socketChannel.toString());
			return true;
		}
		}
		return false;
	}

	public static void sendMessage(MyMessage message,SocketChannel socketChannel,ByteBuffer byteBuffer) throws IOException {
		byte[] messageBytes = ObjectFlidByte.objectToByteArray(message);
		byteBuffer.clear();
		byteBuffer.put(getHeadByte(messageBytes.length));
		byteBuffer.put(messageBytes);
		byteBuffer.flip();
		socketChannel.write(byteBuffer);
		byteBuffer.clear();
	}
	/*
	 * 封装包头
	 */
	
	public static byte[] getHeadByte(int value) {
		byte[] result = new byte[4];
		result[0] = (byte) ((value >> 24) & 0xFF);
		result[1] = (byte) ((value >> 16) & 0xFF);
		result[2] = (byte) ((value >> 8) & 0xFF);
		result[3] = (byte) (value & 0xFF);
		return result;
	}
	
	/*
	 * 解封包头
	 */
	public static int getHeadInt(byte[] bytes) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (bytes[i] & 0x000000FF) << shift;// 往高位游
		}
		return value;
	}

}
