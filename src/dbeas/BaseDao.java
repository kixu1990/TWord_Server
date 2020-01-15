package dbeas;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.dbutils.ResultSetHandler;

import com.mysql.cj.protocol.Resultset;

import message.MyMessage;
import servers.User;

public class BaseDao {

	public static Object query(String sql,ResultSetHandler<?> rsh,Object... params) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(sql);
			for(int i=0; params != null && i < params.length; i++) {
				pstmt.setObject(i+1, params[i]);
			}
			rs = pstmt.executeQuery();
			Object obj = rsh.handle(rs);
			return obj;
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		return rs;
	}
	
	/*
	 * 高并发时，派发messageID错乱的话，请在这个方法内改进！！
	 */
	public static long getAutoIncrementInsert(String sql,Object... params) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		long id = 0;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			for(int i=0;params != null && i < params.length; i++) {
				pstmt.setObject(i+1, params[i]);
			}
			pstmt.executeUpdate();
			ResultSet rs = pstmt.getGeneratedKeys();
			id = rs.next()?rs.getLong(1):-1;
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		return id;
	}
	
	/*
	 * 后期优化成储存过程，减少数据库访问
	 */
	public static ArrayList<MyMessage> getOffLineMessages (User user) throws SQLException{
		ArrayList<MyMessage> messages = new ArrayList<MyMessage>();
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String selectSql = "select msgId,lastTimeLine from tb_offline_content where (receiver = ?) and (heardlin = ?)";
		String selectMsgSql = "select headline,time from tb_messages where megId = ?";
		String selectMemberSql = "select userId from tb_meg_members where megId = ?";
		String selectContent = "select sender,sendTime,stringContent from tb_contents where (megId = ?) and (sendTime >= ?)";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setInt(1, user.getUserId());
			pstmt.setString(2, "createMessage");
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				long messageId = rs.getLong("msgId");
				pstmt = conn.prepareStatement(selectMsgSql);
				pstmt.setLong(1, messageId);
				ResultSet headlineRs = pstmt.executeQuery();
				String headline = "";
				java.sql.Date date = null;
				if(headlineRs.next()) {
					headline = headlineRs.getString("headline");
					date = headlineRs.getDate("time");
				}
				pstmt = conn.prepareStatement(selectMemberSql);
				pstmt.setLong(1, messageId);
				ResultSet memberRs = pstmt.executeQuery();
				memberRs.last();
				int size = memberRs.getRow();
				memberRs.beforeFirst();
				int[] receivers = new int[size];
//				System.out.println(size);
				int i = 0;
				while(memberRs.next()) {
					receivers[i] = memberRs.getInt("userId");
					i++;
				}
				MyMessage message = new MyMessage(0, receivers, "createMessage");
				message.setStringContent(headline);
				message.setMessageId(messageId);
				message.setDate(date);
				messages.add(message);
			}
			
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setInt(1, user.getUserId());
			pstmt.setString(2, "messageContent");
			ResultSet rsContent = pstmt.executeQuery();
			while(rsContent.next()) {
				long msgId = rsContent.getLong("msgId");
				java.sql.Date lastTime = rsContent.getDate("lastTimeLine");
				pstmt = conn.prepareStatement(selectContent);
				pstmt.setLong(1, msgId);
				pstmt.setDate(2, lastTime);
				ResultSet rsc = pstmt.executeQuery();
				while(rsc.next()) {
					MyMessage message = new MyMessage(rsc.getInt("sender"), new int[] {user.getUserId()}, "messageContent");
					message.setStringContent(rsc.getString("stringContent"));
					message.setMessageId(msgId);
					message.setDate(rsc.getDate("sendTime"));
//					System.out.println(message.getStringContent());
					messages.add(message);
				}
				
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return messages;
	}
	

	public static void deleteOfflineMessage(MyMessage message,User user) throws SQLException {
		int receiver = user.getUserId();
		String heardlin = message.getHeader();
		long msgId = message.getMessageId();
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String deleteOfflineSql = "delete from tb_offline_content where receiver = ? and heardlin = ? and msgId = ?";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(deleteOfflineSql);
			pstmt.setInt(1, receiver);
			pstmt.setString(2, heardlin);
			pstmt.setLong(3, msgId);
			int  i = pstmt.executeUpdate();
//			System.out.println("删除消息："+receiver+"  "+msgId+"  "+heardlin+"  "+i);
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
	}
	
	public static Object[] getResSatff(HashMap<Integer, Integer> usersVersion,HashMap<String, Integer> departmentsVersion) throws SQLException {
		ArrayList<Object[]> departments = new ArrayList<Object[]>();
		ArrayList<Object[]> users = new ArrayList<Object[]>();
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String departmentsSql = "select * from tb_departments";
		String usersSql = "select * from tb_users";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(departmentsSql);
			ResultSet departmentSet = pstmt.executeQuery();
			while(departmentSet.next()) {
				String departmentName = departmentSet.getString("departmentName");        //部门名   0
				String departmentManager = departmentSet.getString("departmentManager");  //部门主管 1 
				int departmentVersion = departmentSet.getInt("version");                  //版本     2
//				System.out.println(departmentName+" : "+departmentManager+" : "+departmentVersion);
				if(departmentsVersion.containsKey(departmentName)) {
					if(departmentsVersion.get(departmentName) != departmentVersion) {
						Object[] department = {departmentName,departmentManager,departmentVersion};
						departments.add(department);
					}
				}else {
					Object[] department = {departmentName,departmentManager,departmentVersion};
					departments.add(department);
				}
			}
			
			pstmt = conn.prepareStatement(usersSql);
			ResultSet usersSet = pstmt.executeQuery();
			while(usersSet.next()) {
				int userId = usersSet.getInt("userId");              //用户ID   0
				String userName = usersSet.getString("userName");    //用户名   1
				String department = usersSet.getString("department");//所属部门        2
				String userImage = usersSet.getString("userImage");  //头像后缀 3
				int userVersion = usersSet.getInt("version");        //版本     4
				String post = usersSet.getString("post");            //职位     5
				String email = usersSet.getString("email");            //电邮   6
				String phoneNumber = usersSet.getString("phoneNumber");//电话          7
				String state = usersSet.getString("state");            //状态   8 
				if(usersVersion.containsKey(userId)) {
					if(usersVersion.get(userId) != userVersion) {
						Object[] user = {userId,userName,department,getUserImage(userImage),userVersion,post,email,phoneNumber,state};
						users.add(user);
					}
				}else {
					Object[] user = {userId,userName,department,getUserImage(userImage),userVersion};
					users.add(user);
				}
			}
						
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		System.out.println(departments.size() + " : "+users.size());
		
		return new Object[] {departments,users};
	}
	
	public static Object[] getSatfflist(User user) throws SQLException {
		
		String[] departments = null;
		int[][] usersId = null;
		HashMap<Integer, String> usersName = null;
		HashMap<Integer, byte[]> usersImage = null;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String departmentsSql = "select departmentName from tb_departments";
		String usersSql = "select * from tb_users";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(departmentsSql);
			ResultSet departmentSet = pstmt.executeQuery();
			
			pstmt = conn.prepareStatement(usersSql);
			ResultSet userSet = pstmt.executeQuery();
			
			departmentSet.last();
			int row=departmentSet.getRow();
			departmentSet.beforeFirst();
			
			userSet.last();
			int row2=userSet.getRow();
			userSet.beforeFirst();
			
			departments = new String[row];
			usersId = new int[row][];
			usersName = new HashMap<Integer, String>();
			usersImage =  new HashMap<Integer, byte[]>();
			
			int i = 0;
			while(departmentSet.next()) {
				String departmentName = departmentSet.getString("departmentName");
				departments[i] = departmentName;
				int j = 0;
				int size = 0;
				userSet.beforeFirst();
				while(userSet.next()) {
					if(userSet.getString("department").equals(departmentName) & user.getUserId() != userSet.getInt("userId")) {
						size ++;
					}
				}
//				System.out.println(departmentName+": "+size);
				int[] usersN = new int[size];
				userSet.beforeFirst();
				while(userSet.next()) {
					if(userSet.getString("department").equals(departmentName)) {
						if(user.getUserId() != userSet.getInt("userId")) {
							usersN[j] = userSet.getInt("userId");
						    usersName.put(userSet.getInt("userId"),userSet.getString("userName"));
						    usersImage.put(userSet.getInt("userId"), getUserImage(userSet.getString("userImage")));
						    j++;
						}
//						System.out.println(usersN.length+": "+usersN[j]+": "+j+": "+userSet.getString("userName"));
					}
				}
				usersId[i] = usersN;
				i++;
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return new Object[] {departments,usersId,usersName,usersImage};
		
	}
	
	private static byte[] getUserImage(String src){
		BufferedInputStream bis = null;
		ByteArrayOutputStream bop = null;
		String path = "d:/TWimg/"+src;
//		System.out.println(path);
		byte[] b = new byte[1024];
		try {
			bis = new BufferedInputStream(new FileInputStream(new File(path)));
			bop = new ByteArrayOutputStream();
			int len;
			while((len = bis.read(b)) != -1) {
				bop.write(b,0,len);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return bop.toByteArray();
	}
	
	public static void insterMessageContent(MyMessage message) throws SQLException {
		int sender = message.getSender();
		String stringContent = message.getStringContent();
		long megId = message.getMessageId();
		
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String insterSql = "insert into tb_contents(sender,stringContent,megId) values(?,?,?)";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(insterSql);
			pstmt.setInt(1, sender);
			pstmt.setString(2, stringContent);
			pstmt.setLong(3, megId);
			pstmt.executeUpdate();
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
	}
	
	public static void insterOffLineContent(int receiver,long megId,String heardlin) throws SQLException {
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String selectSql = "select msgId from tb_offline_content where (receiver = ?) and (msgId = ?) and (heardlin = ?)";			
		String insterSql = "insert into tb_offline_content(receiver,msgId,heardlin) values (?,?,?)";
		
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setInt(1, receiver);
			pstmt.setLong(2, megId);
			pstmt.setString(3, heardlin);
			ResultSet rs = pstmt.executeQuery();
			if(!rs.next()) {
				pstmt = conn.prepareStatement(insterSql);
				pstmt.setInt(1, receiver);
				pstmt.setLong(2, megId);
				pstmt.setString(3, heardlin);
				pstmt.executeUpdate();
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
	}
	
	public static int[] getMessageMembers(long megId) throws SQLException {
		int[] members = null;
		
		String selectSql = "select userId from tb_meg_members where megId = ?";
		
		Connection conn = null;
		PreparedStatement  pstmt = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setLong(1, megId);
			ResultSet rs = pstmt.executeQuery();
			rs.last();
			members = new int[rs.getRow()];
//			System.out.println(rs.getRow());
			rs.beforeFirst();
			int i = 0;
			while(rs.next()) {
				members[i] = rs.getInt("userId");
				i++;
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		return members;
	}
	
	/*
	 * 添加消息成员组
	 */
	public static void insterMessageMembers(long megId,int[] receivers) throws SQLException {
		String sql = "insert into tb_meg_members(userId,megId) values (?,?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(sql);
			for(int i=0; i<receivers.length; i++) {
				pstmt.setInt(1, receivers[i]);
				pstmt.setLong(2, megId);
				pstmt.executeUpdate();
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
	}

}
