package minecraftflightsimulator.itemrenders;

import minecraftflightsimulator.models.ModelWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class RenderItemWheel implements IItemRenderer{
	private static final ModelWheel model = new ModelWheel();
	
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
				GL11.glTranslatef(0, 0.5F, 0);
				GL11.glRotatef(-90, 0, 1, 0);
			}
			GL11.glTranslatef(0.5F, 0.5F, 1.0F);
		}else{
			GL11.glTranslatef(0, -0.4F, 0);
		}
		if(item.getItemDamage()==1){
			Minecraft.getMinecraft().renderEngine.bindTexture(model.innerTexture);
			model.renderLargeInnerWheel(0);
			Minecraft.getMinecraft().renderEngine.bindTexture(model.outerTexture);
			model.renderLargeOuterWheel(0);
		}else{
			Minecraft.getMinecraft().renderEngine.bindTexture(model.innerTexture);
			model.renderSmallInnerWheel(0);
			Minecraft.getMinecraft().renderEngine.bindTexture(model.outerTexture);
			model.renderSmallOuterWheel(0);
		}
		GL11.glPopMatrix();
	}
}
