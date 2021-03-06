package minecrafttransportsimulator.guis.instances;

import java.awt.Color;

import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.mcinterface.InterfaceCore;

public class GUIPackMissing extends AGUIBase{
	GUIComponentLabel noticeLabel;
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		addLabel(noticeLabel = new GUIComponentLabel(guiLeft + 130, guiTop + 10, Color.RED, InterfaceCore.translate("gui.packmissing.title"), null, TextPosition.CENTERED, 0, 3.0F, false));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 40, Color.BLACK, InterfaceCore.translate("gui.packmissing.reason"), null, TextPosition.LEFT_ALIGNED, 320, 0.75F, false));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 65, Color.BLACK, InterfaceCore.translate("gui.packmissing.nomod"), null, TextPosition.LEFT_ALIGNED, 320, 0.75F, false));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 90, Color.BLACK, InterfaceCore.translate("gui.packmissing.modlink"), null, TextPosition.LEFT_ALIGNED, 320, 0.75F, false));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 115, Color.BLACK, InterfaceCore.translate("gui.packmissing.misplaced"), null, TextPosition.LEFT_ALIGNED, 320, 0.75F, false));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 150, Color.BLACK, InterfaceCore.translate("gui.packmissing.versionerror"), null, TextPosition.LEFT_ALIGNED, 320, 0.75F, false));
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