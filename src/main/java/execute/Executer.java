package execute;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import datasrc.TWSEDataLoder;
import datasrc.YahooStockDataLoder;

/**
 * Victory Function
 * 
 * (v2.1) 2021/02/04
 * 
 * @version 2.2
 * @author CL
 */
public class Executer {

	public static String DATE_FORMAT_yMd = "yyyyMMdd";
	public static String outputFilename ;
	
	public static void main(String[] args) {
		System.out.println("============= Victory Function 2.2 =============");
		
		Calendar cal = Calendar.getInstance();
		outputFilename = "vf-"+new SimpleDateFormat(DATE_FORMAT_yMd).format(cal.getTime())+".txt";
		
		try {
			new TWSEDataLoder().startup();
			new YahooStockDataLoder().startup();
			
			//for test 
//			new YahooStockDataLoder().isAbnormalVol("2330");
//			Executer.getYuanTaStockData("2330");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	

	
	

}
