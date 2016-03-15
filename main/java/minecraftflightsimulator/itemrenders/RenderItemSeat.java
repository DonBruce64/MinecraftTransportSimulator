package minecraftflightsimulator.itemrenders;

import minecraftflightsimulator.models.ModelSeat;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class RenderItemSeat implements IItemRenderer{
	private static final ModelSeat model = new ModelSeat();
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type){
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data){
		GL11.glPushMatrix();
		if(!type.equals(type.INVENTORY)){
			if(type.equals(type.EQUIPPED_FIRST_PERSON)){
				GL11.glRotatef(-90, 0, 1, 0);
			}
			GL11.glTranslatef(0.5F, 0.8F, 0.5F);
		}else{
			GL11.glTranslatef(0, -0.4F, 0);
		}
		Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("minecraft", "textures/blocks/" + Blocks.wooden_slab.getIcon(0, item.getItemDamage() & 7).getIconName()  + ".png"));
		model.renderFrame();
		Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation("minecraft", "textures/blocks/" + Blocks.wool.getIcon(0, item.getItemDamage() >> 3).getIconName()  + ".png"));
		model.renderCushion();
		GL11.glPopMatrix();
	}
}
