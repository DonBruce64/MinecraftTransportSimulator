package mcinterface1165;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.world.World;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Builder for a basic MC Entity class.  This builder provides basic entity logic that's common
 * to all entities we may want to spawn.
 *
 * @author don_bruce
 */
//Need to extend LivingEntity since spawn syncing packets don't work with the base Entity class.
public abstract class ABuilderEntityBase extends Entity {
    protected static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, InterfaceLoader.MODID);
    protected static EntityType<ABuilderEntityBase> E_TYPE;

    /**
     * The builder's whole state as NBT (what {@link #saveWithoutId} writes), carried as synched entity data so it travels
     * with the spawn: a client that starts tracking, a recording (Replay Mod) and a replay all get the state the same way
     * as the entity itself.  The server never stores it here (that would re-broadcast it to every tracker on each set): it
     * sends it to a tracking player once, see {@link #startSeenByPlayer}.  A client entity holds the value it received,
     * which is also what a recorder's snapshot reads, see {@link #getAddEntityPacket}.
     */
    private static final DataParameter<CompoundNBT> IV_STATE = EntityDataManager.defineId(ABuilderEntityBase.class, DataSerializers.COMPOUND_TAG);

    /**
     * Data loaded on last NBT call.  Saved here to prevent loading of things until the update method.  This prevents
     * loading entity data when this entity isn't being ticked.  Some mods love to do this by making a lot of entities
     * to do their funky logic.  I'm looking at YOU The One Probe!  This should be either set by NBT loaded from disk
     * on servers, or taken from the received {@link #IV_STATE} on clients.
     */
    public CompoundNBT lastLoadedNBT;
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
    private final List<ServerPlayerEntity> playersAwaitingState = new ArrayList<>();
    /**
     * An idle tick counter.  This is set to 0 each time the update method is called, but is incremented each game tick.
     * This allows us to track how long this entity has been idle, and do logic if it's been idle too long.
     **/
    public int idleTickCounter;

    public ABuilderEntityBase(World level) {
        super(E_TYPE, level);
    }

    public ABuilderEntityBase(EntityType<?> etype, World level) {
        super(etype, level);
    }

    @Override
    public void tick() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.onUpdate();
        idleTickCounter = 0;
        baseTick();
    }

    @Override
    public void baseTick() {
        //Don't call the super, because some mods muck with our logic here.
        //Said mods are Sponge plugins, but I'm sure there are others.
        //super.onEntityUpdate();

        //Clients wait for the state to arrive (see onSyncedDataUpdated), servers send it to the players waiting for it.
        if (!level.isClientSide && loadedFromSavedNBT && !playersAwaitingState.isEmpty()) {
            SEntityMetadataPacket statePacket = createStatePacket();
            for (ServerPlayerEntity player : playersAwaitingState) {
                player.connection.send(statePacket);
            }
            playersAwaitingState.clear();
        }

        //If we are on the server, set the NBT flag.
        if (!loadedFromSavedNBT && lastLoadedNBT != null && !level.isClientSide) {
            loadFromSavedNBT = true;
        }
    }

    /**
     * 1.16.5's entity data packet only takes the entity's own data manager.  Packing the one value into a buffer and reading
     * the packet from it sends the state without storing it in the server's entity data.
     */
    private SEntityMetadataPacket createStatePacket() {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeVarInt(getId());
            EntityDataManager.pack(Collections.<EntityDataManager.DataEntry<?>>singletonList(new EntityDataManager.DataEntry<>(IV_STATE, saveWithoutId(new CompoundNBT()))), buffer);
            SEntityMetadataPacket packet = new SEntityMetadataPacket();
            packet.read(buffer);
            return packet;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            buffer.release();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayerEntity player) {
        //Called by MC right after it sent the spawn packet and the entity's own data to this player (TrackedEntity.addPairing),
        //so the state follows the spawn and the default value in that data can't overwrite it.
        super.startSeenByPlayer(player);
        playersAwaitingState.add(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayerEntity player) {
        super.stopSeenByPlayer(player);
        playersAwaitingState.remove(player);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);
        //Only clients take the state, and only as the start of a builder that has none.  Once the mccore object exists
        //it is kept up to date by its own packets, so a repeated state is ignored.
        //This also covers the value getAddEntityPacket puts there.
        if (IV_STATE.equals(key) && level.isClientSide && !loadFromSavedNBT && !loadedFromSavedNBT) {
            CompoundNBT state = getEntityData().get(IV_STATE);
            if (!state.isEmpty()) {
                lastLoadedNBT = state.copy();
                loadFromSavedNBT = true;
                onStateReceived();
            }
        }
    }

    /**
     * Called on a client right after the state arrived, which is the moment the mccore object can be built.  Building it
     * here, and not on the next tick, keeps the packets that follow the state from finding no object.
     */
    protected void onStateReceived() {
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport) {
        //Overridden due to stupid tracker behavior.
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        //Catch unloaded entities from when the chunk goes away.
        if (isAlive()) {
            remove();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        //Don't render entities, this gets done by the overrider.
        return false;
    }

    @Override
    public void load(CompoundNBT tag) {
        super.load(tag);
        //Save the NBT for loading in the next update call.
        lastLoadedNBT = tag;
    }

    @Override
    public CompoundNBT saveWithoutId(CompoundNBT tag) {
        super.saveWithoutId(tag);
        //Need to have this here as some mods will load us from NBT and then save us back
        //without ticking.  This causes data loss if we don't merge the last loaded NBT tag.
        //If we did tick, then the last loaded will be null and this doesn't apply.
        if (lastLoadedNBT != null) {
            tag.merge(lastLoadedNBT);
        }
        return tag;
    }

    //Junk methods.
    @Override
    protected void addAdditionalSaveData(CompoundNBT pCompound) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT pCompound) {
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(IV_STATE, new CompoundNBT());
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        //MC calls this on servers when a player starts tracking us, and recorders (Replay Mod) may call it on the client
        //entity to write a snapshot, followed by a read of the entity data.  Publish the current state for that read.
        if (level.isClientSide && loadedFromSavedNBT) {
            getEntityData().set(IV_STATE, saveWithoutId(new CompoundNBT()));
        }
        //Spawn object, we have a mixin override this code on the client though since it doesn't know to handle us.
        return new SSpawnObjectPacket(this);
    }
}
