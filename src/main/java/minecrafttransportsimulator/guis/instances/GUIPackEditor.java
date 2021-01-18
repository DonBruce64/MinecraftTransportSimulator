package minecrafttransportsimulator.guis.instances;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

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
	private static final Border LABEL_PADDING = BorderFactory.createEmptyBorder(2, 0, 2, 16);
	
	//Run-time variables.
	private Class<?> currentJSONClass = null;
	private Object currentJSON = null;
	private final JPanel editingPanel;
	private final GridBagConstraints labelConstraint;
	private final GridBagConstraints fieldConstraint;
	
	public GUIPackEditor(){
		//Init master settings.
		setTitle("MTS Pack Edtior");  
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        //Create a panel to hold the file I/O components.
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
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
				JFileChooser fileSelection = new JFileChooser();
		        fileSelection.setFont(MAIN_BUTTON_FONT);
		        fileSelection.setDialogTitle(openButton.getText());
		        if(fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION){
		        	try{
			        	File file = fileSelection.getSelectedFile();
			        	FileReader reader = new FileReader(file);
			            currentJSON = JSONParser.parseStream(reader, currentJSONClass);
			            reader.close();
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
				JFileChooser fileSelection = new JFileChooser();
		        fileSelection.setFont(MAIN_BUTTON_FONT);
		        fileSelection.setDialogTitle(saveButton.getText());
		        if(fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION){
		        	try{
			        	File file = fileSelection.getSelectedFile();
			        	FileWriter writer = new FileWriter(file);
			        	JSONParser.exportStream(currentJSON, writer);
			            writer.close();
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
        jsonClasses.put(JSONDecor.class.getSimpleName(), JSONDecor.class);
        jsonClasses.put(JSONSkin.class.getSimpleName(), JSONSkin.class);
        jsonClasses.put(JSONText.class.getSimpleName(), JSONText.class);
        
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
        add(editingPanel, BorderLayout.CENTER);
        
        //Create panel constraints and other constants.
		labelConstraint = new GridBagConstraints();
		labelConstraint.anchor = GridBagConstraints.LINE_END;
		fieldConstraint = new GridBagConstraints();
		fieldConstraint.anchor = GridBagConstraints.LINE_START;
		fieldConstraint.gridwidth = GridBagConstraints.REMAINDER;
        
        //Make the editor visible.
        pack();
        setVisible(true);
	}
	
	private void initEditor(){
		editingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), "Editing: " + currentJSON.getClass().getSimpleName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
		//editingPanel.setMinimumSize(new Dimension(0, 600));
		editingPanel.removeAll();
		populatePanel(editingPanel, currentJSON);
        pack();
	}
	
	private void populatePanel(JPanel panel, Object objectToParse){
		
		
		//Create scroll pane.
		//JScrollPane listScroller = new JScrollPane(container);
		//listScroller.setPreferredSize(new Dimension(250, 80));
		//container.add(listScroller);
		
		for(Field field : objectToParse.getClass().getFields()){
			try{
				if(field.isAnnotationPresent(JSONDescription.class)){
					Object obj = field.get(objectToParse);
					Class<?> fieldClass = field.getType();
					String annotationText = field.getAnnotation(JSONDescription.class).value();
					
					JComponent newComponent;
					if(fieldClass.equals(Boolean.TYPE)){
						JCheckBox checkBox = new JCheckBox();
						checkBox.setSelected((Boolean) obj);
						checkBox.addActionListener(new ActionListener(){
							@Override
							public void actionPerformed(ActionEvent event){
								try{
									field.set(objectToParse, checkBox.isSelected());
								}catch(Exception e){
								}
							}
				        });
						newComponent = checkBox;
					}else if(fieldClass.equals(Integer.TYPE)){
						JTextField textBox = new JTextField();
						textBox.setText(String.valueOf(obj));
						textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
						textBox.addFocusListener(new FieldChanger(textBox, field, objectToParse, Integer.class));
						newComponent = textBox;
					}else if(fieldClass.equals(Float.TYPE)){
						JTextField textBox = new JTextField();
						textBox.setText(String.valueOf(obj));
						textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
						textBox.addFocusListener(new FieldChanger(textBox, field, objectToParse, Float.class));
						newComponent = textBox;
					}else if(fieldClass.equals(String.class)){
						JTextField textBox = new JTextField();
						textBox.setPreferredSize(STRING_TEXT_BOX_DIM);
						if(obj != null){
							textBox.setText(String.valueOf(obj));
						}else{
							textBox.setText("");
						}
						textBox.addFocusListener(new FieldChanger(textBox, field, objectToParse, String.class));
						newComponent = textBox;
					}else if(fieldClass.equals(Point3d.class)){
						JPanel pointPanel = new JPanel();
						pointPanel.setLayout(new FlowLayout());
						pointPanel.setBorder(BorderFactory.createLoweredBevelBorder());
						FieldChanger changer = new FieldChanger(pointPanel, field, objectToParse, Point3d.class);
						pointPanel.addFocusListener(changer);
						
						JLabel xLabel = new JLabel("X:"); 
						xLabel.setFont(NORMAL_FONT);
						JTextField xText = new JTextField();
						xText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
						xText.addFocusListener(new FocusForwarder(changer));
						
						JLabel yLabel = new JLabel("Y:"); 
						yLabel.setFont(NORMAL_FONT);
						JTextField yText = new JTextField();
						yText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
						yText.addFocusListener(new FocusForwarder(changer));
						
						JLabel zLabel = new JLabel("Z:"); 
						zLabel.setFont(NORMAL_FONT);
						JTextField zText = new JTextField();
						zText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
						zText.addFocusListener(new FocusForwarder(changer));
						
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
						newComponent = pointPanel;
					}else if(fieldClass.isInstance(Collection.class)){
						continue;
					}else{
						if(obj == null){
							if(JSONParser.checkRequiredState(field, obj, "") != null){
								//If we are a member class, check for an extended class in our object.
								if(fieldClass.isMemberClass()){
									for(Class<?> objectClass : objectToParse.getClass().getDeclaredClasses()){
										if(fieldClass.isAssignableFrom(objectClass)){
											obj = objectClass.getConstructor(objectToParse.getClass()).newInstance(objectToParse);
										}
									}
								}else{
									obj = fieldClass.getConstructor(objectToParse.getClass()).newInstance(objectToParse);
								}
							}else{
								continue;
							}
						}
							
						JPanel subPanel = new JPanel();
						subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), annotationText, TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
						subPanel.setLayout(new GridBagLayout());
				        populatePanel(subPanel, obj);
				        newComponent = subPanel;
					}
					
					//Add label before adding component.
					//This gets the tooltip.
					JLabel componentLabel = new JLabel(field.getName() + ":"); 
					componentLabel.setFont(NORMAL_FONT);
					componentLabel.setBorder(LABEL_PADDING);
			        panel.add(componentLabel, labelConstraint);
			        
					//Need to split the string to prevent long tooltips.
					String tooltipText = "<html>";
					int breakIndex = annotationText.indexOf(" ", 100);
					while(breakIndex != -1){
						tooltipText += annotationText.substring(0, breakIndex) + "<br>";
						annotationText = annotationText.substring(breakIndex);
						breakIndex = annotationText.indexOf(" ", 100);
					}
					tooltipText += annotationText + "</html>";
					componentLabel.setToolTipText(tooltipText);
			        
			        //Add component.
					newComponent.setFont(NORMAL_FONT);
					panel.add(newComponent, fieldConstraint);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private class FieldChanger implements FocusListener{

		private final JComponent component;
		private final Field field;
		private final Object objectToParse;
		private final Object caster;
		
		private FieldChanger(JComponent component, Field field, Object objectToParse, Object caster){
			this.component = component;
			this.field = field;
			this.objectToParse = objectToParse;
			this.caster = caster;
		}
		
		@Override
		public void focusGained(FocusEvent arg0){}

		@Override
		public void focusLost(FocusEvent arg0){
			try{
				if(caster.equals(Integer.class)){
					field.set(objectToParse, Integer.valueOf(((JTextField) component).getText()));
				}else if(caster.equals(Float.class)){
					field.set(objectToParse, Float.valueOf(((JTextField) component).getText()));
				}else if(caster.equals(String.class)){
					if(((JTextField) component).getText().isEmpty()){
						field.set(objectToParse, null);
					}else{
						field.set(objectToParse, ((JTextField) component).getText());
					}
				}else if(caster.equals(Point3d.class)){
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
							field.set(objectToParse, null);
						}else{
							field.set(objectToParse, newPoint);
						}
						return;
					}catch(Exception e){
						component.getComponent(fieldChecking).setBackground(Color.RED);
						return;
					}
				}
				component.setBackground(Color.WHITE);
			}catch(Exception e){
				component.setBackground(Color.RED);
			}
		}
	}
	
	private class FocusForwarder implements FocusListener{
		
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
