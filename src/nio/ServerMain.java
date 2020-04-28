package nio;

import java.io.IOException;
import java.sql.SQLException;

import dbeas.BaseDao;
import poi.ReadExcel;

public class ServerMain {
	public static void main(String[] args) {
		NioSocketServer server = new NioSocketServer();
//		insterErpData();
		server.start();
	}
	
	/**
	 * ����erp����
	 */
	private static void insterErpData() {
		try {
			BaseDao.insterErpReportData(ReadExcel.readErpExcel());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
