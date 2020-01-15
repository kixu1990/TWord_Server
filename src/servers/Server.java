package servers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.handlers.BeanListHandler;

import dbeas.BaseDao;
import message.MyMessage;


/**
 * 这个类已作废
 * @author kixu 2019/11/29 
 *
 */
public class Server {
	private static final int PORT= 11001;
	
	private List<User> users = new ArrayList<User>();
	
	public static void main(String[] args) {
		
		HeartBeat hearBeatServer = new HeartBeat();
			new Thread() {
				public void run() {
					try {
						hearBeatServer.init();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
			}.start();
		Server server = new Server();
		try {
//			server.listen();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
//	private static void texstst() {
//		Boolean b = false;
//		BaseDao baseDao = new BaseDao();
//		String sql = "select * from tb_users where lotMember = ?";
//		System.out.print("1");
//		try {
//			ArrayList<User> list = (ArrayList<User>)baseDao.query(sql, new BeanListHandler(User.class), "00088");
//			for(int i=0; i<list.size(); i++) {
//				System.out.print(list.get(i).getUserName());
//				}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	
//	private void listen() throws Exception {
//		ServerSocket serverSocket = new ServerSocket(PORT);
//		System.out.println("服务器启动....");
//		while(true) {
//			final Socket client = serverSocket.accept();
//			System.out.println("收接到信息："+client.getInetAddress().toString());
//			new Thread() {
//				public void run() {		
//					InputStream in;
//					ObjectInputStream oin;
//					InetAddress inetAddress;
//					try {
//						in = client.getInputStream();
//						inetAddress = client.getInetAddress();
//						oin = new ObjectInputStream( new BufferedInputStream(in));
//						Object obj = null;
//						if((obj = oin.readObject()) != null) {
//							MyMessage message = (MyMessage)obj;
//							ubindHeader(message, inetAddress);
//						}
//						oin.close();
//						in.close();
//						client.close();
//					}catch (Exception e) {
//						// TODO: handle exception
//						e.printStackTrace();
//					}
//				};
//			}.start();
//		}
//	}
//	
//	private void ubindHeader(MyMessage message,InetAddress senderInetAddress) {		
//		if (message.getHeader().equals("login")) {
//			User user = null;
//			for(User u:users) {
//				if(u.getUserName().equals(message.getSender())) {
//					user = u;
//					user.userLogin(message.getLoginName(),message.getLoginPassword(),senderInetAddress);
//					break;
//				}
//			}
//			if(user == null) {
//				user = new User(message.getSender());
//				user.userLogin(message.getLoginName(),message.getLoginPassword(),senderInetAddress);
//				users.add(user);
//			}
//			return;
//		}
//		for(int i=0;i<message.getReceivers().length;i++) {
//			switch(message.getHeader()) {
//			case "reply_link": getUser(message.getReceivers()[i]).setIsOnline(User.ONLINE);
//			                   System.out.println("与"+senderInetAddress+"握手成功！"); 
//			                   break;
//			default:
//
//				getUser(message.getReceivers()[i]).addMessage(message);
//			}
//		}
//	}
//	
//	private void unbindHeader(String message,InetAddress senderInetAddress) {     //接收名;接收名\n发送名\n标头\n消息内容
//		String[] unbind = message.split("\n");
//		String[] userNames = unbind[0].split(";");
//		String sender = unbind[1];
//		String header = unbind[2];
//		if(header.equals("login")) {
//			User user = null;
//			for(User u:users) {
//				if(u.getUserName().equals(sender)) {
//					user = u;
////					user.userLogin(senderInetAddress);
//					break;
//				}
//			}
//			if (user == null) {
//				user = new User(sender);
////				user.userLogin(senderInetAddress);
//				users.add(user);
//			}
//			return;
//		}
//		String content = "";
//		if(unbind.length > 4) {
//			for(int i=3; i<unbind.length; i++) {
//				content += unbind[i]+"\n";
//			}
//		}else {
//			content = unbind[3];
//		}
//
//		
//		for(int i=0; i<userNames.length; i++) {
//
//			switch(header) {
//			       case "string_message":getUser(userNames[i]).addMessage(content, sender);
////			                             System.out.println(getUser(userNames[i]).getIsOnline()+"   user名:"+getUser(userNames[i]).toString());
//			                             break;
//			       case "reply_link":    getUser(userNames[i]).setIsOnline(User.ONLINE);
////			                             System.out.println(getUser(userNames[i]).getIsOnline()+"   user名:"+getUser(userNames[i]).toString());
//			                             System.out.println("与"+senderInetAddress+"握手成功！");
//			                             break;                       
//			}
//		}
//		
//	}
//	public User getUser(String userName) {
//		for(User u:users) {
//			if(u.getUserName().equals(userName)) {
//				return u;
//			}
//		}
//		User user = new User(userName);
//		users.add(user);
//		return user;
//	}

}
