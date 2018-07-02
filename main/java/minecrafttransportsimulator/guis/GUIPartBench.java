package minecrafttransportsimulator.guis;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.BlockPartBench;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.packets.general.PlayerCraftingPacket;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GUIPartBench extends GuiScreen{
	private static final ResourceLocation background = new ResourceLocation(MTS.MODID, "textures/guis/crafting_parts.png");	
	private final String[] partTypes;
	private final EntityPlayer player;
	
	private GuiButton leftPackButton;
	private GuiButton rightPackButton;
	private GuiButton leftPartButton;
	private GuiButton rightPartButton;
	private GuiButton startButton;
	
	private int guiLeft;
	private int guiTop;
	
	private String packName = "";
	private String prevPackName = "";
	private String nextPackName = "";
	
	private String partName = "";
	private String prevPartName = "";
	private String nextPartName = "";
	
	/**Display list GL integers.  Keyed by part name.*/
	private final Map<String, Integer> partDisplayLists = new HashMap<String, Integer>();
	
	/**Part texture name.  Keyed by part name.*/
	private final Map<String, ResourceLocation> textureMap = new HashMap<String, ResourceLocation>();
	
	public GUIPartBench(BlockPartBench bench, EntityPlayer player){
		this.partTypes = bench.partTypes;
		this.player = player;
		updatePartNames();
	}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 256)/2;
		guiTop = (this.height - 201)/2;
		
		buttonList.add(leftPackButton = new GuiButton(0, guiLeft + 25, guiTop + 5, 20, 20, "<"));
		buttonList.add(rightPackButton = new GuiButton(0, guiLeft + 215, guiTop + 5, 20, 20, ">"));
		buttonList.add(leftPartButton = new GuiButton(0, guiLeft + 25, guiTop + 25, 20, 20, "<"));
		buttonList.add(rightPartButton = new GuiButton(0, guiLeft + 215, guiTop + 25, 20, 20, ">"));
		buttonList.add(startButton = new GuiButton(0, guiLeft + 188, guiTop + 170, 20, 20, ""));
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		
		//Draw header text, graphics, and buttons.
		GL11.glColor3f(1, 1, 1); //Not sure why buttons make this grey, but whatever...
		mc.getTextureManager().bindTexture(background);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, 256, 201);
		if(startButton.enabled){
			drawTexturedModalRect(guiLeft + 140, guiTop + 173, 0, 201, 44, 16);
		}
		drawCenteredString(!packName.isEmpty() ? I18n.format("itemGroup." + packName) : "", guiLeft + 130, guiTop + 10);
		drawCenteredString(!partName.isEmpty() ? I18n.format(MTSRegistry.partItemMap.get(partName).getUnlocalizedName() + ".name") : "", guiLeft + 130, guiTop + 30);
		
		//Render descriptive text.
		ItemStack tempStack = new ItemStack(MTSRegistry.partItemMap.get(partName));
		tempStack.setTagCompound(new NBTTagCompound());
		List<String> descriptiveLines = new ArrayList<String>();
		tempStack.getItem().addInformation(tempStack, player, descriptiveLines, false);
		int lineOffset = 55;
		for(String line : descriptiveLines){
			mc.fontRendererObj.drawStringWithShadow(line, guiLeft + 10, guiTop + lineOffset, Color.WHITE.getRGB());
			lineOffset += 10;
		}
		
		//Set button states and render.
		startButton.enabled = PlayerCraftingPacket.doesPlayerHaveMaterials(player, partName);
		leftPackButton.enabled = !prevPackName.isEmpty();
		rightPackButton.enabled = !nextPackName.isEmpty();
		leftPartButton.enabled = !prevPartName.isEmpty();
		rightPartButton.enabled = !nextPartName.isEmpty();
		for(Object obj : buttonList){
			((GuiButton) obj).drawButton(mc, mouseX, mouseY);
		}
		this.drawRect(guiLeft + 190, guiTop + 188, guiLeft + 206, guiTop + 172, startButton.enabled ? Color.GREEN.getRGB() : Color.RED.getRGB());
		
		//Render materials in the bottom slots.
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.enableGUIStandardItemLighting();
		int stackOffset = 9;
		for(ItemStack craftingStack : PackParserSystem.getMaterials(partName)){
			this.itemRender.renderItemAndEffectIntoGUI(craftingStack, guiLeft + stackOffset, guiTop + 172);
			this.itemRender.renderItemOverlays(fontRendererObj, craftingStack, guiLeft + stackOffset, guiTop + 172);
			stackOffset += 18;
		}
		
		//Render the 3D model.  Cache the model if we haven't done so already.
		if(!partDisplayLists.containsKey(partName)){
			ResourceLocation partModelLocation;
			if(PackParserSystem.getPartPack(partName).general.modelName != null){
				partModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + PackParserSystem.getPartPack(partName).general.modelName + ".obj");
			}else{
				partModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + partName.substring(partName.indexOf(':') + 1) + ".obj");
			}
			
			Map<String, Float[][]> parsedModel = OBJParserSystem.parseOBJModel(partModelLocation);
			int displayListIndex = GL11.glGenLists(1);
			GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
			GL11.glBegin(GL11.GL_TRIANGLES);
			for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				for(Float[] vertex : entry.getValue()){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(-vertex[0], vertex[1], vertex[2]);
				}
			}
			GL11.glEnd();
			GL11.glEndList();
			partDisplayLists.put(partName, displayListIndex);
		}
		if(!textureMap.containsKey(partName)){
			ResourceLocation partTextureLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "textures/parts/" + partName.substring(partName.indexOf(':') + 1) + ".png");
			textureMap.put(partName, partTextureLocation);
		}
		
		GL11.glPushMatrix();
		GL11.glDisable(GL11.GL_LIGHTING);
		mc.getTextureManager().bindTexture(textureMap.get(partName));
		GL11.glTranslatef(guiLeft + 190, guiTop + 110, 100);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glRotatef(45, 0, 1, 0);
		GL11.glRotatef(35.264F, 1, 0, 1);
		GL11.glRotatef(-player.worldObj.getTotalWorldTime()*2, 0, 1, 0);
		float scale = 30F;
		GL11.glScalef(scale, scale, scale);
		GL11.glCallList(partDisplayLists.get(partName));
		GL11.glPopMatrix();
		
	}
    
	@Override
    protected void actionPerformed(GuiButton buttonClicked) throws IOException{
		super.actionPerformed(buttonClicked);
		if(buttonClicked.equals(startButton)){
			MTS.proxy.playSound(player.getPositionVector(), MTS.MODID + ":bench_running", 1, 1);
			MTS.MTSNet.sendToServer(new PlayerCraftingPacket(player, partName));
			mc.thePlayer.closeScreen();
			return;
		}else{
			if(buttonClicked.equals(leftPackButton)){
				packName = prevPackName;
				partName = "";
			}else if(buttonClicked.equals(rightPackButton)){
				packName = nextPackName;
				partName = "";
			}else if(buttonClicked.equals(leftPartButton)){
				partName = prevPartName;
			}else if(buttonClicked.equals(rightPartButton)){
				partName = nextPartName;
			}
			updatePartNames();
		}
	}
	
	@Override
	public boolean doesGuiPauseGame(){
		return false;
	}
	
	@Override
    public void onGuiClosed(){
		//Clear out the displaylists to free RAM once we no longer need them here.
		for(int displayListID : partDisplayLists.values()){
			GL11.glDeleteLists(displayListID, 1);
		}
    }
	
	private void drawCenteredString(String stringToDraw, int x, int y){
		mc.fontRendererObj.drawString(stringToDraw, x - mc.fontRendererObj.getStringWidth(stringToDraw)/2, y, 4210752);
	}
	
	private void updatePartNames(){
		prevPackName = "";
		nextPackName = "";	
		prevPartName = "";
		nextPartName = "";
		
		boolean passedPack = false;
		boolean passedPart = false;
		for(String name : MTSRegistry.partItemMap.keySet()){
			for(String partType : partTypes){
				if(PackParserSystem.getPartPack(name).general.type.equals(partType)){
					if(packName.isEmpty()){
						packName = name.substring(0, name.indexOf(':'));
					}else if(!passedPack && !name.startsWith(packName)){
						prevPackName = name.substring(0, name.indexOf(':'));
					}
					if(name.startsWith(packName)){
						passedPack = true;
						if(partName.isEmpty()){
							partName = name;
							passedPart = true;
						}else if(partName.equals(name)){
							passedPart = true;
						}else if(!passedPart){
							prevPartName = name;
						}else if(nextPartName.isEmpty()){
							nextPartName = name;
						}
					}else if(nextPackName.isEmpty() && passedPack){
						nextPackName = name.substring(0, name.indexOf(':'));
					}
				}
			}
		}
	}
}
