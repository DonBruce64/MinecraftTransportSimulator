package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackDecorObject;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.items.blocks.ItemBlockBench;
import minecrafttransportsimulator.items.core.ItemDecor;
import minecrafttransportsimulator.items.core.ItemVehicle;
import minecrafttransportsimulator.items.parts.AItemPart;
import minecrafttransportsimulator.packets.general.PacketManualPageUpdate;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
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
	private final byte totalInfoPages;
	private final byte totalPages;
	private final List<ItemBlockBench> craftingBenches = new ArrayList<ItemBlockBench>();
    private static final ResourceLocation craftingTableTexture = new ResourceLocation("textures/gui/container/crafting_table.png");
	
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
		
		byte benches = 0;
		for(Field field : MTSRegistry.class.getFields()){
			if(field.getType().equals(Item.class)){
				try{
					Item item = (Item) field.get(null);
					if(item instanceof ItemBlockBench){
						craftingBenches.add((ItemBlockBench) item);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
				
		for(byte i=1; i<50; ++i){
			if(I18n.format("manual." + String.valueOf(i) + ".title").equals("manual." + String.valueOf(i) + ".title")){
				this.totalInfoPages = (byte) ((i - 1)*2);
				this.totalPages = (byte) (totalInfoPages + (craftingBenches.size() - 1) + 2);
				if(pageNumber > totalPages){
					pageNumber = 0;
				}
				return;
			}
		}
		this.totalInfoPages = this.totalPages = 0;
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
			if(wheelMovement > 0 && pageNumber + 1 < totalPages){
				pageNumber += pageNumber == 0 ? 1 : 2;
			}else if(wheelMovement < 0 && pageNumber > 0){
				pageNumber -= pageNumber == 1 ? 1 : 2;
			}
		}
		
		GL11.glColor3f(1, 1, 1);
		if(pageNumber != 0){
			this.mc.getTextureManager().bindTexture(background);
			this.drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, 280, 180, 512, 256);
			fontRendererObj.drawString(String.valueOf(pageNumber) + "/" + String.valueOf(totalPages), guiLeft + 15, guiTop + 10, Color.BLACK.getRGB());
			fontRendererObj.drawString(String.valueOf(pageNumber + 1) + "/" + String.valueOf(totalPages), guiLeft + 240, guiTop + 10, Color.BLACK.getRGB());
			
			leftButton.visible = true;
			leftButton.enabled = true;
			leftButton.drawButton(mc, mouseX, mouseY);

			
			if(pageNumber < totalPages){
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
		}else if(pageNumber < totalInfoPages + 3){
			drawInfoPage();
		}else{
			drawCraftingPage(mouseX, mouseY);
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
		for(byte i=1; i<=totalInfoPages/2; ++i){
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
	
	private void drawCraftingPage(int mouseX, int mouseY){
		//This is done for both sides of the book.
		//One bench for each page.
		byte benchNumber = (byte) (pageNumber - totalInfoPages - 2);
		ItemBlockBench benchItemLeft = craftingBenches.get(benchNumber);
		ItemBlockBench benchItemRight = benchNumber + 1 < craftingBenches.size() ? craftingBenches.get(benchNumber + 1) : null;
		
		//Render the header.
		String title = I18n.format(benchItemLeft.getUnlocalizedName() + ".name");
		fontRendererObj.drawString(title, (guiLeft + 75 - fontRendererObj.getStringWidth(title) / 2), guiTop + 25, Color.BLACK.getRGB());
		if(benchItemRight != null){
			title = I18n.format(benchItemRight.getUnlocalizedName() + ".name");
			fontRendererObj.drawString(title, (guiLeft + 75 + 140 - fontRendererObj.getStringWidth(title) / 2), guiTop + 25, Color.BLACK.getRGB());	
		}
		
		//Render the crafting grid.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(craftingTableTexture);
        this.drawTexturedModalRect(guiLeft + 50, guiTop + 100, 29, 16, 54, 54);
        if(benchItemRight != null){
        	this.drawTexturedModalRect(guiLeft + 50 + 140, guiTop + 100, 29, 16, 54, 54);
        }
		
        
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
		//Render the bench as an item.
        GL11.glPushMatrix();
		GL11.glTranslatef(guiLeft + 50F, guiTop + 40F, 0);
		GL11.glScalef(3, 3, 3);
		this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(benchItemLeft), 0, 0);
		GL11.glPopMatrix();
		if(benchItemRight != null){
			GL11.glPushMatrix();
			GL11.glTranslatef(guiLeft + 50F + 140F, guiTop + 40F, 0);
			GL11.glScalef(3, 3, 3);
			this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(benchItemRight), 0, 0);
			GL11.glPopMatrix();
		}
		
		//Render the items themselves..
		ItemStack hoveredStack = null;
		for(IRecipe recipe : CraftingManager.getInstance().getRecipeList()){
			if(recipe.getRecipeOutput() != null){
				int xOffset = 0;
				if(recipe.getRecipeOutput().getItem().equals(benchItemLeft)){
					xOffset = 51;
				}else if(recipe.getRecipeOutput().getItem().equals(benchItemRight)){
					xOffset = 51 + 140;
				}
				if(xOffset != 0){
					int i = guiLeft + xOffset;
					int j = guiTop + 100;
					for(ItemStack stack : ((ShapedRecipes) recipe).recipeItems){
						if(stack != null){
							//Need the ItemStack call here as the stack contains blocks sometimes that don't render.
							this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(stack.getItem()), i, j);
							if(mouseX > i && mouseX < i + 16 && mouseY > j && mouseY < j + 16){
								hoveredStack = stack;
							}
						}
						i += 18;
						if(i == guiLeft + xOffset + 18*3){
							i = guiLeft + xOffset;
							j += 18;
						}
					}
				}
			}
		}
		
		
		//We render the text for the crafting item mouseover last to ensure it doesn't render behind the items.
		if(hoveredStack != null){
			renderToolTip(hoveredStack, mouseX, mouseY);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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