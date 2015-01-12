package com.ibm.ServerWizard2;

import java.io.Writer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Attribute implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public String name = "";
	public AttributeValue value;

	public String inherited = "";
	public String desc = "";
	public Boolean readable = false;
	public Boolean writeable = false;
	public Boolean priority = false;
	public Boolean connectivity = false;
	public Persistency persistency = Persistency.NO_PERSISTENCY;

	public enum Persistency {
		NO_PERSISTENCY, VOLATILE_ZEROED, NON_VOLATILE, VOLATILE
	};

	public Attribute() {
	}

	public Attribute(Attribute a) {
		this.name = a.name;
		this.desc = a.desc;
		this.priority=a.priority;
		this.connectivity=a.connectivity;
		this.persistency = a.persistency;
		this.readable = a.readable;
		this.writeable = a.writeable;
		this.inherited = a.inherited;
		
		if (a.value instanceof AttributeValueComplex) {
			this.value = new AttributeValueComplex((AttributeValueComplex)a.value);
		}
		else if(a.value instanceof AttributeValueSimple) {
			this.value = new AttributeValueSimple((AttributeValueSimple)a.value);
		}
		else if(a.value instanceof AttributeValueNative) {
			this.value = new AttributeValueNative((AttributeValueNative)a.value);
		}
		else if(a.value instanceof AttributeValueXml) {
			this.value = new AttributeValueXml((AttributeValueXml)a.value);
		}
		else {
			
		}
		
	}

	public AttributeValue getValue() {
		return value;
	}

	public String toString() {
		String rtn="Attribute: "+name+" = ";
		rtn="Attribute: "+name+" = "+value.toString()+" inherited="+this.inherited;
		return rtn;
	}

	public void setPersistence(String p) {
		if (p.equals("non-volatile")) {
			persistency = Persistency.NON_VOLATILE;
		} else if (p.equals("volatile-zeroed")) {
			persistency = Persistency.VOLATILE_ZEROED;
		} else if (p.equals("volatile")) {
			persistency = Persistency.VOLATILE;
		} else {
			throw new NullPointerException("Invalid Peristence: "+p);
		}
	}
	public String getPersistence() {
		if (persistency == Persistency.NON_VOLATILE) {
			return "non-volatile";
		} else if(persistency == Persistency.VOLATILE_ZEROED) {
			return "volatile-zeroed";
		} else if(persistency == Persistency.NO_PERSISTENCY) {
			return "";
		} else if(persistency == Persistency.VOLATILE) {
			return "volatile";
		} else { return ""; }
		
	}
	public void readModelXML(Element attribute) {
		//name = attribute.getElementsByTagName("id").item(0).getChildNodes().item(0).getNodeValue();
		name = SystemModel.getElement(attribute, "id");
		desc = SystemModel.getElement(attribute,"description");
		
		String p = SystemModel.getElement(attribute,"persistency");
		if (!p.isEmpty()) { setPersistence(p); }
		
		if (SystemModel.isElementDefined(attribute,"readable")) {
			readable=true;
		}
		if (SystemModel.isElementDefined(attribute,"writeable")) {
			writeable=true;
		}
		if (SystemModel.isElementDefined(attribute,"priority")) {
			priority=true;
		}
		if (SystemModel.isElementDefined(attribute,"connectivity")) {
			connectivity=true;
		}
		Node simpleType = attribute.getElementsByTagName("simpleType").item(0);
		if (simpleType!=null) { 
			value = new AttributeValueSimple();
			value.readXML((Element)simpleType);
		}
		Node complexType = attribute.getElementsByTagName("complexType").item(0);
		if (complexType!=null) {
			value = new AttributeValueComplex();
			value.readXML((Element)complexType);
		}
		Node nativeType = attribute.getElementsByTagName("nativeType").item(0);
		if (nativeType!=null) {
			value = new AttributeValueNative();
			value.readXML((Element)nativeType);
		}
		Node xmlType = attribute.getElementsByTagName("xmlType").item(0);
		if (xmlType!=null) {
			value = new AttributeValueXml();
			value.readXML((Element)xmlType);
		}
	}
	public void writeBusInstanceXML(Writer out) throws Exception {
		out.write("\t\t<bus_attribute>\n");
		out.write("\t\t\t<id>"+name+"</id>\n");
		value.writeInstanceXML(out);
		out.write("\t\t</bus_attribute>\n");
	}
	public void writeInstanceXML(Writer out) throws Exception {
		out.write("\t<attribute>\n");
		out.write("\t\t<id>"+name+"</id>\n");
		value.writeInstanceXML(out);
		out.write("\t</attribute>\n");
	}

}
