package nio;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.print.attribute.standard.SheetCollate;


import dbeas.BaseDao;
import erp.ReportDatas;
import message.MyMessage;
import servers.User;
import servers.Users;

/**
 * 建立一个用于SelectionKey处理的顶层接口
 * 
 * @author kixu 2019/11/28
 *
 */
interface ServerHandlerBs{
	void handleAccept(SelectionKey selectionKey) throws IOException;
	void handleRead(SelectionKey selectionKey) throws IOException, SQLException;
}

/**
 * 处理SelectionKey内容的支持器
 * 
 * @author kixu 2019/11/28
 *
 */

public class ServerHandlerImpl implements ServerHandlerBs {
	
    private ByteBuffer cacheBuffer = ByteBuffer.allocate(1024 * 10000);
    private boolean cache = false;
    int bodyLen = -1;
    int cacheBufferLen = -1;
	
	private int bufferSize = 4096;  //buffer的默认大小
	private String localCharset = "UTF-8";  //默认编码
	private UsersSocketChannel usersSocketChannel = UsersSocketChannel.getInstance();//存放每个用户的临时通道
	
	public ServerHandlerImpl() {
		// TODO Auto-generated constructor stub
	}
	
	public ServerHandlerImpl(int bufferSize,String localCharset) {
		this.bufferSize = bufferSize > 0? bufferSize:this.bufferSize;
		this.localCharset = localCharset == null? this.localCharset:localCharset;
	}
	
	/*
	 * 接收到selectionKey的accept事件
	 */

	@Override
	public void handleAccept(SelectionKey selectionKey) throws IOException {
		// TODO Auto-generated method stub
		//通过ServerSocketChannel得到触发accept事件的通道，也就是得到申请接入服务器的客户端通道
		SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
		//设置为非阻塞
		socketChannel.configureBlocking(false);
		//在原选择器里注册这条通道，并注册读事件
		socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
		
		System.out.println("建立请求...");
	}
	
	/*
	 * 接收到selectionKey的read事件
	 */

	@Override
    public void handleRead(SelectionKey selectionKey) throws IOException,SQLException {
        int head_length = 4;
        byte[] headByte = new byte[4];

        MyMessage message = null;
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
        byteBuffer.clear();

        if(cache){
            cacheBuffer.flip();                      //转化成读模式
            byteBuffer.put(cacheBuffer);             //写入上次缓存
        }
        try {
        int count = socketChannel.read(byteBuffer);  //写入这次读到的
        if(count > 0) {
            byteBuffer.flip();                       //转化成读模式
            int position = 0;
            int i = 0;
            while (byteBuffer.remaining() > 0) {     //如果有可读内容
                if (bodyLen == -1) {                 //如果没有读出包头，先读包头
                    if (byteBuffer.remaining() >= head_length) { //可以读出包头，否则缓存 （总包头长度小于4的情况，多包粘连有可以发生）
                        byteBuffer.mark();           //去除包头
                        byteBuffer.get(headByte);    //读出包头
                        bodyLen = IntFlidByte.getHeadInt(headByte);
                    } else {
                        byteBuffer.reset();
                        cache = true;
                        cacheBuffer.clear();
                        cacheBuffer.put(byteBuffer);
                    }
                } else {                                                      //有包头，开始读包体
                    
                    if (byteBuffer.remaining() >= bodyLen) {                  //剩余数量大于等于一个包，否则缓存
                    	                                                      //可以读成一个完整包
                        byte[] bodyByte = new byte[bodyLen]; 
                        byteBuffer.get(bodyByte, 0, bodyLen);                 //将所有数据写入bodyByte
                        position += bodyLen;
                        byteBuffer.mark();                                    //去除已读数据（粘包的时候有用） 
                        bodyLen = -1;
                        message = (MyMessage) ObjectFlidByte.byteArrayToObject(bodyByte);
                        
                        //cacheBuffer 读模式
                        //byteBuffer  读模式
                        
                    	switch (message.getHeader()) {
                    	//登录
                    	case "login" :
                    		if(!User.userLogin(message.getLoginName(), message.getLoginPassword(),message.getObjects(), socketChannel,byteBuffer)) {
                    			socketChannel.shutdownOutput();
                    			socketChannel.shutdownInput();
                    			socketChannel.close();
                    			return;
                    		}
                    		break;
                    	//创建消息	
                    	case "createMessage" :	
                    		createMessage(message);
                    		break;
                    	//消息内容	
                    	case "messageContent":
                    		messageContent(message);
                    		break;
                    	//心跳 暂时停用	
                    	case "heartBeat" :
                    		heartBeat(message);
                    		break;
                    	//重建消息	
                    	case "resCreateMessage"	:
                    		resCreateMessage(message);
                    		break;
                    	//ERP报表	
                    	case "getErpReport"	 :
                    		erpReport(message);
                    		break;
                    	//更新用户资料	
                    	case "resSatff" :
                    		resSatff(message);
                    		break;
                    	}
                        cache = false;
                    } else {
                        cacheBuffer.clear();
                        cacheBufferLen = byteBuffer.remaining();
                        cacheBuffer.put(byteBuffer);
                        cache = true;
                        break;
                    }
                }
            }
            socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, byteBuffer);
        } else if(count == -1){
            socketChannel.close();
        }
        }catch (IOException e){
        	String s;
        	int userId = UsersSocketChannel.getInstance().getUserId(socketChannel);
        	if(userId == 0) {
        		s = "未知";
        	}else {
        		s = User.getUser(userId).getUserName();
        	}
        	
        	System.out.println(s+" 强迫断开连接！");
        	socketChannel.close();
        	
        }
    }
	
	/**
	 *  用户自身更新自已的头像等资料
	 * @param message
	 */
	private void resSatff(MyMessage message) {
//		System.out.println("进入resSatff()");
		Object[] datas = message.getObjects();
		BaseDao.resSatff(datas);
	}
	
	/**
	 * ERP报表数据获取和发送方法
	 * @param message
	 * @throws SQLException
	 */
	private void erpReport(MyMessage message) throws SQLException {		
		HashMap<String,ArrayList<Object[]>> datas;
		
		if(message.getStringContent()== null) {
//		    datas = ReportDatas.getINSTANCE().getDatas();
			datas = BaseDao.getErpReportData();
		}else {
			datas = BaseDao.getErpReportData(message.getStringContent());
		}
		
//		Set pressKeySet = datas.keySet();
//		Iterator it = pressKeySet.iterator();
//		while(it.hasNext()) {
//			String press = (String)it.next();
////			System.out.println(press);
//			ArrayList<Object[]> lotNumbers = (ArrayList<Object[]>)datas.get(press);
////			System.out.println(lotNumbers.size());
//			
//			for(Object[] lot :lotNumbers) {
//				String lotNumber = (String)lot[0];
//				int allCount = (int)lot[1];
//				HashMap<String, HashMap<String, int[]>> colours = (HashMap<String, HashMap<String, int[]>>)lot[2];
//				
//				Set colourKeySet = colours.keySet();
//				Iterator coloursIt = colourKeySet.iterator();
//				while(coloursIt.hasNext()) {
//					String colour = (String)coloursIt.next();
//					HashMap<String, int[]> sizes = (HashMap<String, int[]>)colours.get(colour);
//					
//					Set sizeKeySet = sizes.keySet();
//					Iterator sizeIt = sizeKeySet.iterator();
//					while(sizeIt.hasNext()) {
//						String size = (String)sizeIt.next();
////						System.out.println("工序："+press+"  批号："+lotNumber+"  颜色："+colour+"  尺码："+size);
//						int[] counts = sizes.get(size);
//						
//						int todayCount = (int)counts[0];
//						int allCounts = (int)counts[1];
//						
////						System.out.println("工序："+press+"  批号："+lotNumber+"  颜色："+colour+"  尺码："+size+"  今日完成数："+todayCount+"  总完成："+allCounts);
//						
//					}
//				}
//			}
//		}
		
		MyMessage rsMessage = new MyMessage(0, new int[]{message.getSender()}, "getErpReport");
		rsMessage.setObjects(new Object[] {datas});
		if(datas.size() > 0) {
		    rsMessage.setStringContent("true");    //表示报表有内容
		}else {
			rsMessage.setStringContent("false");   //表示是空报表
		}
		
		User user = User.getUser(message.getSender());
		NioSocketServer.sendMessage(rsMessage, user);
	}
	
	/**
	 *  重新创建主消息方法
	 * @param message
	 * @throws SQLException
	 */
	private void resCreateMessage(MyMessage message) throws SQLException {
		int[] rsUsers = BaseDao.getMessageMembers(message.getMessageId());
		String headline = BaseDao.getMessageHeadline(message.getMessageId());
		int sponsor = BaseDao.getMessageSponsor(message.getMessageId());
		
		MyMessage rsMessage = new MyMessage(sponsor, rsUsers, "createMessage");
		rsMessage.setMessageId(message.getMessageId());
		rsMessage.setDate(new Date(System.currentTimeMillis()));
		rsMessage.setStringContent(headline);
		
		User user = User.getUser(message.getSender());
		NioSocketServer.sendMessage(rsMessage, user);
	}

private void unbindHeadr(MyMessage message ,SocketChannel socketChannel,ByteBuffer byteBuffer) throws IOException,SQLException {
		// TODO Auto-generated method stub
	switch (message.getHeader()) {
	case "login" :
		if(!User.userLogin(message.getLoginName(), message.getLoginPassword(),message.getObjects(), socketChannel,byteBuffer)) {
			socketChannel.shutdownOutput();
			socketChannel.shutdownInput();
			socketChannel.close();
			return;
		}else {
			return;
		}
//		break;
	case "createMessage" :	
		createMessage(message);
		break;
	case "messageContent":
		messageContent(message);
		break;
	case "heartBeat" :
		heartBeat(message);
		break;
	}
		
}

	private void heartBeat(MyMessage message) {
		Calendar calendar = Calendar.getInstance();
		System.out.println(message.getSender() + "发来心跳包 ： "+calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND));
	}
	
	private void messageContent(MyMessage message) throws SQLException {
		long messageId = message.getMessageId();
		message.setDate(new Date(System.currentTimeMillis()));
		
		BaseDao.insterMessageContent(message);
		int[] users = BaseDao.getMessageMembers(messageId);
		
		for(int i:users) {
			User user = User.getUser(i);
			if(user != null) {
				if(!NioSocketServer.sendMessage(message, user)) {
					BaseDao.insterOffLineContent(user.getUserId(), messageId, "messageContent");
				}			
			}
		}
	}
	
	private void createMessage(MyMessage message) throws SQLException {
		int sponsor = message.getSender();
		String headline = message.getStringContent();
		String megTag = message.getMessageLable();
		String sql = "insert into tb_messages(sponsor,headline,megTag,time) values (?,?,?,?)";
		long messageId = BaseDao.getAutoIncrementInsert(sql, new Object[] {sponsor,headline,megTag,new Date(System.currentTimeMillis())});		
		
		int[] users = message.getReceivers();
		int[] rsUsers = new int[users.length +1];
		for(int i=0; i<users.length; i++) {
			rsUsers[i] = users[i];
		}
		rsUsers[users.length] = sponsor;
		
		BaseDao.insterMessageMembers(messageId, rsUsers);
				
		MyMessage rsMessage = new MyMessage(message.getSender(), rsUsers, "createMessage");
		rsMessage.setMessageId(messageId);
		rsMessage.setDate(new Date(System.currentTimeMillis()));
		rsMessage.setStringContent(headline);
		
		for(int i:rsUsers) {
			User user = User.getUser(i);
			if(user != null) {
				if(!NioSocketServer.sendMessage(rsMessage, user)) {
					BaseDao.insterOffLineContent(user.getUserId(), messageId, "createMessage");
				}
			}
		}
		
//		User u = User.getUser(sponsor);
//		NioSocketServer.sendMessage(rsMessage, u);
	}

}
