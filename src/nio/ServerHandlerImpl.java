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
import java.util.List;

import javax.print.attribute.standard.SheetCollate;


import dbeas.BaseDao;
import message.MyMessage;
import servers.User;

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
	
    private ByteBuffer cacheBuffer = ByteBuffer.allocate(1024 * 100);
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
            cacheBuffer.flip();
            byteBuffer.put(cacheBuffer);
        }
        int count = socketChannel.read(byteBuffer);
        if(count > 0) {
            byteBuffer.flip();
//            Log.d("ByteBufferԭʼ������", "--------------------------------------------" + String.valueOf(byteBuffer.remaining()));
            int position = 0;
            int i = 0;
            while (byteBuffer.remaining() > 0) {
//           Log.d("�� ",String.valueOf(i++)+" ��");
                if (bodyLen == -1) { //û�ж�����ͷ���ȶ���ͷ
//                    Log.d("�������ͷ��", "0000");
                    if (byteBuffer.remaining() >= head_length) { //���Զ�����ͷ�����򻺴�
                        byteBuffer.mark();
                        byteBuffer.get(headByte);
                        bodyLen = IntFlidByte.getHeadInt(headByte);
//                        Log.d("��ͷ���ȣ�", String.valueOf(bodyLen));
                    } else {
                        byteBuffer.reset();
                        cache = true;
                        cacheBuffer.clear();
                        cacheBuffer.put(byteBuffer);
                    }
                } else {
//                    Log.d("�������Ϣ�壡���ж���δ����", String.valueOf(byteBuffer.remaining()) + "���峤�ȣ� " + bodyLen);
                    if (byteBuffer.remaining() >= bodyLen) { //���ڵ���һ���������򻺴�
                        byte[] bodyByte = new byte[bodyLen];
                        byteBuffer.get(bodyByte, 0, bodyLen);
                        position += bodyLen;
                        byteBuffer.mark();
                        bodyLen = -1;
//                        Log.d("����һ����Ϣ", "Position = " + String.valueOf(position));
                        message = (MyMessage) ObjectFlidByte.byteArrayToObject(bodyByte);
//                        Log.d("��Ϣͷ��", message.getHeader());
//                        unbindHeadr(message, null);
                    	switch (message.getHeader()) {
                    	case "login" :
                    		if(!User.userLogin(message.getLoginName(), message.getLoginPassword(),message.getObjects(), socketChannel,byteBuffer)) {
                    			socketChannel.shutdownOutput();
                    			socketChannel.shutdownInput();
                    			socketChannel.close();
                    			return;
                    		}
                    		byteBuffer.flip();
                    		break;
                    	case "createMessage" :	
                    		createMessage(message);
                    		byteBuffer.flip();
                    		break;
                    	case "messageContent":
                    		messageContent(message);
                    		byteBuffer.flip();
//                    		System.out.println(byteBuffer.remaining());
                    		break;
                    	case "heartBeat" :
                    		heartBeat(message);
                    		byteBuffer.flip();
                    		break;
                    	}
                        cache = false;
                    } else {
//                        Log.d("���뻺�棺", "----------------------------------------��һ����ͷ��" + String.valueOf(bodyLen));
                        byteBuffer.mark();
                        byteBuffer.reset();
                        cacheBuffer.clear();
//                        Log.d("ByteBuffer���뻺��������", String.valueOf(byteBuffer.remaining()));
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

//	public void handleRead(SelectionKey selectionKey) throws IOException, SQLException {
//		
//        int head_length = 4;
//        byte[] headByte = new byte[4];
//        
//        MyMessage message = null;
//		
//		// TODO Auto-generated method stub
//		//�õ������¼���ͨ��
//		SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
//		ByteBuffer byteBuffer = (ByteBuffer)selectionKey.attachment();//�õ�������KEY�����buffer��
//		byteBuffer.clear();
//		
//        if(cache){
//            cacheBuffer.flip();
//            byteBuffer.put(cacheBuffer);
//        }
//		
//		if(socketChannel.read(byteBuffer) == -1) {//��ȡbuffer�������
//			//ͨ���ر�
//			UsersSocketChannel.getInstance().removeChannel(socketChannel);
//			
//			System.out.println("���ӶϿ�... ");
//		}else {
//			byteBuffer.flip();
////			MyMessage message = (MyMessage)ObjectFlidByte.byteArrayToObject(buffer.array());
//			
//            int position = 0;
//            int i = 0;
//            while (byteBuffer.remaining() > 0) {
////           Log.d("�� ",String.valueOf(i++)+" ��");
//                if (bodyLen == -1) { //û�ж�����ͷ���ȶ���ͷ
////                    Log.d("�������ͷ��", "0000");
//                    if (byteBuffer.remaining() >= head_length) { //���Զ�����ͷ�����򻺴�
//                        byteBuffer.mark();
//                        byteBuffer.get(headByte);
//                        bodyLen = IntFlidByte.getHeadInt(headByte);
////                        Log.d("��ͷ���ȣ�", String.valueOf(bodyLen));
//                    } else {
//                        byteBuffer.reset();
//                        cache = true;
//                        cacheBuffer.clear();
//                        cacheBuffer.put(byteBuffer);
//                    }
//                } else {
////                    Log.d("�������Ϣ�壡���ж���δ����", String.valueOf(byteBuffer.remaining()) + "���峤�ȣ� " + bodyLen);
//                    if (byteBuffer.remaining() >= bodyLen) { //���ڵ���һ���������򻺴�
//                        byte[] bodyByte = new byte[bodyLen];
//                        byteBuffer.get(bodyByte, 0, bodyLen);
//                        position += bodyLen;
//                        byteBuffer.mark();
//                        bodyLen = -1;
////                        Log.d("����һ����Ϣ", "Position = " + String.valueOf(position));
//                        message = (MyMessage) ObjectFlidByte.byteArrayToObject(bodyByte);
////                        Log.d("��Ϣͷ��", message.getHeader());
////                        unbindHeadr(message, null);
//            			switch (message.getHeader()) {
//            			case "login" :
//            				if(!User.userLogin(message.getLoginName(), message.getLoginPassword(), socketChannel,byteBuffer)) {
//            					socketChannel.shutdownOutput();
//            					socketChannel.shutdownInput();
//            					socketChannel.close();
//            					return;
//            				}else {
//            					return;
//            				}
// //           				break;
//            			case "createMessage" :	
//            				createMessage(message);
//            				break;
//            			case "messageContent":
//            				messageContent(message);
//            				break;
//            			case "heartBeat" :
//            				heartBeat(message);
//            				break;
//            			}
//                        cache = false;
//                    } else {
////                        Log.d("���뻺�棺", "----------------------------------------��һ����ͷ��" + String.valueOf(bodyLen));
//                        byteBuffer.mark();
//                        byteBuffer.reset();
//                        cacheBuffer.clear();
// //                       Log.d("ByteBuffer���뻺��������", String.valueOf(byteBuffer.remaining()));
//                        cacheBufferLen = byteBuffer.remaining();
//                        cacheBuffer.put(byteBuffer);
//                        cache = true;
//                        break;
//                    }
//                }
//            }
//            socketChannel.register(selectionKey.selector(), selectionKey.OP_READ, byteBuffer);			
//		}
//	}
//	
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
				
		MyMessage rsMessage = new MyMessage(0, rsUsers, "createMessage");
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
