package vo;

public class TwseIndex {
	
	
	
	public TwseIndex(String name, Float close, Float change, Float changePercentage) {
		super();
		this.name = name;
		this.close = close;
		this.change = change;
		this.changePercentage = changePercentage;
	}
	
	public String name;
	public Float close;
	public Float change;
	public Float changePercentage;
	
	@Override
	public String toString() {
		return "TwseIndex [name=" + name + ", close=" + close + ", change=" + change + ", changePercentage="
				+ changePercentage + "]";
	}
	
	
}
