package datasrc;

import java.io.IOException;

import org.json.JSONObject;

import util.HardCodeProp;
import util.UrlLoader;

public class Index {
	
	public static void main(String[] args) {
		new Index().getIndices();
	}
	
	
	public void getIndices() {
		String url = HardCodeProp.TwseIndicesUrl+"20201215";
		try {
			String resp = UrlLoader.getResponse(url, "GET",null);
			
			System.out.println(resp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		JSONObject jo = new JSONObject("");
	}
}
