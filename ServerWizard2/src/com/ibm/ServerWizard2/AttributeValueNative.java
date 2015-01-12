package com.ibm.ServerWizard2;


import java.io.Writer;

import org.w3c.dom.Element;

public class AttributeValueNative extends AttributeValue {
	String value="";
	String name="";
	
	public AttributeValueNative() {
		
	}
	public AttributeValueNative(AttributeValueNative a) {
		value=a.value;
		name=a.name;
	}
	
	public void readXML(Element e) {
		value = SystemModel.getElement(e, "default");
		name = SystemModel.getElement(e, "name");
	}
	public void readInstanceXML(Element e) {
		value = SystemModel.getElement(e, "default");
	}
	@Override
	public void writeInstanceXML(Writer out) throws Exception {
		out.write("\t\t<default>"+value+"</default>\n");
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return this.value;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "default("+name+")";
	}
	@Override
	public void setValue(String value) {
		this.value=value;
		
	}
	@Override
	public Boolean isEmpty() {
		return value.isEmpty();
	}
	@Override
	public void setValue(AttributeValue value) {
		AttributeValueNative n = (AttributeValueNative) value;
		this.value=n.value;
		this.name=n.name;
	}

}
