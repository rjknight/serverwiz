package com.ibm.ServerWizard2;

import org.eclipse.swt.widgets.TableItem;

public class AttributeTableItem {
	public AttributeValue attribute;
	public Field field = null;
	public TableItem item;
	public AttributeValue getAttribute() {
		return attribute;
	}
	public void setAttributeValue(AttributeValue attribute) {
		this.attribute = attribute;
	}
	public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}
	public TableItem getItem() {
		return item;
	}
	public void setItem(TableItem item) {
		this.item = item;
	}
}
