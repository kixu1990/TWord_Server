package poi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.collections4.bidimap.*;
import org.apache.poi.POIDocument;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hslf.record.AnimationInfo;
import org.apache.poi.hslf.record.AnimationInfoAtom;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * excel读取类
 * @author kixu 2020-3-14
 *
 */
public class ReadExcel {
	private static FileInputStream in;
	private static POIFSFileSystem POIS;
	
	
	public static ArrayList<Object[]> readErpExcel(String part) throws IOException {
		ArrayList<Object[]> readDatas = new ArrayList<Object[]>();
		
		try {
			 in = new FileInputStream(part);
			 POIS = new POIFSFileSystem(in);
			
			HSSFWorkbook workbook = new HSSFWorkbook(POIS);
			HSSFSheet sheet = workbook.getSheetAt(0);
						
			SummaryInformation sif = workbook.getSummaryInformation();
			
			System.out.println(sif.getApplicationName());
			System.out.println(sif.getCreateDateTime());
			System.out.println(sif.getLastSaveDateTime());
			
			if(sheet == null) {
				return null;
			}
			
			
			for(int rowNum=0; rowNum<sheet.getLastRowNum(); rowNum++) {
				HSSFRow hsRow = sheet.getRow(rowNum);
				Object[] data = new Object[8];
				
				if(hsRow != null && hsRow.getRowNum() != 0) {
					for(int cellNum=0; cellNum<hsRow.getLastCellNum(); cellNum++) {
						HSSFCell hsRowCell = hsRow.getCell(cellNum);
						if(hsRowCell != null) {
							//ArrayList<Object[]{发织单号，批号，条码，发织数，工序，颜色，尺码，收货时间}>
//							System.out.print(""+hsRowCell.getColumnIndex()+"—"+hsRowCell.getRowIndex()+ "\t");
							switch (cellNum) {
							case 1 : data[0] = hsRowCell.getStringCellValue();
							         break;
							case 4 : data[1] = hsRowCell.getStringCellValue();
							         break;
							case 20 : data[2] = hsRowCell.getStringCellValue();
							         break;
							case 18 : data[3] = hsRowCell.getNumericCellValue();
							         break;
							case 6 : data[4] = (hsRowCell.getStringCellValue()).substring(3);
							         break;
							case 19 : data[5] = hsRowCell.getStringCellValue();
							         break;
							case 15 : data[6] = hsRowCell.getStringCellValue();
							         break;
							}
						}
					}
					data[7] = new Date(System.currentTimeMillis());
//				    System.out.println();
					readDatas.add(data);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			POIS.close();
			in.close();
		}
		
		return readDatas;
	}
	
	private static String getValue(HSSFCell hsCell) {
		String cellValue = "";
		
		switch (hsCell.getCellType()) {
		case BOOLEAN : cellValue = String.valueOf(hsCell.getBooleanCellValue());			
			break;
		case NUMERIC : cellValue = String.valueOf(hsCell.getNumericCellValue());
		    break;
		case FORMULA : cellValue = String.valueOf(hsCell.getCellFormula());
		    break;
		default:       cellValue = hsCell.getStringCellValue();
			break;
		}
		
		return cellValue;
	}
}
