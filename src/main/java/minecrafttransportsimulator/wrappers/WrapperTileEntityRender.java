package minecrafttransportsimulator.wrappers;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**Wrapper for MC TESR classes. This should NOT be used directly for any rendering as it's a wrapper.
 * Instead, use {@link ARenderTileEntityBase}, and return an instance of that class when
 * {@link ATileEntityBase#getRenderer()}} is called.  This will be cached and used as needed.
 *
 * @author don_bruce
 */
public class WrapperTileEntityRender extends TileEntitySpecialRenderer<WrapperTileEntity>{
	private static final Map<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity>> renders = new HashMap<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity>>();
	
	public WrapperTileEntityRender(){}
	
	@Override
	@SuppressWarnings("unchecked")
	public void render(WrapperTileEntity wrapper, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
		if(!renders.containsKey(wrapper.tileEntity)){
			ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity> render = wrapper.tileEntity.getRenderer();
			if(render == null){
				//Don't render, as we don't have a TESR.
				return;
			}
			renders.put(wrapper.tileEntity, render);
		}
		if(wrapper.tileEntity.world != null && wrapper.tileEntity.getBlock() instanceof IBlockTileEntity){
			//First translate and rotate to the TE location.
			//Makes for less boilerplate code.
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
			GL11.glRotatef(-wrapper.tileEntity.getBlock().getRotation(wrapper.tileEntity.world, wrapper.tileEntity.position), 0, 1, 0);
			
			//Now get and render the TE.
			ARenderTileEntityBase<ATileEntityBase, IBlockTileEntity> render = (ARenderTileEntityBase<ATileEntityBase, IBlockTileEntity>) renders.get(wrapper.tileEntity);
			render.render(wrapper.tileEntity, (IBlockTileEntity) wrapper.tileEntity.getBlock(), partialTicks);
			
			//End render matrix.
			GL11.glPopMatrix();
		}
	}
}
