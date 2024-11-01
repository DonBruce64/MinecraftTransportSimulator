package mcinterface1182;

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
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity implements IWrapperPlayer {
    private static final Map<Player, WrapperPlayer> playerClientWrappers = new HashMap<>();
    private static final Map<Player, WrapperPlayer> playerServerWrappers = new HashMap<>();

    protected final Player player;

    /**
     * Returns a wrapper instance for the passed-in player instance.
     * Null may be passed-in safely to ease function-forwarding.
     * Note that the wrapped player class MAY be side-specific, so avoid casting
     * the wrapped entity directly if you aren't sure what its class is.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     */
    public static WrapperPlayer getWrapperFor(Player player) {
        if (player != null) {
            Map<Player, WrapperPlayer> playerWrappers = player.level.isClientSide ? playerClientWrappers : playerServerWrappers;
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

    protected WrapperPlayer(Player player) {
        super(player);
        this.player = player;
    }

    @Override
    public double getSeatOffset() {
        //Vanilla players have a -0.35 offset, which is horridly wrong.
        //Player legs are 12 pixels, but the player model has a funky scale.
        //It's supposed to be 32px, but is scaled to 30px, so we need to factor that here.
        //Only return this offset if super returns a non-zero number, which indicates we're in a sitting seat.
        if (super.getSeatOffset() != 0) {
            return (-12D / 16D) * (30D / 32D);
        } else {
            return 0;
        }
    }

    @Override
    public boolean isOP() {
        return player.getServer() == null || player.getServer().getPlayerList().getOps().get(player.getGameProfile()) != null || player.getServer().isSingleplayer();
    }

    @Override
    public void displayChatMessage(LanguageEntry language, Object... args) {
        Minecraft.getInstance().gui.getChat().addMessage(new TextComponent(String.format(language.getCurrentValue(), args)));
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
        return player.getMainArm() == HumanoidArm.RIGHT;
    }

    @Override
    public IWrapperEntity getLeashedEntity() {
        for (Mob mobEntity : player.level.getEntitiesOfClass(Mob.class, new AABB(player.position().x - 7.0D, player.position().y - 7.0D, player.position().z - 7.0D, player.position().x + 7.0D, player.position().y + 7.0D, player.position().z + 7.0D))) {
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
        return new WrapperItemStack(player.getInventory().getItem(getHotbarIndex()));
    }

    @Override
    public void setHeldStack(IWrapperItemStack stack) {
        player.getInventory().setItem(getHotbarIndex(), ((WrapperItemStack) stack).stack);
    }

    @Override
    public int getHotbarIndex() {
        return player.getInventory().selected;
    }

    @Override
    public IWrapperInventory getInventory() {
        return new WrapperInventory(player.getInventory()) {
            @Override
            public int getSize() {
                return player.getInventory().items.size();
            }
        };
    }

    @Override
    public void sendPacket(APacketBase packet) {
        InterfaceManager.packetInterface.sendToPlayer(packet, this);
    }

    @Override
    public void openCraftingGUI() {
        player.openMenu(new SimpleMenuProvider((containerID, playerInventory, player) -> {
            return new CraftingMenu(containerID, playerInventory, ContainerLevelAccess.create(player.level, player.blockPosition())) {
                @Override
                public boolean stillValid(Player pPlayer) {
                    return true;
                }
            };
        }, new TextComponent("")));
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