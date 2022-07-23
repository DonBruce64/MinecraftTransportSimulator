package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;

public class GUIPackMissing extends AGUIBase {
    private GUIComponentLabel noticeLabel;

    @Override
    public void setupComponents() {
        super.setupComponents();
        addComponent(noticeLabel = new GUIComponentLabel(guiLeft + 130, guiTop + 10, ColorRGB.RED, JSONConfigLanguage.GUI_PACKMISSING_TITLE.value, TextAlignment.CENTERED, 3.0F));
        addComponent(new GUIComponentLabel(guiLeft + 10, guiTop + 40, ColorRGB.BLACK, JSONConfigLanguage.GUI_PACKMISSING_TEXT.value, TextAlignment.LEFT_ALIGNED, 1.0F, 240));
    }

    @Override
    public void setStates() {
        super.setStates();
        noticeLabel.visible = inClockPeriod(40, 20);
    }
}