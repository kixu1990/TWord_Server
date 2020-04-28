package nio;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import servers.User;


/**
 * 单例模式
 * 用于全局存放每个用户的SocketChannel的Map
 * 
 * @author kixu 2019/11/27 16:37
 *
 */
public class UsersSocketChannel {
	
	private Map<Integer, HashMap<SocketChannel, Buffer>> usersSocketChannel = new HashMap<Integer, HashMap<SocketChannel, Buffer>>();
	
	private UsersSocketChannel() {};
	
	private static UsersSocketChannel INSTANCE = new UsersSocketChannel();
	
	public static UsersSocketChannel getInstance() {
		return INSTANCE;
	}
	
	public HashMap<SocketChannel, Buffer> getSocketChannel(int userId) {
		Set keySet = usersSocketChannel.keySet();
		Iterator it = keySet.iterator();
		while(it.hasNext()) {
			int id = (int)it.next();
			if(id == userId) {
				return usersSocketChannel.get(userId);
			}
		}
		return null;

	}
	
	public int getUserId(SocketChannel socketChannel) {
		int id = 0;
		Set keySet = usersSocketChannel.keySet();
		Iterator it = keySet.iterator();
		while(it.hasNext()) {
			int userId = (int) it.next();
			HashMap map = usersSocketChannel.get(userId);
			Set mapKeySet = map.keySet();
			Iterator mapIt = mapKeySet.iterator();
			while(mapIt.hasNext()) {
				SocketChannel channel = (SocketChannel)mapIt.next();
				if(channel.equals(socketChannel)) {
					id = userId; 
				}
			}
		}
		return id;
	}
	
	public void removeChannel(SocketChannel socketChannel) {
		int removerId = 0;
		Set keySet = usersSocketChannel.keySet();
		Iterator it = keySet.iterator();
		while(it.hasNext()) {
			int userId = (int)it.next();
			HashMap map = usersSocketChannel.get(userId);
			Set mapKeySet = map.keySet();
			Iterator mapIt = mapKeySet.iterator();
			while(mapIt.hasNext()) {
				SocketChannel channel = (SocketChannel)mapIt.next();
				if(channel.equals(socketChannel)) {
					removerId = userId;
				}
			}
		}
		if(removerId != 0) {
			usersSocketChannel.remove(removerId);
		}
		try {
			socketChannel.shutdownInput();
			socketChannel.shutdownOutput();
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateChannel(User user,SocketChannel socketChannel,ByteBuffer buffer) {
		HashMap<SocketChannel,Buffer> map = new HashMap<SocketChannel, Buffer>();
		map.put(socketChannel, buffer);
		
		usersSocketChannel.put(user.getUserId(), map);
	}
	
	public void setSocketChannel(int userId, SocketChannel socketChannel,Buffer buffer) {
		HashMap<SocketChannel, Buffer> m = new HashMap<SocketChannel, Buffer>();
		m.put(socketChannel, buffer);
		usersSocketChannel.put(userId, m);
		System.out.println("存放的通道数量："+usersSocketChannel.size());
	}

}
