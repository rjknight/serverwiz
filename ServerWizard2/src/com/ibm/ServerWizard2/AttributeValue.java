package com.ibm.ServerWizard2;

import java.io.Writer;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.w3c.dom.Element;

public abstract class AttributeValue {
	
	public String type="";
	public Enumerator enumerator = null;
	protected TableItem item = null;
	
	public abstract void readXML(Element value);
	public abstract void readInstanceXML(Element value);
	public abstract void writeInstanceXML(Writer out) throws Exception;
	public abstract String getValue();
	public abstract void setValue(AttributeValue value);
	public abstract void setValue(String value);
	public abstract String toString();
	public abstract Boolean isEmpty();
	public abstract Control getEditor(Table table,AttributeTableItem item);

	public AttributeValue() {
		
	}
	public AttributeValue(AttributeValue v) {
		
	}
	public String getType() {
		return type;
	}
}
