package com.ibm.ServerWizard2;

import java.io.Writer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Element;

public class AttributeValueSimple extends AttributeValue {

	public String value = "";
	public String array = "";
	public String name = "";
	
	public AttributeValueSimple() {

	}

	public AttributeValueSimple(AttributeValueSimple v) {
		// deep copy
		value = v.value;
		type = v.type;
		name = v.name;
		array = v.array;
		enumerator = v.enumerator;
	}

	@Override
	public void readXML(Element e) {
		// value = e.getElementsByTagName("default").;
		Element a = (Element) e.getElementsByTagName("uint8_t").item(0);
		if (a != null) {
			type = "uint8_t";
			value = SystemModel.getElement(a, "default");
		}
		a = (Element) e.getElementsByTagName("uint16_t").item(0);
		if (a != null) {
			type = "uint16_t";
			value = SystemModel.getElement(a, "default");
		}
		a = (Element) e.getElementsByTagName("uint32_t").item(0);
		if (a != null) {
			type = "uint32_t";
			value = SystemModel.getElement(a, "default");
		}
		a = (Element) e.getElementsByTagName("string").item(0);
		if (a != null) {
			type = "string";
			value = SystemModel.getElement(a, "default");
		}
		a = (Element) e.getElementsByTagName("enumeration").item(0);
		if (a != null) {
			type = "enumeration";
			value = SystemModel.getElement(a, "default");
			name = SystemModel.getElement(a, "id");
		}
		array = SystemModel.getElement(e, "array");

	}

	public void readInstanceXML(Element e) {
		value = SystemModel.getElement(e, "default");
	}

	@Override
	public void writeInstanceXML(Writer out) throws Exception {
		out.write("\t\t<default>" + value + "</default>\n");
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return value + " (" + type + ")";
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public Boolean isEmpty() {
		return value.isEmpty();
	}

	@Override
	public void setValue(AttributeValue value) {
		AttributeValueSimple n = (AttributeValueSimple) value;
		this.value = n.value;
		this.name = n.name;
	}

	public Control getEditor(Table table,AttributeTableItem item) {
		Control control = null;
		if (this.type.equals("enumeration")) {
			CCombo combo = new CCombo(table, SWT.NONE);
			combo.setText(this.value);
			Vector<String> enumList = this.enumerator.enumList;
			for (int i = 0; i < enumList.size(); i++) {
				combo.add(enumList.get(i));
				combo.setData(item);
			}
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					CCombo c = (CCombo) event.getSource();
					AttributeTableItem a = (AttributeTableItem) c.getData();
					AttributeValueSimple v = (AttributeValueSimple) a.getAttribute();
					v.value = c.getText();
					a.getItem().setText(2, v.value);
				}
			});
			control = combo;
		} else {
			Text text = new Text(table, SWT.NONE);
			text.setData(item);
			text.setText(value);
			text.setSelection(text.getText().length());
			text.addVerifyListener(new VerifyListener() {

				@Override
				public void verifyText(VerifyEvent e) {
					String string = e.text;
					Text text = (Text) e.getSource();
					AttributeTableItem a = (AttributeTableItem) text.getData();
					AttributeValueSimple v = (AttributeValueSimple) a.getAttribute();

					if (v.type.equals("string") || !v.array.isEmpty()) {
						e.doit = true;
						return;
					}
					if (!string.equals("x")) {
						string = string.toUpperCase();
					}
					String validChars = "x0123456789ABCDEF\b";
					if (e.keyCode == SWT.DEL || validChars.indexOf(string) >= 0) {
						e.text = string;
						e.doit = true;
					} else {
						e.doit = false;
					}
				}
			});
			text.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(ModifyEvent e) {
					Text text = (Text) e.getSource();
					AttributeTableItem a = (AttributeTableItem) text.getData();
					AttributeValueSimple v = (AttributeValueSimple) a.getAttribute();
					v.value = text.getText();
					a.getItem().setText(2, v.value);
				}
			});
			control = text;
		}
		return control;
	}
}
