package minecraftflightsimulator.itemrenders;

import minecraftflightsimulator.models.ModelPlane;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class RenderItemPlane implements IItemRenderer{
	private ModelPlane model;
	
	public RenderItemPlane(ModelPlane model){
		super();
		this.model=model;
	}
	
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
		GL11.glScalef(0.25F, 0.25F, 0.25F);
		model.renderPlane(Minecraft.getMinecraft().renderEngine, (byte) item.getItemDamage(), 0, 0, 0, 0);
		GL11.glPopMatrix();
	}
}
