package com.ibm.ServerWizard2;

import java.io.Writer;

import org.w3c.dom.Element;

public class AttributeValueSimple extends AttributeValue {

	public String value="";
	public String array="";
	public String name="";
	//public Vector<String> enumList;
	//public HashMap<String,String> enumValues = new HashMap<String,String>();
	
	public AttributeValueSimple() {
		
	}
	
	public AttributeValueSimple(AttributeValueSimple v) {
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
		//value = e.getElementsByTagName("default").;
		Element a=(Element)e.getElementsByTagName("uint8_t").item(0);
		if (a!=null) {
			type="uint8_t";
			value=SystemModel.getElement(a,"default");
		}
		a=(Element)e.getElementsByTagName("uint16_t").item(0);
		if (a!=null) {
			type="uint16_t";
			value=SystemModel.getElement(a,"default");
		}
		a=(Element)e.getElementsByTagName("uint32_t").item(0);
		if (a!=null) {
			type="uint32_t";
			value=SystemModel.getElement(a,"default");
		}
		a=(Element)e.getElementsByTagName("string").item(0);
		if (a!=null) {
			type="string";
			value=SystemModel.getElement(a,"default");
		}
		a=(Element)e.getElementsByTagName("enumeration").item(0);
		if (a!=null) {
			type="enumeration";
			value=SystemModel.getElement(a,"default");
			name=SystemModel.getElement(a,"id");
		}
		array = SystemModel.getElement(e, "array");
		
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
		AttributeValueSimple n = (AttributeValueSimple) value;
		this.value=n.value;
		this.name=n.name;
	}
}
