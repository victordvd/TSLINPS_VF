package datasrc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import execute.Executer;
import util.ConstantStore;
import util.HardCodeProp;
import util.UrlLoader;
import vo.AbnormalStockVO;
import vo.VolumeVO;

public class YahooStockDataLoder {

	public static final int MAX_V2_PRICE = 300;

	private ArrayList<String> exOccuredStocks = new ArrayList<String>();
	private ArrayList<String> noDataStocks = new ArrayList<String>();
	private ArrayList<AbnormalStockVO> bingoStocks = new ArrayList<AbnormalStockVO>();

	public void startup() {

		ArrayList<String> allISINs = new ArrayList<String>();

//		allISINs.add("1217");//test

		allISINs.addAll(ConstantStore.listedISINs);
		allISINs.addAll(ConstantStore.tpexISINs);
		allISINs.addAll(ConstantStore.etfISINs);

		System.out.println("total items: " + allISINs.size());

		try {

			int sLen = 0;

			for (int i = 0; i < allISINs.size(); i++) {

				String no = allISINs.get(i);
				String name = ConstantStore.stockNoNameMap.get(no);

				StringBuilder backSb = new StringBuilder();

				for (int j = 0; j <= sLen; j++) {
					backSb.append("\b");
				}

				String s = String.format(backSb.toString() + "\r %.1f%%. checking %s %s.....               ",
						(i + 1F) / allISINs.size() * 100, no, name);
				sLen = s.length();

				System.out.print(s);

				try {

					AbnormalStockVO re = getVolumnInfo(no);

					if (re.isAbnormal) {

						bingoStocks.add(re);

//						System.out.println(no+" bingo!");

					}
				} catch (Exception e) {

					System.out.println("ERROR: " + no);

					exOccuredStocks.add(no);

					e.printStackTrace();
				}

				Thread.sleep(50);
			}

//			System.out.println("2330: "+isAbnormalVol("2330"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Path outputFilePath = Paths.get(Executer.outputFilename);
		try (BufferedWriter bw = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8,
				StandardOpenOption.APPEND)) {
			System.out.println("\nStocks of Volume Abnormal:");
			bw.newLine();
			int bCnt = 0;

			bw.append("\nVF1 spec stocks:\n");
			for (AbnormalStockVO vo : bingoStocks) {
				String bingoS = vo.no + "-" + vo.name + "\t";
				bw.write(bingoS);
				System.out.print(bingoS);

				if (bCnt > 0 && bCnt % 5 == 0)
					bw.newLine();

				bCnt++;
			}

			for (int catg = 2; catg < 5; catg++) {
				bCnt = 0;
				System.out.println("\nStocks of VF2 category: " + catg);
				bw.append("\nVF2 stocks category:" + catg + "\n");
				for (AbnormalStockVO vo : bingoStocks) {
					if (vo.category == catg) {
						String bingoS = vo.no + "-" + vo.name + "\t";
						bw.write(bingoS);
						System.out.print(bingoS);

						if (bCnt > 0 && bCnt % 5 == 0)
							bw.newLine();

						bCnt++;
					}
				}
			}

			// Write TOP
			writeTopChange(bw);

			bw.write("\n\nStocks of failed-process:");
			System.out.println("\n\nStocks of failed-process:");

			for (String no : exOccuredStocks) {
				bw.write(no + "\t");
				System.out.print(no + "\t");
			}

			bw.write("\nStocks of no data:");
			System.out.println("\nStocks of no data:");

			for (String no : noDataStocks) {
				bw.write(no + ", ");
				System.out.print(no + ", ");
			}

			bw.flush();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public AbnormalStockVO getVolumnInfo(String stockNo) throws Exception {

		SimpleDateFormat sdf = new SimpleDateFormat(Executer.DATE_FORMAT_yMd);

		ArrayList<VolumeVO> dateVols = new ArrayList<VolumeVO>();

		String url = HardCodeProp.YahooStockUrl + stockNo;
		String resp = UrlLoader.getResponse(url, "GET", null);

		resp = resp.substring(resp.indexOf("(") + 1, resp.indexOf(")"));

		// print result
//		System.out.println("response:\n"+resp+"\n");

		JSONObject jo = null;
		try {

			jo = new JSONObject(resp);

		} catch (Exception e) {
			e.printStackTrace();
//			System.out.println("response:\n"+resp+"\n");

			exOccuredStocks.add(stockNo);
			return new AbnormalStockVO(false);
		}

		JSONArray data = jo.getJSONArray("ta");

		if (data.length() == 0) {

			noDataStocks.add(stockNo);
			return new AbnormalStockVO(false);
		}

//		JSONObject info = jo.getJSONObject("mem");

//		 String name = info.getString("name");

//		 System.out.println("sn1: "+new String(name.getBytes(StandardCharsets.ISO_8859_1)));

		for (int j = 0; j < data.length(); j++) {

			JSONObject r = data.getJSONObject(j);

			String date_s = r.get("t").toString();
			String vol_s = r.get("v").toString();

			Long vol = Long.valueOf(vol_s.replaceAll(",", ""));
			Date date = sdf.parse(date_s);

			VolumeVO vo = new VolumeVO(date, vol);

//			 System.out.println(date +" : "+vol);

			if (j == data.length() - 1 || j == data.length() - 2)
				vo.price = Double.valueOf(r.get("c").toString());

			dateVols.add(vo);
		}

		AbnormalStockVO re = abnormalAnalyze(stockNo, dateVols);

		if (!re.isAbnormal)
			return re;

		re.no = stockNo;
//		re.name = name;
		re.name = ConstantStore.stockNoNameMap.get(stockNo);
		re.dateVols = dateVols;

		return re;

	}

	private AbnormalStockVO abnormalAnalyze(String stockNo, ArrayList<VolumeVO> dateVols) {

		AbnormalStockVO asvo = new AbnormalStockVO(false);

		long lastDaysNum = 20;
		long sum = 0;
		long sumOfLatestDays = 0;

		for (int i = 0; i < dateVols.size(); i++) {

			VolumeVO vo = dateVols.get(i);
			sum += vo.vol;

			if ((dateVols.size() - 1) - i < lastDaysNum) {
				sumOfLatestDays += vo.vol;
			}
		}

		long avg = sum / dateVols.size();
		long avgOfLatestDays = sumOfLatestDays / lastDaysNum;

		VolumeVO lastVO = dateVols.get(dateVols.size() - 1);
		VolumeVO secLastVO = dateVols.get(dateVols.size() - 2);

		if (secLastVO != null && secLastVO.price > 0) {
			lastVO.stockNo = stockNo;
			lastVO.change = lastVO.price - secLastVO.price;
			lastVO.changePercentage = lastVO.change / secLastVO.price*100;

			ConstantStore.stockInfos.add(lastVO);
		}

//		System.out.println("last: "+lastVO.date +" : "+ lastVO.vol);

		// VF1
		if (avg > 0 && lastVO.vol / avg >= 3) {
			asvo.category = 1;
			asvo.isAbnormal = true;
		}

		// VF2 #1
		if (dateVols.get(dateVols.size() - 1).price < MAX_V2_PRICE && avgOfLatestDays > 1000) {
			asvo.category = 2;
			final int backTestDay = 10;
			final int nextTestDay = 3;

			for (int i = backTestDay; i < dateVols.size(); i++) {
				VolumeVO vo = dateVols.get(i);

				long sumOfBackDays = 0;
				for (int j = i - backTestDay; j < i; j++) {
//					System.out.printf("backtest, i:%d j:%d%n",i,j);
					sumOfBackDays += vo.vol;
				}

				long avgVolOfBackDays = sumOfBackDays / backTestDay;

				if (vo.vol >= avgVolOfBackDays * 3) {// VF #2
					asvo.category = 3;

					long sumOfNextDays = 0;
					for (int j = i + 1; j <= i + nextTestDay; j++) {
						sumOfNextDays += vo.vol;
					}

					long avgVolOfNextDays = sumOfNextDays / nextTestDay;

					if (avgVolOfNextDays >= avgVolOfBackDays * 2) {// VF #3
						asvo.category = 4;
						break;
					}
				}
			}

		}

		return asvo;

	}

	private static void writeTopChange(BufferedWriter bw) throws IOException {
		ConstantStore.stockInfos.sort((s1, s2) -> s2.changePercentage.compareTo(s1.changePercentage));

		bw.write("\nTop 100 Raise:\n");
		for (int i = 0; i < 100; i++) {
			VolumeVO vo = ConstantStore.stockInfos.get(i);
			bw.write(String.format("[%03d] %s, %.2f\n", (i + 1), vo.stockNo, vo.changePercentage));
		}

		bw.write("\nTop 100 Fall:\n");
		for (int i = 0; i < 100; i++) {
			VolumeVO vo = ConstantStore.stockInfos.get(ConstantStore.stockInfos.size() - 1 - i);
			bw.write(String.format("[%03d] %s, %.2f\n", (i + 1), vo.stockNo, vo.changePercentage));
		}
	}

}
