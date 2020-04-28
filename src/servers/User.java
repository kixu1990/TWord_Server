package servers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.dbutils.handlers.BeanListHandler;

import dbeas.BaseDao;
import message.MyMessage;
import nio.NioSocketServer;
import nio.UsersSocketChannel;

public class User {
	
	public static final int ONLINE = 1;
	public static final int ONT_ONLINE = 2;
	
	private static final int PORT = 11002;
	
	private int isOnline = ONT_ONLINE;
	private InetAddress inetAddress = null;
	
	private List<String> messageQueue = new ArrayList();
	private List<MyMessage> messageQueue2 = new ArrayList<MyMessage>();
	
	private int userId;
	private String userName;
	private String password;
	private String lotMember;
	private String department;
	private String userImage;
	
	public String getUserImage() {
		return userImage;
	}
	public void setUserImage(String userImage) {
		this.userImage = userImage;
	}
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getLotMember() {
		return lotMember;
	}
	public void setLotMember(String lotMember) {
		this.lotMember = lotMember;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	
	public User(String userName) {
		// TODO Auto-generated constructor stub
		this.userName = userName;
	}
	
	public static User getUser(String lotMember) {
		User user = new User();
		BaseDao baseDao = new BaseDao();
		String sql = "select * from tb_users where lotMember = ?";
		try {
			ArrayList<User> list = (ArrayList<User>)baseDao.query(sql, new BeanListHandler(User.class), lotMember);
			for(int i=0; i<list.size(); i++) {
				user = list.get(i);
				return user;
			}
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	public static User getUser(int userId) {
		User user = null;
		
		if((user = Users.getINSTANCE().getUser(userId)) == null) {
		
		BaseDao baseDao = new BaseDao();
		String sql = "select * from tb_users where userId = ?";
		try {
			ArrayList<User> list = (ArrayList<User>)baseDao.query(sql, new BeanListHandler(User.class), userId);
			for(int i=0; i<list.size(); i++) {
				user = list.get(i);
				Users.getINSTANCE().addUser(user);
				return user;
			}
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		}else {
			return user;
		}
		return null;
	}
	
	public User() {
		
	}

	
	public InetAddress getInetAddress() {
		return inetAddress;
	}


	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}


	public int getIsOnline() {
		return isOnline;
	}
	
	public void setIsOnline(int isOnline) {
		if(this.isOnline != isOnline) {
			this.isOnline = isOnline;
		}
	}
	
	/**
	 * 客户端登录方法
	 * @param lotMember 用户Id
	 * @param passwrod 用户密码
	 * @param object 部门版本信息和人员版本信息    
	 * @param socketChannel NIO通道
	 * @param buffer 
	 * @return true=登录成功，false=登录失败
	 */
	public static Boolean userLogin(String lotMember,String passwrod,Object[] object,SocketChannel socketChannel,Buffer buffer) {
		Boolean b = false;
		BaseDao baseDao = new BaseDao();
		String sql = "select * from tb_users where lotMember = ?";
		try {
			ArrayList<User> list = (ArrayList<User>)baseDao.query(sql, new BeanListHandler(User.class), lotMember);
			for(int i=0; i<list.size(); i++) {
				if(list.get(i).getPassword().equals(passwrod)) {
					UsersSocketChannel.getInstance().setSocketChannel(list.get(i).getUserId(), socketChannel,buffer);
					MyMessage message = new MyMessage(0, new int[] {list.get(i).getUserId()}, "login");
					message.setUserName(list.get(i).getUserName());
					message.setStringContent("true");
					message.setObjects(BaseDao.getResSatff((HashMap<Integer, Integer>)object[0], (HashMap<String, Integer>)object[1]));
					list.get(i).sendMessage(message);
					b = true;
					Users.getINSTANCE().addUser(list.get(i));
					ArrayList<MyMessage> messages = BaseDao.getOffLineMessages(list.get(i));
					if(!messages.isEmpty()) {
					for(MyMessage rsMessage : messages) {
						if(NioSocketServer.sendMessage(rsMessage, list.get(i))) {
							BaseDao.deleteOfflineMessage(rsMessage, list.get(i));
						}
						
					}
//						MyMessage rsMessage = new MyMessage(0, new int[] {list.get(i).getUserId()}, "rsMessages");
//						MyMessage[] rsMessages = new MyMessage[messages.size()];
//						for(int j=0;j<rsMessages.length;j++) {
//							rsMessages[j] = messages.get(j);
//						}
//						rsMessage.setObjects(rsMessages);
//						list.get(i).sendMessage(rsMessage);
					}
					
					System.out.println(list.get(i).getUserName()+" 登录成功！");
				}
			}
			
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		if(!b) {
			MyMessage msg = new MyMessage(0, new int[] {0}, "login");
			msg.setStringContent("false");
			try {
				NioSocketServer.sendMessage(msg, socketChannel, (ByteBuffer)buffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return b;
	}

	public Boolean sendMessage(MyMessage message) {

			return NioSocketServer.sendMessage(message, this);
		
	}
	
	
	public List<String> getMessageQueue(){
		return messageQueue;
	}
	
}
