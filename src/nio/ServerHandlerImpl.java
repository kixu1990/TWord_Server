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
 * ����һ������SelectionKey����Ķ���ӿ�
 * 
 * @author kixu 2019/11/28
 *
 */
interface ServerHandlerBs{
	void handleAccept(SelectionKey selectionKey) throws IOException;
	void handleRead(SelectionKey selectionKey) throws IOException, SQLException;
}

/**
 * ����SelectionKey���ݵ�֧����
 * 
 * @author kixu 2019/11/28
 *
 */

public class ServerHandlerImpl implements ServerHandlerBs {
	
    private ByteBuffer cacheBuffer = ByteBuffer.allocate(1024 * 10000);
    private boolean cache = false;
    int bodyLen = -1;
    int cacheBufferLen = -1;
	
	private int bufferSize = 4096;  //buffer��Ĭ�ϴ�С
	private String localCharset = "UTF-8";  //Ĭ�ϱ���
	private UsersSocketChannel usersSocketChannel = UsersSocketChannel.getInstance();//���ÿ���û�����ʱͨ��
	
	public ServerHandlerImpl() {
		// TODO Auto-generated constructor stub
	}
	
	public ServerHandlerImpl(int bufferSize,String localCharset) {
		this.bufferSize = bufferSize > 0? bufferSize:this.bufferSize;
		this.localCharset = localCharset == null? this.localCharset:localCharset;
	}
	
	/*
	 * ���յ�selectionKey��accept�¼�
	 */

	@Override
	public void handleAccept(SelectionKey selectionKey) throws IOException {
		// TODO Auto-generated method stub
		//ͨ��ServerSocketChannel�õ�����accept�¼���ͨ����Ҳ���ǵõ��������������Ŀͻ���ͨ��
		SocketChannel socketChannel = ((ServerSocketChannel)selectionKey.channel()).accept();
		//����Ϊ������
		socketChannel.configureBlocking(false);
		//��ԭѡ������ע������ͨ������ע����¼�
		socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
		
		System.out.println("��������...");
	}
	
	/*
	 * ���յ�selectionKey��read�¼�
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
            cacheBuffer.flip();                      //ת���ɶ�ģʽ
            byteBuffer.put(cacheBuffer);             //д���ϴλ���
        }
        try {
        int count = socketChannel.read(byteBuffer);  //д����ζ�����
        if(count > 0) {
            byteBuffer.flip();                       //ת���ɶ�ģʽ
            int position = 0;
            int i = 0;
            while (byteBuffer.remaining() > 0) {     //����пɶ�����
                if (bodyLen == -1) {                 //���û�ж�����ͷ���ȶ���ͷ
                    if (byteBuffer.remaining() >= head_length) { //���Զ�����ͷ�����򻺴� ���ܰ�ͷ����С��4����������ճ���п��Է�����
                        byteBuffer.mark();           //ȥ����ͷ
                        byteBuffer.get(headByte);    //������ͷ
                        bodyLen = IntFlidByte.getHeadInt(headByte);
                    } else {
                        byteBuffer.reset();
                        cache = true;
                        cacheBuffer.clear();
                        cacheBuffer.put(byteBuffer);
                    }
                } else {                                                      //�а�ͷ����ʼ������
                    
                    if (byteBuffer.remaining() >= bodyLen) {                  //ʣ���������ڵ���һ���������򻺴�
                    	                                                      //���Զ���һ��������
                        byte[] bodyByte = new byte[bodyLen]; 
                        byteBuffer.get(bodyByte, 0, bodyLen);                 //����������д��bodyByte
                        position += bodyLen;
                        byteBuffer.mark();                                    //ȥ���Ѷ����ݣ�ճ����ʱ�����ã� 
                        bodyLen = -1;
                        message = (MyMessage) ObjectFlidByte.byteArrayToObject(bodyByte);
                        
                        //cacheBuffer ��ģʽ
                        //byteBuffer  ��ģʽ
                        
                    	switch (message.getHeader()) {
                    	//��¼
                    	case "login" :
                    		if(!User.userLogin(message.getLoginName(), message.getLoginPassword(),message.getObjects(), socketChannel,byteBuffer)) {
                    			socketChannel.shutdownOutput();
                    			socketChannel.shutdownInput();
                    			socketChannel.close();
                    			return;
                    		}
                    		break;
                    	//������Ϣ	
                    	case "createMessage" :	
                    		createMessage(message);
                    		break;
                    	//��Ϣ����	
                    	case "messageContent":
                    		messageContent(message);
                    		break;
                    	//���� ��ʱͣ��	
                    	case "heartBeat" :
                    		heartBeat(message);
                    		break;
                    	//�ؽ���Ϣ	
                    	case "resCreateMessage"	:
                    		resCreateMessage(message);
                    		break;
                    	//ERP����	
                    	case "getErpReport"	 :
                    		erpReport(message);
                    		break;
                    	//�����û�����	
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
        		s = "δ֪";
        	}else {
        		s = User.getUser(userId).getUserName();
        	}
        	
        	System.out.println(s+" ǿ�ȶϿ����ӣ�");
        	socketChannel.close();
        	
        }
    }
	
	/**
	 *  �û�����������ѵ�ͷ�������
	 * @param message
	 */
	private void resSatff(MyMessage message) {
//		System.out.println("����resSatff()");
		Object[] datas = message.getObjects();
		BaseDao.resSatff(datas);
	}
	
	/**
	 * ERP�������ݻ�ȡ�ͷ��ͷ���
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
////						System.out.println("����"+press+"  ���ţ�"+lotNumber+"  ��ɫ��"+colour+"  ���룺"+size);
//						int[] counts = sizes.get(size);
//						
//						int todayCount = (int)counts[0];
//						int allCounts = (int)counts[1];
//						
////						System.out.println("����"+press+"  ���ţ�"+lotNumber+"  ��ɫ��"+colour+"  ���룺"+size+"  �����������"+todayCount+"  ����ɣ�"+allCounts);
//						
//					}
//				}
//			}
//		}
		
		MyMessage rsMessage = new MyMessage(0, new int[]{message.getSender()}, "getErpReport");
		rsMessage.setObjects(new Object[] {datas});
		if(datas.size() > 0) {
		    rsMessage.setStringContent("true");    //��ʾ����������
		}else {
			rsMessage.setStringContent("false");   //��ʾ�ǿձ���
		}
		
		User user = User.getUser(message.getSender());
		NioSocketServer.sendMessage(rsMessage, user);
	}
	
	/**
	 *  ���´�������Ϣ����
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
		System.out.println(message.getSender() + "���������� �� "+calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND));
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
