package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIManual extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/manual.png");
	private static final ResourceLocation cover = new ResourceLocation(MTS.MODID, "textures/guis/manual_cover.png");
	
	private short pageNumber;
	private short maxPages;
	private int guiLeft;
	private int guiTop;
	private int leftSideOffset;
	private int rightSideOffset;
	private final ItemStack stack;
	private final NBTTagCompound stackTag;
	private final Map<String, List<String>> packDataMap = new HashMap<String, List<String>>();
	
	private GuiButton leftButton;
	private GuiButton rightButton;
	
	public GUIManual(ItemStack stack){
		this.stack = stack;
		if(!stack.hasTagCompound()){
			this.stackTag = stack.writeToNBT(new NBTTagCompound());
		}else{
			this.stackTag = stack.getTagCompound();
		}
		this.pageNumber = stackTag.getShort("page");
		
		maxPages += PackParserSystem.getRegisteredNames().size()*2;
		for(InfoPages pageDef : InfoPages.values()){
			maxPages += pageDef.pages;
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
			if(wheelMovement > 0 && pageNumber < maxPages){
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
			rightButton.visible = true;
			leftButton.drawButton(mc, mouseX, mouseY);
			rightButton.drawButton(mc, mouseX, mouseY);
		}else{
			this.mc.getTextureManager().bindTexture(cover);
			this.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 280, 180, 512, 256);
			leftButton.visible = false;
			rightButton.visible = true;
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
		fontRendererObj.drawString("CONTENTS", guiLeft + 50, guiTop + 25, Color.BLACK.getRGB());
		fontRendererObj.drawString("06: Introduction", leftSideOffset, guiTop + 45, Color.BLACK.getRGB());
		fontRendererObj.drawString("08: Basic Crafting", leftSideOffset, guiTop + 55, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Vehicle Assembly", leftSideOffset, guiTop + 65, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Fueling", leftSideOffset, guiTop + 75, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Controls", leftSideOffset, guiTop + 85, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Pre-Flight Prep", leftSideOffset, guiTop + 95, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: First Flight", leftSideOffset, guiTop + 105, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: In-Flight", leftSideOffset, guiTop + 115, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Landing", leftSideOffset, guiTop + 125, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Instruments", leftSideOffset, guiTop + 135, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Engine Health", rightSideOffset, guiTop + 45, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Theft Prevention", rightSideOffset, guiTop + 55, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Propeller Specs", rightSideOffset, guiTop + 65, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Body Damage", rightSideOffset, guiTop + 75, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Joystick Notes", rightSideOffset, guiTop + 85, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: Vehicle Data", rightSideOffset, guiTop + 95, Color.BLACK.getRGB());
		fontRendererObj.drawString("    & Crafting Info", rightSideOffset, guiTop + 105, Color.BLACK.getRGB());
		fontRendererObj.drawString("00: JSON Pack System", rightSideOffset, guiTop + 125, Color.BLACK.getRGB());
	}
	
	private void drawInfoPage(){
		short pageIndex = 3;
		int TopOffset = guiTop + 25;
		
		for(InfoPages pageDef : InfoPages.values()){
			//System.out.println(pageIndex);
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
				fontRendererObj.drawSplitString(I18n.format("manual." + sectionName + "." + String.valueOf(pageNumber - pageIndex + 1)), leftSideOffset, TopOffset + headerOffset, 110, Color.BLACK.getRGB());	
				if(pageNumber - pageIndex + 2 <= pageDef.pages){
					fontRendererObj.drawSplitString(I18n.format("manual." + sectionName + "." + String.valueOf(pageNumber - pageIndex + 2)), rightSideOffset, TopOffset, 110, Color.BLACK.getRGB());
				}
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
		stackTag.setShort("page", pageNumber);
		stack.setTagCompound(stackTag);
	}
	
	public static enum InfoPages{
		INTRODUCTION((byte) 5),
		BASIC_CRAFTING((byte) 4),
		VEHICLE_ASSEMBLY((byte) 7),
		FUELING((byte) 6),
		CONTROLS((byte) 10);
		
		public final byte pages;
		
		private InfoPages(byte pages){
			this.pages = pages;
		}
	}
}