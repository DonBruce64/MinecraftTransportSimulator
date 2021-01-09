package mcinterface1122;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**Builder for the MC Tile Entity class (called BlockEntity in later MC versions cause
 * the people who maintain the mappings like to make life difficult through constant
 * re-naming of things).  This class interfaces with all the MC-specific code, and is 
 * constructed on the server automatically by MC.  After construction, a tile entity
 * class that extends {@link ATileEntityBase} should be assigned to it.  This is either
 * done manually on the first placement, or automatically via loading from NBT.
 * <br><br>
 * Of course, one might ask, "why not just construct the TE class when we construct this one?".
 * That's a good point, but MC doesn't work like that.  MC waits to assign the world and position
 * to TEs, so if we construct our TE right away, we'll end up with TONs of NPES.  To avoid this,
 * we only construct our TE after the world and position get assigned, and if we have NBT
 * At that point, we make the TE if we're on the server.  If we're on the client, we always way 
 * for NBT, as we need to sync with the server's data.
 *
 * @author don_bruce
 */
public class BuilderTileEntity<TileEntityType extends ATileEntityBase<?>> extends TileEntity{
	protected TileEntityType tileEntity;
	
	public BuilderTileEntity(){
		//Blank constructor for MC.
	}
	
	@Override
	protected void setWorldCreate(World world){
		//MC is stupid and doesn't actually do anything here.
		//This means the world isn't set when we create this TE, leading to NPEs.
        this.setWorld(world);
    }
	
	@Override
	public void invalidate(){
		super.invalidate();
		if(tileEntity != null){
			tileEntity.remove();
		}
	}
	
	@Override
	public NBTTagCompound getUpdateTag(){
		//Gets called when the server sends this TE over as NBT data.
		//Get the full NBT tag, not just the position!
        return this.writeToNBT(new NBTTagCompound());
    }
	
	@Override
    public SPacketUpdateTileEntity getUpdatePacket(){
		if(tileEntity != null){
			//Gets called when we do a blockstate update for this TE.
			//Done during initial placedown so we need to get the full data for initial state. 
			WrapperNBT data = new WrapperNBT();
			tileEntity.save(data);
			data.setString("teid", tileEntity.getClass().getSimpleName());
		    return new SPacketUpdateTileEntity(getPos(), -1, data.tag);
		}else{
			return super.getUpdatePacket();
		}
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
		//Called when the client gets a TE update packet.
		//Create our client-side TE here if required.
		if(tileEntity == null){
			//Get the block that makes this TE and restore it from saved state.
			tileEntity = (TileEntityType) BuilderBlock.tileEntityMap.get(pkt.getNbtCompound().getString("teid")).createTileEntity(WrapperWorld.getWrapperFor(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperNBT(pkt.getNbtCompound()));
		}
	}
	
	@Override
	public boolean shouldRenderInPass(int pass){
		//We can render in all passes.
        return true;
    }
	
	@Override
	public AxisAlignedBB getRenderBoundingBox(){
		//Return a box of size 16x16 here to ensure this entity doesn't disappear when we aren't looking at it exactly.
		return new AxisAlignedBB(pos).grow(8);
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		if(tileEntity == null && tag.hasKey("teid")){
			//Restore the TE from saved state.
			tileEntity = (TileEntityType) BuilderBlock.tileEntityMap.get(tag.getString("teid")).createTileEntity(WrapperWorld.getWrapperFor(world), new Point3i(pos.getX(), pos.getY(), pos.getZ()), new WrapperNBT(tag));
		}
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(tileEntity != null){
			tileEntity.save(new WrapperNBT(tag));
			//Also save the class ID so we know what to construct when MC loads this TE back up.
			tag.setString("teid", tileEntity.getClass().getSimpleName());
		}else{
			invalidate();
		}
        return tag;
    }
	
	/**Tickable builder for {@link BuilderTileEntity}.
    *
    * @author don_bruce
    */
	public static class Tickable<TickableTileEntity extends ATileEntityBase<? extends AJSONItem<?>>> extends BuilderTileEntity<TickableTileEntity> implements ITickable{
	    
		public Tickable(){
			super();
		}
		
		@Override
		public void update(){
			if(tileEntity != null){
				((ITileEntityTickable) tileEntity).update();
			}
		}
	}
}