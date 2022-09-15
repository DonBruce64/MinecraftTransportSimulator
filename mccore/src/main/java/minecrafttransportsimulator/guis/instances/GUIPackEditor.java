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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.JSONParser.JSONDefaults;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * This is a special GUI that doesn't use the normal GUI code.  Instead, it uses the Swing
 * libraries to allow for an interactive JSON editor.  This allows pack authors to edit
 * JSONs with a comprehensive system rather than though Notepad or something.
 *
 * @author don_bruce
 */
public class GUIPackEditor extends JFrame {

    private static final long serialVersionUID = 1L;
    //Static variables for parameters.
    private static final Font MAIN_BUTTON_FONT = new Font("Arial", Font.BOLD, 30);
    private static final Font NORMAL_FONT = new Font("Arial", Font.PLAIN, 15);
    private static final Dimension NUMBER_TEXT_BOX_DIM = new Dimension(100, NORMAL_FONT.getSize() + 5);
    private static final Dimension STRING_TEXT_BOX_DIM = new Dimension(200, NORMAL_FONT.getSize() + 5);
    private static final GridBagConstraints LABEL_CONSTRAINTS = new GridBagConstraints();
    private static final GridBagConstraints FIELD_CONSTRAINTS = new GridBagConstraints();

    //Run-time variables.
    private static File lastFileAccessed = new File(InterfaceManager.gameDirectory);
    private Class<?> currentJSONClass = null;
    private Object currentJSON = null;
    private final JPanel editingPanel;

    public GUIPackEditor() {
        //Init master settings.
        setTitle("MTS Pack Edtior");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        long displaySize = InterfaceManager.clientInterface.getPackedDisplaySize();
        setMaximumSize(new Dimension((int) (displaySize >> Integer.SIZE), (int) displaySize));
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
        newButton.addActionListener(event -> {
            try {
                currentJSON = currentJSONClass.newInstance();
                if (currentJSON != null) {
                    initEditor();
                }
            } catch (Exception e) {
            }
        });
        filePanel.add(newButton);

        //Create open button.
        JButton openButton = new JButton();
        openButton.setEnabled(false);
        openButton.setFont(MAIN_BUTTON_FONT);
        openButton.setText("Open JSON");
        openButton.addActionListener(event -> {
            JFileChooser fileSelection = lastFileAccessed != null ? new JFileChooser(lastFileAccessed.getParent()) : new JFileChooser();
            fileSelection.setFont(MAIN_BUTTON_FONT);
            fileSelection.setDialogTitle(openButton.getText());
            if (fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileSelection.getSelectedFile();
                    currentJSON = JSONParser.parseStream(Files.newInputStream(file.toPath()), currentJSONClass, null, null);
                    lastFileAccessed = file;
                    if (currentJSON != null) {
                        initEditor();
                        lastFileAccessed = file;
                    }
                } catch (Exception e) {
                }
            }
        });
        filePanel.add(openButton);

        //Create save button.
        JButton saveButton = new JButton();
        saveButton.setEnabled(false);
        saveButton.setFont(MAIN_BUTTON_FONT);
        saveButton.setText("Save JSON");
        saveButton.addActionListener(event -> {
            JFileChooser fileSelection = lastFileAccessed != null ? new JFileChooser(lastFileAccessed.getParent()) : new JFileChooser();
            fileSelection.setApproveButtonText("Save");
            fileSelection.setApproveButtonToolTipText("Saves the JSON in the editor to the file.");
            fileSelection.setFont(MAIN_BUTTON_FONT);
            fileSelection.setDialogTitle(saveButton.getText());
            if (lastFileAccessed != null) {
                fileSelection.setSelectedFile(lastFileAccessed);
            }
            if (fileSelection.showOpenDialog(filePanel) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileSelection.getSelectedFile();
                    JSONParser.exportStream(currentJSON, Files.newOutputStream(file.toPath()));
                    lastFileAccessed = file;
                    try {
                        if (currentJSON instanceof AJSONItem) {
                            AJSONItem definition = (AJSONItem) currentJSON;
                            if (definition.packID != null && definition.systemName != null) {
                                if (definition instanceof AJSONMultiModelProvider) {
                                    JOptionPane.showMessageDialog(null, JSONParser.hotloadJSON(file, PackParser.getItem(definition.packID, definition.systemName, ((AJSONMultiModelProvider) definition).definitions.get(0).subName).definition));
                                } else {
                                    JOptionPane.showMessageDialog(null, JSONParser.hotloadJSON(file, PackParser.getItem(definition.packID, definition.systemName).definition));
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                } catch (Exception e) {
                }
            }
        });
        filePanel.add(saveButton);

        //Create drop-down for JSON type selection.
        //Create map to store entries.  Putting these in directly messes up the box formatting.
        Map<String, Class<?>> jsonClasses = new LinkedHashMap<>();
        jsonClasses.put("JSON Type - Select first!.", null);
        jsonClasses.put(JSONPack.class.getSimpleName(), JSONPack.class);
        jsonClasses.put(JSONVehicle.class.getSimpleName(), JSONVehicle.class);
        jsonClasses.put(JSONPart.class.getSimpleName(), JSONPart.class);
        jsonClasses.put(JSONInstrument.class.getSimpleName(), JSONInstrument.class);
        jsonClasses.put(JSONDecor.class.getSimpleName(), JSONDecor.class);
        jsonClasses.put(JSONPoleComponent.class.getSimpleName(), JSONPoleComponent.class);
        jsonClasses.put(JSONSkin.class.getSimpleName(), JSONSkin.class);

        //Create the box itself.
        JComboBox<String> typeComboBox = new JComboBox<>();
        typeComboBox.setFont(MAIN_BUTTON_FONT);
        typeComboBox.setAlignmentX(LEFT_ALIGNMENT);
        for (String className : jsonClasses.keySet()) {
            typeComboBox.addItem(className);
        }
        typeComboBox.setRenderer(generateClassTooltipRenderer(jsonClasses.values().toArray(new Class<?>[jsonClasses.size()])));
        typeComboBox.addActionListener(e -> {
            currentJSONClass = jsonClasses.get(typeComboBox.getSelectedItem());
            if (currentJSONClass != null) {
                newButton.setEnabled(true);
                openButton.setEnabled(true);
                saveButton.setEnabled(true);
            } else {
                newButton.setEnabled(false);
                openButton.setEnabled(false);
                saveButton.setEnabled(false);
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

    private void initEditor() {
        editingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), "Editing: " + currentJSON.getClass().getSimpleName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
        editingPanel.removeAll();
        populatePanel(editingPanel, currentJSON);
        pack();
    }

    private static void populatePanel(JPanel panel, Object objectToParse) {
        for (Field field : objectToParse.getClass().getFields()) {
            if (field.isAnnotationPresent(JSONDescription.class)) {
                JComponent newComponent = null;
                try {
                    FieldChanger listenter = new FieldChanger(field, objectToParse);
                    if (field.isAnnotationPresent(JSONDefaults.class)) {
                        newComponent = createNewStringBox(field.getAnnotation(JSONDefaults.class).value(), listenter);
                    } else {
                        newComponent = getComponentForObject(field.get(objectToParse), field.getType(), listenter, listenter);
                    }

                    if (newComponent == null) {
                        newComponent = getComponentForField(field, objectToParse);
                    } else {
                        listenter.component = newComponent;
                        if (field.isAnnotationPresent(JSONRequired.class)) {
                            newComponent.setBackground(Color.CYAN);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (newComponent != null) {
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

    private static JComponent getComponentForField(Field field, Object declaringObject) {
        Object obj;
        try {
            obj = field.get(declaringObject);
        } catch (Exception e) {
            return null;
        }

        Class<?> fieldClass = field.getType();
        if (List.class.isAssignableFrom(fieldClass)) {
            if (obj == null) {
                obj = new ArrayList<>();
                try {
                    field.set(declaringObject, obj);
                } catch (Exception e) {
                }
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
            for (Object listEntry : listObject) {
                ListElementPanel newPanel = new ListElementPanel(listContentsPanel, listObject, listEntry);
                listContentsPanel.add(newPanel);
            }
            listObjectPanel.add(listContentsPanel, BorderLayout.CENTER);

            //Now create an add button for the main panel.  This will allow for adding of elements.
            JButton newEntryButton = new JButton();
            newEntryButton.setFont(NORMAL_FONT);
            newEntryButton.setText("New Entry");
            newEntryButton.addActionListener(event -> {
                try {
                    Object listEntry = createNewObjectInstance(paramClass, declaringObject);
                    listObject.add(listEntry);
                    ListElementPanel newPanel = new ListElementPanel(listContentsPanel, listObject, listEntry);
                    listContentsPanel.add(newPanel);
                    listContentsPanel.revalidate();
                    listContentsPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listObjectPanel.add(newEntryButton, BorderLayout.PAGE_START);

            //Done with the list object.  Set it as the object to be added.
            return listObjectPanel;
        } else {
            if (obj == null) {
                obj = createNewObjectInstance(fieldClass, declaringObject);
                try {
                    field.set(declaringObject, obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            JPanel subPanel = new JPanel();
            subPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK, 2), field.getName(), TitledBorder.LEFT, TitledBorder.TOP, NORMAL_FONT));
            subPanel.setFont(NORMAL_FONT);
            subPanel.setLayout(new GridBagLayout());
            populatePanel(subPanel, obj);
            return subPanel;
        }
    }

    private static JComponent getComponentForObject(Object obj, Class<?> objectClass, FocusListener focusListener, ActionListener actionListener) {
        if (objectClass.equals(Boolean.TYPE)) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected((Boolean) obj);
            checkBox.addActionListener(actionListener);
            return checkBox;
        } else if (objectClass.equals(Integer.TYPE) || objectClass.equals(Integer.class)) {
            JTextField textBox = new JTextField();
            textBox.setFont(NORMAL_FONT);
            textBox.setText(String.valueOf(obj));
            textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
            textBox.addFocusListener(focusListener);
            return textBox;
        } else if (objectClass.equals(Float.TYPE) || objectClass.equals(Float.class)) {
            JTextField textBox = new JTextField();
            textBox.setFont(NORMAL_FONT);
            textBox.setText(String.valueOf(obj));
            textBox.setPreferredSize(NUMBER_TEXT_BOX_DIM);
            textBox.addFocusListener(focusListener);
            return textBox;
        } else if (objectClass.equals(String.class)) {
            JTextField textBox = new JTextField();
            textBox.setPreferredSize(STRING_TEXT_BOX_DIM);
            if (obj != null) {
                textBox.setText(String.valueOf(obj));
            } else {
                textBox.setText("");
            }
            textBox.addFocusListener(focusListener);
            return textBox;
        } else if (objectClass.equals(Point3D.class)) {
            JPanel pointPanel = new JPanel();
            pointPanel.setLayout(new FlowLayout());
            pointPanel.setBorder(BorderFactory.createLoweredBevelBorder());
            pointPanel.addFocusListener(focusListener);

            JLabel xLabel = new JLabel("X:");
            xLabel.setFont(NORMAL_FONT);
            JTextField xText = new JTextField();
            xText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
            xText.addFocusListener(new FocusForwarder(focusListener));

            JLabel yLabel = new JLabel("Y:");
            yLabel.setFont(NORMAL_FONT);
            JTextField yText = new JTextField();
            yText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
            yText.addFocusListener(new FocusForwarder(focusListener));

            JLabel zLabel = new JLabel("Z:");
            zLabel.setFont(NORMAL_FONT);
            JTextField zText = new JTextField();
            zText.setPreferredSize(NUMBER_TEXT_BOX_DIM);
            zText.addFocusListener(new FocusForwarder(focusListener));

            if (obj != null) {
                Point3D point = ((Point3D) obj);
                xText.setText(String.valueOf(point.x));
                yText.setText(String.valueOf(point.y));
                zText.setText(String.valueOf(point.z));
            } else {
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
        } else if (objectClass.isEnum()) {
            return createNewEnumBox(obj, objectClass, focusListener);
        } else {
            return null;
        }
    }

    private static <EnumType> JComboBox<EnumType> createNewEnumBox(Object obj, Class<EnumType> objectClass, FocusListener listener) {
        JComboBox<EnumType> comboBox = new JComboBox<>();
        comboBox.setFont(NORMAL_FONT);
        comboBox.setPreferredSize(STRING_TEXT_BOX_DIM);
        EnumType[] enumConstants = objectClass.getEnumConstants();
        for (EnumType enumConstant : enumConstants) {
            comboBox.addItem(enumConstant);
        }
        comboBox.addFocusListener(listener);
        comboBox.setRenderer(generateEnumTooltipRenderer(enumConstants));
        comboBox.setSelectedItem(obj);
        return comboBox;
    }

    private static <EnumType> JComboBox<String> createNewStringBox(Class<EnumType> objectClass, ItemListener listener) {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setEditable(true);
        comboBox.setFont(NORMAL_FONT);
        comboBox.setPreferredSize(STRING_TEXT_BOX_DIM);
        EnumType[] enumConstants = objectClass.getEnumConstants();
        for (EnumType enumConstant : enumConstants) {
            comboBox.addItem(((Enum<?>) enumConstant).name().toLowerCase());
        }
        comboBox.addItemListener(listener);
        comboBox.setRenderer(generateEnumTooltipRenderer(enumConstants));
        return comboBox;
    }

    private static <EnumType> DefaultListCellRenderer generateEnumTooltipRenderer(EnumType[] enumConstants) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index > -1 && index < enumConstants.length) {
                    EnumType currentEnum = enumConstants[index];
                    try {
                        String enumName = ((Enum<?>) currentEnum).name();
                        Field enumField = currentEnum.getClass().getField(enumName);
                        if (enumField.isAnnotationPresent(JSONDescription.class)) {
                            String tooltipText = formatTooltipText(enumField.getAnnotation(JSONDescription.class).value());
                            list.setToolTipText(tooltipText);
                        }
                    } catch (Exception e) {
                    }
                }
                return component;
            }
        };
    }

    private static DefaultListCellRenderer generateClassTooltipRenderer(Class<?>[] classes) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent component = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index > -1 && index < classes.length) {
                    Class<?> currentClass = classes[index];
                    try {
                        String tooltipText = formatTooltipText(currentClass.getAnnotation(JSONDescription.class).value());
                        list.setToolTipText(tooltipText);
                    } catch (Exception e) {
                    }
                }
                return component;
            }
        };
    }

    private static Object createNewObjectInstance(Class<?> fieldClass, Object declaringObject) {
        try {
            if (fieldClass.isMemberClass()) {
                for (Class<?> objectClass : declaringObject.getClass().getClasses()) {
                    if (fieldClass.isAssignableFrom(objectClass)) {
                        return objectClass.getConstructor(fieldClass.getDeclaringClass()).newInstance(declaringObject);
                    }
                }
                return null;
            } else if (fieldClass.equals(Integer.class)) {
                return 0;
            } else if (fieldClass.equals(Float.class)) {
                return (float) 0;
            } else {
                try {
                    return fieldClass.getConstructor().newInstance();
                } catch (Exception e) {
                    return fieldClass.getConstructor(declaringObject.getClass()).newInstance(declaringObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String formatTooltipText(String annotationText) {
        StringBuilder tooltipText = new StringBuilder("<html>");
        for (String annotationSegment : annotationText.split("\n")) {
            int breakIndex = annotationSegment.indexOf(" ", 150);
            while (breakIndex != -1) {
                tooltipText.append(annotationSegment.substring(0, breakIndex)).append("<br>");
                annotationSegment = annotationSegment.substring(breakIndex);
                breakIndex = annotationSegment.indexOf(" ", 150);
                int listStartIndex = annotationSegment.indexOf("<ul>");
                if (listStartIndex > 0 && listStartIndex < breakIndex) {
                    breakIndex = annotationSegment.indexOf("</ul>");
                }
            }
            tooltipText.append(annotationSegment).append("<br><br>");
        }
        return tooltipText + "</html>";
    }

    private static class ListElementPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private ListElementPanel(JPanel parentPanel, List<Object> list, Object listEntry) {
            //Create new box container for holding buttons.
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

            //Set main states.
            //If we are a list entry for a single field, add that to the buttonPanel.
            //If not, we need to parse our fields and add them in their own layout.
            ListElementChanger listenter = new ListElementChanger(list, listEntry);
            JComponent newComponent = getComponentForObject(listEntry, listEntry.getClass(), listenter, null);
            if (newComponent != null) {
                listenter.component = newComponent;
                buttonPanel.add(newComponent);
            } else {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createLineBorder(Color.BLACK));
                populatePanel(this, listEntry);
            }
            JPanel thisPanel = this;

            JButton deleteEntryButton = new JButton();
            deleteEntryButton.setFont(NORMAL_FONT);
            deleteEntryButton.setText("Delete");
            deleteEntryButton.addActionListener(event -> {
                list.remove(listEntry);
                parentPanel.remove(thisPanel);
                parentPanel.revalidate();
                parentPanel.repaint();
            });
            buttonPanel.add(deleteEntryButton);

            JButton copyEntryButton = new JButton();
            copyEntryButton.setFont(NORMAL_FONT);
            copyEntryButton.setText("Copy");
            copyEntryButton.addActionListener(event -> {
                try {
                    Object newObj = JSONParser.duplicateJSON(listEntry);
                    list.add(newObj);
                    parentPanel.add(new ListElementPanel(parentPanel, list, newObj));
                    parentPanel.revalidate();
                    parentPanel.repaint();
                } catch (Exception e) {
                }
            });
            buttonPanel.add(copyEntryButton);

            add(buttonPanel);
        }
    }

    private static class ListElementChanger implements FocusListener {

        private final List<Object> list;
        private final Class<?> objectClass;
        private final int index;
        private JComponent component;

        private ListElementChanger(List<Object> list, Object obj) {
            this.list = list;
            this.objectClass = obj.getClass();
            this.index = list.indexOf(obj);

        }

        @Override
        public void focusGained(FocusEvent arg0) {
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            try {
                if (objectClass.equals(Integer.TYPE)) {
                    list.set(index, Integer.valueOf(((JTextField) component).getText()));
                } else if (objectClass.equals(Float.TYPE)) {
                    list.set(index, Float.valueOf(((JTextField) component).getText()));
                } else if (objectClass.equals(String.class)) {
                    list.set(index, ((JTextField) component).getText());
                } else if (objectClass.equals(Point3D.class)) {
                    //Don't want to change the color of the whole panel.  Just the box we are in.
                    int fieldChecking = 1;
                    try {
                        double x = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);
                        fieldChecking += 2;
                        double y = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);
                        fieldChecking += 2;
                        double z = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);

                        Point3D newPoint = new Point3D(x, y, z);
                        list.set(index, newPoint);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        component.getComponent(fieldChecking).setBackground(Color.RED);
                        return;
                    }
                } else if (objectClass.isEnum()) {
                    list.set(index, ((JComboBox<?>) component).getSelectedItem());
                }
                component.setBackground(Color.WHITE);
            } catch (Exception e) {
                e.printStackTrace();
                component.setBackground(Color.RED);
            }
        }
    }

    private static class FieldChanger implements FocusListener, ItemListener, ActionListener {

        private final Field objectField;
        private final Class<?> objectClass;
        private final Object declaringObject;
        private JComponent component;

        private FieldChanger(Field objectField, Object declaringObject) {
            this.objectField = objectField;
            this.objectClass = objectField.getType();
            this.declaringObject = declaringObject;
        }

        @Override
        public void focusGained(FocusEvent arg0) {
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            try {
                if (objectClass.equals(Integer.TYPE)) {
                    objectField.set(declaringObject, Integer.valueOf(((JTextField) component).getText()));
                } else if (objectClass.equals(Float.TYPE)) {
                    objectField.set(declaringObject, Float.valueOf(((JTextField) component).getText()));
                } else if (objectClass.equals(String.class)) {
                    String text = ((JTextField) component).getText();
                    if (text.isEmpty()) {
                        objectField.set(declaringObject, null);
                    } else {
                        objectField.set(declaringObject, text);
                    }
                } else if (objectClass.equals(Point3D.class)) {
                    //Don't want to change the color of the whole panel.  Just the box we are in.
                    int fieldChecking = 1;
                    try {
                        double x = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);
                        fieldChecking += 2;
                        double y = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);
                        fieldChecking += 2;
                        double z = Float.parseFloat(((JTextField) component.getComponent(fieldChecking)).getText());
                        component.getComponent(fieldChecking).setBackground(Color.WHITE);

                        Point3D newPoint = new Point3D(x, y, z);
                        if (newPoint.isZero()) {
                            objectField.set(declaringObject, null);
                        } else {
                            objectField.set(declaringObject, newPoint);
                        }
                        return;
                    } catch (Exception e) {
                        component.getComponent(fieldChecking).setBackground(Color.RED);
                        return;
                    }
                } else if (objectClass.isEnum()) {
                    objectField.set(declaringObject, ((JComboBox<?>) component).getSelectedItem());
                }
                component.setBackground(Color.WHITE);
            } catch (Exception e) {
                component.setBackground(Color.RED);
            }
        }

        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.DESELECTED) {
                try {
                    if (component instanceof JComboBox) {
                        String text = ((JComboBox<?>) component).getSelectedItem().toString();
                        if (text == null || text.isEmpty()) {
                            objectField.set(declaringObject, null);
                        } else {
                            objectField.set(declaringObject, text);
                        }
                        component.setBackground(Color.WHITE);
                    }
                } catch (Exception e) {
                    component.setBackground(Color.RED);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                if (objectClass.equals(Boolean.TYPE)) {
                    objectField.set(declaringObject, ((JCheckBox) component).isSelected());
                }
            } catch (Exception e) {
                component.setBackground(Color.RED);
            }
        }
    }

    private static class FocusForwarder implements FocusListener {

        private final FocusListener target;

        private FocusForwarder(FocusListener target) {
            this.target = target;
        }

        @Override
        public void focusGained(FocusEvent arg0) {
            target.focusGained(arg0);
        }

        @Override
        public void focusLost(FocusEvent arg0) {
            target.focusLost(arg0);
        }
    }
}
