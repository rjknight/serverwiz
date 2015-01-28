package com.ibm.ServerWizard2;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SystemModel {
	public Target rootTarget;
	private DocumentBuilder builder;
	private Integer nextId = 0;
	private HashMap<String, Vector<Target>> targetInstances = new HashMap<String, Vector<Target>>();
	private TreeMap<String, Target> targetModels = new TreeMap<String, Target>();
	public HashMap<String, Enumerator> enumerations = new HashMap<String, Enumerator>();
	public HashMap<String, Attribute> attributes = new HashMap<String, Attribute>();
	private Vector<Target> targetList = new Vector<Target>();
	public HashMap<String, Vector<Target>> childTargetTypes = new HashMap<String, Vector<Target>>();
	private Vector<Target> busTypes = new Vector<Target>();
	private PropertyChangeSupport changes = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener l) {
		changes.addPropertyChangeListener(l);
	}

	public void updateTargetSdr(SdrRecord sdr) throws Exception {
		// Find target that matches sdr entity id and entity instance
		ServerWizard2.LOGGER.info("Looking for matching target: " + sdr.toString());
		Boolean imported = false;
		for (Target target : targetList) {
			String strEntityId = target.getAttribute("ENTITY_ID");
			int entityInst = target.getPosition();
			if (!strEntityId.isEmpty()) {
				String ids[] = strEntityId.split(",");
				for (int i = 0; i < ids.length; i++) {
					Integer entityId = getEnumValue("ENTITY_ID", ids[i]);
					if (entityId == null) {
						String msg = ids[i] + " is invalid for target: " + target.getName();
						ServerWizard2.LOGGER.severe(msg);
						throw new Exception(msg);
					} else {
						if (entityId == (int) sdr.getEntityId()
								&& entityInst == sdr.getEntityInstance()) {
							ServerWizard2.LOGGER.info("SDRImport: Target=" + target.getName()
									+ "; EntityID=" + entityId + "; EntityInst=" + entityInst);
							imported = true;
							sdr.setTarget(target);
							sdr.setEntityName(ids[i]);
						}
					}
				}
			}
		}
		if (!imported) {
			String msg = sdr.toString() + "was not imported and is a virtual sensor.";
			ServerWizard2.LOGGER.severe(msg);
			throw new Exception(msg);
		}
	}

	public void initBusses(Target target) {
		target.initBusses(busTypes);
	}

	public Vector<Target> getChildTargetTypes(String targetType) {
		if (childTargetTypes.get(targetType) != null) {
			Collections.sort(childTargetTypes.get(targetType));
		}
		return childTargetTypes.get(targetType);
	}

	public Integer getEnumValue(String enumerator, String value) {
		Enumerator e = enumerations.get(enumerator);
		return e.getEnumInt(value);
	}

	public String getEnumValueStr(String enumerator, String value) {
		Enumerator e = enumerations.get(enumerator);
		return e.getEnumStr(value);
	}

	public static String getElement(Element a, String e) {
		Node n = a.getElementsByTagName(e).item(0);
		if (n != null) {
			Node cn = n.getChildNodes().item(0);
			if (cn == null) {
				return "";
			}
			return n.getChildNodes().item(0).getNodeValue();
		}
		return "";
	}

	public static Boolean isElementDefined(Element a, String e) {
		Node n = a.getElementsByTagName(e).item(0);
		if (n != null) {
			Node cn = n.getChildNodes().item(0);
			if (cn == null) {
				return true;
			}
			return true;
		}
		return false;
	}

	public TreeMap<String, Target> getTargetModels() {
		return targetModels;
	}

	public Target getTargetModel(String t) {
		return targetModels.get(t);
	}

	public void deleteAllInstances() {
		targetList.clear();
		// busses.clear();
	}

	public void deleteTarget(Target deleteTarget, Target currentTarget) {
		if (currentTarget == deleteTarget) {
			// targetInstanceTree.remove(deleteTarget);
			currentTarget.getChildren().remove(deleteTarget);
			return;
		}
		// Vector<Target> targetList = targetInstanceTree.get(currentTarget);
		Vector<Target> targetList = currentTarget.getChildren();
		if (targetList == null) {
			return;
		}
		for (int i = targetList.size() - 1; i >= 0; i--) {
			Target t = targetList.get(i);
			if (t == deleteTarget) {
				targetList.remove(i);
			} else {
				deleteTarget(deleteTarget, t);
			}
		}
		changes.firePropertyChange("DELETE_TARGET", "", "");
	}

	// Reads a previously saved MRW
	public void readXML(String filename) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// delete all existing instances
		this.deleteAllInstances();

		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new XmlHandler());
		Document document = builder.parse(filename);
		HashMap<Target, Vector<TargetName>> childrenLookup = new HashMap<Target, Vector<TargetName>>();
		HashMap<String, Target> targetLookup = new HashMap<String, Target>();

		NodeList targetInstanceList = document.getElementsByTagName("targetInstance");
		for (int i = 0; i < targetInstanceList.getLength(); ++i) {
			Element t = (Element) targetInstanceList.item(i);
			String type = SystemModel.getElement(t, "type");
			if (type.length() > 0) {
				Target targetModel = this.getTargetModel(type);
				if (targetModel == null) {
					ServerWizard2.LOGGER.severe("Invalid target type: " + type);
					throw new Exception("Invalid target type: " + type);
				} else {
					Target target = new Target(targetModel);
					target.initBusses(busTypes);
					Vector<TargetName> children = new Vector<TargetName>();
					childrenLookup.put(target, children);
					target.readInstanceXML(t, children, targetModels);
					targetLookup.put(target.getName(), target);
					targetList.add(target);
					if (target.getAttribute("CLASS").equals("SYS")) {
						this.rootTarget = target;
					}
				}
			} else {
				throw new Exception("Empty Target Type");
			}
		}
		for (Target target : targetList) {
			// TODO: error checking
			// add children targets to parent
			Vector<TargetName> c = childrenLookup.get(target);
			for (TargetName child : c) {
				Target childTarget = targetLookup.get(child.name);
				if (childTarget == null) {
					throw new Exception("Child Target " + child.name + " not found");
				}
				childTarget.hide(child.hidden);
				target.addChild(childTarget);
			}

			// add source and destination targets for connections from path id
			// that is in xml
			for (Map.Entry<Target, Vector<Connection>> entry : target.getBusses().entrySet()) {
				for (Connection conn : entry.getValue()) {
					Target t = targetLookup.get(conn.source.getTargetName());
					if (t == null) {
						throw new Exception("Invalid connector target: "
								+ conn.source.getTargetName());
					}
					conn.source.setTarget(t);
					t = targetLookup.get(conn.dest.getTargetName());
					if (t == null) {
						throw new Exception("Invalid connector target: "
								+ conn.dest.getTargetName());
					}
					conn.dest.setTarget(t);
				}
			}
		}
	}

	public void writeEnumeration(Writer out) throws Exception {
		for (String enumeration : enumerations.keySet()) {
			Enumerator e = enumerations.get(enumeration);
			out.write("<enumerationType>\n");
			out.write("\t<id>" + enumeration + "</id>\n");
			for (Map.Entry<String, String> entry : e.enumValues.entrySet()) {
				out.write("\t\t<enumerator>\n");
				out.write("\t\t<name>" + entry.getKey() + "</name>\n");
				out.write("\t\t<value>" + entry.getValue() + "</value>\n");
				out.write("\t\t</enumerator>\n");
			}
			out.write("</enumerationType>\n");
		}
	}

	// Writes MRW to file
	public void writeXML(String filename) throws Exception {
		Writer out = new BufferedWriter(new FileWriter(filename));
		out.write("<targetInstances>\n");
		out.write("<version>" + ServerWizard2.VERSION + "</version>\n");
		writeEnumeration(out);
		// writeEnumeration(out, "CLASS");
		// writeEnumeration(out, "MRU_PREFIX");

		for (Target target : targetList) {
			target.writeInstanceXML(out);
		}
		out.write("</targetInstances>\n");
		out.close();
		ServerWizard2.LOGGER.info(filename + " Saved");
	}

	public void addTarget(Target parentTarget, Target newTarget) {
		newTarget.setId(nextId);
		nextId++;
		if (parentTarget == null) {

		} else {
			parentTarget.addChild(newTarget);
		}
		targetList.add(newTarget);
		initBusses(newTarget);
		changes.firePropertyChange("ADD_TARGET", "", "");
	}

	public Vector<Target> getTargetInstances(String n) {
		// TODO: add error checking
		return targetInstances.get(n);
	}

	public void addParentAttributes(Target childTarget, Target t) {
		if (t == null) {
			return;
		}
		Target parent = targetModels.get(t.parent);
		if (parent == null) {
			return;
		}
		childTarget.copyAttributesFromParent(parent);
		addParentAttributes(childTarget, parent);
	}

	public void loadTargetTypes(DefaultHandler errorHandler, String fileName) throws SAXException,
			IOException, ParserConfigurationException {
		ServerWizard2.LOGGER.info("Loading Target Types: " + fileName);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
		builder.setErrorHandler(errorHandler);

		Document document = builder.parse(fileName);
		NodeList targetList = document.getElementsByTagName("targetType");
		for (int i = 0; i < targetList.getLength(); ++i) {
			Element t = (Element) targetList.item(i);
			Target target = new Target();
			target.readModelXML(t, attributes);
			targetModels.put(target.getType(), target);
			Vector<String> parentTypes = target.getParentType();
			for (int j = 0; j < parentTypes.size(); j++) {
				String parentType = parentTypes.get(j);
				Vector<Target> childTypes = childTargetTypes.get(parentType);
				if (childTypes == null) {
					childTypes = new Vector<Target>();
					childTargetTypes.put(parentType, childTypes);
				}
				childTypes.add(target);
			}
		}
		for (Map.Entry<String, Target> entry : targetModels.entrySet()) {
			Target target = entry.getValue();

			// add inherited attributes
			addParentAttributes(target, target);
			if (target.getAttribute("CLASS").equals("BUS")) {
				busTypes.add(target);
			}
		}
	}

	public void loadAttributes(DefaultHandler errorHandler, String fileName) throws SAXException,
			IOException, ParserConfigurationException {
		ServerWizard2.LOGGER.info("Loading Attributes: " + fileName);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
		builder.setErrorHandler(errorHandler);

		Document document = builder.parse(fileName);
		NodeList enumList = document.getElementsByTagName("enumerationType");
		for (int i = 0; i < enumList.getLength(); ++i) {
			Element t = (Element) enumList.item(i);
			Enumerator en = new Enumerator();
			en.readXML(t);
			enumerations.put(en.id, en);
		}
		NodeList attrList = document.getElementsByTagName("attribute");
		for (int i = 0; i < attrList.getLength(); ++i) {
			Element t = (Element) attrList.item(i);
			Attribute a = new Attribute();
			a.readModelXML(t);
			attributes.put(a.name, a);

			if (a.getValue().getType().equals("enumeration")) {
				a.getValue().setEnumerator(enumerations.get(a.name));
			}
		}
	}

	public void loadTargetInstances(DefaultHandler errorHandler, String fileName)
			throws SAXException, IOException, ParserConfigurationException {
		ServerWizard2.LOGGER.info("Loading Target Instances: " + fileName);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
		builder.setErrorHandler(errorHandler);

		Document document = builder.parse(fileName);
		HashMap<String, Vector<TargetName>> children = new HashMap<String, Vector<TargetName>>();
		HashMap<String, Target> instanceLookup = new HashMap<String, Target>();
		NodeList targetInstanceList = document.getElementsByTagName("targetInstance");
		for (int i = 0; i < targetInstanceList.getLength(); ++i) {
			Element t = (Element) targetInstanceList.item(i);
			// TODO: error checking
			String targetType = SystemModel.getElement(t, "type");
			String instanceId = SystemModel.getElement(t, "id");

			Target modelTarget = targetModels.get(targetType);
			if (modelTarget == null) {
				throw new IOException("Target type " + targetType + " not valid");
			}
			Target target = new Target(modelTarget);
			// target.setPosition(-1);
			target.setName(instanceId);
			instanceLookup.put(instanceId, target);

			// load children
			NodeList childList = t.getElementsByTagName("child_id");
			for (int j = 0; j < childList.getLength(); ++j) {
				// found child
				Vector<TargetName> v = children.get(targetType);
				// Vector<TargetName> v = children.get(instanceId);
				if (v == null) {
					v = new Vector<TargetName>();
					children.put(targetType, v);
					// children.put(instanceId, v);
				}
				Element c = (Element) childList.item(j);
				String childInstanceId = c.getFirstChild().getNodeValue();
				TargetName tn = new TargetName(childInstanceId, false);
				v.add(tn);
			}
			// load children
			childList = t.getElementsByTagName("hidden_child_id");
			for (int j = 0; j < childList.getLength(); ++j) {
				// found child
				Vector<TargetName> v = children.get(targetType);
				// Vector<TargetName> v = children.get(instanceId);
				if (v == null) {
					v = new Vector<TargetName>();
					children.put(targetType, v);
					// children.put(instanceId, v);
				}
				Element c = (Element) childList.item(j);
				String childInstanceId = c.getFirstChild().getNodeValue();
				TargetName tn = new TargetName(childInstanceId, true);
				v.add(tn);
			}

			// update attribute values
			NodeList attrList = t.getElementsByTagName("attribute");
			for (int j = 0; j < attrList.getLength(); ++j) {
				Element attr = (Element) attrList.item(j);
				String attributeId = SystemModel.getElement(attr, "id");
				String attributeValue = SystemModel.getElement(attr, "default");
				target.updateAttributeValue(attributeId, attributeValue);
			}
		}
		for (String targetType : children.keySet()) {
			Vector<Target> childTargets = new Vector<Target>();
			targetInstances.put(targetType, childTargets);
			for (TargetName instanceId : children.get(targetType)) {
				Target child = instanceLookup.get(instanceId.name);

				if (child == null) {
					throw new IOException("Child target not found: " + instanceId.name);
				}
				child.hide(instanceId.hidden);
				childTargets.add(child);
			}
		}
	}

	public void updateTargetPosition(Target target, Target parentTarget, int position) {
		if (position > 0) {
			target.setPosition(position);
			return;
		}
		int p = -1;
		// set target position to +1 of any target found of same type
		for (int i = 0; i < targetList.size(); i++) {
			Target t = targetList.get(i);
			if (t.getType().equals(target.getType())) {
				if (t.getPosition() >= p) {
					p = t.getPosition();
				}
			}
		}
		target.setPosition(p + 1);
		target.setSpecialAttributes();
	}
}
