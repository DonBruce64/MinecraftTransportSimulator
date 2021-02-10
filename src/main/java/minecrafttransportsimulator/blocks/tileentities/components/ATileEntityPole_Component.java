package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.instances.AnimationsPole;
import minecrafttransportsimulator.rendering.instances.RenderPole;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component extends AEntityC_Definable<JSONPoleComponent>{
	
	private final TileEntityPole core;
	
	public ATileEntityPole_Component(TileEntityPole core, ItemPoleComponent item, WrapperNBT data){
		super(core.world, null, item.definition, data);
		this.core = core;
	}
	
	/**
	 *  Returns the light level on this component.  Used for dynamic lighting states
	 *  based on the state of the component.
	 */
	public abstract float lightLevel();

	@Override
	@SuppressWarnings("unchecked")
	public RenderPole getRenderer(){
		return core.getRenderer();
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnimationsPole getAnimator(){
		return core.getAnimator();
	}
}
