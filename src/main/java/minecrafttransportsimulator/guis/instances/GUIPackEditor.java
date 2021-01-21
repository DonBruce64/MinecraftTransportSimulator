package minecrafttransportsimulator.guis.instances;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import net.minecraft.client.Minecraft;

/**This is a special GUI that doesn't use the normal GUI code.  Instead, it uses the Swing
 * libraries to allow for an interactive JSON editor.  This allows pack authors to edit
 * JSONs with a comprehensive system rather than though Notepad or something.
 * 
 * @author don_bruce
 */
public class GUIPackEditor extends JFrame{

	//Static variables for parameters.
	private static final Font MAIN_BUTTON_FONT = new Font("Arial", Font.BOLD, 30);
	private static final Font NORMAL_FONT = new Font("Arial", Font.PLAIN, 15);
	private static final Dimension NUMBER_TEXT_BOX_DIM = new Dimension(100, NORMAL_FONT.getSize() + 5);
	private static final Dimension STRING_TEXT_BOX_DIM = new Dimension(200, NORMAL_FONT.getSize() + 5);
	private static final GridBagConstraints LABEL_CONSTRAINTS = new GridBagConstraints();
	private static final GridBagConstraints FIELD_CONSTRAINTS = new GridBagConstraints();
	
	//Run-time variables.
	private static File lastDirectoryAccessed = new File(MasterLoader.gameDirectory);
	private Class<?> currentJSONClass = null;
	private Object currentJSON = null;
	private final JPanel editingPanel;
	
	public GUIPackEditor(){
		//Init master settings.
		setTitle("MTS Pack Edtior");  
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight));
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        //Create a panel to hold the file I/O components.
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), "File Selection", TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
        add(filePanel, BorderLayout.PAGE_START);

        //Create file selector buttons.
        //Create new button.
        JButton newButton = new JButton();
        newButton.setEnabled(false);
        newButton.setFont(MAIN_BUTTON_FONT);
        newButton.setText("New JSON");
        newButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				try{
					currentJSON = currentJSONClass.newInstance();
					if(currentJSON != null){
		            	initEditor();
		            }
				}catch(Exception e){}
			}
        });
        filePanel.add(newButton);
        
        //Create open button.
        JButton openButton = new JButton();
        openButton.setEnabled(false);
        openButton.setFont(MAIN_BUTTON_FONT);
        openButton.setText("Open JSON");
        openButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				JFileChooser fileSelection = lastDirectoryAccessed != null ? new JFileChooser(lastDirectoryAccessed) : new JFileChooser();
		        fileSelection.setFont(MAIN_BUTTON_FONT);
		        fileSelection.setDialogTitle(openButton.getText());
		        if(fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION){
		        	try{
			        	File file = fileSelection.getSelectedFile();
			        	FileReader reader = new FileReader(file);
			            currentJSON = JSONParser.parseStream(reader, currentJSONClass);
			            reader.close();
			            lastDirectoryAccessed = file.getParentFile();
			            if(currentJSON != null){
			            	initEditor();
			            }
		        	}catch(Exception e){}
		        }
			}
        });
        filePanel.add(openButton);
        
        //Create save button.
        JButton saveButton = new JButton();
        saveButton.setEnabled(false);
        saveButton.setFont(MAIN_BUTTON_FONT);
        saveButton.setText("Save JSON");
        saveButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event){
				JFileChooser fileSelection = lastDirectoryAccessed != null ? new JFileChooser(lastDirectoryAccessed) : new JFileChooser();
				fileSelection.setApproveButtonText("Save");
				fileSelection.setApproveButtonToolTipText("Saves the JSON in the editor to the file.");				
		        fileSelection.setFont(MAIN_BUTTON_FONT);
		        fileSelection.setDialogTitle(saveButton.getText());
		        if(fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION){
		        	try{
			        	File file = fileSelection.getSelectedFile();
			        	FileWriter writer = new FileWriter(file);
			        	JSONParser.exportStream(currentJSON, writer);
			            writer.close();
			            lastDirectoryAccessed = file.getParentFile();
		        	}catch(Exception e){}
		        }
			}
        });
        filePanel.add(saveButton);
        
        //Create drop-down for JSON type selection.
        //Create map to store entries.  Putting these in directly messes up the box formatting.
        Map<String, Class<?>> jsonClasses = new LinkedHashMap<String, Class<?>>();
        jsonClasses.put("JSON Type - Select first!.", null);
        jsonClasses.put(JSONVehicle.class.getSimpleName(), JSONVehicle.class);
        jsonClasses.put(JSONInstrument.class.getSimpleName(), JSONInstrument.class);
        jsonClasses.put(JSONDecor.class.getSimpleName(), JSONDecor.class);
        jsonClasses.put(JSONPoleComponent.class.getSimpleName(), JSONPoleComponent.class);
        jsonClasses.put(JSONSkin.class.getSimpleName(), JSONSkin.class);
        
        //Create the box itself.
        JComboBox<String> typeComboBox = new JComboBox<String>();
        typeComboBox.setFont(MAIN_BUTTON_FONT);
        typeComboBox.setAlignmentX(LEFT_ALIGNMENT);
        for(String className : jsonClasses.keySet()){
        	typeComboBox.addItem(className);
        }
        typeComboBox.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				currentJSONClass = jsonClasses.get(typeComboBox.getSelectedItem());
				if(currentJSONClass != null){
					newButton.setEnabled(true);
					openButton.setEnabled(true);
					saveButton.setEnabled(true);
				}else{
					newButton.setEnabled(false);
					openButton.setEnabled(false);
					saveButton.setEnabled(false);
				}
			}
        });
        filePanel.add(typeComboBox);
        
        //Create the frame to hold the JSON information.  This is for the editing parts.
        //We don't add this to the main container.  Instead, we create a scroll frame to render it.
        editingPanel = new JPanel();
        editingPanel.setLayout(new GridBagLayout());
        JScrollPane editingPane = new JScrollPane();
		editingPane.add(editingPanel);
		editingPane.setViewportView(editingPanel);
		editingPane.getVerticalScrollBar().setUnitIncrement(20);
        add(editingPane, BorderLayout.CENTER);
        
        //Set constraint properties.
		LABEL_CONSTRAINTS.anchor = GridBagConstraints.LINE_END;
		FIELD_CONSTRAINTS.anchor = GridBagConstraints.LINE_START;
		FIELD_CONSTRAINTS.gridwidth = GridBagConstraints.REMAINDER;
        
        //Make the editor visible.
        pack();
        setVisible(true);
	}
	
	private void initEditor(){
		editingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), "Editing: " + currentJSON.getClass().getSimpleName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
		editingPanel.removeAll();
		populatePanel(editingPanel, currentJSON);
        pack();
	}
	
	private static void populatePanel(JPanel panel, Object objectToParse){
		for(Field field : objectToParse.getClass().getFields()){
			if(field.isAnnotationPresent(JSONDescription.class)){
				JComponent newComponent = null;
				try{
					FieldChanger listenter = new FieldChanger(field, objectToParse);
					newComponent = getComponentForObject(field.get(objectToParse), field.getType(), listenter);
					if(newComponent == null){
						newComponent = getPanelForField(field, objectToParse);
					}else{
						listenter.component = newComponent;
					}
				}catch(Exception e){}
				
				
				if(newComponent != null){
					String annotationText = field.getAnnotation(JSONDescription.class).value();
					
					//Add label before adding component.
					//This gets the tooltip.
					JLabel componentLabel = new JLabel(field.getName() + ":"); 
					componentLabel.setFont(NORMAL_FONT);
			        panel.add(componentLabel, LABEL_CONSTRAINTS);
					componentLabel.setToolTipText(formatTooltipText(annotationText));
			        
			        //Add component.
					panel.add(newComponent, FIELD_CONSTRAINTS);
				}
			}
		}
	}
	
	private static JPanel getPanelForField(Field field, Object declaringObject){
		Object obj;
		try{
			obj = field.get(declaringObject);
		}catch(Exception e){
			return null;
		}
		
		Class<?> fieldClass = field.getType();
		if(List.class.isAssignableFrom(fieldClass)){
			if(obj == null){
				obj = new ArrayList<>();
			}
			Class<?> paramClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
			@SuppressWarnings("unchecked")
			List<Object> listObject = (List<Object>) obj;
			
			//Create the main list panel for the whole list object.
			JPanel listObjectPanel = new JPanel();
			listObjectPanel.setLayout(new BorderLayout());
			listObjectPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), field.getName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
			
			//Create the inner window for the list panel's content.
			//This will contain all the list element sub-panels.
			JPanel listContentsPanel = new JPanel();
			listContentsPanel.setLayout(new BoxLayout(listContentsPanel, BoxLayout.Y_AXIS));
			for(Object listEntry : listObject){
				ListPanel newPanel = new ListPanel(listContentsPanel, listObject, listEntry);
				listContentsPanel.add(newPanel);
			}
			listObjectPanel.add(listContentsPanel, BorderLayout.CENTER);
			
			//Now create an add button for the main panel.  This will allow for adding of elements.
			JButton newEntryButton = new JButton();
	        newEntryButton.setFont(NORMAL_FONT);
	        newEntryButton.setText("New Entry");
	        newEntryButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					try{
						Object listEntry = createNewObjectInstance(paramClass, declaringObject);
						ListPanel newPanel = new ListPanel(listContentsPanel, listObject, listEntry);
						listContentsPanel.add(newPanel);
						listObject.add(listEntry);
						listContentsPanel.revalidate();
						listContentsPanel.repaint();
					}catch(Exception e){e.printStackTrace();}
				}
	        });
	        listObjectPanel.add(newEntryButton, BorderLayout.PAGE_START);
	        
	        //Done with the list object.  Set it as the object to be added.
	        return listObjectPanel;
		}else{
			if(obj == null){
				obj = createNewObjectInstance(fieldClass, declaringObject);
			}
				
			JPanel subPanel = new JPanel();
			subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), field.getName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
			subPanel.setFont(NORMAL_FONT);
			subPanel.setLayout(new GridBagLayout());
	        populatePanel(subPanel, obj);
	        return subPanel;
		}
	}
		
	private static JComponent getComponentForObject(Object obj, Class<?> objectClass, FocusListener listener){
		if(objectClass.equals(Boolean.TYPE)){
			JCheckBox checkBox = new JCheckBox();
			checkBox.setSelected((Boolean) obj);
			return checkBox;
		}else if(objectClass.equals(Integer.TYPE)){
			JTextField textBox = new JTextField();
			textBox.setFont(NORMAL_FONT);
			textBox.setText(String.valueOf(obj));
			textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
			textBox.addFocusListener(listener);
			return textBox;
		}else if(objectClass.equals(Float.TYPE)){
			JTextField textBox = new JTextField();
			textBox.setFont(NORMAL_FONT);
			textBox.setText(String.valueOf(obj));
			textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
			textBox.addFocusListener(listener);
			return textBox;
		}else if(objectClass.equals(String.class)){
			JTextField textBox = new JTextField();
			textBox.setPreferredSize(STRING_TEXT_BOX_DIM);
			if(obj != null){
				textBox.setText(String.valueOf(obj));
			}else{
				textBox.setText("");
			}
			textBox.addFocusListener(listener);
			return textBox;
		}else if(objectClass.equals(Point3d.class)){
			JPanel pointPanel = new JPanel();
			pointPanel.setLayout(new FlowLayout());
			pointPanel.setBorder(BorderFactory.createLoweredBevelBorder());
			pointPanel.addFocusListener(listener);
			
			JLabel xLabel = new JLabel("X:"); 
			xLabel.setFont(NORMAL_FONT);
			JTextField xText = new JTextField();
			xText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
			xText.addFocusListener(new FocusForwarder(listener));
			
			JLabel yLabel = new JLabel("Y:"); 
			yLabel.setFont(NORMAL_FONT);
			JTextField yText = new JTextField();
			yText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
			yText.addFocusListener(new FocusForwarder(listener));
			
			JLabel zLabel = new JLabel("Z:"); 
			zLabel.setFont(NORMAL_FONT);
			JTextField zText = new JTextField();
			zText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
			zText.addFocusListener(new FocusForwarder(listener));
			
			if(obj != null){
				Point3d point = ((Point3d) obj);
				xText.setText(String.valueOf(point.x));
				yText.setText(String.valueOf(point.x));
				zText.setText(String.valueOf(point.x));
			}else{
				xText.setText(String.valueOf(0.0));
				yText.setText(String.valueOf(0.0));
				zText.setText(String.valueOf(0.0));
			}
			
			pointPanel.add(xLabel);
			pointPanel.add(xText);
			pointPanel.add(yLabel);
			pointPanel.add(yText);
			pointPanel.add(zLabel);
			pointPanel.add(zText);
			return pointPanel;
		}else if(objectClass.isEnum()){
			return createNewEnumBox(obj, objectClass, listener);
		}else{
			return null;
		}
	}
	
	private static <EnumType> JComboBox<EnumType> createNewEnumBox(Object obj, Class<EnumType> objectClass, FocusListener listener){
		JComboBox<EnumType> comboBox = new JComboBox<EnumType>();
		comboBox.setFont(NORMAL_FONT);
		comboBox.setPreferredSize(STRING_TEXT_BOX_DIM);
		EnumType[] enumConstants = objectClass.getEnumConstants();
		for(EnumType enumConstant : enumConstants){
			comboBox.addItem(enumConstant);
		}
		comboBox.addFocusListener(listener);
		comboBox.setRenderer(new DefaultListCellRenderer(){
		    @Override
		    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus){
		        JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        if(index > -1){
		        	EnumType currentEnum = enumConstants[index];
		        	try{
		        		String enumName = ((Enum<?>) currentEnum).name();
		        		String tooltipText = formatTooltipText(currentEnum.getClass().getField(enumName).getAnnotation(JSONDescription.class).value());
		        		list.setToolTipText(tooltipText);
		        	}catch(Exception e){}
		        }
		        return comp;
		    }
		});
		return comboBox;
	}
	
	private static Object createNewObjectInstance(Class<?> fieldClass, Object declaringObject){
		try{
			if(fieldClass.isMemberClass()){
				for(Class<?> objectClass : declaringObject.getClass().getDeclaredClasses()){
					if(fieldClass.isAssignableFrom(objectClass)){
						return objectClass.getConstructor(declaringObject.getClass()).newInstance(declaringObject);
					}
				}
				return null;
			}else{
				try{
					return fieldClass.getConstructor().newInstance(); 
				}catch(Exception e){
					return fieldClass.getConstructor(declaringObject.getClass()).newInstance(declaringObject);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static String formatTooltipText(String annotationText){
		String tooltipText = "<html>";
		for(String annotationSegment : annotationText.split("\n")){
			int breakIndex = annotationSegment.indexOf(" ", 100);
			while(breakIndex != -1){
				tooltipText += annotationSegment.substring(0, breakIndex) + "<br>";
				annotationSegment = annotationSegment.substring(breakIndex);
				breakIndex = annotationSegment.indexOf(" ", 100);
			}
			tooltipText += annotationSegment  + "<br><br>";
		}
		return tooltipText + "</html>";
	}
	
	private static class ListPanel extends JPanel{
		
		private ListPanel(JPanel parentPanel, List<Object> list, Object listEntry){
			//Create new box container for holding buttons.
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			
			//Set main states.
			//If we are a list entry for a single field, add that to the buttonPanel.
			//If not, we need to parse our fields and add them in their own layout.
			ListElementChanger listenter = new ListElementChanger(list, listEntry);
			JComponent newComponent = getComponentForObject(listEntry, listEntry.getClass(), listenter);
			if(newComponent != null){
				listenter.component = newComponent;
				buttonPanel.add(newComponent);
			}else{
				setLayout(new GridBagLayout());
				setBorder(BorderFactory.createLineBorder(Color.BLACK));
				populatePanel(this, listEntry);
			}
			JPanel thisPanel = this;
			
			JButton deleteEntryButton = new JButton();
	        deleteEntryButton.setFont(NORMAL_FONT);
	        deleteEntryButton.setText("Delete");
	        deleteEntryButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					list.remove(listEntry);
					parentPanel.remove(thisPanel);
					parentPanel.revalidate();
					parentPanel.repaint();
				}
	        });
	        buttonPanel.add(deleteEntryButton);
	        
	        JButton copyEntryButton = new JButton();
	        copyEntryButton.setFont(NORMAL_FONT);
	        copyEntryButton.setText("Copy");
	        copyEntryButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent event){
					try{
						Object newObj = JSONParser.duplicateJSON(listEntry);
						list.add(newObj);
						parentPanel.add(new ListPanel(parentPanel, list, newObj));
						parentPanel.revalidate();
						parentPanel.repaint();
					}catch(Exception e){}
				}
	        });
	        buttonPanel.add(copyEntryButton);
	        
	        add(buttonPanel);
		}
	}
	
	private static class ListElementChanger implements FocusListener{
		
		private final List<Object> list;
		private final Class<?> objectClass;
		private final int index;
		private JComponent component;
		
		private ListElementChanger(List<Object> list, Object obj){
			this.list = list;
			this.objectClass = obj.getClass();
			this.index = list.indexOf(obj);
			
		}
		
		@Override
		public void focusGained(FocusEvent arg0){}

		@Override
		public void focusLost(FocusEvent arg0){
			try{
				if(objectClass.equals(Boolean.TYPE)){
					list.set(index, ((JCheckBox) component).isSelected());
					return;
				}if(objectClass.equals(Integer.TYPE)){
					list.set(index, Integer.valueOf(((JTextField) component).getText()));
				}else if(objectClass.equals(Float.TYPE)){
					list.set(index, Float.valueOf(((JTextField) component).getText()));
				}else if(objectClass.equals(String.class)){
					list.set(index, ((JTextField) component).getText());
				}else if(objectClass.equals(Point3d.class)){
					//Don't want to change the color of the whole panel.  Just the box we are in.
					int fieldChecking = 1;
					try{
						double x = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						fieldChecking += 2;
						double y = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						fieldChecking += 2;
						double z = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						
						Point3d newPoint = new Point3d(x, y, z);
						list.set(index, newPoint);
						return;
					}catch(Exception e){
						e.printStackTrace();
						component.getComponent(fieldChecking).setBackground(Color.RED);
						return;
					}
				}
				component.setBackground(Color.WHITE);
			}catch(Exception e){
				e.printStackTrace();
				component.setBackground(Color.RED);
			}
		}
	}
	
	private static class FieldChanger implements FocusListener{

		private final Field objectField;
		private final Class<?> objectClass;
		private final Object declaringObject;
		private JComponent component;
		
		private FieldChanger(Field objectField, Object declaringObject){
			this.objectField = objectField;
			this.objectClass = objectField.getType();
			this.declaringObject = declaringObject;
		}
		
		@Override
		public void focusGained(FocusEvent arg0){}

		@Override
		public void focusLost(FocusEvent arg0){
			try{
				if(objectClass.equals(Boolean.TYPE)){
					objectField.set(declaringObject, ((JCheckBox) component).isSelected());
					return;
				}if(objectClass.equals(Integer.TYPE)){
					objectField.set(declaringObject, Integer.valueOf(((JTextField) component).getText()));
				}else if(objectClass.equals(Float.TYPE)){
					objectField.set(declaringObject, Float.valueOf(((JTextField) component).getText()));
				}else if(objectClass.equals(String.class)){
					if(((JTextField) component).getText().isEmpty()){
						objectField.set(declaringObject, null);
					}else{
						objectField.set(declaringObject, ((JTextField) component).getText());
					}
				}else if(objectClass.equals(Point3d.class)){
					//Don't want to change the color of the whole panel.  Just the box we are in.
					int fieldChecking = 1;
					try{
						double x = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						fieldChecking += 2;
						double y = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						fieldChecking += 2;
						double z = Float.valueOf(((JTextField) component.getComponent(fieldChecking)).getText());
						component.getComponent(fieldChecking).setBackground(Color.WHITE);
						
						Point3d newPoint = new Point3d(x, y, z);
						if(newPoint.isZero()){
							objectField.set(declaringObject, null);
						}else{
							objectField.set(declaringObject, newPoint);
						}
						return;
					}catch(Exception e){
						e.printStackTrace();
						component.getComponent(fieldChecking).setBackground(Color.RED);
						return;
					}
				}else if(objectClass.isEnum()){
					objectField.set(declaringObject, ((JComboBox<?>) component).getSelectedItem());
				}
				component.setBackground(Color.WHITE);
			}catch(Exception e){
				e.printStackTrace();
				component.setBackground(Color.RED);
			}
		}
	}
	
	private static class FocusForwarder implements FocusListener{
		
		private final FocusListener target;
		
		private FocusForwarder(FocusListener target){
			this.target = target;
		}
		
		@Override
		public void focusGained(FocusEvent arg0){
			target.focusGained(arg0);
		}

		@Override
		public void focusLost(FocusEvent arg0){
			target.focusLost(arg0);
		}
	}
}
