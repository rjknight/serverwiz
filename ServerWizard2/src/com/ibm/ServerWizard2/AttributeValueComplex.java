package com.ibm.ServerWizard2;

import java.io.Writer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AttributeValueComplex extends AttributeValue {


	public Vector<Field> fields;
	public AttributeValueComplex() {
		
	}
	public AttributeValueComplex(AttributeValueComplex a) {
		//deep copy
		fields = new Vector<Field>();
		type = a.type;
		
		for (int i=0;i<a.fields.size();i++) {
			Field f = new Field(a.fields.get(i));
			fields.add(f);
		}
	}

	public void readXML(Element value) {
		fields = new Vector<Field>();
		type="complex";
		NodeList fieldList = value.getElementsByTagName("field");
		for (int i = 0; i < fieldList.getLength(); ++i) {
			Field f = new Field();
			f.name = SystemModel.getElement((Element) fieldList.item(i), "name");
			f.desc = SystemModel
					.getElement((Element) fieldList.item(i), "description");
			f.type = SystemModel.getElement((Element) fieldList.item(i), "type");
			f.bits = SystemModel.getElement((Element) fieldList.item(i), "bits");
			f.defaultv = SystemModel.getElement((Element) fieldList.item(i), "default");
			fields.add(f);
		}
	}
	public void readInstanceXML(Element value) {
		NodeList fieldList = value.getElementsByTagName("field");
		for (int i = 0; i < fieldList.getLength(); ++i) {
			String fid=SystemModel.getElement((Element) fieldList.item(i), "id");
			String v=SystemModel.getElement((Element) fieldList.item(i), "value");
			for(int x=0;x<fields.size();x++) {
				Field f = fields.get(x);
				if (f.name.equals(fid)) {
					f.value=v;
				}
			}
		}
	}
	@Override
	public void writeInstanceXML(Writer out) throws Exception {
		String t="\t\t";
		String r=t+"<default>\n";
		for (int i=0;i<fields.size();i++) {
			Field f = new Field(fields.get(i));
			r=r+t+"\t\t<field><id>"+f.name+"</id><value>"+f.value+"</value></field>\n";
		}
		r=r+t+"</default>\n";
		out.write(r);
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return "complex";
	}

	@Override
	public String toString() {
		String r="COMPLEX:\n";
		for (int i=0;i<this.fields.size();i++) {
			Field f = new Field(this.fields.get(i));
			r=f.name+"="+f.value;
		}
		return r;
	}
	@Override
	public void setValue(String value) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Boolean isEmpty() {
		return false;
	}
	@Override
	public void setValue(AttributeValue value) {
		fields.clear();
		AttributeValueComplex c = (AttributeValueComplex)value;
		for (int i=0;i<c.fields.size();i++) {
			Field f = new Field(c.fields.get(i));
			fields.add(f);
		}
	}
	@Override
	public Control getEditor(Table table,AttributeTableItem item) {
		Control control;
		if (item.getField().type.equals("boolean")) {
			CCombo combo = new CCombo(table, SWT.NONE);
			combo.setText(item.getField().value);
			combo.add("true"); 
			combo.setData(item);
			combo.add("false");
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					CCombo c = (CCombo) event.getSource();
					AttributeTableItem a = (AttributeTableItem) c.getData();
					a.getField().value=c.getText();
					a.getItem().setText(2, a.getField().value);
				}
			});
			control=combo;
		} else {
			Text text = new Text(table, SWT.NONE);
			text.setData(item);
			text.setText(item.getField().value);
			text.setSelection(text.getText().length());
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					Text text = (Text) e.getSource();
					AttributeTableItem a = (AttributeTableItem) text.getData();
					a.getField().value = text.getText();
					a.getItem().setText(2, a.getField().value);
				}
			});
			control=text;
		}
		return control;
	}	

}
