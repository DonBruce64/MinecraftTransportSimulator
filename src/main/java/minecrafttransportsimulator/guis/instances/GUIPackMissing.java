package minecrafttransportsimulator.guis.instances;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;

public class GUIPackMissing extends AGUIBase{
	GUIComponentLabel noticeLabel;
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		addLabel(noticeLabel = new GUIComponentLabel(guiLeft + 130, guiTop + 10, ColorRGB.RED, InterfaceCore.translate("gui.packmissing.title"), null, TextAlignment.CENTERED, 3.0F, 0));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 40, ColorRGB.BLACK, InterfaceCore.translate("gui.packmissing.reason"), null, TextAlignment.LEFT_ALIGNED, 0.75F, 320));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 65, ColorRGB.BLACK, InterfaceCore.translate("gui.packmissing.nomod"), null, TextAlignment.LEFT_ALIGNED, 0.75F, 320));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 90, ColorRGB.BLACK, InterfaceCore.translate("gui.packmissing.modlink"), null, TextAlignment.LEFT_ALIGNED, 0.75F, 320));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 115, ColorRGB.BLACK, InterfaceCore.translate("gui.packmissing.misplaced"), null, TextAlignment.LEFT_ALIGNED, 0.75F, 320));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 150, ColorRGB.BLACK, InterfaceCore.translate("gui.packmissing.versionerror"), null, TextAlignment.LEFT_ALIGNED, 0.75F, 320));
	}

	@Override
	public void setStates(){
		noticeLabel.visible = inClockPeriod(40, 20);
	}
	
	@Override
	public boolean renderDarkBackground(){
		return true;
	}
}