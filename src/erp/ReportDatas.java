package erp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import dbeas.BaseDao;

/**
 * 存放当天收货数量，减少数据库访问次数
 * @author 2020-3-23 by kixu
 *
 */
public class ReportDatas {
	
	private HashMap<String, ArrayList<Object[]>> datas = null;
	
	private static ReportDatas INSTANCE = new ReportDatas() ;
	
	private ReportDatas() {
	}
	
	public static ReportDatas getINSTANCE() {
		return INSTANCE;
	}
	
	public HashMap<String, ArrayList<Object[]>> getDatas() throws SQLException {
		if(datas == null) {
			datas =  BaseDao.getErpReportData();
		}
		return datas;
	}

}
