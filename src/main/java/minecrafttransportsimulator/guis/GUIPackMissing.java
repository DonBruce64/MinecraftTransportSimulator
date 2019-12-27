package minecrafttransportsimulator.guis;

import java.awt.Color;

import minecrafttransportsimulator.guis.components.GUIComponentLabel;

public class GUIPackMissing extends GUIBase{
	GUIComponentLabel noticeLabel;
	
	@Override
	public void setupComponents(int guiLeft, int guiTop){
		addLabel(noticeLabel = new GUIComponentLabel(guiLeft + 90, guiTop + 10, Color.RED, 3.0F, true, false, -1, translate("packmissing.title")){public boolean visible(){return inClockPeriod(40, 20);}});
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 40, Color.BLACK, 0.75F, false, false, 320, translate("packmissing.reason")));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 65, Color.BLACK, 0.75F, false, false, 320, translate("packmissing.nomod")));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 90, Color.BLACK, 0.75F, false, false, 320, translate("packmissing.modlink")));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 115, Color.BLACK, 0.75F, false, false, 320, translate("packmissing.misplaced")));
		addLabel(new GUIComponentLabel(guiLeft + 10, guiTop + 150, Color.BLACK, 0.75F, false, false, 320, translate("packmissing.versionerror")));
	}

	@Override
	public void setStates(){}
	
	@Override
	public boolean renderDarkBackground(){
		return true;
	}
}