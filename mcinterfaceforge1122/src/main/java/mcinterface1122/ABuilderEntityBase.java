package mcinterface1122;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.netty.buffer.Unpooled;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.world.World;

/**
 * Builder for a basic MC Entity class.  This builder provides basic entity logic that's common
 * to all entities we may want to spawn.
 *
 * @author don_bruce
 */
public abstract class ABuilderEntityBase extends Entity {
    /**
     * The builder's whole state as NBT (what {@link #writeToNBT} writes), carried as synched entity data so it travels
     * with the spawn: a client that starts tracking, a recording (Replay Mod) and a replay all get the state the same way
     * as the entity itself, through a vanilla packet ({@link SPacketEntityMetadata}).  The server never stores it here
     * (that would re-broadcast it to every tracker on each set): it sends it to a tracking player once, see
     * {@link #addTrackingPlayer}.  The default (empty) value is what MC's own metadata packet carries with the spawn.
     */
    private static final DataParameter<NBTTagCompound> IV_STATE = EntityDataManager.createKey(ABuilderEntityBase.class, DataSerializers.COMPOUND_TAG);

    /**
     * Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
     * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
     * to do their funky logic.  I'm looking at YOU The One Probe!  This should be either set by NBT loaded from disk
     * on servers, or taken from the received {@link #IV_STATE} on clients.
     */
    public NBTTagCompound lastLoadedNBT;
    /**
     * Set to true when NBT is loaded on servers from disk, or when the state arrives on clients.  This is set on the update loop when data is
     * detected from server NBT loading, but for clients this is set when the state arrives.  This prevents loading client-based NBT before
     * it arrives, which is possible if a partial NBT load is performed by the core game or a mod.
     **/
    public boolean loadFromSavedNBT;
    /**
     * Set to true when loaded NBT is parsed and loaded.  This is done to prevent re-parsing of NBT from triggering a second load command.
     * Note that if this entity is being spawned manually rather than loaded from disk, this should be set prior to ticking.
     **/
    public boolean loadedFromSavedNBT;
    /**
     * Players that started tracking this builder and haven't received its state yet.  Each tick, once the mccore object is
     * loaded, players in this list are sent the state and the list is cleared.  Waiting for the load keeps the server from
     * sending a state that is missing the mccore object, as that is created on the update call and not on construction.
     **/
    private final List<EntityPlayerMP> playersAwaitingState = new ArrayList<>();
    /**
     * An idle tick counter.  This is set to 0 each time the update method is called, but is incremented each game tick.
     * This allows us to track how long this entity has been idle, and do logic if it's been idle too long.
     **/
    public int idleTickCounter;

    public ABuilderEntityBase(World world) {
        super(world);
    }

    @Override
    public void onUpdate() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.onUpdate();
        idleTickCounter = 0;
        onEntityUpdate();
    }

    @Override
    public void onEntityUpdate() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.onEntityUpdate();

        //Clients wait for the state to arrive (see notifyDataManagerChange), servers send it to the players waiting for it.
        if (!world.isRemote && loadedFromSavedNBT && !playersAwaitingState.isEmpty()) {
            SPacketEntityMetadata statePacket = createStatePacket();
            if (statePacket != null) {
                for (EntityPlayerMP player : playersAwaitingState) {
                    player.connection.sendPacket(statePacket);
                }
            }
            playersAwaitingState.clear();
        }

        //If we are on the server, set the NBT flag.
        if (!loadedFromSavedNBT && lastLoadedNBT != null && !world.isRemote) {
            loadFromSavedNBT = true;
        }
    }

    /**
     * Builds a metadata packet that carries only the state, not the rest of this entity's data manager.  MC's own
     * constructor takes the whole manager and marks it clean, which would swallow a pending broadcast of the other
     * entries, so the packet is read from a hand-written buffer instead.
     */
    private SPacketEntityMetadata createStatePacket() {
        try {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeVarInt(getEntityId());
            EntityDataManager.writeEntries(Collections.<EntityDataManager.DataEntry<?>>singletonList(new EntityDataManager.DataEntry<>(IV_STATE, writeToNBT(new NBTTagCompound()))), buffer);
            SPacketEntityMetadata packet = new SPacketEntityMetadata();
            packet.readPacketData(buffer);
            return packet;
        } catch (IOException e) {
            InterfaceManager.coreInterface.logError("Failed to build the state packet of a builder: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        //Called by MC right after it sent the spawn packet and the metadata to this player, so the state follows the spawn.
        super.addTrackingPlayer(player);
        playersAwaitingState.add(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        playersAwaitingState.remove(player);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        //Only clients take the state, and only as the start of a builder that has none.  Once the mccore object exists
        //it is kept up to date by its own packets, so a repeated state is ignored.  MC's own metadata packet with the
        //spawn carries the empty default, which is skipped.  The key is equal by ID, not the same object, as it was read from the packet.
        if (IV_STATE.equals(key) && world.isRemote && !loadFromSavedNBT && !loadedFromSavedNBT) {
            NBTTagCompound state = getDataManager().get(IV_STATE);
            if (!state.isEmpty()) {
                lastLoadedNBT = state.copy();
                loadFromSavedNBT = true;
                onStateReceived();
            }
        }
    }

    /**
     * Called on a client right after the state arrived, which is the moment the mccore object can be built.  Building it
     * here, and not on the next tick, keeps the packets that follow the state from finding no object: a replay can feed
     * a whole stretch of packets before the client ticks once.
     */
    protected void onStateReceived() {
    }

    @Override
    public void setPositionAndRotationDirect(double posX, double posY, double posZ, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        //Overridden due to stupid tracker behavior.
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        //Catch unloaded entities from when the chunk goes away.
        if (!isDead) {
            setDead();
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        //Don't render entities, this gets done by the overrider.
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        //Save the NBT for loading in the next update call.
        lastLoadedNBT = tag;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        //Need to have this here as some mods will load us from NBT and then save us back
        //without ticking.  This causes data loss if we don't merge the last loaded NBT tag.
        //If we did tick, then the last loaded will be null and this doesn't apply.
        if (lastLoadedNBT != null) {
            tag.merge(lastLoadedNBT);
        }
        return tag;
    }

    //Junk methods, forced to pull in.
    @Override
    protected void entityInit() {
        getDataManager().register(IV_STATE, new NBTTagCompound());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
    }
}
