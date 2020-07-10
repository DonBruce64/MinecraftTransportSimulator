package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import mcinterface.BuilderGUI;
import mcinterface.InterfaceInput;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.components.GUIComponentButton;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.items.packs.ItemBooklet;
import minecrafttransportsimulator.jsondefs.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONBooklet.BookletText;

public class GUIBooklet extends AGUIBase{
	//Buttons and text.
	private GUIComponentButton leftButton;
	private GUIComponentButton rightButton;
	private GUIComponentButton contentsButton;
	private List<List<GUIComponentLabel>> pageTextLabels = new ArrayList<List<GUIComponentLabel>>();
	private List<ContentsButton> contentsButtons = new ArrayList<ContentsButton>();
	
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
		
		//Contents text labels and buttons.
		if(!booklet.definition.general.disableTOC){
			//TOC page label.
			GUIComponentLabel contentsLabel = new GUIComponentLabel(guiLeft + 50, guiTop + 25, Color.BLACK, "CONTENTS");
			addLabel(contentsLabel);
			List<GUIComponentLabel> contentsLabels = new ArrayList<GUIComponentLabel>();
			contentsLabels.add(contentsLabel);
			pageTextLabels.add(contentsLabels);
			
			//TOC buttons with text for pages.
			contentsButtons.clear();
			int leftSideOffset = guiLeft + 20;
			int rightSideOffset = guiLeft + 155;
			for(int i=0; i<booklet.definition.general.pages.length; ++i){
				ContentsButton contentsButton = new ContentsButton(i < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10*(i%10), i){
					@Override
					public void onClicked(){
						booklet.pageNumber = contentsIndex + 2;
					}
				};
				contentsButtons.add(contentsButton);
				addButton(contentsButton);
			}
			
			//Button on other pages to go back to TOC.
			addButton(contentsButton = new GUIComponentButton(leftButton.x + leftButton.width, guiTop + 150, 30, "TOC", 20, true, 20, 20, 0, 196, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(){
					booklet.pageNumber = 1;
				}
			});
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
		int wheelMovement = InterfaceInput.getTrackedMouseWheel();
		if(wheelMovement > 0 && rightButton.visible){
			++booklet.pageNumber;
		}else if(wheelMovement < 0 && leftButton.visible){
			--booklet.pageNumber;
		}
		
		//Set the visible labels based on the current page.
		for(int i=0; i<pageTextLabels.size(); ++ i){
			for(GUIComponentLabel label : pageTextLabels.get(i)){
				label.visible = booklet.pageNumber == i;
			}
		}
		
		//Set the TOC buttons visible if we're on the TOC page.
		for(GUIComponentButton button : contentsButtons){
			button.visible = booklet.pageNumber == 1 && !booklet.definition.general.disableTOC;
		}
		
		//Set the TOC button to be visible on other pages.
		if(contentsButton != null){
			contentsButton.visible = booklet.pageNumber > 1;
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
	
	
	/**Custom implementation of the button class that doesn't use textures for the button rendering.
	 * This is needed for the click-able TOC page.
	 *
	 * @author don_bruce
	 */
	private abstract class ContentsButton extends GUIComponentButton{
		protected final int contentsIndex;

		public ContentsButton(int x, int y, int contentsIndex){
			super(x, y, 110, (contentsIndex + 1) + ": " + booklet.definition.general.pages[contentsIndex].title, 10, false);
			this.contentsIndex = contentsIndex;
		}

		@Override
		public void renderButton(int mouseX, int mouseY){
			//Don't render the texture.
		}
		
		@Override
		public void renderText(){
	    	if(visible){
	    		//Override the color of the text here.
	    		BuilderGUI.drawText(text, centeredText ? x + width/2 : x, y + (height-8)/2, Color.decode(booklet.definition.general.pages[contentsIndex].pageText[0].color), centeredText, false, 0);
	    	}
	    }
	}
}