package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;

/**Builder for the MC Tile Entity class   This class interfaces with all the MC-specific 
 * code, and is constructed on the server automatically by MC.  After construction, a tile entity
 * class that extends {@link ATileEntityBase} should be assigned to it.  This is either
 * done manually on the first placement, or automatically via loading from NBT.
 * <br><br>
 * Of course, one might ask, "why not just construct the TE class when we construct this one?".
 * That's a good point, but MC doesn't work like that.  MC waits to assign the world and position
 * to TEs, so if we construct our TE right away, we'll end up with TONs of null checks.  To avoid this,
 * we only construct our TE after the world and position get assigned, and if we have NBT
 * At that point, we make the TE if we're on the server.  If we're on the client, we always way 
 * for NBT, as we need to sync with the server's data.
 *
 * @author don_bruce
 */
public class BuilderTileEntity<TileEntityType extends ATileEntityBase<?>> extends TileEntity implements ITickable{
	public TileEntityType tileEntity;
	/**Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
	 * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
	 * to do their funky logic.  I'm looking at YOU The One Probe!**/
	private NBTTagCompound lastLoadedNBT;
	
	public BuilderTileEntity(){
		//Blank constructor for MC.
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void update(){
		if(tileEntity != null){
			tileEntity.update();
		}else if(lastLoadedNBT != null){
			if(world != null && pos != null){
				if(world.isBlockLoaded(pos)){
					try{
						//Get the block that makes this TE and restore it from saved state.
						WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
						Point3d position = new Point3d(pos.getX(), pos.getY(), pos.getZ());
						ABlockBaseTileEntity block = (ABlockBaseTileEntity) worldWrapper.getBlock(position);
						tileEntity = (TileEntityType) block.createTileEntity(worldWrapper, position, new WrapperNBT(lastLoadedNBT));
						lastLoadedNBT = null;
					}catch(Exception e){
						InterfaceCore.logError("Failed to load entity on builder from saved NBT.  Did a pack change?");
						InterfaceCore.logError(e.getMessage());
						invalidate();
					}
				}
			}
		}
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
		//Invalidate happens when we break the block this TE is on.
		if(tileEntity != null){
			tileEntity.remove();
		}
	}
	
	@Override
	public void onChunkUnload(){
		super.onChunkUnload();
		//Catch unloaded TEs from when the chunk goes away.
		if(tileEntity != null && tileEntity.isValid){
			invalidate();
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
			return new SPacketUpdateTileEntity(getPos(), -1, tileEntity.save(new WrapperNBT()).tag);
		}else{
			return super.getUpdatePacket();
		}
    }
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt){
		//Called when the client gets a TE update packet.
		//Save the NBT so we can load the TE in the next update call.
		lastLoadedNBT = pkt.getNbtCompound();
	}
	
	@Override
    public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		//Don't directly load the TE here.  This causes issues because Minecraft loads TEs before blocks.
		//This is horridly stupid, because then you can't get the block for the TE, but whatever, Mojang be Mojang.
		lastLoadedNBT = tag;
    }
    
	@Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		if(tileEntity != null){
			tileEntity.save(new WrapperNBT(tag));
		}else{
			invalidate();
		}
        return tag;
    }
	
	/**Tickable builder for {@link BuilderTileEntity}.
    *
    * @author don_bruce
    */
	//TODO remove this when all TEs are converted in V21.
	public static class Tickable<TickableTileEntity extends ATileEntityBase<? extends AJSONItem>> extends BuilderTileEntity<TickableTileEntity>{
	    
		public Tickable(){
			super();
		}
	}
}