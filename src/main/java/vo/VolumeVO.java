package vo;

import java.util.Date;

public class VolumeVO {

	public VolumeVO(Date date,Long vol) {
		
		this.date = date;
		this.vol = vol;
	}
	
	
	public String stockNo;
	public Date date;
	
	public Long vol;
	
	public Double price;
	
	public Double change;
	public Double changePercentage;
}
