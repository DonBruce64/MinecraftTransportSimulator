package minecraftflightsimulator.itemrenders;

import minecraftflightsimulator.models.ModelPropeller;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class RenderItemPropeller implements IItemRenderer{
	private static final ModelPropeller model = new ModelPropeller();
	
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
			GL11.glTranslatef(0, -0.2F, 0);
		}
		GL11.glScalef(0.8F, 0.8F, 0.8F);
		if(item.getItemDamage()%10==1){
			Minecraft.getMinecraft().renderEngine.bindTexture(model.tierTwoTexture);
		}else if(item.getItemDamage()%10==2){
			Minecraft.getMinecraft().renderEngine.bindTexture(model.tierThreeTexture);
		}else{
			Minecraft.getMinecraft().renderEngine.bindTexture(model.tierOneTexture);
		}
		model.renderPropellor(item.getItemDamage()%100/10, 75, (float) (Math.PI/4));
		GL11.glPopMatrix();
	}
}
