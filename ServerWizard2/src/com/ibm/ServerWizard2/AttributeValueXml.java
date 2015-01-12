package com.ibm.ServerWizard2;

import java.io.Writer;

import org.w3c.dom.Element;

public class AttributeValueXml extends AttributeValue {

	public String value="";
	public String array="";
	public String name="";
	//public Vector<String> enumList;
	//public HashMap<String,String> enumValues = new HashMap<String,String>();
	
	public AttributeValueXml() {
		
	}
	
	public AttributeValueXml(AttributeValueXml v) {
		//deep copy
		//enumList = new Vector<String>();
		value=v.value;
		type=v.type;
		name=v.name;
		//for (int i=0;i<v.enumList.size();i++) {
			//enumList.add(v.enumList.get(i));
		//}
	}

	@Override
	public void readXML(Element e) {
		
		value = SystemModel.getElement(e, "default");
		//Node n = e.getElementsByTagName("default").item(0).getFirstChild();
	}
	public void readInstanceXML(Element e) {
		value = SystemModel.getElement(e, "default");
	}
	
	@Override
	public void writeInstanceXML(Writer out) throws Exception {
		out.write("\t\t<default><![CDATA["+value+"]]></default>\n");
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return value+" ("+type+")";
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
		AttributeValueXml n = (AttributeValueXml) value;
		this.value=n.value;
		this.name=n.name;
	}
}
