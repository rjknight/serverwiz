package com.ibm.ServerWizard2;

import java.io.Writer;

import org.w3c.dom.Element;

public abstract class AttributeValue {
	
	public String type="";
	public abstract void readXML(Element value);
	public abstract void readInstanceXML(Element value);
	public abstract void writeInstanceXML(Writer out) throws Exception;
	public abstract String getValue();
	public abstract void setValue(AttributeValue value);
	public abstract void setValue(String value);
	public abstract String toString();
	public abstract Boolean isEmpty();

	public AttributeValue() {
		
	}
	public AttributeValue(AttributeValue v) {
		
	}
	public String getType() {
		return type;
	}
}
