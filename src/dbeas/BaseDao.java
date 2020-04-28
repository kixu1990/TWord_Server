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
	
	/**
	 *  得到客户端需要的通迅录列表
	 *  @param usersVersion 客户端提交的通迅录名单对应的版本号 
	 *  @param departmentsVersion 客户端提交的部门名单对应的版本号
	 *  @return object[1]=更新后的部门名单，object[2]=更新后的人员名单
	 */
	public static Object[] getResSatff(HashMap<Integer, Integer> usersVersion,HashMap<String, Integer> departmentsVersion) throws SQLException {
		ArrayList<Object[]> departments = new ArrayList<Object[]>(); //部门
		ArrayList<Object[]> users = new ArrayList<Object[]>();       //人员
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		String departmentsSql = "select * from tb_departments";  //提取所有的部门信息
		String usersSql = "select * from tb_users";              //提取所有的人员信息
		
		try {
			conn = C3p0Utils.getConnection();              //从数据库连接池中拿到connection
			pstmt = conn.prepareStatement(departmentsSql); 
			ResultSet departmentSet = pstmt.executeQuery();//得到部门信息
			
			//循环对比数据库部门信息与客户端部门信息
			while(departmentSet.next()) {
				String departmentName = departmentSet.getString("departmentName");        //部门名   0
				String departmentManager = departmentSet.getString("departmentManager");  //部门主管 1 
				int departmentVersion = departmentSet.getInt("version");                  //版本     2
//				System.out.println(departmentName+" : "+departmentManager+" : "+departmentVersion);
				if(departmentsVersion.containsKey(departmentName)) {                               //如果部门信息相匹配则
					if(departmentsVersion.get(departmentName) != departmentVersion) {              //如果部门信息版本号有更新
						Object[] department = {departmentName,departmentManager,departmentVersion};//更新部门信息
						departments.add(department);
					}
				}else {                                                                            //如果数据库信息在客户端不存在，则为新部门
					Object[] department = {departmentName,departmentManager,departmentVersion};    //添加部门信息
					departments.add(department);
				}
			}
			
			pstmt = conn.prepareStatement(usersSql);
			ResultSet usersSet = pstmt.executeQuery(); //得到人员信息
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
				if(usersVersion.containsKey(userId)) {             //如果数据库人员信息与客户端相匹配
					if(usersVersion.get(userId) != userVersion) {  //如果人员信息版本号有更新
						Object[] user = {userId,userName,department,getUserImage(userImage),userVersion,post,email,phoneNumber,state};
						users.add(user);//更新人员信息
					}
				}else {                                            //如果数据库人员信息在客户端不存在，则为新人员
					Object[] user = {userId,userName,department,getUserImage(userImage),userVersion,post,email,phoneNumber,state}; //添加新人员信息
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
	 * 更新通迅录
	 * @param datas 数据结构： Object[] {departments,usersId,usersName,usersPasswrod,usersImage}
	 */
	public static void resSatff(Object[] datas) {
		
		String department;      // = datas[0] == null? "":(String) datas[0];  //部门
		int userId = datas[1] == null? 0:(int)datas[1];               //ID
		String userName;        //= datas[2] == null? "":(String)datas[2];     //用户名
		String password;    //= datas[3] == null? "":(String)datas[3]; //用户密码
		String userImage;        //= datas[4]== null? null : (byte[])datas[3];  //用户头像
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
	 * 提取所有的通迅录数据
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
	 * 将imagePath转成byte[]
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
	
	/**
	 * 插入ERP的实时数据
	 * @param src ArrayList<Object[]{发织单号，批号，条码，发织数，工序，颜色，尺码，收货时间}>
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
				pstmt.setString(1, (String)data[0]); //发织单号
				pstmt.setString(2, (String)data[1]); //批号
				pstmt.setInt(3, ((Double)data[2]).intValue());       //条码
				pstmt.setInt(4, ((Double)data[3]).intValue());       //发织数
				pstmt.setString(5, (String)data[4]); //工序
				pstmt.setString(6, (String)data[5]); //颜色
				pstmt.setString(7, (String)data[6]); //尺码
				java.sql.Date time = new java.sql.Date(System.currentTimeMillis());
				pstmt.setDate(8, time);              //收货时间 现在  
				pstmt.addBatch();                    //添加批量处理
			}
			pstmt.executeBatch();                    //批量插入
		}catch (SQLException e) {
			// TODO: handle exception
			pstmt.close();
			conn.close();
		}
	}
	
	/**
	 * 提取某个批号的ERP的实时数据
	 */
	public static HashMap<String, ArrayList<Object[]>> getErpReportData(String searchLot) throws SQLException{
        //----------------------------------------------------------------------------------------------
        //   数据构造： 按照如下结构逐层打包和解包
        //   map<工序，批号>
        //              |
        //             list<object[]{0-批号,1-发织数，2-颜色}>
        //                                        |
        //                                         map<颜色，尺码>
        //                                                   |
        //                                                 map<尺码,数量>
        //                                                           |
        //                                                          int[]{0-今日完成数，1-总完成数}
		// 发织单号-lot 批号-lotNumber 条码-barcode 发织数-lotCount 工序-process 颜色-colour 尺码-size 收货时间-time 
        //-----------------------------------------------------------------------------------------------
		
		Calendar calendar = Calendar.getInstance();                   
		calendar.add(Calendar.DATE, -1);                                                //减去一天 
		java.sql.Date dateLine = new java.sql.Date(calendar.getTimeInMillis());         //得到时间
		
		String sqlProcess = "select distinct process from tb_erp_datas where lotNumber like ?"; //得到工序
		String sqlLotNumber = "select distinct lotNumber,lotCount,lot from tb_erp_datas where lotNumber like ?";//得到批号和发织数
		String sqlColour = "select distinct colour from tb_erp_datas where (lot = ?)";//得到某个工序和某些批号的颜色
		String sqlSize = "select distinct size from tb_erp_datas where (lot = ? and colour = ?)";//得到某个发织单号某个颜色的尺码
		String sqlTodayCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and time > ? and process = ?)";
		String sqlCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and process = ?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		HashMap<String,ArrayList<Object[]>> processMap = null;     //打包好的数据源
		
		try {
			conn = C3p0Utils.getConnection();               //得到数据库连接
			
			pstmt = conn.prepareStatement(sqlLotNumber);    //得到批号
			pstmt.setString(1, "%"+searchLot+"%");
			ResultSet lotNumberRs = pstmt.executeQuery();   //批号SET集合
			
			pstmt = conn.prepareStatement(sqlProcess);      //得到工序SQL
			pstmt.setString(1, "%"+searchLot+"%");
			ResultSet processRs = pstmt.executeQuery();     //工序SET集合
			//遍历工序
			processMap = new HashMap<String, ArrayList<Object[]>>();
			while(processRs.next()) {
				String process = processRs.getString("process");             //工序
//				System.out.println(process);
				//遍历批号
				ArrayList<Object[]> aLLoters = new ArrayList<Object[]>();
				lotNumberRs.beforeFirst();                                   //重置批号SET 这个很重要
				while(lotNumberRs.next()) {
					String lotNumber = lotNumberRs.getString("lotNumber");   //批号
					String lot = lotNumberRs.getString("lot");               //发织单号
					int lotCount = lotNumberRs.getInt("lotCount");           //发织数
//					System.out.println(process + "  "+lotNumber+"  "+lot+"  "+lotCount);
					
					pstmt = conn.prepareStatement(sqlColour);                //得到颜色
//					pstmt.setString(1, process);
					pstmt.setString(1, lot);
					ResultSet colourRs = pstmt.executeQuery();               //颜色SET集合
					//遍历颜色
					HashMap<String, Map<String, int[]>> colourMap = new HashMap<String, Map<String,int[]>>();
					while(colourRs.next()) {
						String colour = colourRs.getString("colour");        //颜色
						
						pstmt = conn.prepareStatement(sqlSize);              //得到尺码
						pstmt.setString(1,lot);
						pstmt.setString(2,colour);
						ResultSet sizeRs = pstmt.executeQuery();             //尺码SET集合
						//遍历尺码
						HashMap<String, int[]> sizeMap = new HashMap<String, int[]>();  //尺码数量打包
						while(sizeRs.next()) {
							int[] counts = new int[2];
							String size = sizeRs.getString("size");          //尺码
							
							pstmt = conn.prepareStatement(sqlTodayCount);    //今日完成
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setDate(4, dateLine);
							pstmt.setString(5, process);
							ResultSet todayCount = pstmt.executeQuery();     //今日完成SET集合
							int todayCounts = 0;                             //今日完成数
							while(todayCount.next()) {
								todayCounts = todayCount.getInt("counts");
							}
							
							pstmt = conn.prepareStatement(sqlCount);         //总完成
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setString(4, process);
							ResultSet count = pstmt.executeQuery();          //总完成数SET集合
							int allCounts = 0;
							while(count.next()) {
								allCounts = count.getInt("counts");
							}
							//数量打包int[]
							counts[0] = todayCounts;
							counts[1] = allCounts;
							sizeMap.put(size, counts);                      //尺码数量打包
//							System.out.println("工序："+process+"  批号："+lotNumber+"  颜色："+colour+"  尺码："+size+"  今日完成数："+counts[0]+"  总完成数："+counts[1]);
						}
						colourMap.put(colour, sizeMap);                     //颜色尺码打包
					}
					Object[] loters = new Object[3];
					loters[0] = lotNumber;                                  //批号发织数颜色打包
					loters[1] = lotCount;
					loters[2] =colourMap;
					aLLoters.add(loters);
				}
				processMap.put(process, aLLoters);                            //工序批号打包
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
	 * 提取ERP的实时数据
	 * @return
	 * @throws SQLException 
	 */
	public static HashMap<String, ArrayList<Object[]>> getErpReportData() throws SQLException {
        //----------------------------------------------------------------------------------------------
        //   数据构造： 按照如下结构逐层打包和解包
        //   map<工序，批号>
        //              |
        //             list<object[]{0-批号,1-发织数，2-颜色}>
        //                                        |
        //                                         map<颜色，尺码>
        //                                                   |
        //                                                 map<尺码,数量>
        //                                                           |
        //                                                          int[]{0-今日完成数，1-总完成数}
		// 发织单号-lot 批号-lotNumber 条码-barcode 发织数-lotCount 工序-process 颜色-colour 尺码-size 收货时间-time 
        //-----------------------------------------------------------------------------------------------
		
		Calendar calendar = Calendar.getInstance();                   
		calendar.add(Calendar.DATE, -1);                                                //减去一天 
		java.sql.Date dateLine = new java.sql.Date(calendar.getTimeInMillis());         //得到时间
		
		String sqlProcess = "select distinct process from tb_erp_datas where time > ?"; //得到24小时内有收货的工序
		String sqlLotNumber = "select distinct lotNumber,lotCount,lot from tb_erp_datas where time > ?";//得到24小时内有收货的批号和发织数
		String sqlColour = "select distinct colour from tb_erp_datas where (lot = ?)";//得到某个工序和某些批号的颜色
		String sqlSize = "select distinct size from tb_erp_datas where (lot = ? and colour = ?)";//得到某个发织单号某个颜色的尺码
		String sqlTodayCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and time > ? and process = ?)";
		String sqlCount = "select count(barcode) as counts from tb_erp_datas where (lot = ? and colour = ? and size = ? and process = ?)";
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		HashMap<String,ArrayList<Object[]>> processMap = null;     //打包好的数据源
		
		try {
			conn = C3p0Utils.getConnection();               //得到数据库连接
			
			pstmt = conn.prepareStatement(sqlLotNumber);    //得到批号
			pstmt.setDate(1, dateLine);
			ResultSet lotNumberRs = pstmt.executeQuery();   //批号SET集合
			
			pstmt = conn.prepareStatement(sqlProcess);      //得到工序SQL
			pstmt.setDate(1, dateLine);
			ResultSet processRs = pstmt.executeQuery();     //工序SET集合
			//遍历工序
			processMap = new HashMap<String, ArrayList<Object[]>>();
			while(processRs.next()) {
				String process = processRs.getString("process");             //工序
//				System.out.println(process);
				//遍历批号
				ArrayList<Object[]> aLLoters = new ArrayList<Object[]>();
				lotNumberRs.beforeFirst();                                   //重置批号SET 这个很重要
				while(lotNumberRs.next()) {
					String lotNumber = lotNumberRs.getString("lotNumber");   //批号
					String lot = lotNumberRs.getString("lot");               //发织单号
					int lotCount = lotNumberRs.getInt("lotCount");           //发织数
//					System.out.println(process + "  "+lotNumber+"  "+lot+"  "+lotCount);
					
					pstmt = conn.prepareStatement(sqlColour);                //得到颜色
//					pstmt.setString(1, process);
					pstmt.setString(1, lot);
					ResultSet colourRs = pstmt.executeQuery();               //颜色SET集合
					//遍历颜色
					HashMap<String, Map<String, int[]>> colourMap = new HashMap<String, Map<String,int[]>>();
					while(colourRs.next()) {
						String colour = colourRs.getString("colour");        //颜色
//						System.out.println(process + "  "+lotNumber+"  "+colour);
						
						pstmt = conn.prepareStatement(sqlSize);              //得到尺码
						pstmt.setString(1,lot);
						pstmt.setString(2,colour);
						ResultSet sizeRs = pstmt.executeQuery();             //尺码SET集合
						//遍历尺码
						HashMap<String, int[]> sizeMap = new HashMap<String, int[]>();  //尺码数量打包
						while(sizeRs.next()) {
							int[] counts = new int[2];
							String size = sizeRs.getString("size");          //尺码
							
							pstmt = conn.prepareStatement(sqlTodayCount);    //今日完成
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setDate(4, dateLine);
							pstmt.setString(5, process);
							ResultSet todayCount = pstmt.executeQuery();     //今日完成SET集合
							int todayCounts = 0;                             //今日完成数
							while(todayCount.next()) {
								todayCounts = todayCount.getInt("counts");
							}
							
							pstmt = conn.prepareStatement(sqlCount);         //总完成
							pstmt.setString(1, lot);
							pstmt.setString(2, colour);
							pstmt.setString(3, size);
							pstmt.setString(4, process);
							ResultSet count = pstmt.executeQuery();          //总完成数SET集合
							int allCounts = 0;
							while(count.next()) {
								allCounts = count.getInt("counts");
							}
							//数量打包int[]
							counts[0] = todayCounts;
							counts[1] = allCounts;
							sizeMap.put(size, counts);                      //尺码数量打包
//							System.out.println("工序："+process+"  批号："+lotNumber+"  颜色："+colour+"  尺码："+size+"  今日完成数："+counts[0]+"  总完成数："+counts[1]);
						}
						colourMap.put(colour, sizeMap);                     //颜色尺码打包
					}
					Object[] loters = new Object[3];
					loters[0] = lotNumber;                                  //批号发织数颜色打包
					loters[1] = lotCount;
					loters[2] =colourMap;
					aLLoters.add(loters);
				}
				processMap.put(process, aLLoters);                            //工序批号打包
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
