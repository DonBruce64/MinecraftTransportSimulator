package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;

/**Base Tile Entity class.  In essence, this class holds the data and state of a Tile Entity in the world.
 * All TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.  
 * <br><br>
 * Note that this constructor is called on the server when first creating the TE or loading it from disk,
 * but on the client this is called after the server sends over the saved data, not when the player first clicks.
 * Because of this, there there may be a slight delay in the TE showing up from when the block is first clicked.
 * Also note that the position of the TE is set by the constructor.  This is because TEs have their positions
 * set when they are created by the setting of a block.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONMultiModelProvider> extends AEntityD_Definable<JSONDefinition>{
	
	private float lastLightLevel;
	
	public ATileEntityBase(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, placingPlayer, data);
		this.position.setTo(position);
		if(placingPlayer != null){
			int clampAngle = getRotationIncrement();
			//Need to set the angles so the TE is facing the player, not the direction the player was facing.
			angles.y = Math.round((placingPlayer.getYaw()+180)/clampAngle)*clampAngle%360;
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(lastLightLevel != getLightProvided()){
				lastLightLevel = getLightProvided();
				world.updateLightBrightness(position);
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean shouldRenderBeams(){
    	return ConfigSystem.configObject.clientRendering.blockBeams.value;
    }
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		//Check generic block variables.
		switch(variable){
			case("redstone_active"): return world.getRedstonePower(position) > 0 ? 1 : 0;	
			case("redstone_level"): return world.getRedstonePower(position);
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	/**
	 *  Returns the rotation increment for this TE.  This will ensure the TE is
	 *  rotated only in this increment when placed into the world.  If no rotation
	 *  should be performed, return 360.
	 */
	public int getRotationIncrement(){
		return 15;
    }
	
	/**
	 *  Returns the collision box for this TE.  Returns it in the global space, offset to the
	 *  TE's center.
	 */
	public BoundingBox getCollisionBox(){
		return boundingBox;
    }
}
