package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.jsondefs.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONBooklet.BookletText;
import minecrafttransportsimulator.wrappers.WrapperInput;

public class GUIBooklet extends AGUIBase{
	//Buttons and text.
	private GUIComponentButton leftButton;
	private GUIComponentButton rightButton;
	private GUIComponentLabel contentsLabel;
	private List<List<GUIComponentLabel>> pageTextLabels = new ArrayList<List<GUIComponentLabel>>();
	
	//Item properties.
	private final ItemBooklet booklet;
	private final int totalPages;
	
	public GUIBooklet(ItemBooklet booklet){
		this.booklet = booklet;
		this.totalPages = booklet.definition.general.disableTOC ? 1 + booklet.definition.general.pages.length : 2 + booklet.definition.general.pages.length;
	}
	
	@Override 
	public void setupComponents(int guiLeft, int guiTop){
		pageTextLabels.clear();
		
		//Page navigation buttons.
		//We auto-calculate the texture size from here based on the GUI size.
		//This is needed to tell the buttons what texture size they are using.
		addButton(leftButton = new GUIComponentButton(guiLeft + 10, guiTop + 150, 20, "<", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				--booklet.pageNumber;
			}
		});
		addButton(rightButton = new GUIComponentButton(guiLeft + 250, guiTop + 150, 20, ">", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
			@Override
			public void onClicked(){
				++booklet.pageNumber;
			}
		});
		
		//Title text labels.
		List<GUIComponentLabel> titleLabels = new ArrayList<GUIComponentLabel>();
		for(BookletText text : booklet.definition.general.titleText){
			GUIComponentLabel titleLabel = new GUIComponentLabel(guiLeft + text.offsetX, guiTop + text.offsetY, Color.decode(text.color), text.text, text.scale, text.centered, false, text.wrapWidth);
			titleLabels.add(titleLabel);
			addLabel(titleLabel);
		}
		pageTextLabels.add(titleLabels);
		
		//Contents text labels.
		if(!booklet.definition.general.disableTOC){
			addLabel(contentsLabel = new GUIComponentLabel(guiLeft + 50, guiTop + 25, Color.BLACK, "CONTENTS"));
			int leftSideOffset = guiLeft + 20;
			int rightSideOffset = guiLeft + 155;
			List<GUIComponentLabel> contentsLabels = new ArrayList<GUIComponentLabel>();
			for(int i=0; i<booklet.definition.general.pages.length; ++i){
				GUIComponentLabel contentLabel = new GUIComponentLabel(i < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10*(i%10), Color.decode(booklet.definition.general.pages[i].pageText[0].color), (i + 1) + ": " + booklet.definition.general.pages[i].title);
				contentsLabels.add(contentLabel);
				addLabel(contentLabel);
			}
			pageTextLabels.add(contentsLabels);
		}
		
		//Regular page labels.
		for(BookletPage page : booklet.definition.general.pages){
			List<GUIComponentLabel> pageLabels = new ArrayList<GUIComponentLabel>();
			for(BookletText text : page.pageText){
				try{
					GUIComponentLabel pageLabel = new GUIComponentLabel(guiLeft + text.offsetX, guiTop + text.offsetY, Color.decode(text.color), text.text, text.scale, text.centered, false, text.wrapWidth);
					pageLabels.add(pageLabel);
					addLabel(pageLabel);
				}catch(Exception e){
					int pageNumber = -1;
					for(byte i=0;i<booklet.definition.general.pages.length; ++i){
						if(booklet.definition.general.pages[i].equals(page)){
							pageNumber = i + 1;
						}
					}
					MTS.MTSLog.error("AN ERROR WAS ENCOUNTERED WHEN CREATING BOOKLET PAGE: " + pageNumber);
					MTS.MTSLog.error(e.getMessage());
				}
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
		contentsLabel.visible = booklet.pageNumber == 1 && !booklet.definition.general.disableTOC;
		for(int i=0; i<pageTextLabels.size(); ++ i){
			for(GUIComponentLabel label : pageTextLabels.get(i)){
				label.visible = booklet.pageNumber == i;
			}
		}
	}
	
	@Override
	public int getWidth(){
		return booklet.definition.general.textureWidth;
	}
	
	@Override
	public int getHeight(){
		return booklet.definition.general.textureHeight;
	}
	
	@Override
	public String getTexture(){
		if(booklet.pageNumber == 0){
			return booklet.definition.general.coverTexture;
		}else if(!booklet.definition.general.disableTOC){
			if(booklet.pageNumber == 1){
				return booklet.definition.general.pages[0].pageTexture;
			}else{
				return booklet.definition.general.pages[booklet.pageNumber - 2].pageTexture;
			}
		}else{
			return booklet.definition.general.pages[booklet.pageNumber - 1].pageTexture;
		}
	}
}