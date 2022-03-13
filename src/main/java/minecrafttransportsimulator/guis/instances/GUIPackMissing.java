package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

public class GUIPackMissing extends AGUIBase{
	private GUIComponentLabel noticeLabel;
	
	@Override
	public void setupComponents(){
		super.setupComponents();
		addComponent(noticeLabel = new GUIComponentLabel(guiLeft + 130, guiTop + 10, ColorRGB.RED, InterfaceCore.translate("gui.error.title"), TextAlignment.CENTERED, 3.0F));
		addComponent(new GUIComponentLabel(guiLeft + 10, guiTop + 40, ColorRGB.BLACK, InterfaceCore.translate("gui.error.packmissing"), TextAlignment.LEFT_ALIGNED, 1.0F, 240));
	}

	@Override
	public void setStates(){
		super.setStates();
		noticeLabel.visible = inClockPeriod(40, 20);
	}
}