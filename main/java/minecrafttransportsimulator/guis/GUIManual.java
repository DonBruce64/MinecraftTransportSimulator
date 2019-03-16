package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackDecorObject;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.dataclasses.PackVehicleObject;
import minecrafttransportsimulator.items.core.ItemDecor;
import minecrafttransportsimulator.items.core.ItemVehicle;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.packets.general.PacketManualPageUpdate;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
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
	private int guiLeft;
	private int guiTop;
	private int leftSideOffset;
	private int rightSideOffset;
	private final ItemStack stack;
	private final NBTTagCompound stackTag;
	private final List<PackVehicleObject> packList = new ArrayList<PackVehicleObject>();
	private final byte totalInfoPages;
	
	private GuiButton leftButton;
	private GuiButton rightButton;
	
	//These are only used in devMode for making fake item icons from models for item icon images.
	int xOffset = 160;
	int yOffset = 200;
	float scale = 6F;
	
	public GUIManual(ItemStack stack){
		//Get saved page data
		this.stack = stack;
		if(!stack.hasTagCompound()){
			this.stackTag = stack.writeToNBT(new NBTTagCompound());
		}else{
			this.stackTag = stack.getTagCompound();
		}
		this.pageNumber = stackTag.getShort("page");
		
		for(byte i=1; i<50; ++i){
			if(I18n.format("manual." + String.valueOf(i) + ".title").equals("manual." + String.valueOf(i) + ".title")){
				this.totalInfoPages = (byte) ((i - 1)*2);
				if(pageNumber > totalInfoPages + 2){
					pageNumber = 0;
				}
				return;
			}
		}
		this.totalInfoPages = 0;
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
			if(wheelMovement > 0 && pageNumber < totalInfoPages + 1){
				pageNumber += pageNumber == 0 ? 1 : 2;
			}else if(wheelMovement < 0 && pageNumber > 0){
				pageNumber -= pageNumber == 1 ? 1 : 2;
			}
		}
		
		GL11.glColor3f(1, 1, 1);
		if(pageNumber != 0){
			this.mc.getTextureManager().bindTexture(background);
			this.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 280, 180, 512, 256);
			fontRendererObj.drawString(String.valueOf(pageNumber) + "/" + String.valueOf(totalInfoPages + 2), guiLeft + 15, guiTop + 10, Color.BLACK.getRGB());
			fontRendererObj.drawString(String.valueOf(pageNumber + 1) + "/" + String.valueOf(totalInfoPages + 2), guiLeft + 240, guiTop + 10, Color.BLACK.getRGB());
			
			leftButton.visible = true;
			leftButton.enabled = true;
			leftButton.drawButton(mc, mouseX, mouseY);

			
			if(pageNumber < totalInfoPages + 1){
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
		
		//We cheat and render an item here in devMode for making item icon pictures.
		//If no devMode, then do things as normal.
		if(ConfigSystem.getBooleanConfig("DevMode") && mc.isSingleplayer()){
			ItemStack stack = mc.thePlayer.getHeldItemOffhand();
			if(stack != null && stack.getItem() != null){
				final ResourceLocation modelLocation;
				final ResourceLocation textureLocation;
				if(stack.getItem() instanceof ItemVehicle){
					ItemVehicle item = (ItemVehicle) stack.getItem();
					String packName = item.vehicleName.substring(0, item.vehicleName.indexOf(':'));
					modelLocation = new ResourceLocation(packName, "objmodels/vehicles/" + PackParserSystem.getVehicleJSONName(item.vehicleName) + ".obj");
					textureLocation = new ResourceLocation(packName, "textures/vehicles/" + item.vehicleName.substring(item.vehicleName.indexOf(':') + 1) + ".png");
				}else if(stack.getItem() instanceof AItemPart){
					AItemPart item = (AItemPart) stack.getItem();
					PackPartObject pack = PackParserSystem.getPartPack(item.partName);
					String packName = item.partName.substring(0, item.partName.indexOf(':'));
					if(pack.general.modelName != null){
						modelLocation = new ResourceLocation(packName, "objmodels/parts/" + pack.general.modelName + ".obj");
					}else{
						modelLocation = new ResourceLocation(packName, "objmodels/parts/" + item.partName.substring(item.partName.indexOf(':') + 1) + ".obj");
					}
					textureLocation = new ResourceLocation(packName, "textures/parts/" + item.partName.substring(item.partName.indexOf(':') + 1) + ".png");
				}else if(stack.getItem() instanceof ItemDecor){
					ItemDecor item = (ItemDecor) stack.getItem();
					PackDecorObject pack = PackParserSystem.getDecor(item.decorName);
					String packName = item.decorName.substring(0, item.decorName.indexOf(':'));
					modelLocation = new ResourceLocation(packName, "objmodels/decors/" + item.decorName.substring(item.decorName.indexOf(':') + 1) + ".obj");
					textureLocation = new ResourceLocation(packName, "textures/decors/" + item.decorName.substring(item.decorName.indexOf(':') + 1) + ".png");
				}else{
					modelLocation = null;
					textureLocation = null;
				}
				
				if(modelLocation != null){
					this.mc.getTextureManager().bindTexture(textureLocation);
					GL11.glTranslatef(guiLeft + xOffset, guiTop + yOffset, 0);
					GL11.glRotatef(180, 0, 0, 1);
					GL11.glRotatef(45, 0, 1, 0);
					GL11.glRotatef(35.264F, 1, 0, 1);
					GL11.glScalef(scale, scale, scale);
					GL11.glBegin(GL11.GL_TRIANGLES);
					for(Entry<String, Float[][]> entry : OBJParserSystem.parseOBJModel(modelLocation.getResourceDomain(), modelLocation.getResourcePath()).entrySet()){
						for(Float[] vertex : entry.getValue()){
							GL11.glTexCoord2f(vertex[3], vertex[4]);
							GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
							GL11.glVertex3f(-vertex[0], vertex[1], vertex[2]);
						}
					}
					GL11.glEnd();
				}
			}
		}
		
		
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
		for(byte i=1; i<totalInfoPages/2; ++i){
			String title = I18n.format("manual." + String.valueOf(i) + ".title");
			fontRendererObj.drawString(String.valueOf(i*2 + 1) + ": " + title, contentsCount < 10 ? leftSideOffset : rightSideOffset, guiTop + 45 + 10*contentsLine, Color.BLACK.getRGB());
			++contentsCount;
			++contentsLine;
			if(contentsLine == 10){
				contentsLine = 0;
			}
		}
	}
	
	private void drawInfoPage(){
		int topOffset = guiTop + 25;
		byte headerOffset = 20;
		byte sectionNumber = (byte) ((pageNumber - 1)/2);
		
		String title = I18n.format("manual." + sectionNumber  + ".title");
		fontRendererObj.drawString(title, (guiLeft + 75 - fontRendererObj.getStringWidth(title) / 2), topOffset, Color.BLACK.getRGB());
		GL11.glPushMatrix();
		GL11.glTranslatef(leftSideOffset, topOffset + headerOffset, 0);
		GL11.glScalef(0.75F, 0.75F, 0.75F);
		fontRendererObj.drawSplitString(I18n.format("manual." + sectionNumber + "." + "1"), 0, 0, 150, Color.BLACK.getRGB());	
		GL11.glTranslatef((rightSideOffset - leftSideOffset)/0.75F, -headerOffset/0.75F, 0);
		fontRendererObj.drawSplitString(I18n.format("manual." + sectionNumber + "." + "2"), 0, 0, 150, Color.BLACK.getRGB());
		GL11.glPopMatrix();
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
		MTS.MTSNet.sendToServer(new PacketManualPageUpdate(pageNumber));
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
        }else if(ConfigSystem.getBooleanConfig("DevMode") && mc.isSingleplayer()){
        	//Do devMode manipulation here.
        	if(keyCode == Keyboard.KEY_UP){
        		--yOffset;
        	}else if(keyCode == Keyboard.KEY_DOWN){
        		++yOffset;
        	}else if(keyCode == Keyboard.KEY_LEFT){
        		--xOffset;
        	}else if(keyCode == Keyboard.KEY_RIGHT){
        		++xOffset;
        	}else if(keyCode == Keyboard.KEY_PRIOR){
        		scale += 0.5F;
        	}else if(keyCode == Keyboard.KEY_NEXT){
        		scale -= 0.5F;
        	}
        }
	}
}