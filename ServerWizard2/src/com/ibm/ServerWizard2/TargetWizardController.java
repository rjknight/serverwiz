package com.ibm.ServerWizard2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TargetWizardController {
	SystemModel model;
	MainDialog view;

	public TargetWizardController() {
	}

	public void init() {
		LibraryManager xmlLib = new LibraryManager();
		xmlLib.init();
		if (xmlLib.doUpdateCheck()) {
			xmlLib.update();
		}
		try {
			xmlLib.initModel(model);
			
			String parentTargetName = "sys-sys-power8";
			Target parentTarget = model.getTargetModels().get(parentTargetName);
			if (parentTarget == null) {
				throw new Exception("Parent model " + parentTargetName
						+ " is not valid");
			}
			// Create root instance
			Target sys = new Target(parentTarget);
			sys.setPosition(0);
			model.rootTarget = sys;
			model.addTarget(null, sys);
			updateTree();
			
			
		} catch (Exception e) {
			String btns[] = { "Close" };
			ServerWizard2.LOGGER.severe(e.getMessage());
			MessageDialog errDlg = new MessageDialog(view.getShell(), "Error",
					null, e.getMessage(), MessageDialog.ERROR, btns, 0);
			errDlg.open();
			e.printStackTrace();
			System.exit(4);
		}
		
	}
	public Target getTargetModel(String type) {
		return model.getTargetModel(type);
	}
	public void setView(MainDialog view) {
		this.view = view;
	}

	public void setModel(SystemModel model) {
		this.model = model;
	}

	public Vector<String> getEnums(String e) {
		if (model.enumerations.get(e)==null) {
			ServerWizard2.LOGGER.severe("Enum not found: "+e);
			return null;
		}
		return model.enumerations.get(e).enumList;
	}

	public void deleteTarget(Target target) {
		model.deleteTarget(target, model.rootTarget);
	}

	public void addTargetInstance(Target targetModel, Target parentTarget,
			TreeItem parentItem,String nameOverride) {
		Target targetInstance = new Target(targetModel);
		targetInstance.setName(nameOverride);
		model.updateTargetPosition(targetInstance, parentTarget, -1);
		model.addTarget(parentTarget, targetInstance);
		
		this.addChildTargetInstances(targetInstance);
		view.refreshTree(targetInstance, parentItem);
	}
	public Target copyTargetInstance(Target target, Target parentTarget,Boolean incrementPosition) {
		Target newTarget = new Target(target);
		if (incrementPosition) { 
			newTarget.setPosition(newTarget.getPosition()+1);
			newTarget.setSpecialAttributes();
		}
		model.addTarget(parentTarget, newTarget);

		for (Target child : target.getChildren()) {
			newTarget.addChild(child);
		}
		return newTarget;
	}
	public void deleteConnection(Target target,Target busTarget,Connection conn) {
		target.deleteConnection(busTarget,conn);
	}
	public Target getRootTarget() {
		return model.rootTarget;
	}
	public void updateTree() {
		view.refreshTree(model.rootTarget, null);
		view.refreshConnections(model.rootTarget);
	}

	public void writeXML(String filename) {
		try {
			String filename2=filename;
			if (filename.endsWith(".xml")) {
				filename2=filename2.substring(0,filename2.length()-4);
			}
			filename2=filename2+"_sw.xml";
			model.writeXML(filename);
		} catch (Exception exc) {
			MessageDialog.openError(null, "Error", exc.getMessage());
			exc.printStackTrace();
		}
	}

	public void readXML(String filename) {
		try {
			model.readXML(filename);
		} catch (Exception e) {
			MessageDialog.openError(null, "Error", e.getMessage());
			e.printStackTrace();
		}
		view.refreshTree(model.rootTarget, null);
	}
	public void importSDR(String filename) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		Vector<SdrRecord> sdrs = new Vector<SdrRecord>();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new XmlHandler());

			Document document = builder.parse(filename);

			NodeList deviceList = document
					.getElementsByTagName("device");

			for (int i = 0; i < deviceList.getLength(); ++i) {
				Element deviceElement = (Element) deviceList.item(i);
				SdrRecord s = new SdrRecord();
				s.readXML(deviceElement);
				model.updateTargetSdr(s);
				sdrs.add(s);
			}
		} catch (Exception e) {
			MessageDialog.openError(null, "SDR Import Error", e.getMessage());
			e.printStackTrace();
		}
		
		HashMap<Target,Vector<String>> ipmiAttr = new HashMap<Target,Vector<String>>();
		for (SdrRecord sdr : sdrs){
			Target t = sdr.getTarget();
			Vector<String> ipmiSensors = ipmiAttr.get(t);
			if (ipmiSensors==null) {
				ipmiSensors = new Vector<String>();
				ipmiAttr.put(t, ipmiSensors);
			}
			ipmiSensors.add(String.format("0x%02x", sdr.getEntityId())+","+
					String.format("0x%02x", sdr.getSensorId()));
		}
		for (Map.Entry<Target, Vector<String>> entry : ipmiAttr.entrySet()) {
			Target t=entry.getKey();
			String ipmiStr = "";
			Vector<String> attrs = entry.getValue();
			for (String a : attrs) {
				ipmiStr = ipmiStr+a+",";
			}
			for (int i=attrs.size();i<16;i++) {
				ipmiStr = ipmiStr+"0xFF,0xFF,";
			}
			t.setAttributeValue("IPMI_SENSORS", ipmiStr);
		}
	}
	public void loadAttributes(String filename) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		HashMap<String, Vector<Attribute>> loadedAttributes = new HashMap<String, Vector<Attribute>>();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new XmlHandler());

			Document document = builder.parse(filename);

			NodeList targetList = document
					.getElementsByTagName("targetInstance");

			for (int i = 0; i < targetList.getLength(); ++i) {
				Vector<Attribute> attrs = new Vector<Attribute>();

				Element t = (Element) targetList.item(i);

				// Update attribute values
				NodeList attrList = t.getElementsByTagName("attribute");
				String affinityPath = "";
				for (int j = 0; j < attrList.getLength(); ++j) {
					Element attrElement = (Element) attrList.item(j);
					String name = SystemModel.getElement(attrElement, "id");

					Attribute attr = new Attribute(model.attributes.get(name));
					attr.value.readInstanceXML(attrElement);
					attrs.add(attr);
					if (name.equals("PHYS_PATH")) {
						affinityPath = attr.value.getValue();
					}
				}
				loadedAttributes.put(affinityPath, attrs);
			}
		} catch (Exception e) {
			MessageDialog.openError(null, "Error", e.getMessage());
			e.printStackTrace();
		}
		updateAttributes(loadedAttributes, model.rootTarget);
		//view.refreshTree(model.getTargetTree(), model.rootTarget, null);

	}

	public void updateAttributes(
			HashMap<String, Vector<Attribute>> loadedAttributes, Target target) {
		String id = target.getAttribute("PHYS_PATH");

		Vector<Attribute> attrs = loadedAttributes.get(id);
		if (attrs != null) {
			for (int i = 0; i < attrs.size(); i++) {
				Attribute a = attrs.get(i);
				target.updateAttributeValue(a.name, a.value);
			}
		}
		//Vector<Target> children = model.getTargetTree().get(target);
		Vector<Target> children = target.getChildren();
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				Target childTarget = children.get(i);
				updateAttributes(loadedAttributes, childTarget);
			}
		}
	}

	public void addChildTargetInstances(Target target) {
		Vector<Target> v = model.getTargetInstances(target.getType());
		if (v == null) {
			return;
		}
		for (int i = 0; i < v.size(); i++) {
			Target t = v.get(i);
			Target unitTarget = new Target(t);
			model.addTarget(target, unitTarget);
			unitTarget.setSpecialAttributes();
			addChildTargetInstances(unitTarget);
		}
	}

	public Vector<Target> getChildTargets(Target target) {
		return model.getChildTargetTypes(target.getType());
	}
	public Vector<ConnectionEndpoint> getBusTypes(Target target,String type,Boolean isSource) {
		Vector<ConnectionEndpoint> v = new Vector<ConnectionEndpoint>();
		target.getBusTypes(v, type, "",isSource,true);
		return v;
	}
	
	public void clearAllTargets() {
		model.deleteAllInstances();
		String parentTargetName = "sys-sys-power8";
		Target parentTarget = model.getTargetModels().get(parentTargetName);
		// Create root instance
		Target sys = new Target(parentTarget);
		sys.setPosition(0);
		model.rootTarget = sys;
		//sys.setIdFromPosition(0, null);
		model.addTarget(null, sys);
		this.updateTree();
	}
	public void initBusses(Target target) {
		model.initBusses(target);
	}
	public void runChecks(String filename) {
		String includePath = LibraryManager.getWorkingDir()+"scripts";
		String script = LibraryManager.getWorkingDir()+"scripts"+System.getProperty("file.separator")+"processMrw.pl";
		
		String commandLine[] = {
				"perl",
				"-I",
				includePath,
				script,
				"-x",
				filename,
				"-f"
		};
		String commandLineStr="";
		for (int i=0;i<commandLine.length;i++) {
			commandLineStr=commandLineStr+commandLine[i]+" ";
		}
		ServerWizard2.LOGGER.info("Running: "+commandLineStr);
		String line;
		String msg="";
		int numLines=0;
		try {
			Process proc = Runtime.getRuntime().exec(commandLine);
			proc.waitFor();
			InputStream error = proc.getErrorStream();
			InputStream stdout = proc.getInputStream();
			BufferedReader reader = new BufferedReader (new InputStreamReader(error));
			BufferedReader reader2 = new BufferedReader (new InputStreamReader(stdout));
			while ((line = reader.readLine ()) != null) {
				ServerWizard2.LOGGER.severe("ERROR: " + line);
			}
			while ((line = reader2.readLine ()) != null) {
				if (numLines<15) {
					msg=msg+line+"\n";
				}
				if (numLines==15) {
					msg=msg+"\nToo many errors.  Output truncated.  Check stdout.\n";
				}
				ServerWizard2.LOGGER.info(line);
				numLines++;
			}
			MessageDialog.openInformation(null, "Check Output", msg);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}