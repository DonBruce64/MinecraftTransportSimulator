package minecrafttransportsimulator.blocks.tileentities.components;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.IAnimationProvider;
import minecrafttransportsimulator.rendering.instances.AnimationsPole;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component implements IAnimationProvider{
	public final TileEntityPole core;
	public final ItemPoleComponent item;
	public final JSONPoleComponent definition;
	public final Set<String> activeVariables = new HashSet<String>();
	
	private static final AnimationsPole animator = new AnimationsPole();
	
	public ATileEntityPole_Component(TileEntityPole core, ItemPoleComponent item){
		this.core = core;
		this.item = item;
		this.definition = item.definition;
	}
	
	/**
	 *  Gets the light level for this component.  If non-zero, this block will emit light
	 *  when this component is present.  If multiple components are placed, the highest value
	 *  of all components is used.  Value is from 0.0-1.0.
	 */
	public abstract float lightLevel();
	
	@Override
    public Point3d getProviderPosition(){
		return core.doublePosition;
	}
	
	@Override
    public WrapperWorld getProviderWorld(){
		return core.world;
	}
	
	@Override
    public AnimationsPole getAnimationSystem(){
		return animator;
	}
	
	@Override
	public float getLightPower(){
		return 1.0F;
	}
	
	@Override
	public Set<String> getActiveVariables(){
		return activeVariables;
	}
}
