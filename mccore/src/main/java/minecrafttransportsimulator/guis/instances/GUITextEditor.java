package minecrafttransportsimulator.guis.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponent3DModel;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityTextChange;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.systems.LanguageSystem;

public class GUITextEditor extends AGUIBase {
    private static final int signScale = 32;

    //General components.
    private final boolean forSigns;
    private GUIComponent3DModel modelRender;
    private GUIComponentButton confirmButton;
    private int boxWidth;
    private int populatingPageHeight;

    //Input boxes and their field names.
    private final List<Map<String, TextBoxStruct>> textInputBoxes = new ArrayList<>();
    private GUIComponentButton prevPageButton;
    private GUIComponentButton nextPageButton;
    private int currentPage;

    //Entity clicked.
    private final AEntityD_Definable<?> entity;

    //Labels for sign.  These do fancy rendering.
    private final Map<GUIComponentLabel, GUIComponentTextBox> signTextLabels = new HashMap<>();

    public GUITextEditor(AEntityD_Definable<?> entity) {
        super();
        this.entity = entity;
        this.forSigns = entity instanceof TileEntityPole_Sign;
    }

    @Override
    public void setupComponents() {
        super.setupComponents();

        //First get the model render and box width set, since that's used in text rendering.
        if (forSigns) {
            modelRender = new GUIComponent3DModel(guiLeft + 3 * getWidth() / 4, guiTop + 110, signScale, false, false, true);
            addComponent(modelRender);
            modelRender.modelLocation = entity.definition.getModelLocation(entity.subDefinition);
            modelRender.textureLocation = entity.definition.getTextureLocation(entity.subDefinition, 0);
            boxWidth = 100;
        } else {
            boxWidth = 200;
        }

        //Now populate the text boxes.
        textInputBoxes.clear();
        signTextLabels.clear();

        //Add text on this entity.
        addTextFromEntity(entity);
        if (entity instanceof AEntityF_Multipart) {
            ((AEntityF_Multipart<?>) entity).allParts.forEach(part -> addTextFromEntity(part));
        }

        //Add navigation buttons.
        addComponent(prevPageButton = new GUIComponentButton(this, guiLeft + 20, guiTop + 10, 20, 20, "<") {
            @Override
            public void onClicked(boolean leftSide) {
                --currentPage;
            }
        });
        addComponent(nextPageButton = new GUIComponentButton(this, guiLeft + 100, guiTop + 10, 20, 20, ">") {
            @Override
            public void onClicked(boolean leftSide) {
                ++currentPage;
            }
        });

        //Add the confirm button.
        addComponent(confirmButton = new GUIComponentButton(this, guiLeft + 150, guiTop + 15, 80, 20, LanguageSystem.GUI_CONFIRM.getCurrentValue()) {
            @Override
            public void onClicked(boolean leftSide) {
                textInputBoxes.forEach(page -> page.forEach((textKey, textStruct) -> textStruct.entities.forEach(textEntity -> InterfaceManager.packetInterface.sendToServer(new PacketEntityTextChange(textEntity, textKey, textStruct.textBox.text)))));
                close();
            }
        });
    }

    private void addTextFromEntity(AEntityD_Definable<?> entity) {
        for (Entry<JSONText, String> textEntry : entity.text.entrySet()) {
            addSingleTextEntry(entity, textEntry);
        }
    }

    private void addSingleTextEntry(AEntityD_Definable<?> entity, Entry<JSONText, String> textEntry) {
        //First see if we have this text on any page.
        JSONText textDef = textEntry.getKey();
        String textKey = textDef.fieldName;
        if (textKey != null) {
            for (Map<String, TextBoxStruct> page : textInputBoxes) {
                if (page.containsKey(textKey)) {
                    //Just add ourselves to the list.
                    page.get(textKey).entities.add(entity);
                    return;
                }
            }

            //No entry found, make one at the end of the page.
            int textRowsRequired = 1 + 5 * textDef.maxLength / boxWidth;
            int textBoxHeight = 12 * textRowsRequired;
            int boxSpacing = 12;
            int boxOffsetFromLabel = 10;
            int maxLowerLimit = getHeight() - boxSpacing;
            if (textInputBoxes.isEmpty() || populatingPageHeight + boxSpacing + boxOffsetFromLabel + textBoxHeight > maxLowerLimit) {
                //Need to add a page.
                textInputBoxes.add(new LinkedHashMap<>());
                populatingPageHeight = 0;
            }

            //Create the box and label, then add them to a new struct.
            GUIComponentLabel textLabel = new GUIComponentLabel(guiLeft + 20, guiTop + 30 + populatingPageHeight, ColorRGB.BLACK, textKey);
            addComponent(textLabel);
            GUIComponentTextBox textBox = new GUIComponentTextBox(this, guiLeft + 20, textLabel.constructedY + boxOffsetFromLabel, boxWidth, textBoxHeight, entity.text.get(textDef), ColorRGB.WHITE, textDef.maxLength);
            addComponent(textBox);
            textLabel.setComponent(textBox);
            if (forSigns) {
                GUIComponentLabel signLabel = new GUIComponentLabel(modelRender.constructedX + (int) (textDef.pos.x * signScale), modelRender.constructedY - (int) (textDef.pos.y * signScale), textDef.color, textBox.text, TextAlignment.values()[textDef.renderPosition], textDef.scale * signScale / 16F, textDef.wrapWidth * signScale / 16, textDef.fontName, textDef.autoScale);
                addComponent(signLabel);
                signTextLabels.put(signLabel, textBox);
            }
            textInputBoxes.get(textInputBoxes.size() - 1).put(textKey, new TextBoxStruct(textBox, entity));
            populatingPageHeight += boxOffsetFromLabel + textBox.height + boxSpacing;
        }
    }

    @Override
    public void setStates() {
        super.setStates();
        prevPageButton.enabled = currentPage > 0;
        nextPageButton.enabled = currentPage + 1 < textInputBoxes.size();
        confirmButton.enabled = true;
        for (int i = 0; i < textInputBoxes.size(); ++i) {
            for (TextBoxStruct textBoxStruct : textInputBoxes.get(i).values()) {
                textBoxStruct.textBox.visible = i == currentPage;
            }
        }
        signTextLabels.forEach((label, textBox) -> label.text = textBox.getText());
    }

    @Override
    protected boolean canStayOpen() {
        return super.canStayOpen() && entity.isValid;
    }

    @Override
    public int getWidth() {
        return super.getWidth();
    }

    private class TextBoxStruct {
        private final GUIComponentTextBox textBox;
        private final List<AEntityD_Definable<?>> entities = new ArrayList<>();

        private TextBoxStruct(GUIComponentTextBox textBox, AEntityD_Definable<?> entity) {
            this.textBox = textBox;
            entities.add(entity);
        }
    }
}
