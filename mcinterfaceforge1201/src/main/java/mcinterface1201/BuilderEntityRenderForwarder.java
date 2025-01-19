package mcinterface1201;

import minecrafttransportsimulator.baseclasses.Point3D;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.RegistryObject;

/**
 * Builder for an entity to forward rendering calls to all internal renderer.  This is due to prevent
 * MC from culling entities when it should be rendering them instead.  MC does this when you can't see
 * the chunk the entity is in, or if they are above the world, but we don't want that.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntityRenderForwarder extends ABuilderEntityBase {
    public static RegistryObject<EntityType<BuilderEntityRenderForwarder>> E_TYPE4;

    protected Player playerFollowing;

    public BuilderEntityRenderForwarder(EntityType<? extends BuilderEntityRenderForwarder> eType, Level world) {
        super(eType, world);
    }

    public BuilderEntityRenderForwarder(Player playerFollowing) {
        this(E_TYPE4.get(), playerFollowing.level());
        this.playerFollowing = playerFollowing;
        Vec3 playerPos = playerFollowing.position();
        this.setPos(playerPos.x, playerPos.y, playerPos.z);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (playerFollowing != null && playerFollowing.level() == this.level() && playerFollowing.isAlive()) {
            //Need to move the fake entity forwards to account for the partial ticks interpolation MC does.
            //If we don't do this, and we move faster than 1 block per tick, we'll get flickering.
            WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(playerFollowing);
            double playerVelocity = playerFollowing.getDeltaMovement().length();
            Point3D playerEyesVec = playerWrapper.getLineOfSight(Math.max(1, playerVelocity / 2));
            Point3D playerEyeOffset = new Point3D(0, 0.5, 0).rotate(playerWrapper.getOrientation()).add(playerWrapper.getPosition()).add(playerEyesVec);
            setPos(playerEyeOffset.x, playerEyeOffset.y + (playerWrapper.getEyeHeight() + playerWrapper.getSeatOffset()) * playerWrapper.getVerticalScale(), playerEyeOffset.z);
        } else if (!level().isClientSide) {
            //Don't restore saved entities on the server.
            //These get loaded, but might not tick if they're out of chunk range.
            discard();
        } else if (!loadedFromSavedNBT && loadFromSavedNBT) {
            //Load player following from client NBT.
            playerFollowing = level().getPlayerByUUID(lastLoadedNBT.getUUID("playerFollowing"));
            loadedFromSavedNBT = true;
            lastLoadedNBT = null;
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        //Need to render in pass 1 to render transparent things in the world like light beams.
        return true;
    }

    @Override
    public CompoundTag saveWithoutId(CompoundTag tag) {
        super.saveWithoutId(tag);
        if (playerFollowing != null) {
            //Player valid, save it and return the modified tag.
            tag.putUUID("playerFollowing", playerFollowing.getUUID());
        }
        return tag;
    }
}
