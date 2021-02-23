package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.instances.AnimationsDecor;
import minecrafttransportsimulator.rendering.instances.RenderPole;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component extends AEntityC_Definable<JSONPoleComponent>{
	
	private final TileEntityPole core;
	
	public ATileEntityPole_Component(TileEntityPole core, WrapperNBT data){
		super(core.world, data);
		this.core = core;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RenderPole getRenderer(){
		return core.getRenderer();
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnimationsDecor getAnimator(){
		return core.getAnimator();
	}
}
