package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.parts.EntitySeat;
import minecraftflightsimulator.models.ModelSeat;
import minecraftflightsimulator.utilities.RenderHelper.RenderEntityBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderSeat extends RenderEntityBase{
	private static final ModelSeat model = new ModelSeat();
	private static ResourceLocation[] woodTextures = getWoodTextures();
	private static ResourceLocation[] woolTextures = getWoolTextures();
	
    public RenderSeat(RenderManager manager){
        super(manager);
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntitySeat seat=(EntitySeat) entity;
		if(seat.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-seat.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(seat.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(seat.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.bindTexture(woodTextures[seat.propertyCode >> 4 > 5 ? 0 : seat.propertyCode >> 4]);
			model.renderFrame();
			this.bindTexture(woolTextures[seat.propertyCode & 15]);
			model.renderCushion();
			GL11.glPopMatrix();
		}
	}
	
	private static ResourceLocation[] getWoodTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
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