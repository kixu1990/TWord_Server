package nio;

import java.io.IOException;
import java.sql.SQLException;

import dbeas.BaseDao;
import poi.ReadExcel;
import ui.MainUI;

public class ServerMain {
	public static void main(String[] args) {
//		new MainUI();
		NioSocketServer server = new NioSocketServer();
// 		insterErpData();
		server.start();
	}
	
	/**
	 * 导入erp数据
	 */
//	private static void insterErpData() {
//		try {
////			BaseDao.insterErpReportData(ReadExcel.readErpExcel());
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

}
