package com.ibm.ServerWizard2;

public class Field {
	public String name="";
	public String desc="";
	public String type="";
	public String bits="";
	public String defaultv="";
	public String value="";
	public Field() {
	
	}
	public Field(Field f) {
		name=f.name;
		desc=f.desc;
		type=f.type;
		bits=f.bits;
		value=f.value;
		defaultv=f.defaultv;
	}
}
