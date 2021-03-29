package datasrc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import execute.Executer;
import util.ConstantStore;
import util.HardCodeProp;
import util.UrlLoader;
import vo.TwseIndex;

public class TWSEDataLoder {

	public static void main(String[] args) throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLSv1.2");
		ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
		SSLContext.setDefault(ctx);

		new TWSEDataLoder().getIndices();
	}

	private static class DefaultTrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	class VolumeVO {

		public VolumeVO(Date date, Long vol) {

			this.date = date;
			this.vol = vol;
		}

		Date date;

		Long vol;

	}

	private ArrayList<String> failedStocks = new ArrayList<String>();

	String DATE_FORMAT_yMd = "yyyyMMdd";

	private void getIndices()
			throws IOException, JSONException, ParseException, ParserConfigurationException, SAXException {

		LocalDate now = LocalDate.now();
		int weekDayValue = now.getDayOfWeek().getValue();

		// If now is not a workingday, offset to Friday
		if (weekDayValue > 5) {
			now = now.minusDays(weekDayValue - 5);
		}
		// 20201216
		DateTimeFormatter dtf = DateTimeFormatter.BASIC_ISO_DATE;
		String date = dtf.format(now);
		String url = HardCodeProp.TwseIndicesUrl + date;
		
		System.out.println("Data date: "+date);
		System.out.println(url);

		String resp = UrlLoader.getResponse(url, "GET", null);

		resp = new String(resp.getBytes(),StandardCharsets.UTF_8);
		
//		System.out.println(resp);
		
		JSONObject jo = new JSONObject(resp);

		ArrayList<TwseIndex> indices = new ArrayList<>();
		NumberFormat format = NumberFormat.getInstance();
		for (int i = 1; i <= 6; i++) {
			JSONArray data = jo.getJSONArray("data" + i);

			for (int j = 0; j < data.length(); j++) {
				JSONArray idx = data.getJSONArray(j);

				String name = idx.getString(0);
				String closeStr = idx.getString(1);
				
				float close = NumberUtils.isParsable(closeStr) ? format.parse(idx.getString(1)).floatValue() :0;
				String dir = idx.getString(2);
				
				String chgStr = idx.getString(3);
				float change = NumberUtils.isParsable(chgStr) ?format.parse(chgStr).floatValue():0;
				
				String chgPercStr = idx.getString(4);
				float changePercentage = NumberUtils.isParsable(chgPercStr)?format.parse(chgPercStr).floatValue():0;

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(dir));
				dir = builder.parse(is).getChildNodes().item(0).getTextContent();

				if ("-".equals(dir)) {
					change *= -1;
				}

				indices.add(new TwseIndex(name, close, change, changePercentage));
			}
		}

		// Sort by change percentage desc
		Collections.sort(indices, (i1, i2) -> Float.compare(i2.changePercentage, i1.changePercentage));

		Path outputFilePath = Paths.get(Executer.outputFilename);
		try (BufferedWriter bw = Files.newBufferedWriter(outputFilePath/*, StandardCharsets.UTF_8*/)) {
			bw.write("TOP raising indices:\n");

			System.out.println("TOP raising indices:");
			for (int i = 0; i < 3; i++) {
				TwseIndex idx = indices.get(i);
				String msg = String.format(" %d. %s: %.2f%%%n", (i + 1), idx.name, idx.changePercentage);
				bw.write(msg);
				System.out.print(msg);
			}

			bw.write("\nTOP falliing indices:\n");
			System.out.println("\nTOP falliing indices:");
			for (int i = 0; i < 3; i++) {
				TwseIndex idx = indices.get(indices.size() - i - 1);
				String msg = String.format(" %d. %s: %.2f%%%n", (i + 1), idx.name, idx.changePercentage);
				bw.write(msg);
				System.out.print(msg);
			}
		}
	}

	public void startup() {
		try {
			SSLContext ctx = SSLContext.getInstance("TLSv1.2");
			ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
			SSLContext.setDefault(ctx);

			getIndices();
			getListedISINCode();
			getTPExISINCode();

			System.out.println();
			System.out.println("Number of listing stocks: " + ConstantStore.listedISINs.size());
			System.out.println("Number of OTC stocks: " + ConstantStore.tpexISINs.size());
			System.out.println("Number of ETF stocks: " + ConstantStore.etfISINs.size());

			/*
			 * for(String no:ConstantStore.stockNoList) {
			 * 
			 * try { if(isAbnormalVol(no)) {
			 * 
			 * overStocks.add(no);
			 * 
			 * System.out.println(no); } }catch(Exception e) {
			 * 
			 * System.out.println("ERROR: "+no); e.printStackTrace(); break; }
			 * 
			 * }
			 */

//			System.out.println("2330: "+isAbnormalVol("2330"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out.println("Stocks of Volume Abnormal:");
//		
//		for(String no:overStocks) {
//			
//			System.out.print(no+", ");
//		}

	}

	private void getListedISINCode() {

		Document doc;
		try {
			Connection conn = Jsoup.connect(HardCodeProp.ListedISINUrl).timeout(60000) //
					.method(Connection.Method.GET) //
					.userAgent(
							"5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
//			URLConnection conn = new URL(HardCodeProp.ListedISINUrl).openConnection();
//			conn.maxBodySize(0);
//			
//			InputStream response = conn.getInputStream();
//			
//			 java.util.Scanner s = new java.util.Scanner(response).useDelimiter("\\A");
//		s.hasNext() ? s.next() : "";

			doc = conn.get();

			Element compTbody = doc.getElementsByTag("tbody").get(0);

			for (int i = 0; i < compTbody.childNodes().size(); i++) {

				Node n = compTbody.childNodes().get(i);

				if (i > 1 && "tr".equals(n.nodeName())) {

					Node noTd = n.childNode(0);

					String[] noName = noTd.childNode(0).toString().split("\u3000");
					String no = noName[0];
					String name = null;
					if (noName.length >= 2 && noName[1] != null)
						name = noName[1];

					if (no.length() == 4) {

						ConstantStore.listedISINs.add(no);
					} else if (no.indexOf("00") == 0) {
//						System.out.println(no);

						ConstantStore.etfISINs.add(no);
					}

					ConstantStore.stockNoNameMap.put(no, name);

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void getTPExISINCode() {

		Document doc;
		try {

			Connection conn = Jsoup.connect(HardCodeProp.TPExISINUrl);
			conn.maxBodySize(0);

			doc = conn.get();

			Element compTbody = doc.getElementsByTag("tbody").get(0);

			for (int i = 0; i < compTbody.childNodes().size(); i++) {

				Node n = compTbody.childNodes().get(i);

				if (i > 1 && "tr".equals(n.nodeName())) {

					Node noTd = n.childNode(0);

					String[] noName = noTd.childNode(0).toString().split("\u3000");
					String no = noName[0];
					String name = null;
					if (noName.length >= 2 && noName[1] != null)
						name = noName[1];

					if (no.length() == 4) {
//						System.out.println(no);
						ConstantStore.tpexISINs.add(no);
					} else if (no.indexOf("00") == 0) {

						ConstantStore.etfISINs.add(no);
					}

					ConstantStore.stockNoNameMap.put(no, name);

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * deprecated for TWSE connection limit
	 */
	@Deprecated
	private boolean isAbnormalVol(String stockNo) throws Exception {

		System.out.println("checking " + stockNo + "...");

		Calendar pCal = Calendar.getInstance();
		pCal.set(Calendar.DAY_OF_MONTH, 1);

		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_yMd);

		ArrayList<VolumeVO> dateVols = new ArrayList<VolumeVO>();

		pCal.add(Calendar.MONTH, -2);

		for (int i = 0; i < 3; i++) {

			if (i > 0)
				pCal.add(Calendar.MONTH, 1);

			String pDate = sdf.format(pCal.getTime());

			String url = HardCodeProp.TwseStockUrl + "&date=" + pDate + "&stockNo=" + stockNo;
			String resp = UrlLoader.getResponse(url, "GET", null);

			Thread.sleep(1500);
			// print result
//			System.out.println("response:\n"+resp+"\n");

			JSONObject jo = null;
			try {

				jo = new JSONObject(resp);

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("response:\n" + resp + "\n");
				return false;
			}

			String stat = jo.getString("stat");

			if (stat.indexOf("OK") == -1) {

				failedStocks.add(stockNo);
				return false;
			}

			JSONArray data = jo.getJSONArray("data");

			for (int j = 0; j < data.length(); j++) {

				List r = data.getJSONArray(j).toList();

				String date_s = r.get(0).toString();
				String vol_s = r.get(1).toString();

				Long vol = Long.valueOf(vol_s.replaceAll(",", ""));

				String[] date_sa = date_s.split("/");

				Calendar cal = Calendar.getInstance();

				cal.set(Calendar.YEAR, Integer.parseInt(date_sa[0]) + 1911);
				cal.set(Calendar.MONTH, Integer.parseInt(date_sa[1]) - 1);
				cal.set(Calendar.DATE, Integer.parseInt(date_sa[2]));

				dateVols.add(new VolumeVO(cal.getTime(), vol));

//				 System.out.println("date: "+cal.getTime().toString());
//				 System.out.println("volume: "+vol);

			}

		}

		return abnormalAnalyze(dateVols);
	}

	@Deprecated
	private boolean abnormalAnalyze(ArrayList<VolumeVO> dateVols) {

		boolean isAbnormal = false;

		long sum = 0;
		long avg = 0;

		for (VolumeVO vo : dateVols) {

			sum += vo.vol;

		}

		avg = sum / dateVols.size();

		if (dateVols.get(dateVols.size() - 1).vol / avg >= 3)
			isAbnormal = true;

		return isAbnormal;

	}

}
