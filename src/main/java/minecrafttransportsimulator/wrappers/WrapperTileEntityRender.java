package minecrafttransportsimulator.wrappers;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.rendering.blocks.ARenderTileEntityBase;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

/**Wrapper for MC TESR classes. This should NOT be used directly for any rendering as it's a wrapper.
 * Instead, use {@link ARenderTileEntityBase} and call {@link #registerTESR(ARenderTileEntityBase)}.
 * This will automatically register that render to be called when it's time.
 *
 * @author don_bruce
 */
public class WrapperTileEntityRender extends TileEntitySpecialRenderer<WrapperTileEntity>{
	private static final Map<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider>> renders = new HashMap<ATileEntityBase, ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider>>();
	
	public WrapperTileEntityRender(){}
	
	@Override
	public void render(WrapperTileEntity wrapper, double x, double y, double z, float partialTicks, int destroyStage, float alpha){
		//FIXME Find out which TE this wrapper goes to and call its render routine.
		if(!renders.containsKey(wrapper.tileEntity)){
			ARenderTileEntityBase<? extends ATileEntityBase, ? extends WrapperTileEntity.IProvider> render = wrapper.tileEntity.getRenderer();
			if(render == null){
				//Don't render, as we don't have a TESR.
				return;
			}
			renders.put(wrapper.tileEntity, render);
		}
		//FIXME find out how Forge does casting.
		if(wrapper.tileEntity.world != null && wrapper.tileEntity.getBlock() instanceof WrapperTileEntity.IProvider){
			renders.get(wrapper.tileEntity).render(wrapper.tileEntity, partialTicks);
		}
	}
}
