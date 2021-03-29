package util;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"rawtypes","unchecked"})
public class UrlLoader {
	
	private static final int BUFFER_SIZE = 8192;
	
	private String fileName = "undefined";
	boolean printHeader = false;

	private void printResponseHeader(HttpURLConnection conn,boolean printHeader) {

		Map<String,List<String>> headerMap = conn.getHeaderFields();
		TreeMap<String,List<String>> headerTreeMap = new TreeMap<String,List<String>>();

		if(printHeader)
			System.out.println("----- Headers -----");
		
		for (Iterator it = headerMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry mapEntry = (Map.Entry) it.next();
			
			if(mapEntry.getKey() != null)
				headerTreeMap.put((String)mapEntry.getKey(),(List<String>)mapEntry.getValue());
			else if(printHeader)
				System.out.println(mapEntry.getKey() + " : " + mapEntry.getValue());
		}

		for (Iterator it = headerTreeMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry mapEntry = (Map.Entry) it.next();
			
			if(printHeader)
				System.out.println(mapEntry.getKey() + " : " + mapEntry.getValue());
		}
		
		if(printHeader)
			System.out.println("----- Headers -----");
	}

	private void download(HttpURLConnection conn, String fileURL, String saveDir,String fName) throws IOException {

		String disposition = conn.getHeaderField("Content-Disposition");

		if (disposition != null) {
			// extracts file name from header field
			int index = disposition.indexOf("filename=");
			if (index > 0) {
				fileName = disposition.substring(index + 10, disposition.length() - 1);
			}
		} else {
			// extracts file name from URL
			fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
		}

		if(fName!=null &&!fName.isEmpty() )
			fileName=fName;
		
		// opens input stream from the HTTP connection
		InputStream inputStream = conn.getInputStream();
		String saveFilePath = saveDir + File.separator + fileName;// + fileName

//		System.out.println("download path: "+saveFilePath);
		// opens an output stream to save into file
		FileOutputStream outputStream = new FileOutputStream(saveFilePath);

		int bytesRead = -1;
		byte[] buffer = new byte[BUFFER_SIZE];
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		outputStream.close();
		inputStream.close();

	}

	public boolean downloadFile(String fileURL, String saveDir,String method,String params,String fName) throws IOException {
		boolean result = false;
		
		byte[] formData = params.getBytes(StandardCharsets.UTF_8);

		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod(method);
		httpConn.setDoOutput(true);

		DataOutputStream wr = new DataOutputStream(httpConn.getOutputStream());
			
		wr.write(formData);
		wr.flush();
		wr.close();
		

		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			printResponseHeader(httpConn,printHeader);
			download(httpConn, fileURL, saveDir,fName);

			result = true;
//		} else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
//
//			printResponseHeader(httpConn,printHeader);
//			// download(httpConn,fileURL,saveDir);
//			result = false;
		}else {
			System.out.println("HTTP " + responseCode);

			result = false;
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		httpConn.disconnect();

		return result;
	}
	
	public static String getResponse(String urlStr,String method,String params) throws IOException {
		
		String result = null;

		URL url = new URL(urlStr);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod(method);
		httpConn.setDoOutput(true);

		if(params != null) {
			byte[] formData = params.getBytes(StandardCharsets.UTF_8);
			DataOutputStream wr = new DataOutputStream(httpConn.getOutputStream());
			
			wr.write(formData);
			wr.flush();
			wr.close();
		}
		

		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

//			printResponseHeader(httpConn,printHeader);
			
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(httpConn.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			
//			System.out.println(response);

			result = response.toString();

//		} else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
//
//			printResponseHeader(httpConn,printHeader);
//			// download(httpConn,fileURL,saveDir);
//			result = false;
		}else {
			System.out.println("HTTP " + responseCode);

//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

		httpConn.disconnect();

		return result;
	}
}
