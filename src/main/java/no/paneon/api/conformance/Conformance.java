package no.paneon.api.conformance;

public class Conformance {

	String label;
	String condition;
	String comment;
	
	public Conformance(String label) {
		this(label, "", "");
	}
	
	public Conformance(String label, String condition) {
		this(label, condition, "");
	}
	
	public Conformance(String label, String condition, String comment) {
		this.label = label;
		this.condition = condition;
		this.comment = comment;
		
	}
	
	public String getCondition() {
		return condition;
	}

	public String getComment() {
		return comment;
	}
	
	public String toString() {
		return "Conformance(" + label + ", " + condition + ", " + comment + ")";
	}
	
}
