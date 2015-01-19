package com.ibm.ServerWizard2;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SdrRecord {
	private String name = "";
	private String sdrName = "";
	private Byte sensorId = 0x00;
	private Byte entityId = 0x00;
	private Byte entityInstance = 0x00;
	private Target target = null;
	private String entityName = "";
		
	public void setTarget(Target target) {
		this.target=target;
	}
	public Target getTarget() {
		return target;
	}
	public String getName() {
		return name;
	}
	public String getSdrName() {
		return sdrName;
	}
	public Byte getSensorId() {
		return sensorId;
	}
	public Byte getEntityId() {
		return entityId;
	}
	public Byte getEntityInstance() {
		return entityInstance;
	}
	public void setEntityName(String entityName) {
		this.entityName=entityName;
	}
	public String getEntityName() {
		return this.entityName;
	}

	public void readXML(Element t) {
		name = SystemModel.getElement(t, "name");
		sensorId = Byte.decode(SystemModel.getElement(t, "sensor_id"));
		entityId = Byte.decode(SystemModel.getElement(t, "entity_id"));
		entityInstance = Byte.decode(SystemModel.getElement(t, "entity_instance"));
	}
	public String toString() {
		return "SDR: "+name+"; "+sdrName+"; SENSOR_NUM: "+sensorId+" ENT_ID: "+entityId+" ENT_INST: "+entityInstance;
	}
}
