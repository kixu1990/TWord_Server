package servers;

import java.util.ArrayList;

/**
 * 存放user，减少数据库访问次数
 * @author kixu on 2019/12/7
 *
 */		
public class Users {
	
	private ArrayList<User> users = new ArrayList<User>();
	
	public static Users INSTANCE = new Users();
	
	private Users() {}		
	
	public static Users getINSTANCE() {
		return INSTANCE;
	}
	
	public User getUser(int userId) {
		User user = null;
		for(User u:users) {
			if(userId == u.getUserId()) {
				user = u;
			}
		}
		return user;
	}
	
	public void addUser(User user) {
		for(User u:users) {
			if(!(u.getUserId() == user.getUserId())) {
				users.add(user);
			}
		}
	}

}
