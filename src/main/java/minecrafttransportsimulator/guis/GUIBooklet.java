package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.guis.components.GUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.core.ItemBooklet;
import minecrafttransportsimulator.jsondefs.PackBookletObject;
import minecrafttransportsimulator.jsondefs.PackBookletObject.BookletPage;
import minecrafttransportsimulator.jsondefs.PackBookletObject.BookletText;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.wrappers.WrapperInput;

public class GUIBooklet extends GUIBase{
	//Buttons and text.
	private GUIComponentButton leftButton;
	private GUIComponentButton rightButton;
	private GUIComponentLabel contentsLabel;
	private List<List<GUIComponentLabel>> pageTextLabels = new ArrayList<List<GUIComponentLabel>>();
	
	//Manual item.
	private final ItemBooklet booklet;
	private final PackBookletObject pack;
	private final int totalPages;
	
	public GUIBooklet(ItemBooklet booklet){
		this.booklet = booklet;
		pack = PackParserSystem.getBooklet(booklet.bookletName);
		this.totalPages = pack.general.disableTOC ? 1 + pack.general.pages.length : 2 + pack.general.pages.length;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		//Page navigation buttons.
		addButton(leftButton = new GUIComponentButton(guiLeft + 10, guiTop + 150, 20, "<"){
			@Override
			public void onClicked(){
				--booklet.pageNumber;
			}
		});
		addButton(rightButton = new GUIComponentButton(guiLeft + 250, guiTop + 150, 20, ">"){
			@Override
			public void onClicked(){
				++booklet.pageNumber;				
			}
		});
		
		//Title text labels.
		List<GUIComponentLabel> titleLabels = new ArrayList<GUIComponentLabel>();
		for(BookletText text : pack.general.titleText){
			GUIComponentLabel titleLabel = new GUIComponentLabel(guiLeft + text.offsetX, guiTop + text.offsetY, Color.decode(text.color), text.text, text.scale, text.centered, false, text.wrapWidth);
			titleLabels.add(titleLabel);
			addLabel(titleLabel);
		}
		pageTextLabels.add(titleLabels);
		
		//Contents text labels.
		if(!pack.general.disableTOC){
			addLabel(contentsLabel = new GUIComponentLabel(guiLeft + 50, guiTop + 25, Color.BLACK, "CONTENTS"));
			int leftSideOffset = guiLeft + 20;
			int rightSideOffset = guiLeft + 155;
			List<GUIComponentLabel> contentsLabels = new ArrayList<GUIComponentLabel>();
			for(int i=0; i<pack.general.pages.length; ++i){
				GUIComponentLabel contentLabel = new GUIComponentLabel(i < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10*(i%10), Color.decode(pack.general.pages[i].pageText[0].color), (i + 1) + ": " + pack.general.pages[i].title);
				contentsLabels.add(contentLabel);
				addLabel(contentLabel);
			}
			pageTextLabels.add(contentsLabels);
		}
		
		//Regular page labels.
		for(BookletPage page : pack.general.pages){
			List<GUIComponentLabel> pageLabels = new ArrayList<GUIComponentLabel>();
			for(BookletText text : page.pageText){
				GUIComponentLabel pageLabel = new GUIComponentLabel(guiLeft + text.offsetX, guiTop + text.offsetY, Color.decode(text.color), text.text, text.scale, text.centered, false, text.wrapWidth);
				pageLabels.add(pageLabel);
				addLabel(pageLabel);
			}
			pageTextLabels.add(pageLabels);
		}
	}
	
	@Override
	public void setStates(){
		//Set the navigation button states.
		leftButton.visible = booklet.pageNumber > 0;
		rightButton.visible = booklet.pageNumber + 1 < totalPages;
		
		//Check the mouse to see if it updated and we need to change pages.
		int wheelMovement = WrapperInput.getTrackedMouseWheel();
		if(wheelMovement > 0 && rightButton.visible){
			++booklet.pageNumber;
		}else if(wheelMovement < 0 && leftButton.visible){
			--booklet.pageNumber;
		}
		
		//Set the visible labels based on the current page.
		contentsLabel.visible = booklet.pageNumber == 1 && !pack.general.disableTOC;
		for(int i=0; i<pageTextLabels.size(); ++ i){
			for(GUIComponentLabel label : pageTextLabels.get(i)){
				label.visible = booklet.pageNumber == i;
			}
		}
	}
	
	@Override
	public int getWidth(){
		return pack.general.textureWidth;
	}
	
	@Override
	public int getHeight(){
		return pack.general.textureHeight;
	}
	
	@Override
	public String getTexture(){
		if(booklet.pageNumber == 0){
			return pack.general.coverTexture;
		}else if(!pack.general.disableTOC){
			if(booklet.pageNumber == 1){
				return pack.general.pages[0].pageTexture;
			}else{
				return pack.general.pages[booklet.pageNumber - 2].pageTexture;
			}
		}else{
			return pack.general.pages[booklet.pageNumber - 1].pageTexture;
		}
	}
}