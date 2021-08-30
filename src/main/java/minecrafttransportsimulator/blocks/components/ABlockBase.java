package minecrafttransportsimulator.blocks.components;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
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
	
	protected static final BoundingBox SINGLE_BLOCK_BOUNDS = new BoundingBox(new Point3d(), 0.5D, 0.5D, 0.5D);
	
	public ABlockBase(float hardness, float blastResistance){
		this.hardness = hardness;
		this.blastResistance = blastResistance;
	}
	
	/**
	 *  Called when this block is first placed in the world.  Note that this is called
	 *  after NBT is loaded into the TE from saved state, or after its definition is
	 *  set from the item definition if no NBT data was present on the item.
	 */
	public void onPlaced(WrapperWorld world, Point3d position, WrapperPlayer player){}
	
	/**
	 *  Called when this block is removed from the world.  This occurs when the block is broken
	 *  by a player, explosion, vehicle, etc.  This method is called prior to the Tile Entity being
	 *  removed, as logic may be needed to be performed that requires the data from the TE.
	 *  This is ONLY called on the server, so if you have data to sync, do it via packets. 
	 */
	public void onBroken(WrapperWorld world, Point3d position){}
	
	/**
	 *  Adds all collision boxes to the passed-in list.  This is sent back to MC
	 *  to handle collisions with this block.  May be based on state or TE data.
	 *  Note that all collisions are relative to the block's location.
	 */
	public void addCollisionBoxes(WrapperWorld world, Point3d position, List<BoundingBox> collidingBoxes){
		collidingBoxes.add(SINGLE_BLOCK_BOUNDS);
	}
	
	/**
	 *  Returns the main bounding box for this block.  This should normally be the standard full-block size
	 *  to ensure all the appropriate collision checks are done.  However, should the block have a collision
	 *  mapping smaller than this, then a smaller box should be returned.  This prevents the block from 
	 *  interfering with player clicking actions and Bad Mods doing Bad Stuff by checking this rather than 
	 *  the collision box listing.
	 */
	public BoundingBox getCollisionBounds(){
		return SINGLE_BLOCK_BOUNDS;
	}
	
	/**
	 *  Enums for side-specific stuff.
	 */
	public static enum Axis{
		NONE(0, 0, 0, 0, false, false),
		UP(0, 1, 0, 0, true, false),
		DOWN(0, -1, 0, 0, true, false),
		NORTH(0, 0, -1, 180, true, true),
		SOUTH(0, 0, 1, 0, true, true),
		EAST(1, 0, 0, 90, true, true),
		WEST(-1, 0, 0, 270, true, true),
		
		NORTHEAST(1, 0, -1, 135, false, true),
		SOUTHEAST(1, 0, 1, 45, false, true),
		NORTHWEST(-1, 0, -1, 225, false, true),
		SOUTHWEST(-1, 0, 1, 315, false, true);
		
		public final int xOffset;
		public final int yOffset;
		public final int zOffset;
		public final int yRotation;
		public final boolean blockBased;
		public final boolean xzPlanar;
		
		private Axis(int xOffset, int yOffset, int zOffset, int yRotation, boolean blockBased, boolean xzPlanar){
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			this.zOffset = zOffset;
			this.yRotation = yRotation;
			this.blockBased = blockBased;
			this.xzPlanar = xzPlanar;
		}
		
		public Point3d getOffsetPoint(Point3d point){
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
				case NORTHEAST: return SOUTHWEST;
				case SOUTHEAST: return NORTHWEST;
				case NORTHWEST: return SOUTHEAST;
				case SOUTHWEST: return NORTHEAST;
				default: return NONE;
			}
		}
		
		public static Axis getFromRotation(double rotation, boolean checkDiagonals){
			rotation = rotation%360;
			if(rotation < 0){
				rotation += 360;
			}
			int degRotation = checkDiagonals ? (int) (Math.round(rotation/45)*45) : (int) (Math.round(rotation/90)*90);
			for(Axis axis : values()){
				if(axis.xzPlanar && axis.yRotation == degRotation){
					return axis;
				}
			}
			return Axis.NONE;
		}
	}
}
