package mcinterface1165;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.Item;
import net.minecraft.util.HandSide;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity implements IWrapperPlayer {
    private static final Map<PlayerEntity, WrapperPlayer> playerClientWrappers = new HashMap<>();
    private static final Map<PlayerEntity, WrapperPlayer> playerServerWrappers = new HashMap<>();

    protected final PlayerEntity player;

    /**
     * Returns a wrapper instance for the passed-in player instance.
     * Null may be passed-in safely to ease function-forwarding.
     * Note that the wrapped player class MAY be side-specific, so avoid casting
     * the wrapped entity directly if you aren't sure what its class is.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     */
    public static WrapperPlayer getWrapperFor(PlayerEntity player) {
        if (player != null) {
            Map<PlayerEntity, WrapperPlayer> playerWrappers = player.level.isClientSide ? playerClientWrappers : playerServerWrappers;
            WrapperPlayer wrapper = playerWrappers.get(player);
            if (wrapper == null || !wrapper.isValid() || player != wrapper.player) {
                wrapper = new WrapperPlayer(player);
                playerWrappers.put(player, wrapper);
            }
            return wrapper;
        } else {
            return null;
        }
    }

    protected WrapperPlayer(PlayerEntity player) {
        super(player);
        this.player = player;
    }

    @Override
    public double getSeatOffset() {
        //Vanilla players don't sit quite at the bottom of their seats.
        //It's normally 0.14 (animal offset which is based at Y=0) + 0.35 (player offet which is negative) = 0.49, 
        //but it should be 10/16 pixels (legs are 12 pixels long, 4 pixels thick, and rotate on center for 10 pixels delta, or 0.625).
        //Add on the remaining offset here if we see the player is having an offset applied from super.
        double offset = super.getSeatOffset();
        if (offset != 0) {
            offset -= 0.135;
        }
        return offset;
    }

    @Override
    public boolean isOP() {
        return player.getServer() == null || player.getServer().getPlayerList().getOps().get(player.getGameProfile()) != null || player.getServer().isSingleplayer();
    }

    @Override
    public void displayChatMessage(LanguageEntry language, Object... args) {
        Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(String.format(language.getCurrentValue(), args)));
    }

    @Override
    public boolean isCreative() {
        return player.isCreative();
    }

    @Override
    public boolean isSpectator() {
        return player.isSpectator();
    }

    @Override
    public boolean isSneaking() {
        return player.isCrouching();
    }

    @Override
    public boolean isRightHanded() {
        return player.getMainArm() == HandSide.RIGHT;
    }

    @Override
    public IWrapperEntity getLeashedEntity() {
        for (MobEntity mobEntity : player.level.getLoadedEntitiesOfClass(MobEntity.class, new AxisAlignedBB(player.position().x - 7.0D, player.position().y - 7.0D, player.position().z - 7.0D, player.position().x + 7.0D, player.position().y + 7.0D, player.position().z + 7.0D))) {
            if (mobEntity.isLeashed() && player.equals(mobEntity.getLeashHolder())) {
                mobEntity.dropLeash(true, !player.isCreative());
                return WrapperEntity.getWrapperFor(mobEntity);
            }
        }
        return null;
    }

    @Override
    public boolean isHoldingItemType(ItemComponentType type) {
        AItemBase heldItem = getHeldItem();
        return heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type.equals(type);
    }

    @Override
    public AItemBase getHeldItem() {
        Item heldItem = player.getMainHandItem().getItem();
        return heldItem instanceof IBuilderItemInterface ? ((IBuilderItemInterface) heldItem).getWrappedItem() : null;
    }

    @Override
    public IWrapperItemStack getHeldStack() {
        return new WrapperItemStack(player.inventory.getItem(getHotbarIndex()));
    }

    @Override
    public void setHeldStack(IWrapperItemStack stack) {
        player.inventory.setItem(getHotbarIndex(), ((WrapperItemStack) stack).stack);
    }

    @Override
    public int getHotbarIndex() {
        return player.inventory.selected;
    }

    @Override
    public IWrapperInventory getInventory() {
        return new WrapperInventory(player.inventory) {
            @Override
            public int getSize() {
                return player.inventory.items.size();
            }
        };
    }

    @Override
    public void sendPacket(APacketBase packet) {
        InterfaceManager.packetInterface.sendToPlayer(packet, this);
    }

    @Override
    public void openCraftingGUI() {
        player.openMenu(new SimpleNamedContainerProvider((containerID, playerInventory, player) -> {
            return new WorkbenchContainer(containerID, playerInventory, IWorldPosCallable.create(player.level, player.blockPosition())) {
                @Override
                public boolean stillValid(PlayerEntity pPlayer) {
                    return true;
                }
            };
        }, new StringTextComponent("")));
    }

    /**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void onIVWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            playerClientWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.level);
        } else {
            playerServerWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.level);
        }
    }
}