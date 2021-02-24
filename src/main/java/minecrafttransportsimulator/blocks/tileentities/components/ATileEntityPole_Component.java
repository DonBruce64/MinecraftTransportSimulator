package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.rendering.instances.AnimationsPoleComponent;
import minecrafttransportsimulator.rendering.instances.RenderPoleComponent;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component extends AEntityC_Definable<JSONPoleComponent>{
	
	public final TileEntityPole core;
	
	private static final AnimationsPoleComponent animator = new AnimationsPoleComponent();
	private static RenderPoleComponent renderer;
	
	public ATileEntityPole_Component(TileEntityPole core, WrapperNBT data){
		super(core.world, data);
		this.core = core;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public AnimationsPoleComponent getAnimator(){
		return animator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RenderPoleComponent getRenderer(){
		if(renderer == null){
			renderer = new RenderPoleComponent();
		}
		return renderer;
	}
}
