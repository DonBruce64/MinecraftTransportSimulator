package minecraftflightsimulator.rendering.renders.parts;

import org.lwjgl.opengl.GL11;

import minecraftflightsimulator.entities.core.EntityChild;
import minecraftflightsimulator.rendering.models.parts.ModelSeat;
import minecraftflightsimulator.systems.GL11DrawSystem;
import minecraftflightsimulator.systems.RenderSystem.RenderChild;
import net.minecraft.util.ResourceLocation;

public class RenderSeat extends RenderChild{
	private static final ModelSeat model = new ModelSeat();
	private static final ResourceLocation[] woodTextures = getWoodTextures();
	private static final ResourceLocation[] woolTextures = getWoolTextures();
	private static final ResourceLocation leatherTexture = new ResourceLocation("mfs", "textures/parts/leather.png");
	
	@Override
	public void render(EntityChild child, double x, double y, double z, float partialTicks){		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glRotatef(-child.parent.rotationYaw, 0, 1, 0);
		GL11.glRotatef(child.parent.rotationPitch, 1, 0, 0);
		GL11.glRotatef(child.parent.rotationRoll, 0, 0, 1);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11DrawSystem.bindTexture(woodTextures[child.propertyCode%6]);
		model.renderFrame();
		if(child.propertyCode < 96){
			GL11DrawSystem.bindTexture(woolTextures[child.propertyCode/6]);
		}else{
			GL11DrawSystem.bindTexture(leatherTexture);
		}
		model.renderCushion();
		GL11.glPopMatrix();
	}
	
	private static ResourceLocation[] getWoodTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_birch.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}
	
	private static ResourceLocation[] getWoolTextures(){
		ResourceLocation[] texArray = new ResourceLocation[16];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_white.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_orange.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_magenta.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_light_blue.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_yellow.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_lime.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_pink.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_gray.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_silver.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_cyan.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_purple.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_blue.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_brown.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_green.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_red.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/wool_colored_black.png");
		return texArray;
	}
}
