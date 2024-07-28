package mcinterface1122;

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
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity implements IWrapperPlayer {
    private static final Map<EntityPlayer, WrapperPlayer> playerClientWrappers = new HashMap<>();
    private static final Map<EntityPlayer, WrapperPlayer> playerServerWrappers = new HashMap<>();

    protected final EntityPlayer player;

    /**
     * Returns a wrapper instance for the passed-in player instance.
     * Null may be passed-in safely to ease function-forwarding.
     * Note that the wrapped player class MAY be side-specific, so avoid casting
     * the wrapped entity directly if you aren't sure what its class is.
     * Wrapper is cached to avoid re-creating the wrapper each time it is requested.
     */
    public static WrapperPlayer getWrapperFor(EntityPlayer player) {
        if (player != null) {
            Map<EntityPlayer, WrapperPlayer> playerWrappers = player.world.isRemote ? playerClientWrappers : playerServerWrappers;
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

    protected WrapperPlayer(EntityPlayer player) {
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
        return player.getServer() == null || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
    }

    @Override
    public void displayChatMessage(LanguageEntry language, Object... args) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(String.format(language.getCurrentValue(), args)));
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
        return player.isSneaking();
    }

    @Override
    public boolean isRightHanded() {
        return player.getPrimaryHand() == EnumHandSide.RIGHT;
    }

    @Override
    public IWrapperEntity getLeashedEntity() {
        for (EntityLiving entityLiving : player.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))) {
            if (entityLiving.getLeashed() && player.equals(entityLiving.getLeashHolder())) {
                entityLiving.clearLeashed(true, !player.capabilities.isCreativeMode);
                return WrapperEntity.getWrapperFor(entityLiving);
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
        Item heldItem = player.getHeldItemMainhand().getItem();
        return heldItem instanceof IBuilderItemInterface ? ((IBuilderItemInterface) heldItem).getItem() : null;
    }

    @Override
    public IWrapperItemStack getHeldStack() {
        return new WrapperItemStack(player.inventory.getStackInSlot(getHotbarIndex()));
    }

    @Override
    public void setHeldStack(IWrapperItemStack stack) {
        player.inventory.setInventorySlotContents(getHotbarIndex(), ((WrapperItemStack) stack).stack);
    }

    @Override
    public int getHotbarIndex() {
        return player.inventory.currentItem;
    }

    @Override
    public IWrapperInventory getInventory() {
        return new WrapperInventory(player.inventory) {
            @Override
            public int getSize() {
                return player.inventory.mainInventory.size();
            }
        };
    }

    @Override
    public void sendPacket(APacketBase packet) {
        InterfaceManager.packetInterface.sendToPlayer(packet, this);
    }

    @Override
    public void openCraftingGUI() {
        player.displayGui(new BlockWorkbench.InterfaceCraftingTable(player.world, null) {
            @Override
            public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerAccessing) {
                return new ContainerWorkbench(playerInventory, playerAccessing.world, playerAccessing.getPosition()) {
                    @Override
                    public boolean canInteractWith(EntityPlayer playerIn) {
                        return true;
                    }
                };
            }
        });
    }

    /**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void onIVWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            playerClientWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.world);
        } else {
            playerServerWrappers.keySet().removeIf(entity1 -> event.getWorld() == entity1.world);
        }
    }
}