package mcinterface1182;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Interface that un-interfaces our interfaces.  This is ONLY for use by other mods that want to
 * see inside the builders, wrappers, and whatnot that are in the objects of the mcinterface
 * package.  In essence, this could be thought of as an API class, but in reverse.
 * Note that a good number of these methods are rather slow in their lookup code,
 * so it is not recommended to use them frequently.
 *
 * @author don_bruce
 */
public class InterfaceInterface {
    public static WrapperWorld toInternal(Level world) {
        return WrapperWorld.getWrapperFor(world);
    }

    public static Level toExternal(WrapperWorld world) {
        return world.world;
    }

    public static AEntityB_Existing toInternal(BuilderEntityExisting entity) {
        return entity.entity;
    }

    public static BuilderEntityExisting toExternal(AEntityB_Existing entity) {
        if (((WrapperWorld) entity.world).world instanceof net.minecraft.server.level.ServerLevel) {
            for (Entity mcEntity : ((net.minecraft.server.level.ServerLevel) ((WrapperWorld) entity.world).world).getAllEntities()) {
                if (mcEntity instanceof BuilderEntityExisting) {
                    if (entity.equals(((BuilderEntityExisting) mcEntity).entity)) {
                        return (BuilderEntityExisting) mcEntity;
                    }
                }
            }
        } else {
            for (Entity mcEntity : ((net.minecraft.client.multiplayer.ClientLevel) ((WrapperWorld) entity.world).world).entitiesForRendering()) {
                if (mcEntity instanceof BuilderEntityExisting) {
                    if (entity.equals(((BuilderEntityExisting) mcEntity).entity)) {
                        return (BuilderEntityExisting) mcEntity;
                    }
                }
            }
        }
        return null;
    }

    public static ATileEntityBase<?> toInternal(BuilderTileEntity tile) {
        return tile.tileEntity;
    }

    public static BuilderTileEntity toExternal(ATileEntityBase<?> tile) {
        return (BuilderTileEntity) ((WrapperWorld) tile.world).world.getBlockEntity(new BlockPos(tile.position.x, tile.position.y, tile.position.z));
    }

    public static WrapperEntity toInternal(Entity entity) {
        return WrapperEntity.getWrapperFor(entity);
    }

    public static Entity toExternal(WrapperEntity entity) {
        return entity.entity;
    }

    public static WrapperPlayer toInternal(Player player) {
        return WrapperPlayer.getWrapperFor(player);
    }

    public static Player toExternal(WrapperPlayer player) {
        return player.player;
    }

    public static WrapperNBT toInternal(CompoundTag tag) {
        return new WrapperNBT(tag);
    }

    public static CompoundTag toExternal(WrapperNBT data) {
        return data.tag;
    }

    public static WrapperInventory toInternal(Container inventory) {
        return new WrapperInventory(inventory);
    }

    public static ItemStack toExternal(WrapperInventory inventory) {
        throw new UnsupportedOperationException("ERROR: Cannot return external form of inventory wrapper due to external form being based on abstract class IInventory.");
    }

    public static WrapperItemStack toInternal(ItemStack stack) {
        return new WrapperItemStack(stack);
    }

    public static ItemStack toExternal(WrapperItemStack stack) {
        return stack.stack;
    }
}
