package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.instances.RenderPoleComponent;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Base class for components that can go on poles.  Not actually a TE, just sits on one.
 * 
 * @author don_bruce
 */
public abstract class ATileEntityPole_Component extends AEntityD_Definable<JSONPoleComponent>{
	
	public final TileEntityPole core;
	public final Axis axis;
	
	private static RenderPoleComponent renderer;
	
	public ATileEntityPole_Component(TileEntityPole core, WrapperPlayer placingPlayer, Axis axis, WrapperNBT data){
		super(core.world, placingPlayer, data);
		this.core = core;
		this.axis = axis;
	}
	
	@Override
	public boolean shouldRenderBeams(){
    	return ConfigSystem.configObject.clientRendering.blockBeams.value;
    }
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		double value = super.getRawVariableValue(variable, partialTicks);
		if(!Double.isNaN(value)){
			return value;
		}
		
		//Check connector variables.
		if(variable.startsWith("neighbor_present_")){
			Axis connectionAxis = Axis.valueOf(variable.substring("neighbor_present_".length()).toUpperCase());
			ATileEntityBase<?> otherTile = world.getTileEntity(connectionAxis.getOffsetPoint(position));
			return otherTile != null && otherTile instanceof TileEntityPole ? 1 : 0;
		}
		if(variable.startsWith("matching_present_")){
			Axis connectionAxis = Axis.valueOf(variable.substring("matching_present_".length()).toUpperCase());
			ATileEntityBase<?> otherTile = world.getTileEntity(connectionAxis.getOffsetPoint(position));
			return otherTile != null && core.definition.systemName.equals(otherTile.definition.systemName) ? 1 : 0;
		}
		//Check solid block variables.
		if(variable.startsWith("solid_present_")){
			Axis connectionAxis = Axis.valueOf(variable.substring("solid_present_".length()).toUpperCase());
			return world.isBlockSolid(connectionAxis.getOffsetPoint(position), connectionAxis.getOpposite()) ? 1 : 0;
		}
		//Check slab variables.
		switch(variable){
			case("slab_present_up") : return world.isBlockTopSlab(Axis.UP.getOffsetPoint(position)) ? 1 : 0;
			case("slab_present_down") : return world.isBlockBottomSlab(Axis.DOWN.getOffsetPoint(position)) ? 1 : 0;
		}
		
		return Double.NaN;
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
