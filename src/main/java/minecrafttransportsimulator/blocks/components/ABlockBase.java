package minecrafttransportsimulator.blocks.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.mcinterface.BuilderBlock;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Base Block class.  This type is used in the constructor of {@link BuilderBlock} to allow us to use
 * completely custom code that is not associated with MC's standard block code that changes EVERY FREAKING VERSION.
 * Seriously guys, you make a game about blocks.  How many times you gonna re-invent them?
 * Anyways... This code contains methods for the block's hardness, blast resistance, and rotation.
 *
 * @author don_bruce
 */
public abstract class ABlockBase{
	public final float hardness;
	public final float blastResistance;
	
	protected static final BoundingBox SINGLE_BLOCK_BOUNDS = new BoundingBox(new Point3d(0, 0, 0), 0.5D, 0.5D, 0.5D);
	
	public ABlockBase(float hardness, float blastResistance){
		this.hardness = hardness;
		this.blastResistance = blastResistance;
	}
	
	/**
	 *  Called when this block is first placed in the world.  Note that this is called
	 *  after NBT is loaded into the TE from saved state, or after its definition is
	 *  set from the item definition if no NBT data was present on the item.
	 */
	public void onPlaced(WrapperWorld world, Point3i location, WrapperPlayer player){}
	
	/**
	 *  Called when this block is clicked.  Return true if this block does
	 *  a thing, false if the block just exists to be pretty.  Actions may
	 *  or may not be taken.  Note that this is called both on the server and
	 *  on the client, so watch your actions and packets!
	 */
	public abstract boolean onClicked(WrapperWorld world, Point3i location, Axis axis, WrapperPlayer player);
	
	/**
	 *  Called when this block is removed from the world.  This occurs when the block is broken
	 *  by a player, explosion, vehicle, etc.  This method is called prior to the Tile Entity being
	 *  removed, as logic may be needed to be performed that requires the data from the TE.
	 */
	public void onBroken(WrapperWorld world, Point3i location){}

	/**
	 *  Gets the current rotation of the block at the passed-in point.
	 *  Angle will be either 0, 90, 180, or 270.  This is internally
	 *  set by MC-standard methods when the player places the block, and is
	 *  not modifiable by any block-based code.
	 */
	public float getRotation(WrapperWorld world, Point3i location){
		return world.getBlockRotation(location);
	}
	
	/**
	 *  Adds all collision boxes to the passed-in list.  This is sent back to MC
	 *  to handle collisions with this block.  May be based on state or TE data.
	 *  Note that all collisions are relative to the block's location.
	 */
	public void addCollisionBoxes(WrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
		collidingBoxes.add(SINGLE_BLOCK_BOUNDS);
	}
	
	/**
	 *  Enums for side-specific stuff.
	 */
	public static enum Axis{
		NONE(0, 0, 0, 0),
		UP(0, 1, 0, 0),
		DOWN(0, -1, 0, 0),
		NORTH(0, 0, -1, 180),
		SOUTH(0, 0, 1, 0),
		EAST(1, 0, 0, 90),
		WEST(-1, 0, 0, 270);
		
		public final int xOffset;
		public final int yOffset;
		public final int zOffset;
		public final int yRotation;
		
		private Axis(int xOffset, int yOffset, int zOffset, int yRotation){
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			this.yRotation = yRotation;
		}
		
		public Point3i getOffsetPoint(Point3i point){
			return point.copy().add(xOffset, yOffset, zOffset);
		}
		
		public Axis getOpposite(){
			switch(this){
				case UP: return DOWN;
				case DOWN: return UP;
				case NORTH: return SOUTH;
				case SOUTH: return NORTH;
				case EAST: return WEST;
				case WEST: return EAST;
				default: return NONE;
			}
		}
	}
}
