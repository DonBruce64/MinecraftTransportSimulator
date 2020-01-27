package minecrafttransportsimulator.guis;

import java.io.IOException;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.items.packs.ItemDecor;
import minecrafttransportsimulator.items.packs.ItemVehicle;
import minecrafttransportsimulator.items.packs.parts.AItemPart;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.OBJParserSystem;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GUIDevRender extends GuiScreen{	
	private int guiLeft;
	private int guiTop;
	private static int xOffset = 160;
	private static int yOffset = 200;
	private static float scale = 6F;
	
	public GUIDevRender(){}
	
	@Override 
	public void initGui(){
		super.initGui();
		guiLeft = (this.width - 280)/2;
		guiTop = (this.height - 180)/2;
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks){
		super.drawScreen(mouseX, mouseY, renderPartialTicks);
		if(ConfigSystem.configObject.client.devMode.value && mc.isSingleplayer()){
			ItemStack stack = mc.player.getHeldItemOffhand();
			if(stack != null && stack.getItem() != null){
				final ResourceLocation modelLocation;
				final ResourceLocation textureLocation;
				if(stack.getItem() instanceof ItemVehicle){
					ItemVehicle item = (ItemVehicle) stack.getItem();
					String vehicleGenericName = item.definition.systemName.substring(0, item.definition.systemName.length() - item.subName.length() - 1);
					modelLocation = new ResourceLocation(item.definition.packID, "objmodels/vehicles/" + vehicleGenericName + ".obj");
					textureLocation = new ResourceLocation(item.definition.packID, "textures/vehicles/" + item.definition.systemName + ".png");
				}else if(stack.getItem() instanceof AItemPart){
					AItemPart item = (AItemPart) stack.getItem();
					if(item.definition.general.modelName != null){
						modelLocation = new ResourceLocation(item.definition.packID, "objmodels/parts/" + item.definition.general.modelName + ".obj");
					}else{
						modelLocation = new ResourceLocation(item.definition.packID, "objmodels/parts/" + item.definition.systemName + ".obj");
					}
					textureLocation = new ResourceLocation(item.definition.packID, "textures/parts/" + item.definition.systemName + ".png");
				}else if(stack.getItem() instanceof ItemDecor){
					ItemDecor item = (ItemDecor) stack.getItem();
					modelLocation = new ResourceLocation(item.definition.packID, "objmodels/decors/" + item.definition.systemName + ".obj");
					textureLocation = new ResourceLocation(item.definition.packID, "textures/decors/" + item.definition.systemName + ".png");
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
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		if(keyCode == 1 || mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)){
			super.keyTyped('0', 1);
        }else if(ConfigSystem.configObject.client.devMode.value && mc.isSingleplayer()){
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