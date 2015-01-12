package com.ibm.ServerWizard2;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Target implements Comparable<Target>, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id = -1;
	private String name = "";
	private String type = "";
	private int position = -1;
	public String parent = ""; // says which parent to inherit attributes from
								// target_types
	private Vector<String> parentType = new Vector<String>();
	private TreeMap<String, Attribute> attributes = new TreeMap<String, Attribute>();
	private Vector<Target> children = new Vector<Target>();
	private TreeMap<Target,Vector<Connection>> busses = new TreeMap<Target,Vector<Connection>>();
	private Boolean busInited=false;
	private Boolean hidden = false;
	//private Boolean staticName=false;
	
	public Target() {
	}

	public Target(Target s) {
		this.setName(s.name);
		this.setType(s.type);
		this.parent = s.parent;
		this.position = s.position;
		this.hidden = s.hidden;
		// this.parentNode = s.parentNode;
		this.parentType.addAll(s.parentType);
		//this.staticName=s.staticName;
		
		for (Map.Entry<String, Attribute> entry : s.getAttributes().entrySet()) {
			String key = new String(entry.getKey());
			Attribute value = new Attribute(entry.getValue());
			this.attributes.put(key, value);
		}
	}
	
	public TreeMap<Target,Vector<Connection>> getBusses() {
		return busses;
	}
	
	public void hide(Boolean h) {
		this.hidden=h;
	}
	public Boolean isHidden() {
		return this.hidden;
	}
	public void copyAttributes(Target s) {
		for (Map.Entry<String, Attribute> entry : s.getAttributes().entrySet()) {
			String key = new String(entry.getKey());
			Attribute value = new Attribute(entry.getValue());
			this.attributes.put(key, value);
		}
	}

	public void linkAttributes(Target t) {
		this.attributes.clear(); // not sure if this helps garbage collecting or
								 // not
		this.attributes = t.getAttributes();
	}
	public void linkBusses(Target t){
		busses=t.getBusses();
	}

	public Integer getId() {
		return id;
	}
/*
	public String getFullName() {
		String parentPath="";
		return parentPath + "/" + getName();
	}
*/
	public String getName() {
		if (position==-1 && !name.isEmpty()) {
			return name;
		}
		if (!name.isEmpty()) {
			return name + "-" + position;
		}
		return getIdPrefix() + "-" + position;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Vector<Target> getChildren() {
		return this.children;
	}

	public void addChild(Target child) {
		children.add(child);
	}

	public Boolean isPluggable() {
		String c = this.getAttribute("CLASS");

		if (c == null) {
			return false;
		}

		if (c.equals("CONNECTOR")) {
			return true;
		}
		return false;
	}

	public void setPosition(String pos) {
		this.position = Integer.parseInt(pos);
	}

	public void setPosition(int pos) {
		this.position = pos;
	}

	public int getPosition() {
		return this.position;
	}

	public Vector<String> getParentType() {
		return this.parentType;
	}

	public void getBusTypes(Vector<ConnectionEndpoint> busList, String busType, String parentPath, Boolean isSource,Boolean isParent) {
		if (this.isHidden()) { return; }
		String dir = this.getAttribute("DIRECTION");
		if (this.getAttribute("BUS_TYPE").equals(busType)
				&& (isSource && dir.equals("OUT") || isSource && dir.equals("INOUT") || !isSource
						&& dir.equals("IN") || !isSource && dir.equals("INOUT"))) {

			ConnectionEndpoint endpoint = new ConnectionEndpoint();
			endpoint.setTarget(this);
			endpoint.setPath(parentPath);
			busList.add(endpoint);
		}
		if (!isParent) {
			parentPath=parentPath+this.getName()+"/";
		}
		for (Target child : this.getChildren()) {
			child.getBusTypes(busList, busType, parentPath, isSource,false);
		}
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getIdPrefix() {
		String t = type.split("-")[1];
		if (t.equals("processor")) {
			t = "proc";
		}
		return t;
	}

	public TreeMap<String, Attribute> getAttributes() {
		return attributes;
	}

	public String getAttribute(String attribute) {
		if (attributes.get(attribute) == null) {
			return "";
		}
		return attributes.get(attribute).getValue().getValue();
	}

	public void copyAttributesFromParent(Target s) {
		for (Map.Entry<String, Attribute> entry : s.getAttributes().entrySet()) {
			String key = entry.getKey();
			Attribute tmpAttribute = entry.getValue();
			Attribute localAttribute = this.attributes.get(key);
			// inherited attribute was already added when instance created
			if (localAttribute != null) {
				localAttribute.inherited = s.type;
			}
			// only add non-inherited attributes
			// else if (tmpAttribute.inherited.isEmpty()) {
			else {
				Attribute attribute = new Attribute(tmpAttribute);
				attribute.inherited = s.type;
				this.attributes.put(key, attribute);
			}
		}
	}

	public void updateAttributeValue(String attributeName, String value) {
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null) {
			throw new NullPointerException("Invalid Attribute " + attributeName + " in Target "
					+ this.type);
		}
		AttributeValue val = attribute.getValue();
		val.setValue(value);
	}

	public void updateAttributeValue(String attributeName, AttributeValue value) {
		Attribute attribute = attributes.get(attributeName);
		if (attribute == null) {
			throw new NullPointerException("Invalid Attribute " + attributeName + " in Target "
					+ this.type);
		}
		attribute.getValue().setValue(value);
	}
	public Boolean isSystem() {
		return (this.getAttribute("CLASS").equals("SYS"));
	}
	public Boolean isCard() {
		return (this.getAttribute("CLASS").equals("CARD") || this.getAttribute("CLASS").equals(
				"MOTHERBOARD"));
	}
	public Boolean isConnector() {
		return (this.getAttribute("CLASS").equals("CONNECTOR"));
	}
	public Boolean isNode() {
		return (this.getAttribute("CLASS").equals("ENC"));
	}

	/*
	public void writeCard(Writer out) throws Exception {
		Vector<Target> parts = new Vector<Target>();
		Vector<Target> connectors = new Vector<Target>();
		for (Target child : this.getChildren()) {
			if (child.getAttribute("CLASS").contains("CHIP")) {
				parts.add(child);
			}
			if (child.getAttribute("CLASS").contains("CONNECTOR")) {
				connectors.add(child);
			}
		}
		out.write("<card>\n");
		out.write("\t<id>" + this.getName() + "</id>\n");
		out.write("\t<target-id>" + this.getId() + "</target-id>\n");
		out.write("\t<card-type>" + this.getType() + "</card-type>\n");
		out.write("\t<description></description>\n");
		
		out.write("\t<parts-used>\n");
		for (Target part : parts) {
			out.write("\t\t<part-used>" + part.getType() + "</part-used>\n");
		}
		out.write("\t</parts-used>\n");
		out.write("\t<connectors-used>\n");
		for (Target conn : connectors) {
			out.write("\t\t<connector-used>" + conn.getType() + "</connector-used>\n");
		}
		out.write("\t</connectors-used>\n");
		
		out.write("\t<part-instances>\n");
		for (Target part : parts) {
				out.write("\t\t<part-instance>");
				out.write("<id>" + part.getName() + "</id>");
				out.write("<part-id>" + part.getType() + "</part-id>");
				out.write("<position>" + part.getPosition() + "</position>");
				out.write("</part-instance>\n");
		}
		out.write("\t</part-instances>\n");
		out.write("\t<connector-instances>\n");
		for (Target conn : connectors) {
				out.write("\t\t<connector-instance>");
				out.write("<id>" + conn.getName() + "</id>");
				out.write("<connector-id>" + conn.getType() + "</connector-id>");
				out.write("<position>" + conn.getPosition() + "</position>");
				out.write("</connector-instance>\n");
		}
		out.write("\t</connector-instances>\n");
		writeBusses(out);
		out.write("</card>\n");
		
	}
*/
	public void setAttributeValue(String attr, String value) {
		Attribute attribute = this.attributes.get(attr);
		if (attribute == null) {
			return;
		}
		attribute.getValue().setValue(value);
	}

	public void setSpecialAttributes() {
		this.setAttributeValue("POSITION", String.valueOf(this.getPosition()));

	}
/*
	public String toString() {
		String s = "TARGET: " + this.type;
		for (Map.Entry<String, Attribute> entry : this.getAttributes().entrySet()) {
			Attribute attr = new Attribute(entry.getValue());
			s = s + "\t" + attr.toString() + "\n";
		}
		return s;
	}
*/
	@Override
	public int compareTo(Target arg0) {
		Target t = (Target)arg0;
		return this.getType().compareTo(t.getType());
	}
	public Connection addConnection(Target busTarget,ConnectionEndpoint source,ConnectionEndpoint dest, boolean cabled) {
		if (busTarget==null || source==null || dest==null) { 
			//TODO: error message
			return null;
		}
		Vector<Connection> c = busses.get(busTarget);
		if (c==null) {
			c = new Vector<Connection>();
			busses.put(busTarget, c);
		}
		Connection conn = new Connection();
		conn.busType = busTarget.getType();
		conn.source=source;
		conn.dest=dest;
		conn.cabled = cabled;
		conn.busTarget = new Target(busTarget);
		c.add(conn);
		return conn;
	}
	public void deleteConnection(Target busTarget,Connection conn) {
		Vector<Connection> connList = busses.get(busTarget);
		connList.remove(conn);
	}
	public void initBusses(Vector<Target> v) {
		if (busInited) { return; }
		this.busInited=true;
		
		for (Target s : v) {
			Vector<Connection> connections = new Vector<Connection>();
			this.busses.put(s, connections);
		}
	}
	public void readModelXML(Element target, HashMap<String, Attribute> attrMap) {
		type = SystemModel.getElement(target, "id");
		parent = SystemModel.getElement(target, "parent");
		NodeList parentList = target.getElementsByTagName("parent_type");
		for (int i = 0; i < parentList.getLength(); i++) {
			Element e = (Element) parentList.item(i);
			parentType.add(e.getChildNodes().item(0).getNodeValue());
		}
		NodeList attributeList = target.getElementsByTagName("attribute");
		for (int i = 0; i < attributeList.getLength(); ++i) {
			String attrId = SystemModel.getElement((Element) attributeList.item(i), "id");
			Attribute attributeLookup = attrMap.get(attrId);
			if (attributeLookup == null) {
				throw new NullPointerException("Invalid attribute id: " + attrId + "(" + type + ")");
			}
			Attribute a = new Attribute(attributeLookup);
			if (a.value==null) {
				throw new NullPointerException("Unknown attribute value type: " + attrId + "(" + type + ")");
			}
			attributes.put(a.name, a);
			a.value.readInstanceXML((Element) attributeList.item(i));
		}
	}

	public void readInstanceXML(Element t, Vector<TargetName> children, TreeMap<String, Target> targetModels) throws Exception {
		//parentPath = SystemModel.getElement(t, "parent_path");
		name = SystemModel.getElement(t, "instance_name");
		type = SystemModel.getElement(t, "type");
		setPosition(SystemModel.getElement(t, "position"));
		NodeList childList = t.getElementsByTagName("child_id");
		for (int j = 0; j < childList.getLength(); ++j) {
			Element attr = (Element) childList.item(j);
			TargetName targetName = new TargetName(attr.getFirstChild().getNodeValue(),false);
			children.addElement(targetName);
		}				
		childList = t.getElementsByTagName("hidden_child_id");
		for (int j = 0; j < childList.getLength(); ++j) {
			Element attr = (Element) childList.item(j);
			TargetName targetName = new TargetName(attr.getFirstChild().getNodeValue(),true);
			children.addElement(targetName);
		}				

		NodeList attrList = t.getElementsByTagName("attribute");
		for (int j = 0; j < attrList.getLength(); ++j) {
			Element attr = (Element) attrList.item(j);
			String id = SystemModel.getElement(attr, "id");
			Attribute a = attributes.get(id);
			if (a==null) { 
				ServerWizard2.LOGGER.info("Attribute dropped: "+id+" from "+this.getName());
			} else {
				a.value.readInstanceXML(attr);
			}
		}
		NodeList busList = t.getElementsByTagName("bus");
		for (int j = 0; j < busList.getLength(); ++j) {
			Element bus = (Element) busList.item(j);
			String busType = SystemModel.getElement(bus, "bus_type");
			Connection conn = new Connection();
			conn.busType=busType;
			Target busTarget=targetModels.get(busType);
			if (busTarget==null) {
				throw new Exception("Invalid Bus Type "+busType+" for target "+this.getName());
			}
			conn.busTarget = new Target(busTarget);
			conn.readInstanceXML(bus);
			busses.get(busTarget).add(conn);
		}
	}
	public void writeInstanceXML(Writer out) throws Exception {
	
		out.write("<targetInstance>\n");
		out.write("\t<id>" + this.getName() + "</id>\n");
		out.write("\t<type>" + this.getType() + "</type>\n");
		out.write("\t<class>" + this.getAttribute("CLASS") + "</class>\n");
		if (!this.name.isEmpty()) {
			out.write("\t<instance_name>" + this.name + "</instance_name>\n");
		} else {
			out.write("\t<instance_name>" + this.getIdPrefix() + "</instance_name>\n");
		}
		
		out.write("\t<position>" + getPosition() + "</position>\n");
		//write children
		for (Target child : this.getChildren()) {
			if (child.isHidden()) {
				out.write("\t<hidden_child_id>"+child.getName()+"</hidden_child_id>\n");
			} else {
				out.write("\t<child_id>"+child.getName()+"</child_id>\n");
			}
		}
		//write attributes
		for (Map.Entry<String, Attribute> entry : getAttributes().entrySet()) {
			Attribute attr = new Attribute(entry.getValue());
			attr.writeInstanceXML(out);
			
		}
		//write busses
		for (Map.Entry<Target, Vector<Connection>> entry : busses.entrySet()) {
			for (Connection conn : entry.getValue()) {
				conn.writeInstanceXML(out);
			}
		}
		out.write("</targetInstance>\n");
	}

}
