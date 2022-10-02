package mcinterface1122;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

/**
 * Builder for the MC Tile Entity class   This class interfaces with all the MC-specific
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
public class BuilderTileEntity<TileEntityType extends ATileEntityBase<?>> extends TileEntity implements ITickable {
    protected TileEntityType tileEntity;

    /**
     * This flag is true if we need to get server data for syncing.  Set on construction tick, but only used on clients.
     **/
    private boolean needDataFromServer = true;
    /**
     * Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
     * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
     * to do their funky logic.  I'm looking at YOU The One Probe!  This should be either set by NBT loaded from disk
     * on servers, or set by packet on clients.
     */
    protected NBTTagCompound lastLoadedNBT;
    /**
     * Set to true when NBT is loaded on servers from disk, or when NBT arrives from clients on servers.  This is set on the update loop when data is
     * detected from server NBT loading, but for clients this is set when a data packet arrives.  This prevents loading client-based NBT before
     * the packet arrives, which is possible if a partial NBT load is performed by the core game or a mod.
     **/
    protected boolean loadFromSavedNBT;
    /**
     * Set to true when loaded NBT is parsed and loaded.  This is done to prevent re-parsing of NBT from triggering a second load command.
     **/
    protected boolean loadedFromSavedNBT;
    /**
     * Players requesting data for this builder.  This is populated by packets sent to the server.  Each tick players in this list are
     * sent data about this builder, and the list cleared.  Done this way to prevent the server from trying to handle the packet before
     * it has created the entity, as the entity is created on the update call, but the packet might get here due to construction.
     **/
    protected final List<IWrapperPlayer> playersRequestingData = new ArrayList<>();

    public BuilderTileEntity() {
        //Blank constructor for MC.
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update() {
        //World and pos might be null on first few scans.
        if (world != null && pos != null) {
            if (tileEntity != null) {
                tileEntity.update();
                tileEntity.doPostUpdateLogic();
                if (tileEntity.rider != null) {
                    tileEntity.updateRider();
                }
            } else if (!loadedFromSavedNBT) {
                //If we are on the server, set the NBT flag.
                if (lastLoadedNBT != null && !world.isRemote) {
                    loadFromSavedNBT = true;
                }

                //If we have NBT, and haven't loaded it, do so now.
                //Hold off on loading until blocks load: this can take longer than 1 update if the server/client is laggy.
                if (loadFromSavedNBT && world.isBlockLoaded(pos)) {
                    try {
                        //Get the block that makes this TE and restore it from saved state.
                        WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(world);
                        Point3D position = new Point3D(pos.getX(), pos.getY(), pos.getZ());
                        ABlockBaseTileEntity block = (ABlockBaseTileEntity) worldWrapper.getBlock(position);
                        tileEntity = (TileEntityType) block.createTileEntity(worldWrapper, position, null, new WrapperNBT(lastLoadedNBT));
                        tileEntity.world.addEntity(tileEntity);
                        loadedFromSavedNBT = true;
                        lastLoadedNBT = null;
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Failed to load tile entity on builder from saved NBT.  Did a pack change?");
                        InterfaceManager.coreInterface.logError(e.getMessage());
                        world.setBlockToAir(pos);
                    }
                }
            }

            //Now that we have done update/NBT stuff, check for syncing.
            if (world.isRemote) {
                //No data.  Wait for NBT to be loaded.
                //As we are on a client we need to send a packet to the server to request NBT data.
                ///Although we could call this in the constructor, Minecraft changes the
                //entity IDs after spawning and that fouls things up.
                if (needDataFromServer) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityCSHandshakeClient(InterfaceManager.clientInterface.getClientPlayer(), this));
                    needDataFromServer = false;
                }
            } else {
                //Send any packets to clients that requested them.
                if (!playersRequestingData.isEmpty()) {
                    for (IWrapperPlayer player : playersRequestingData) {
                        IWrapperNBT data = InterfaceManager.coreInterface.getNewNBTWrapper();
                        writeToNBT(((WrapperNBT) data).tag);
                        player.sendPacket(new PacketEntityCSHandshakeServer(this, data));
                    }
                    playersRequestingData.clear();
                }
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        //Invalidate happens when we break the block this TE is on.
        if (tileEntity != null) {
            tileEntity.remove();
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        //Catch unloaded TEs from when the chunk goes away and kill them.
        //MC forgets to do this normally.
        if (tileEntity != null && tileEntity.isValid) {
            invalidate();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        //Don't directly load the TE here.  This causes issues because Minecraft loads TEs before blocks.
        //This is horridly stupid, because then you can't get the block for the TE, but whatever, Mojang be Mojang.
        lastLoadedNBT = tag;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (tileEntity != null) {
            tileEntity.save(new WrapperNBT(tag));
        } else if (lastLoadedNBT != null) {
            //Need to have this here as some mods will load us from NBT and then save us back
            //without ticking.  This causes data loss if we don't merge the last loaded NBT tag.
            //If we did tick, then the last loaded will be null and this doesn't apply.
            tag = lastLoadedNBT;
        }
        return tag;
    }
}