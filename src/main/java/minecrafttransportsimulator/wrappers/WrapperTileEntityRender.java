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
@SuppressWarnings("rawtypes")
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
		
		//If the TE exists and has its definition, render it.
		//Definition may take a bit to get to clients due to network lag.
		if(wrapper.tileEntity.world != null && wrapper.tileEntity.getDefinition() != null){
			//Get the render wrapper.
			ARenderTileEntityBase<ATileEntityBase<?>, IBlockTileEntity<?>> render = (ARenderTileEntityBase<ATileEntityBase<?>, IBlockTileEntity<?>>) renders.get(wrapper.tileEntity);
			
			//Translate and rotate to the TE location.
			//Makes for less boilerplate code.
			//Note that if we're on top of a bottom-part half-slab we translate down 0.5 units to make ourselves flush.
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glTranslatef(0.5F, render.translateToSlabs() && wrapper.tileEntity.world.isBlockBottomSlab(wrapper.tileEntity.position.newOffset(0, -1, 0)) ? -0.5F : 0.0F, 0.5F);			
			if(render.rotateToBlock()){
				GL11.glRotatef(-wrapper.tileEntity.getBlock().getRotation(wrapper.tileEntity.world, wrapper.tileEntity.position), 0, 1, 0);
			}
			
			//Render the TE.
			render.render(wrapper.tileEntity, (IBlockTileEntity) wrapper.tileEntity.getBlock(), partialTicks);
			
			//End render matrix.
			GL11.glPopMatrix();
		}
	}
}
