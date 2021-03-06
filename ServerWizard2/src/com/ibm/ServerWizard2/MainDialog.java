package com.ibm.ServerWizard2;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wb.swt.SWTResourceManager;

public class MainDialog extends Dialog {
	
	private TableViewer viewer;
	private Tree tree;
	private Tree treeBus;
	private Text txtInstanceName;
	private Combo combo;
	private Combo comboSource;
	private Combo comboDest;
	Composite container;

	TargetWizardController controller;
	int selectedNode = 0;
	int encNum = 0;
	Target targetClipboard;

	// Buttons
	Button btnAddTargetButton;
	Button btnAddConnection;
	Button btnAddCable;
	Button btnCopyInstance;
	Button btnDeleteTarget;
	Button btnSaveXml;
	Button btnLoadXml;
	private Button btnDeleteConnection;
	private Button btnSaveAs;

	// document state
	private Boolean dirty = false;
	public String mrwFilename = "";

	Vector<Widget> widgets = new Vector<Widget>();
	private Button btnRunChecks;
	private SashForm sashForm;
	private SashForm sashForm_1;

	private Composite compositeBus;
	private Label lblInstanceType;
	private Composite compositeInstance;
	private Composite composite;
	private Label lblNewLabel;

	//private TableEditor editor;
	private Vector<Field> attributes;
	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public MainDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.BORDER | SWT.MIN | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("ServerWiz2");
	}

	public void setController(TargetWizardController t) {
		controller = t;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		container = (Composite) super.createDialogArea(parent);
		container.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		//tableEditor = new Vector<TableEditor>();
		container.setLayout(new GridLayout(1, false));

		lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_LINK_FOREGROUND));
		lblNewLabel
				.setText("To start a new system, click on 'sys-0' instance, then chose instance type, then click 'Add Instance' button");

		composite = new Composite(container, SWT.NONE);
		RowLayout rl_composite = new RowLayout(SWT.HORIZONTAL);
		rl_composite.wrap = false;
		rl_composite.fill = true;
		composite.setLayout(rl_composite);
		GridData gd_composite = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gd_composite.widthHint = 918;
		gd_composite.heightHint = 107;
		composite.setLayoutData(gd_composite);

		compositeInstance = new Composite(composite, SWT.BORDER);
		compositeInstance.setLayoutData(new RowData(402, SWT.DEFAULT));
		compositeInstance.setLayout(new GridLayout(3, false));

		lblInstanceType = new Label(compositeInstance, SWT.NONE);
		lblInstanceType.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblInstanceType.setText("Instance Type:");
		lblInstanceType.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		combo = new Combo(compositeInstance, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_combo.widthHint = 167;
		combo.setLayoutData(gd_combo);
		combo.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		btnAddTargetButton = new Button(compositeInstance, SWT.NONE);
		btnAddTargetButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAddTargetButton.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnAddTargetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Target chk = (Target) combo.getData(combo.getText());
				if (chk != null) {
					TreeItem selectedItem = tree.getSelection()[0];
					Target parentTarget = (Target) selectedItem.getData();
					String nameOverride = txtInstanceName.getText();
					controller.addTargetInstance(chk, parentTarget, selectedItem, nameOverride);
					txtInstanceName.setText("");
					selectedItem.setExpanded(true);
					setDirtyState(true);
				}
			}
		});
		btnAddTargetButton.setText("Add Instance");
		btnAddTargetButton.setEnabled(false);

		Label lblName = new Label(compositeInstance, SWT.NONE);
		lblName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblName.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		lblName.setText("Custom Name:");

		txtInstanceName = new Text(compositeInstance, SWT.BORDER);
		GridData gd_txtInstanceName = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_txtInstanceName.widthHint = 175;
		txtInstanceName.setLayoutData(gd_txtInstanceName);
		txtInstanceName.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		compositeBus = new Composite(composite, SWT.BORDER);
		compositeBus.setLayoutData(new RowData(420, SWT.DEFAULT));

		btnDeleteTarget = new Button(compositeInstance, SWT.NONE);
		btnDeleteTarget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDeleteTarget.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnDeleteTarget.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem treeitem = tree.getSelection()[0];
				TreeItem parentItem = treeitem.getParentItem();
				controller.deleteTarget((Target) treeitem.getData());
				// clearTreeAll();
				// controller.updateTree();
				treeitem.clearAll(true);
				treeitem.dispose();
				Target target = (Target) parentItem.getData();
				TreeItem parentParent = parentItem.getParentItem();
				parentItem.clearAll(true);
				parentItem.dispose();
				refreshTree(target, parentParent);
				setDirtyState(true);
			}
		});
		btnDeleteTarget.setText("Delete Instance");
		new Label(compositeInstance, SWT.NONE);

		btnCopyInstance = new Button(compositeInstance, SWT.NONE);
		btnCopyInstance.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		btnCopyInstance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TreeItem selectedItem = tree.getSelection()[0];
				if (selectedItem == null) {
					return;
				}
				Target target = (Target) selectedItem.getData();
				Target parentTarget = (Target) selectedItem.getParentItem().getData();
				Target newTarget = controller.copyTargetInstance(target, parentTarget, true);
				// clearTreeAll();
				// refreshTree(controller.getRootTarget(), null);
				refreshTree(newTarget, selectedItem.getParentItem());
				TreeItem t = selectedItem.getParentItem();
				tree.select(t.getItem(t.getItemCount() - 1));
				setDirtyState(true);
			}
		});
		btnCopyInstance.setText("Copy Node or Connector");
		btnCopyInstance.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnCopyInstance.setEnabled(false);
		new Label(compositeInstance, SWT.NONE);
		compositeBus.setLayout(new GridLayout(3, false));

		Label lblSource = new Label(compositeBus, SWT.NONE);
		lblSource.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSource.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		lblSource.setText("Source:");

		comboSource = new Combo(compositeBus, SWT.NONE);
		GridData gd_comboSource = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_comboSource.widthHint = 182;
		comboSource.setLayoutData(gd_comboSource);
		comboSource.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		btnAddConnection = new Button(compositeBus, SWT.PUSH);
		btnAddConnection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		// btnAddConnection.setBounds(20, 72, 100, 25);
		// btnAddConnection.setVisible(true);
		btnAddConnection.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnAddConnection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addConnection(false);
			}
		});
		btnAddConnection.setText("Add Connection");

		Label lblDestination = new Label(compositeBus, SWT.NONE);
		lblDestination.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDestination.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		lblDestination.setText("Destination:");

		comboDest = new Combo(compositeBus, SWT.NONE);
		GridData gd_comboDest = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_comboDest.widthHint = 181;
		comboDest.setLayoutData(gd_comboDest);
		comboDest.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		btnAddCable = new Button(compositeBus, SWT.NONE);
		btnAddCable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAddCable.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnAddCable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addConnection(true);
			}
		});
		btnAddCable.setText("Add Cable");
		new Label(compositeBus, SWT.NONE);
		new Label(compositeBus, SWT.NONE);

		btnDeleteConnection = new Button(compositeBus, SWT.NONE);
		btnDeleteConnection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDeleteConnection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				deleteConnection();
			}
		});
		btnDeleteConnection.setText("Delete Connection");
		btnDeleteConnection.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		sashForm_1 = new SashForm(container, SWT.VERTICAL);
		GridData gd_sashForm_1 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_sashForm_1.heightHint = 375;
		gd_sashForm_1.widthHint = 712;
		sashForm_1.setLayoutData(gd_sashForm_1);

		sashForm = new SashForm(sashForm_1, SWT.NONE);

		tree = new Tree(sashForm, SWT.BORDER);
		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateView();
			}
		});
		tree.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent arg0) {
				updateView();
			}
		});
		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				if (tree.getColumnCount() > 0) {
					tree.getColumn(0).setWidth(tree.getSize().x - 25);
				}
			}
		});
		tree.setToolTipText("To add an instance\r\n- click on parent\r\n- choose an instance type\r\n- click add instance");
		tree.setHeaderVisible(true);
		tree.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		treeBus = new Tree(sashForm, SWT.BORDER);
		treeBus.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		treeBus.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				if (treeBus.getColumnCount() > 0) {
					treeBus.getColumn(0).setWidth(treeBus.getSize().x - 25);
				}
			}
		});
		treeBus.setToolTipText("To add a connection, \r\nclick on card then bus type");
		treeBus.setHeaderVisible(true);

		sashForm.setWeights(new int[] { 224, 610 });
		treeBus.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateConnectionCombos();
			}
		});
		
		
		//////////////////////////////////////////
		// Create attribute table
		
		viewer = new TableViewer(sashForm_1, SWT.VIRTUAL | SWT.H_SCROLL
			      | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);


		Table table = viewer.getTable();
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		  table.addListener (SWT.CHANGED, new Listener () {
		      public void handleEvent (Event event) {
		    	  //System.out.println("set dirty");
		    	  setDirtyState(true);
		      }
		  }); 
			
		final TableViewerColumn colName = new TableViewerColumn(viewer, SWT.NONE);
		colName.getColumn().setWidth(256);
		colName.getColumn().setText("Attribute");
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Field f = (Field) element;
				return f.attributeName;
			}
		});
				
		final TableViewerColumn colField = new TableViewerColumn(viewer, SWT.NONE);
		colField.getColumn().setWidth(100);
		colField.getColumn().setText("Field");
		colField.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Field f = (Field) element;
				if (f.attributeName.equals(f.name)) {
					return "";
				}
				return f.name;
			}
		});
				
		final TableViewerColumn colValue = new TableViewerColumn(viewer, SWT.NONE);
		colValue.getColumn().setWidth(100);
		colValue.getColumn().setText("Value");
		colValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Field f = (Field) element;
				return f.value;
			}
		});
		colValue.setEditingSupport(new AttributeEditingSupport(viewer));
		
		final TableViewerColumn colDesc = new TableViewerColumn(viewer, SWT.NONE);
		colDesc.getColumn().setWidth(350);
		colDesc.getColumn().setText("Description");
		colDesc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Field f = (Field) element;
				return f.desc;
			}
		});
				
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		attributes = new Vector<Field>();
		viewer.setInput(attributes); 

		
		sashForm_1.setWeights(new int[] { 1, 1 });
		controller.init();

		this.setDirtyState(false);
		updateView();

		// load file if passed on command line
		if (!mrwFilename.isEmpty()) {
			ServerWizard2.LOGGER.info("Loading MRW: " + mrwFilename);
			clearTreeAll();
			controller.readXML(mrwFilename);
			setFilename(mrwFilename);
		}
		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		parent.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		// @SuppressWarnings("unused")
		Button btnClearAll = createButton(parent, IDialogConstants.NO_ID, "New", false);
		btnClearAll.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnClearAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dirty) {
					if (!MessageDialog.openConfirm(null, "Save Resource", mrwFilename
							+ "has been modified. Ignore changes?")) {
						return;
					}
					ServerWizard2.LOGGER.info("Discarding changes");
				}
				clearTreeAll();
				controller.clearAllTargets();
				setFilename("");
				setDirtyState(false);
			}
		});

		btnLoadXml = createButton(parent, IDialogConstants.NO_ID, "Open", false);
		btnLoadXml.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnLoadXml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dirty) {
					if (!MessageDialog.openConfirm(null, "Save Resource", mrwFilename
							+ "has been modified. Ignore changes?")) {
						return;
					}
					ServerWizard2.LOGGER.info("Discarding changes");
				}
				Button b = (Button) e.getSource();
				FileDialog fdlg = new FileDialog(b.getShell(), SWT.OPEN);
				String ext[] = { "*.xml" };
				fdlg.setFilterExtensions(ext);
				String filename = fdlg.open();
				if (filename == null) {
					return;
				}
				clearTreeAll();
				controller.readXML(filename);
				setFilename(filename);
				setDirtyState(false);
			}
		});
		btnLoadXml.setToolTipText("Loads XML from file");

		btnSaveXml = createButton(parent, IDialogConstants.NO_ID, "Save", false);
		btnSaveXml.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnSaveXml.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String filename = mrwFilename;
				if (mrwFilename.isEmpty()) {
					Button b = (Button) e.getSource();
					FileDialog fdlg = new FileDialog(b.getShell(), SWT.SAVE);
					String ext[] = { "*.xml" };
					fdlg.setFilterExtensions(ext);
					fdlg.setOverwrite(true);
					filename = fdlg.open();
					if (filename == null) {
						return;
					}
				}
				controller.writeXML(filename);
				setFilename(filename);
				setDirtyState(false);
			}
		});
		btnSaveXml.setText("Save");

		btnSaveAs = createButton(parent, IDialogConstants.NO_ID, "Save As...", false);
		btnSaveAs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.getSource();
				FileDialog fdlg = new FileDialog(b.getShell(), SWT.SAVE);
				String ext[] = { "*.xml" };
				fdlg.setFilterExtensions(ext);
				fdlg.setOverwrite(true);
				String filename = fdlg.open();
				if (filename == null) {
					return;
				}
				controller.writeXML(filename);
				setFilename(filename);
				setDirtyState(false);
			}
		});

		btnSaveAs.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnSaveAs.setEnabled(true);

		Button btnImportSDR = createButton(parent, IDialogConstants.NO_ID, "Import SDR", false);
		btnImportSDR.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.getSource();
				FileDialog fdlg = new FileDialog(b.getShell(), SWT.OPEN);
				String ext[] = { "*.xml" };
				fdlg.setFilterExtensions(ext);
				String filename = fdlg.open();
				if (filename == null) {
					return;
				}
				controller.importSDR(filename);
				setDirtyState(true);
			}
		});

		btnImportSDR.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnImportSDR.setEnabled(true);

		btnRunChecks = createButton(parent, IDialogConstants.NO_ID, "Run Checks", false);
		btnRunChecks.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String tempFile = System.getProperty("java.io.tmpdir")
						+ System.getProperty("file.separator") + "~temp.xml";
				controller.writeXML(tempFile);
				controller.runChecks(tempFile);
			}
		});
		btnRunChecks.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));

		Button btnSpacer = createButton(parent, IDialogConstants.NO_ID, "Spacer", false);
		btnSpacer.setVisible(false);

		Button btnForceUpdate = createButton(parent, IDialogConstants.NO_ID, "Force Update", false);
		btnForceUpdate.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnForceUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String workingDir = LibraryManager.getWorkingDir();
				String updateFilename = workingDir + "serverwiz2.update";
				File updateFile = new File(updateFilename);
				if (updateFile.delete()) {
					MessageDialog.openInformation(null, "Force Update",
							"Update will occur upon restart...");
					ServerWizard2.LOGGER.info("Removing update file to force update upon restart.");
				} else {
					MessageDialog.openError(null, "Error",
							"Unable to delete serverwiz2.update.  Try deleting manually.");
					ServerWizard2.LOGGER.severe("Unable to delete serverwiz2.update.");
				}
			}
		});

		Button btnExit = createButton(parent, IDialogConstants.CLOSE_ID, "Exit", false);
		btnExit.setFont(SWTResourceManager.getFont("Arial", 9, SWT.NORMAL));
		btnExit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.getSource();
				b.getShell().close();
			}
		});
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(948, 796);
	}

	// ////////////////////////////////////////////////////
	// Utility helpers

	private Target getSelectedTarget() {
		TreeItem t[] = tree.getSelection();
		if (t.length > 0) {
			Target targetInstance = (Target) t[0].getData();
			return targetInstance;
		}
		return null;
	}

	private void updateChildCombo(Target targetInstance) {
		btnAddTargetButton.setEnabled(false);
		Vector<Target> v = controller.getChildTargets(targetInstance);

		combo.removeAll();
		if (v != null) {
			for (int i = 0; i < v.size(); i++) {
				combo.add(v.get(i).getType());
				combo.setData(v.get(i).getType(), v.get(i));
				if (i == 0) {
					combo.select(0);
				}
			}
			btnAddTargetButton.setEnabled(true);
		}
	}

	private void updateConnectionCombos() {
		btnAddConnection.setEnabled(false);
		btnAddCable.setEnabled(false);
		btnDeleteConnection.setEnabled(false);
		comboSource.removeAll();
		comboDest.removeAll();
		Target selectedTarget = getSelection(tree);

		if (selectedTarget == null || treeBus.getSelectionCount() == 0) {
			return;
		}
		if (treeBus.getSelection()[0].getData() instanceof Connection) {
			btnDeleteConnection.setEnabled(true);
		}
		if (selectedTarget.isConnector()) {
			return;
		}

		String bus = treeBus.getSelection()[0].getText();

		Vector<ConnectionEndpoint> s = controller.getBusTypes(selectedTarget, bus, true);
		Vector<ConnectionEndpoint> d = controller.getBusTypes(selectedTarget, bus, false);

		if (s.size() == 0 || d.size() == 0) {
			// bus connection selected, show attributes
			clearTable();
			if (treeBus.getSelection()[0].getData() instanceof Connection) {
				Connection conn = (Connection) treeBus.getSelection()[0].getData();
				updateAttributes(conn.busTarget);
			}
		} else {
			for (int i = 0; i < s.size(); i++) {
				String sch = s.get(i).getTarget().getAttribute("SCHEMATIC_INTERFACE");
				String cStr = s.get(i).getName();
				if (!sch.isEmpty()) {
					cStr = cStr + " (" + sch + ")";
				}
				comboSource.add(cStr);
				comboSource.setData(cStr, s.get(i));
				if (i == 0) {
					comboSource.select(0);
				}
			}
			for (int i = 0; i < d.size(); i++) {
				String sch = d.get(i).getTarget().getAttribute("SCHEMATIC_INTERFACE");
				String cStr = d.get(i).getName();
				if (!sch.isEmpty()) {
					cStr = cStr + " (" + sch + ")";
				}

				comboDest.add(cStr);
				comboDest.setData(cStr, d.get(i));
				if (i == 0) {
					comboDest.select(0);
				}
			}
			btnAddConnection.setEnabled(true);
			btnAddCable.setEnabled(true);
		}
	}

	private void clearTable() {
		
	}

	public void updateView() {
		Target targetInstance = getSelectedTarget();
		if (targetInstance == null) {
			btnDeleteTarget.setEnabled(false);
			btnCopyInstance.setEnabled(false);
			updateConnectionCombos();
			return;
		}
		updateChildCombo(targetInstance);
		clearTable();
		updateAttributes(targetInstance);
		updateConnectionCombos();
		if (targetInstance.isSystem()) {
			btnDeleteTarget.setEnabled(false);
		} else {
			btnDeleteTarget.setEnabled(true);
		}
		if (targetInstance.isNode() || targetInstance.isConnector()) {
			btnCopyInstance.setEnabled(true);
		} else {
			btnCopyInstance.setEnabled(false);
		}
		refreshConnections(targetInstance);
		updateConnectionCombos();
	}
	private void updateAttributes(Target targetInstance) {
		attributes.clear();
		for (Map.Entry<String, Attribute> entry : targetInstance.getAttributes().entrySet()) {

			Attribute attribute = entry.getValue();
			Boolean showAttribute = !attribute.hide;
		
			if (showAttribute) {
				for (Field field : attribute.getValue().getFields())
				attributes.add(field);
			}
		}
		viewer.refresh();
	}

	public void clearTreeAll() {
		if (tree.getColumnCount() > 0) {
			tree.getColumn(0).dispose();
		}
		clearTree(tree.getItem(0));
		tree.removeAll();
	}

	public void clearBusTreeAll() {
		if (treeBus.getColumnCount() > 0) {
			treeBus.getColumn(0).dispose();
		}
		if (treeBus.getItemCount() > 0) {
			for (int i = 0; i < treeBus.getItemCount(); i++) {
				clearTree(treeBus.getItem(i));
			}
			treeBus.removeAll();
		}
	}

	private void clearTree(TreeItem treeitem) {
		for (int i = 0; i < treeitem.getItemCount(); i++) {
			clearTree(treeitem.getItem(i));
			treeitem.clearAll(true);
			treeitem.getItem(i).dispose();
		}
	}

	public void refreshTree(Target target, TreeItem parentItem) {
		if (target.isHidden()) {
			return;
		}
		TreeItem treeitem;
		if (parentItem == null) {
			TreeColumn columnName = new TreeColumn(tree, SWT.VIRTUAL);
			columnName.setText("Instances");
			columnName
					.setToolTipText("To add a new instance, choose parent instance.  A list of child instances will appear in Instance Type combo.\r\n"
							+ "Select and Instance type.  You can optionally enter a custom name.  Then click 'Add Instance' button.");
			int width = tree.getSize().x - 25;
			if (width == 0) {
				width = 300;
			}
			columnName.setWidth(width);

			treeitem = new TreeItem(tree, SWT.VIRTUAL | SWT.BORDER);
		} else {
			treeitem = new TreeItem(parentItem, SWT.VIRTUAL | SWT.BORDER);
		}

		treeitem.setText(target.getName());
		treeitem.setData(target);

		if (target.isPluggable()) {
			treeitem.setFont(setBold(treeitem.getFont(), true));
		}

		Vector<Target> children = target.getChildren();

		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				Target childTarget = children.get(i);
				refreshTree(childTarget, treeitem);
			}
		}
	}

	public void addConnection(Connection conn, TreeItem parentItem, Boolean showItem) {

		if (parentItem == null && treeBus.getSelectionCount() > 0) {
			parentItem = treeBus.getSelection()[0];
		}
		if (parentItem == null) {
			return;
		}
		TreeItem treeitem = new TreeItem(parentItem, SWT.VIRTUAL | SWT.BORDER);
		treeitem.setText(conn.getName());
		treeitem.setData(conn);
		if (showItem) {
			treeBus.showItem(treeitem);
		}
	}

	public void refreshConnections(Target target) {
		clearBusTreeAll();
		if (target == null) {
			return;
		}
		TreeColumn columnName = new TreeColumn(treeBus, SWT.VIRTUAL);
		columnName.setText("Connections");
		columnName
				.setToolTipText("To add a new bus, first select an instance of a card in instances view, then choose bus type.\r\n"
						+ "If a bus is available, then sources and destinations will appear in connections combos.");
		int width = treeBus.getSize().x - 25;
		if (width == 0) {
			width = 300;
		}
		columnName.setWidth(width);
		TreeMap<Target, Vector<Connection>> busses = target.getBusses();
		for (Map.Entry<Target, Vector<Connection>> entry : busses.entrySet()) {
			Target busTarget = entry.getKey();
			Vector<Connection> c = entry.getValue();
			TreeItem treeitem = new TreeItem(treeBus, SWT.VIRTUAL | SWT.BORDER);
			treeitem.setText(entry.getKey().getType());
			treeitem.setData(busTarget);
			for (int i = 0; i < c.size(); i++) {
				Connection conn = c.get(i);
				addConnection(conn, treeitem, false);
			}
		}
	}

	public Font setBold(Font font, boolean bold) {
		FontData[] fD = font.getFontData();
		fD[0].setStyle(bold ? SWT.ITALIC : 0);
		return new Font(this.getShell().getDisplay(), fD[0]);
	}

	@Override
	public boolean close() {
		// this.clearTreeAll();
		if (dirty) {
			if (!MessageDialog.openConfirm(null, "Save Resource", mrwFilename
					+ "has been modified. Ignore changes?")) {
				return false;
			}
			ServerWizard2.LOGGER.info("Discarding changes and exiting...");
		}
		this.clearTable();
		for (Control c : container.getChildren()) {
			c.dispose();
		}
		return super.close();
	}

	public void addConnection(Boolean cabled) {
		ConnectionEndpoint source = (ConnectionEndpoint) comboSource.getData(comboSource.getText());
		ConnectionEndpoint dest = (ConnectionEndpoint) comboDest.getData(comboDest.getText());
		if (source != null && dest != null && treeBus.getSelectionCount() > 0
				&& tree.getSelectionCount() > 0) {
			TreeItem selectedBus = treeBus.getSelection()[0];
			TreeItem selectedTarget = tree.getSelection()[0];
			Target busTarget = (Target) selectedBus.getData();
			Target target = (Target) selectedTarget.getData();

			Connection conn = target.addConnection(busTarget, source, dest, cabled);
			this.addConnection(conn, selectedBus, true);
		}
		setDirtyState(true);
	}

	public void deleteConnection() {
		TreeItem selectedBus = treeBus.getSelection()[0];
		TreeItem selectedTarget = tree.getSelection()[0];
		Connection conn = (Connection) selectedBus.getData();
		Target target = (Target) selectedTarget.getData();
		Target busTarget = (Target) selectedBus.getParentItem().getData();
		controller.deleteConnection(target, busTarget, conn);
		//TreeItem busType = selectedBus.getParentItem();
		//refreshConnections(target);
		selectedBus.dispose();
		setDirtyState(true);
	}

	public Target getSelection(Tree tree) {
		if (tree.getSelectionCount() == 0) {
			return null;
		}
		return (Target) tree.getSelection()[0].getData();
	}

	public void setDirtyState(Boolean dirty) {
		this.dirty = dirty;
		if (this.btnSaveXml != null) {
			this.btnSaveXml.setEnabled(dirty);
		}
		if (dirty) {
			this.getShell().setText("ServerWiz2 - " + this.mrwFilename + " *");
		} else {
			this.getShell().setText("ServerWiz2 - " + this.mrwFilename);
		}
	}

	public void setFilename(String filename) {
		this.mrwFilename = filename;
		this.btnSaveXml.setEnabled(true);
		this.getShell().setText("ServerWiz2 - " + this.mrwFilename);
	}

}
