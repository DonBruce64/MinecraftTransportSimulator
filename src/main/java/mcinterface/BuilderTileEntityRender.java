package mcinterface;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.components.RenderTickData;
import minecrafttransportsimulator.rendering.instances.ARenderTileEntityBase;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**Builder for MC TESR classes. This should NOT be used directly for any rendering as it's a builder
 * to create an interface that MC will accept that we can forward calls from to our own code.
 * For actual rendering, create a class {@link ARenderTileEntityBase}, and return an instance of that class when
 * {@link ATileEntityBase#getRenderer()}} is called.  This will be cached and used as needed.
 *
 * @author don_bruce
 */
@SuppressWarnings("rawtypes")
public class BuilderTileEntityRender extends TileEntitySpecialRenderer<BuilderTileEntity>{
	private static final Map<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity>> renders = new HashMap<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends IBlockTileEntity>>();
	//RENDER DATA MAPS.  Keyed by each instance of each Tile Entity loaded.
	private static final Map<ATileEntityBase, RenderTickData> renderData = new HashMap<ATileEntityBase, RenderTickData>();
	
	public BuilderTileEntityRender(){}
	
	@Override
	@SuppressWarnings("unchecked")
	public void render(BuilderTileEntity wrapper, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
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
		if(wrapper.tileEntity.world != null && wrapper.tileEntity.position != null && wrapper.tileEntity.getDefinition() != null){
			//Get the render wrapper.
			ARenderTileEntityBase<ATileEntityBase<?>, IBlockTileEntity<?>> render = (ARenderTileEntityBase<ATileEntityBase<?>, IBlockTileEntity<?>>) renders.get(wrapper.tileEntity);
			
			//If we don't have render data yet, create one now.
			if(!renderData.containsKey(wrapper.tileEntity)){
				renderData.put(wrapper.tileEntity, new RenderTickData(wrapper.tileEntity.world));
			}
			
			//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
			int renderPass = InterfaceRender.getRenderPass();
			if(renderPass == -1){
				renderPass = 2;
			}
			
			//If we need to render, do so now.
			if(renderData.get(wrapper.tileEntity).shouldRender(renderPass, partialTicks)){
				//Translate and rotate to the TE location.
				//Makes for less boilerplate code.
				//Note that if we're on top of a bottom-part half-slab we translate down 0.5 units to make ourselves flush.
				GL11.glPushMatrix();
				GL11.glTranslated(x, y, z);
				GL11.glTranslatef(0.5F, render.translateToSlabs() && wrapper.tileEntity.world.isBlockBottomSlab(wrapper.tileEntity.position.copy().add(0, -1, 0)) ? -0.5F : 0.0F, 0.5F);			
				if(render.rotateToBlock()){
					ABlockBase block = wrapper.tileEntity.getBlock();
					if(block != null){
						GL11.glRotatef(-block.getRotation(wrapper.tileEntity.world, wrapper.tileEntity.position), 0, 1, 0);
					}else{
						return;
					}
				}
				
				//Set lighting and Render the TE.
				InterfaceRender.setLightingToBlock(wrapper.tileEntity.position);
				render.render(wrapper.tileEntity, (IBlockTileEntity) wrapper.tileEntity.getBlock(), partialTicks);
				
				//End render matrix and reset states.
				GL11.glPopMatrix();
				InterfaceRender.resetStates();
			}
		}
	}
}
