package minecrafttransportsimulator.mcinterface;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ITickable;

/**Wrapper for the MC Tile Entity class (called BlockEntity in later MC versions cause
 * the people who maintain the mappings like to make life difficult through constant
 * re-naming of things).  This class, unlike {@link BuilderTileEntity}, is responsible
 * for interfacing with MC Tile Entities.  Because of this, some methods are different
 * between the two classes.
 * <br><br>
 * Note that while this wrapper has an update method, it does not necessarily mean the
 * TE that is wrapped will be ticked (updated).  Some TEs don't have ticking functionality,
 * so if the update method is called on one of those then the method call will simply do nothing.
 *
 * @author don_bruce
 */
public class WrapperTileEntity{
	public final TileEntity tile;
	
	public WrapperTileEntity(TileEntity tile){
		this.tile = tile;
	}
	
	/**
	 *  Updates the TE, if the TE supports it.
	 */
	public void update(){
		if(tile instanceof ITickable){
			((ITickable) tile).update();
		}
	}
	
	/**
	 *  Loads the TE data from the passed-in source.
	 */
	public void load(WrapperNBT data){
		tile.readFromNBT(data.tag);
	}
	
	/**
	 *  Saves the TE to the passed-in NBT Wrapper tag.
	 */
	public void save(WrapperNBT data){
		tile.writeToNBT(data.tag);
	}
	
	/**Wrapper for Furnaces.
    *
    * @author don_bruce
    */
	public static class WrapperEntityFurnace extends WrapperTileEntity{
		public WrapperEntityFurnace(WrapperWorld world, WrapperNBT data){
			super(new EntityFurnace());
			tile.setWorld(world.world);
			tile.readFromNBT(data.tag);
		}
		
		private static class EntityFurnace extends TileEntityFurnace{
			private boolean burningStateAtStartOfUpdate;
			private boolean runningUpdate;
			
			public EntityFurnace(){
				super();
				//Make sure we have registered our furnace.  If not, the game won't save it.
				if(TileEntity.getKey(EntityFurnace.class) == null){
					TileEntity.register("furnace_entity", EntityFurnace.class);
				}
			}
			
			@Override
			public boolean isUsableByPlayer(EntityPlayer player){
				//Always return true to prevent furnace GUI from closing.
				return true; 
		    }
			
			//Override this to prevent the furnace from setting blockstates for furnaces in the world that don't exist.
			//The only time this blockstate gets set is if the furnace changes burning states during the update() call,
			//so get the value at the start of the update and return that throughout the update call.
			//Once the update is done, we can update the cached value and return that.  Because MC won't see a change
			//during the update call, it won't set any blockstates of non-existent blocks.
			@Override
			public boolean isBurning(){
				return runningUpdate ? burningStateAtStartOfUpdate : super.isBurning();
		    }
			
			@Override
			public void update(){
				burningStateAtStartOfUpdate = isBurning();
				runningUpdate = true;
				super.update();
				runningUpdate = false;
			}
		}
	}
	
	/**Wrapper for Brewing Stands.
    *
    * @author don_bruce
    */
	public static class WrapperEntityBrewingStand extends WrapperTileEntity{
		public WrapperEntityBrewingStand(WrapperWorld world, WrapperNBT data){
			super(new EntityBrewingStand());
			tile.setWorld(world.world);
			tile.readFromNBT(data.tag);
		}
		
		private static class EntityBrewingStand extends TileEntityBrewingStand{
			public EntityBrewingStand(){
				super();
				//Make sure we have registered our brewing stand.  If not, the game won't save it.
				if(TileEntity.getKey(EntityBrewingStand.class) == null){
					TileEntity.register("brewing_stand_entity", EntityBrewingStand.class);
				}
			}
			
			@Override
			public boolean isUsableByPlayer(EntityPlayer player){
				//Always return true to prevent furnace GUI from closing.
				return true; 
		    }
		}
	}
}
