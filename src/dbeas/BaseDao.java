package dbeas;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * �߲���ʱ���ɷ�messageID���ҵĻ���������������ڸĽ�����
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
	 * �����Ż��ɴ�����̣��������ݿ����
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
//			System.out.println("ɾ����Ϣ��"+receiver+"  "+msgId+"  "+heardlin+"  "+i);
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
	}
	
	/**
	 *  �õ��ͻ�����Ҫ��ͨѸ¼�б�
	 *  @param usersVersion �ͻ����ύ��ͨѸ¼������Ӧ�İ汾�� 
	 *  @param departmentsVersion �ͻ����ύ�Ĳ���������Ӧ�İ汾��
	 *  @return object[1]=���º�Ĳ���������object[2]=���º����Ա����
	 */
	public static Object[] getResSatff(HashMap<Integer, Integer> usersVersion,HashMap<String, Integer> departmentsVersion) throws SQLException {
		ArrayList<Object[]> departments = new ArrayList<Object[]>(); //����
		ArrayList<Object[]> users = new ArrayList<Object[]>();       //��Ա
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String departmentsSql = "select * from tb_departments";  //��ȡ���еĲ�����Ϣ
		String usersSql = "select * from tb_users";              //��ȡ���е���Ա��Ϣ
		
		try {
			conn = C3p0Utils.getConnection();              //�����ݿ����ӳ����õ�connection
			pstmt = conn.prepareStatement(departmentsSql); 
			ResultSet departmentSet = pstmt.executeQuery();//�õ�������Ϣ
			
			//ѭ���Ա����ݿⲿ����Ϣ��ͻ��˲�����Ϣ
			while(departmentSet.next()) {
				String departmentName = departmentSet.getString("departmentName");        //������   0
				String departmentManager = departmentSet.getString("departmentManager");  //�������� 1 
				int departmentVersion = departmentSet.getInt("version");                  //�汾     2
//				System.out.println(departmentName+" : "+departmentManager+" : "+departmentVersion);
				if(departmentsVersion.containsKey(departmentName)) {                               //���������Ϣ��ƥ����
					if(departmentsVersion.get(departmentName) != departmentVersion) {              //���������Ϣ�汾���и���
						Object[] department = {departmentName,departmentManager,departmentVersion};//���²�����Ϣ
						departments.add(department);
					}
				}else {                                                                            //������ݿ���Ϣ�ڿͻ��˲����ڣ���Ϊ�²���
					Object[] department = {departmentName,departmentManager,departmentVersion};    //��Ӳ�����Ϣ
					departments.add(department);
				}
			}
			
			pstmt = conn.prepareStatement(usersSql);
			ResultSet usersSet = pstmt.executeQuery(); //�õ���Ա��Ϣ
			while(usersSet.next()) {
				int userId = usersSet.getInt("userId");              //�û�ID   0
				String userName = usersSet.getString("userName");    //�û���   1
				String department = usersSet.getString("department");//��������        2
				String userImage = usersSet.getString("userImage");  //ͷ���׺ 3
				int userVersion = usersSet.getInt("version");        //�汾     4
				String post = usersSet.getString("post");            //ְλ     5
				String email = usersSet.getString("email");            //����   6
				String phoneNumber = usersSet.getString("phoneNumber");//�绰          7
				String state = usersSet.getString("state");            //״̬   8 
				if(usersVersion.containsKey(userId)) {             //������ݿ���Ա��Ϣ��ͻ�����ƥ��
					if(usersVersion.get(userId) != userVersion) {  //�����Ա��Ϣ�汾���и���
						Object[] user = {userId,userName,department,getUserImage(userImage),userVersion,post,email,phoneNumber,state};
						users.add(user);//������Ա��Ϣ
					}
				}else {                                            //������ݿ���Ա��Ϣ�ڿͻ��˲����ڣ���Ϊ����Ա
					Object[] user = {userId,userName,department,getUserImage(userImage),userVersion,post,email,phoneNumber,state}; //�������Ա��Ϣ
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
	
	/**
	 * ����ͨѸ¼
	 * @param datas ���ݽṹ�� Object[] {departments,usersId,usersName,usersPasswrod,usersImage}
	 */
	public static void resSatff(Object[] datas) {
		
		String department;      // = datas[0] == null? "":(String) datas[0];  //����
		int userId = datas[1] == null? 0:(int)datas[1];               //ID
		String userName;        //= datas[2] == null? "":(String)datas[2];     //�û���
		String password;    //= datas[3] == null? "":(String)datas[3]; //�û�����
		String userImage;        //= datas[4]== null? null : (byte[])datas[3];  //�û�ͷ��
		int version = 0;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String getSatffSql = "select * from tb_users where userId = ?";
		String setSatffSql = "update tb_users set userName = ?,password = ?,userImage = ? , version = ? where userId = ?";
		
		if(userId != 0) {
			try {
				conn = C3p0Utils.getConnection();
				pstmt = conn.prepareStatement(getSatffSql);
				pstmt.setInt(1, userId);
				ResultSet satffSet = pstmt.executeQuery();
				
				while(satffSet.next()) {
					version = satffSet.getInt("version") + 1;
					department = datas[0] == null? satffSet.getString("department"):(String)datas[0];
					userId = (int)datas[1];
					userName = datas[2] == null? satffSet.getString("userName") : (String)datas[2];
					password = datas[3] == null? satffSet.getString("password") : (String)datas[3];
					userImage = datas[4] == null? satffSet.getString("userImage") : setUserImagePath((byte[])datas[4], userId+"_"+version+".jpg");
					
					pstmt = conn.prepareStatement(setSatffSql);
					pstmt.setString(1, userName); 
					pstmt.setString(2, password);
					pstmt.setString(3, userImage);
					pstmt.setInt(4, version);
					pstmt.setInt(5, userId);
					pstmt.executeUpdate();
				}
				
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	
	/**
	 * ��ȡ���е�ͨѸ¼����
	 * @param user
	 * @return Object[] {departments,usersId,usersName,usersImage}
	 * @throws SQLException
	 */
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
	
	private static String setUserImagePath(byte[] src,String name) {
		String path = "d:/TWimg/"+name;
		File file = new File(path);
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
			bos.write(src);
		}catch (Exception e) {
			// TODO: handle exception
		}finally {
			try {
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return name;
	}
	
	/**
	 * ��imagePathת��byte[]
	 * @param src
	 * @return
	 */
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
	
	public static String getMessageHeadline(long messageId) throws SQLException {
		String headline = "";
		
		String selectSql = "select headline from tb_messages where megId = ?";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setLong(1, messageId);
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				headline = rs.getString("headline");
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return headline;
	}
	
	public static int getMessageSponsor(long messageId) throws SQLException{
		int sponsor = 0;
		
		String selectSql = "select headline from tb_messages where megId = ?";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(selectSql);
			pstmt.setLong(1, messageId);
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				sponsor = rs.getInt("sponsor");
			}
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return sponsor;
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
	 * �����Ϣ��Ա��
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
	
	/**
	 * ����ERP��ʵʱ����
	 * @param src ArrayList<Object[]{��֯���ţ����ţ����룬��֯����������ɫ�����룬�ջ�ʱ��}>
	 */
	public static void insterErpReportData(ArrayList<Object[]> src) throws SQLException {
		String sql = "insert into tb_erp_datas(lot,lotNumber,barcode,lotCount,process,colour,size,time) values (?,?,?,?,?,?,?,?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = C3p0Utils.getConnection();
			pstmt = conn.prepareStatement(sql);
//			System.out.println(src.size());
			for(Object[] data : src) {
//				System.out.println(data[0]+""+data[1]+data[2]+data[3]+data[4]+data[5]+data[6]+data[7]);
				pstmt.setString(1, (String)data[0]); //��֯����
				pstmt.setString(2, (String)data[1]); //����
				pstmt.setInt(3, ((Double)data[2]).intValue());       //����
				pstmt.setInt(4, ((Double)data[3]).intValue());       //��֯��
				pstmt.setString(5, (String)data[4]); //����
				pstmt.setString(6, (String)data[5]); //��ɫ
				pstmt.setString(7, (String)data[6]); //����
				java.sql.Date time = new java.sql.Date(System.currentTimeMillis());
				pstmt.setDate(8, time);              //�ջ�ʱ�� ����  
				pstmt.addBatch();                    //�����������
			}
			pstmt.executeBatch();                    //��������
		}catch (SQLException e) {
			// TODO: handle exception
			pstmt.close();
			conn.close();
		}
	}
	
	/**
	 * ��ȡĳ�����ŵ�ERP��ʵʱ����
	 */
	public static HashMap<String, ArrayList<Object[]>> getErpReportData(String searchLot) throws SQLException{
        //----------------------------------------------------------------------------------------------
        //   ���ݹ��죺 �������½ṹ������ͽ��
        //   map<��������>
        //              |
        //             list<object[]{0-����,1-��֯����2-��ɫ}>
        //                                        |
        //                                         map<��ɫ������>
        //                                                   |
        //                                                 map<����,����>
        //                                                           |
        //                                                          int[]{0-�����������1-�������}
		// ��֯����-lot ����-lotNumber ����-barcode ��֯��-lotCount ����-process ��ɫ-colour ����-size �ջ�ʱ��-time 
        //-----------------------------------------------------------------------------------------------
		
		Calendar calendar = Calendar.getInstance();                   
		calendar.add(Calendar.DATE, -1);                                                //��ȥһ�� 
		java.sql.Date dateLine = new java.sql.Date(calendar.getTimeInMillis());         //�õ�ʱ��
		
		String sqlProcess = "select distinct process from tb_erp_datas where lotNumber like ?"; //�õ�����
		String sqlLotNumber = "select distinct lotNumber,lotCount,lot from tb_erp_datas where lotNumber like ?";//�õ����źͷ�֯��
		String sqlColour = "select distinct colour from tb_erp_datas where (lot = ?)";//�õ�ĳ�������ĳЩ���ŵ���ɫ
		String sqlSize = "select distinct size from tb_erp_datas where (lot = ? and colour = ?)";//�õ�ĳ����֯����ĳ����ɫ�ĳ���
		String sqlTodayCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and time > ? and process = ?)";
		String sqlCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and process = ?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		HashMap<String,ArrayList<Object[]>> processMap = null;     //����õ�����Դ
		
		try {
			conn = C3p0Utils.getConnection();               //�õ����ݿ�����
			
			pstmt = conn.prepareStatement(sqlLotNumber);    //�õ�����
			pstmt.setString(1, "%"+searchLot+"%");
			ResultSet lotNumberRs = pstmt.executeQuery();   //����SET����
			
			pstmt = conn.prepareStatement(sqlProcess);      //�õ�����SQL
			pstmt.setString(1, "%"+searchLot+"%");
			ResultSet processRs = pstmt.executeQuery();     //����SET����
			//��������
			processMap = new HashMap<String, ArrayList<Object[]>>();
			while(processRs.next()) {
				String process = processRs.getString("process");             //����
//				System.out.println(process);
				//��������
				ArrayList<Object[]> aLLoters = new ArrayList<Object[]>();
				lotNumberRs.beforeFirst();                                   //��������SET �������Ҫ
				while(lotNumberRs.next()) {
					String lotNumber = lotNumberRs.getString("lotNumber");   //����
					String lot = lotNumberRs.getString("lot");               //��֯����
					int lotCount = lotNumberRs.getInt("lotCount");           //��֯��
//					System.out.println(process + "  "+lotNumber+"  "+lot+"  "+lotCount);
					
					pstmt = conn.prepareStatement(sqlColour);                //�õ���ɫ
//					pstmt.setString(1, process);
					pstmt.setString(1, lot);
					ResultSet colourRs = pstmt.executeQuery();               //��ɫSET����
					//������ɫ
					HashMap<String, Map<String, int[]>> colourMap = new HashMap<String, Map<String,int[]>>();
					while(colourRs.next()) {
						String colour = colourRs.getString("colour");        //��ɫ
						
						pstmt = conn.prepareStatement(sqlSize);              //�õ�����
						pstmt.setString(1,lot);
						pstmt.setString(2,colour);
						ResultSet sizeRs = pstmt.executeQuery();             //����SET����
						//��������
						HashMap<String, int[]> sizeMap = new HashMap<String, int[]>();  //�����������
						while(sizeRs.next()) {
							int[] counts = new int[2];
							String size = sizeRs.getString("size");          //����
							
							pstmt = conn.prepareStatement(sqlTodayCount);    //�������
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setDate(4, dateLine);
							pstmt.setString(5, process);
							ResultSet todayCount = pstmt.executeQuery();     //�������SET����
							int todayCounts = 0;                             //���������
							while(todayCount.next()) {
								todayCounts = todayCount.getInt("counts");
							}
							
							pstmt = conn.prepareStatement(sqlCount);         //�����
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setString(4, process);
							ResultSet count = pstmt.executeQuery();          //�������SET����
							int allCounts = 0;
							while(count.next()) {
								allCounts = count.getInt("counts");
							}
							//�������int[]
							counts[0] = todayCounts;
							counts[1] = allCounts;
							sizeMap.put(size, counts);                      //�����������
//							System.out.println("����"+process+"  ���ţ�"+lotNumber+"  ��ɫ��"+colour+"  ���룺"+size+"  �����������"+counts[0]+"  ���������"+counts[1]);
						}
						colourMap.put(colour, sizeMap);                     //��ɫ������
					}
					Object[] loters = new Object[3];
					loters[0] = lotNumber;                                  //���ŷ�֯����ɫ���
					loters[1] = lotCount;
					loters[2] =colourMap;
					aLLoters.add(loters);
				}
				processMap.put(process, aLLoters);                            //�������Ŵ��
			}
			
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return processMap;
	}
	
	/**
	 * ��ȡERP��ʵʱ����
	 * @return
	 * @throws SQLException 
	 */
	public static HashMap<String, ArrayList<Object[]>> getErpReportData() throws SQLException {
        //----------------------------------------------------------------------------------------------
        //   ���ݹ��죺 �������½ṹ������ͽ��
        //   map<��������>
        //              |
        //             list<object[]{0-����,1-��֯����2-��ɫ}>
        //                                        |
        //                                         map<��ɫ������>
        //                                                   |
        //                                                 map<����,����>
        //                                                           |
        //                                                          int[]{0-�����������1-�������}
		// ��֯����-lot ����-lotNumber ����-barcode ��֯��-lotCount ����-process ��ɫ-colour ����-size �ջ�ʱ��-time 
        //-----------------------------------------------------------------------------------------------
		
		Calendar calendar = Calendar.getInstance();                   
		calendar.add(Calendar.DATE, -1);                                                //��ȥһ�� 
		java.sql.Date dateLine = new java.sql.Date(calendar.getTimeInMillis());         //�õ�ʱ��
		
		String sqlProcess = "select distinct process from tb_erp_datas where time > ?"; //�õ�24Сʱ�����ջ��Ĺ���
		String sqlLotNumber = "select distinct lotNumber,lotCount,lot from tb_erp_datas where time > ?";//�õ�24Сʱ�����ջ������źͷ�֯��
		String sqlColour = "select distinct colour from tb_erp_datas where (lot = ?)";//�õ�ĳ�������ĳЩ���ŵ���ɫ
		String sqlSize = "select distinct size from tb_erp_datas where (lot = ? and colour = ?)";//�õ�ĳ����֯����ĳ����ɫ�ĳ���
		String sqlTodayCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and time > ? and process = ?)";
		String sqlCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and process = ?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		HashMap<String,ArrayList<Object[]>> processMap = null;     //����õ�����Դ
		
		try {
			conn = C3p0Utils.getConnection();               //�õ����ݿ�����
			
			pstmt = conn.prepareStatement(sqlLotNumber);    //�õ�����
			pstmt.setDate(1, dateLine);
			ResultSet lotNumberRs = pstmt.executeQuery();   //����SET����
			
			pstmt = conn.prepareStatement(sqlProcess);      //�õ�����SQL
			pstmt.setDate(1, dateLine);
			ResultSet processRs = pstmt.executeQuery();     //����SET����
			//��������
			processMap = new HashMap<String, ArrayList<Object[]>>();
			while(processRs.next()) {
				String process = processRs.getString("process");             //����
//				System.out.println(process);
				//��������
				ArrayList<Object[]> aLLoters = new ArrayList<Object[]>();
				lotNumberRs.beforeFirst();                                   //��������SET �������Ҫ
				while(lotNumberRs.next()) {
					String lotNumber = lotNumberRs.getString("lotNumber");   //����
					String lot = lotNumberRs.getString("lot");               //��֯����
					int lotCount = lotNumberRs.getInt("lotCount");           //��֯��
//					System.out.println(process + "  "+lotNumber+"  "+lot+"  "+lotCount);
					
					pstmt = conn.prepareStatement(sqlColour);                //�õ���ɫ
//					pstmt.setString(1, process);
					pstmt.setString(1, lot);
					ResultSet colourRs = pstmt.executeQuery();               //��ɫSET����
					//������ɫ
					HashMap<String, Map<String, int[]>> colourMap = new HashMap<String, Map<String,int[]>>();
					while(colourRs.next()) {
						String colour = colourRs.getString("colour");        //��ɫ
//						System.out.println(process + "  "+lotNumber+"  "+colour);
						
						pstmt = conn.prepareStatement(sqlSize);              //�õ�����
						pstmt.setString(1,lot);
						pstmt.setString(2,colour);
						ResultSet sizeRs = pstmt.executeQuery();             //����SET����
						//��������
						HashMap<String, int[]> sizeMap = new HashMap<String, int[]>();  //�����������
						while(sizeRs.next()) {
							int[] counts = new int[2];
							String size = sizeRs.getString("size");          //����
							
							pstmt = conn.prepareStatement(sqlTodayCount);    //�������
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setDate(4, dateLine);
							pstmt.setString(5, process);
							ResultSet todayCount = pstmt.executeQuery();     //�������SET����
							int todayCounts = 0;                             //���������
							while(todayCount.next()) {
								todayCounts = todayCount.getInt("counts");
							}
							
							pstmt = conn.prepareStatement(sqlCount);         //�����
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setString(4, process);
							ResultSet count = pstmt.executeQuery();          //�������SET����
							int allCounts = 0;
							while(count.next()) {
								allCounts = count.getInt("counts");
							}
							//�������int[]
							counts[0] = todayCounts;
							counts[1] = allCounts;
							sizeMap.put(size, counts);                      //�����������
//							System.out.println("����"+process+"  ���ţ�"+lotNumber+"  ��ɫ��"+colour+"  ���룺"+size+"  �����������"+counts[0]+"  ���������"+counts[1]);
						}
						colourMap.put(colour, sizeMap);                     //��ɫ������
					}
					Object[] loters = new Object[3];
					loters[0] = lotNumber;                                  //���ŷ�֯����ɫ���
					loters[1] = lotCount;
					loters[2] =colourMap;
					aLLoters.add(loters);
				}
				processMap.put(process, aLLoters);                            //�������Ŵ��
			}
			
		}catch (SQLException e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally {
			pstmt.close();
			conn.close();
		}
		
		return processMap;
	}

}
