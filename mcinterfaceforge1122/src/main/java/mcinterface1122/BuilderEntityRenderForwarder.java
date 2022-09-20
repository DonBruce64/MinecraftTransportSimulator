package mcinterface1122;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Builder for an entity to forward rendering calls to all internal renderer.  This is due to prevent
 * MC from culling entities when it should be rendering them instead.  MC does this when you can't see
 * the chunk the entity is in, or if they are above the world, but we don't want that.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class BuilderEntityRenderForwarder extends ABuilderEntityBase {
    protected static BuilderEntityRenderForwarder lastClientInstance;

    protected EntityPlayer playerFollowing;
    private final long[] lastTickRendered = new long[]{0L, 0L, 0L};
    private final float[] lastPartialTickRendered = new float[]{0F, 0F, 0F};
    private boolean doneRenderingShaders;
    private static int framesShadersDetected;
    private static boolean shadersDetected;

    public BuilderEntityRenderForwarder(World world) {
        super(world);
        setSize(0.05F, 0.05F);
        if (world.isRemote) {
            lastClientInstance = this;
        }
    }

    public BuilderEntityRenderForwarder(EntityPlayer playerFollowing) {
        this(playerFollowing.world);
        this.playerFollowing = playerFollowing;
        this.setPosition(playerFollowing.posX, playerFollowing.posY, playerFollowing.posZ);
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (playerFollowing != null && playerFollowing.world == this.world && !playerFollowing.isDead) {
            //Need to move the fake entity forwards to account for the partial ticks interpolation MC does.
            //If we don't do this, and we move faster than 1 block per tick, we'll get flickering.
            WrapperPlayer playerWrapper = WrapperPlayer.getWrapperFor(playerFollowing);
            double playerVelocity = Math.sqrt(playerFollowing.motionX * playerFollowing.motionX + playerFollowing.motionY * playerFollowing.motionY + playerFollowing.motionZ * playerFollowing.motionZ);
            Point3D playerEyesVec = playerWrapper.getLineOfSight(Math.max(1, playerVelocity / 2));
            Point3D playerEyeOffset = new Point3D(0, 0.5, 0).rotate(playerWrapper.getOrientation()).add(playerWrapper.getPosition()).add(playerEyesVec);
            setPosition(playerEyeOffset.x, playerEyeOffset.y + (playerWrapper.getEyeHeight() + playerWrapper.getSeatOffset()) * playerWrapper.getVerticalScale(), playerEyeOffset.z);
        } else if (!world.isRemote) {
            //Don't restore saved entities on the server.
            //These get loaded, but might not tick if they're out of chunk range.
            setDead();
        } else if (!loadedFromSavedNBT && loadFromSavedNBT) {
            //Load player following from client NBT.
            playerFollowing = world.getPlayerEntityByUUID(lastLoadedNBT.getUniqueId("playerFollowing"));
            loadedFromSavedNBT = true;
            lastLoadedNBT = null;
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        //Need to render in pass 1 to render transparent things in the world like light beams.
        return true;
    }

    /**
     * Helper method to tell if we need to render this entity on the current pass.
     * Always renders on passes 0 and 1, and sometimes on pass -1 if we didn't
     * render on pass 0 or 1.
     */
    public boolean shouldRenderEntity(float partialTicks) {
        //Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
        int renderPass = MinecraftForgeClient.getRenderPass();
        if (renderPass == -1) {
            renderPass = 2;
        }

        //We always render on pass 0 and 1, but we only render on pass 2 if we haven't rendered on pass 0 or 1.
        //If we are rendering on pass 0 or 1 a second time (before pass 2), it means shaders are present.
        //Set bit to detect these buggers and keep vehicles from rendering funny or disappearing.
        //Note that we wait 1000 frames for this, as we might have a rendering glitch that would cause a no-render on a few frames.
        if (!shadersDetected) {
            if (renderPass != 2 && lastTickRendered[renderPass] > lastTickRendered[2] && lastTickRendered[2] > 0) {
                if (++framesShadersDetected == 1000) {
                    shadersDetected = true;
                }
            } else {
                framesShadersDetected = 0;
            }
        }

        //If we are rendering the object, update times.
        //This may not be the case if shaders are present and we haven't rendered the shader component.
        //Shaders do a pre-render to get their shadow, so the first render pass is actually invalid.
        if (!shadersDetected || doneRenderingShaders) {
            //Rendering the actual model now.
            lastTickRendered[renderPass] = world.getTotalWorldTime();
            lastPartialTickRendered[renderPass] = partialTicks;
        } else if (shadersDetected && !doneRenderingShaders) {
            //Rendering shader components.  If we're on pass 1, then shaders should be done rendering this cycle.
            if (renderPass == 1) {
                doneRenderingShaders = true;
            }
        }

        if (renderPass == 2) {
            //If we already rendered in pass 0, don't render now.
            //Note that shaders may do operations in pass 0 for lighting, but won't render the actual model.
            //In this case, the lastTickPass won't have been updated, so we do render here.
            //We also need to reset the shader render state variable to ensure we are ready for the next cycle.
            if (shadersDetected) {
                doneRenderingShaders = false;
            }
            return lastTickRendered[0] != lastTickRendered[2] || lastPartialTickRendered[0] != lastPartialTickRendered[2];
        } else {
            return true;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (playerFollowing != null) {
            //Player valid, save it and return the modified tag.
            tag.setUniqueId("playerFollowing", playerFollowing.getUniqueID());
        }
        return tag;
    }

    /**
     * Registers our own class for use.
     */
    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create().entity(BuilderEntityRenderForwarder.class).id(new ResourceLocation(InterfaceManager.coreModID, "mts_entity_renderer"), 2).tracker(32 * 16, 5, false).name("mts_entity_renderer").build());
    }
}
