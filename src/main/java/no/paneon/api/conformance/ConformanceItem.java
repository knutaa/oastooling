package no.paneon.api.conformance;

public class ConformanceItem {
	public String label;
	public String condition;
	public String comment;
	
	public ConformanceItem(String label, String condition, String comment) {
		this.label = label;
		this.condition = condition;
		this.comment = comment;
	}
		
	public ConformanceItem(String label) {
		this(label, "", "");
	}

	
	public ConformanceItem(String ...args) {
		this(args[0]);
		if(args.length==2) {
			this.comment = args[1];
		} else if(args.length==3) {
			this.condition = args[1];
			this.comment = args[2];
		}
	}
	
	public Boolean isSubordinate = false;

	public void setSubordinate(boolean value) {
		this.isSubordinate = value;
	}
	
	public String toString() {
		return this.label + ":: " + this.condition + " " + this.comment;
	}
}
