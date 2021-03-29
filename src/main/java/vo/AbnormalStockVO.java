package vo;

import java.util.ArrayList;

public class AbnormalStockVO {
	public String no;
	public String name;
	
	public boolean isAbnormal;
	public int category = 0;
	
	public ArrayList<VolumeVO> dateVols = new ArrayList<VolumeVO>(); 
	
	public AbnormalStockVO(boolean isAbnormal) {
		
		this.isAbnormal = isAbnormal;
	}
	
	
	

}
