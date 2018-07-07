package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackMultipartObject;
import minecrafttransportsimulator.packets.general.ManualPageUpdatePacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIManual extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/manual_pages.png");
	private static final ResourceLocation cover = new ResourceLocation(MTS.MODID, "textures/guis/manual_cover.png");
	
	private short pageNumber;
	private short maxPages;
	private int guiLeft;
	private int guiTop;
	private int leftSideOffset;
	private int rightSideOffset;
	private final ItemStack stack;
	private final NBTTagCompound stackTag;
	private final List<PackMultipartObject> packList = new ArrayList<PackMultipartObject>();
	
	private GuiButton leftButton;
	private GuiButton rightButton;
	
	public GUIManual(ItemStack stack){
		//Get saved page data
		this.stack = stack;
		if(!stack.hasTagCompound()){
			this.stackTag = stack.writeToNBT(new NBTTagCompound());
		}else{
			this.stackTag = stack.getTagCompound();
		}
		this.pageNumber = stackTag.getShort("page");
		
		//Add two pages for the table of contents.
		this.maxPages += 2;
		
		//Calculate the number of info pages, add those to maxPages.
		for(InfoPages pageDef : InfoPages.values()){
			maxPages += pageDef.pages%2 == 1 ? pageDef.pages + 1 : pageDef.pages;
		}
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 280)/2;
		guiTop = (this.height - 180)/2;
		leftSideOffset = guiLeft + 20;
		rightSideOffset = guiLeft + 155;
		buttonList.add(leftButton = new GuiButton(0, guiLeft + 10, guiTop + 150, 20, 20, "<"));
		buttonList.add(rightButton = new GuiButton(0, guiLeft + 250, guiTop + 150, 20, 20, ">"));
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		
		if(Mouse.isCreated() && Mouse.hasWheel()){
			int wheelMovement = Mouse.getDWheel();
			if(wheelMovement > 0 && pageNumber < maxPages - 1){
				pageNumber += pageNumber == 0 ? 1 : 2;
			}else if(wheelMovement < 0 && pageNumber > 0){
				pageNumber -= pageNumber == 1 ? 1 : 2;
			}
		}
		
		if(pageNumber > maxPages){
			pageNumber = maxPages;
		}
		GL11.glColor3f(1, 1, 1);
		if(pageNumber != 0){
			this.mc.getTextureManager().bindTexture(background);
			this.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 280, 180, 512, 256);
			fontRendererObj.drawString(String.valueOf(pageNumber) + "/" + String.valueOf(maxPages), guiLeft + 15, guiTop + 10, Color.BLACK.getRGB());
			fontRendererObj.drawString(String.valueOf(pageNumber + 1) + "/" + String.valueOf(maxPages), guiLeft + 240, guiTop + 10, Color.BLACK.getRGB());
			
			leftButton.visible = true;
			leftButton.enabled = true;
			leftButton.drawButton(mc, mouseX, mouseY);

			
			if(pageNumber < maxPages - 1){
				rightButton.visible = true;
				rightButton.enabled = true;
				rightButton.drawButton(mc, mouseX, mouseY);
			}else{
				rightButton.visible = false;
				rightButton.enabled = false;
			}
		}else{
			this.mc.getTextureManager().bindTexture(cover);
			this.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 280, 180, 512, 256);
			leftButton.visible = false;
			leftButton.enabled = false;
			rightButton.visible = true;
			rightButton.enabled = true;
			rightButton.drawButton(mc, mouseX, mouseY);
		}
		
		if(pageNumber == 0){
			 drawCover();
		}else if(pageNumber == 1){
			drawContentsPage();
		}else{
			drawInfoPage();
		}
	}

	
	private void drawCover(){
		GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 170, guiTop + 25, 0);
		GL11.glScalef(1.5F, 1.5F, 1.5F);
		fontRendererObj.drawString("MINECRAFT", 0, 10, Color.WHITE.getRGB());
		fontRendererObj.drawString("TRANSPORT", 0, 20, Color.WHITE.getRGB());
		fontRendererObj.drawString("SIMULATOR", 0, 30, Color.WHITE.getRGB());
		fontRendererObj.drawString("HANDBOOK", 0, 40, Color.WHITE.getRGB());
		GL11.glScalef(1F/1.5F, 1F/1.5F, 1F/1.5F);
		GL11.glPopMatrix();
		
		fontRendererObj.drawString("Formerly Minecraft", guiLeft + 160, guiTop + 140, Color.WHITE.getRGB());
		fontRendererObj.drawString("  Flight Simulator", guiLeft + 160, guiTop + 150, Color.WHITE.getRGB());
	}
	
	private void drawContentsPage(){
		byte contentsLine = 0;
		byte contentsCount = 0;
		short pageCount = 3;
		
		fontRendererObj.drawString("CONTENTS", guiLeft + 50, guiTop + 25, Color.BLACK.getRGB());
		for(InfoPages pageDef : InfoPages.values()){
			String title = I18n.format("manual." + pageDef.name().toLowerCase() + ".title");
			fontRendererObj.drawString(String.valueOf(pageCount) + ": " + title, contentsCount < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10*contentsLine, Color.BLACK.getRGB());
			pageCount += pageDef.pages%2 == 1 ? pageDef.pages + 1 : pageDef.pages;
			++contentsCount;
			++contentsLine;
			if(contentsLine == 10){
				contentsLine = 0;
			}
		}
	}
	
	private void drawInfoPage(){
		short pageIndex = 3;
		int TopOffset = guiTop + 25;
		
		for(InfoPages pageDef : InfoPages.values()){
			if(pageNumber > pageIndex + pageDef.pages - 1){
				//If there's and odd number of pages in this section add an extra one so sections don't start on the right side.
				pageIndex += pageDef.pages%2 == 1 ? pageDef.pages + 1 : pageDef.pages;
				continue;
			}else{
				String sectionName = pageDef.name().toLowerCase();
				//Check if this section would contain the title and put it on the left page.
				byte headerOffset = 0;
				if(pageNumber == pageIndex){
					String title = I18n.format("manual." + sectionName + ".title");
					fontRendererObj.drawString(title, (guiLeft + 75 - fontRendererObj.getStringWidth(title) / 2), TopOffset, Color.BLACK.getRGB());
					headerOffset = 20;
				}
				
				//If odd page, render on the left side of the book.  Even goes on the right.
				GL11.glPushMatrix();
				GL11.glTranslatef(leftSideOffset, TopOffset + headerOffset, 0);
				GL11.glScalef(0.75F, 0.75F, 0.75F);
				fontRendererObj.drawSplitString(I18n.format("manual." + sectionName + "." + String.valueOf(pageNumber - pageIndex + 1)), 0, 0, 150, Color.BLACK.getRGB());	
				if(pageNumber - pageIndex + 2 <= pageDef.pages){
					GL11.glTranslatef((rightSideOffset - leftSideOffset)/0.75F, -headerOffset/0.75F, 0);
					fontRendererObj.drawSplitString(I18n.format("manual." + sectionName + "." + String.valueOf(pageNumber - pageIndex + 2)), 0, 0, 150, Color.BLACK.getRGB());
				}
				GL11.glPopMatrix();
				return;
				
			}
		}
	}
	
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(leftButton)){
			pageNumber -= pageNumber == 1 ? 1 : 2;
		}else if(buttonClicked.equals(rightButton)){
			pageNumber += pageNumber == 0 ? 1 : 2;
		}
	}
	
	@Override
	public void onGuiClosed(){
		MTS.MTSNet.sendToServer(new ManualPageUpdatePacket(pageNumber));
		stackTag.setShort("page", pageNumber);
		stack.setTagCompound(stackTag);
	}
	
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		if(keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)){
			super.keyTyped('0', 1);
        }
	}
	
	public static enum InfoPages{
		INTRODUCTION((byte) 3),
		BASIC_CRAFTING((byte) 2),
		VEHICLE_ASSEMBLY((byte) 3),
		FUELING((byte) 2),
		CONTROLS((byte) 4),
		ENGINES((byte) 2),
		THEFT_PREVENTION((byte) 2),
		PRE_FLIGHT_PREP((byte) 2),
		IN_FLIGHT((byte) 2),
		LANDING((byte) 2),
		INSTRUMENTS((byte) 2),
		PROPELLER_SPECS((byte) 2);
		
		public final byte pages;
		
		private InfoPages(byte pages){
			this.pages = pages;
		}
	}
}