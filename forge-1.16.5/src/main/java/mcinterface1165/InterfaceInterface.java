package mcinterface1165;

import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    public static WrapperWorld toInternal(World world) {
        return WrapperWorld.getWrapperFor(world);
    }

    public static World toExternal(WrapperWorld world) {
        return world.world;
    }

    public static AEntityB_Existing toInternal(BuilderEntityExisting entity) {
        return entity.entity;
    }

    public static BuilderEntityExisting toExternal(AEntityB_Existing entity) {
        if (((WrapperWorld) entity.world).world instanceof ServerWorld) {
            for (Entity mcEntity : ((ServerWorld) ((WrapperWorld) entity.world).world).iterateEntities()) {
                if (mcEntity instanceof BuilderEntityExisting) {
                    if (entity.equals(((BuilderEntityExisting) mcEntity).entity)) {
                        return (BuilderEntityExisting) mcEntity;
                    }
                }
            }
        } else {
            for (Entity mcEntity : ((net.minecraft.client.world.ClientWorld) ((WrapperWorld) entity.world).world).getEntities()) {
                if (mcEntity instanceof BuilderEntityExisting) {
                    if (entity.equals(((BuilderEntityExisting) mcEntity).entity)) {
                        return (BuilderEntityExisting) mcEntity;
                    }
                }
            }
        }
        return null;
    }

    public static ATileEntityBase<?> toInternal(BuilderBlockEntity tile) {
        return tile.tileEntity;
    }

    public static BuilderBlockEntity toExternal(ATileEntityBase<?> tile) {
        return (BuilderBlockEntity) ((WrapperWorld) tile.world).world.getBlockEntity(new BlockPos(tile.position.x, tile.position.y, tile.position.z));
    }

    public static WrapperEntity toInternal(Entity entity) {
        return WrapperEntity.getWrapperFor(entity);
    }

    public static Entity toExternal(WrapperEntity entity) {
        return entity.entity;
    }

    public static WrapperPlayer toInternal(PlayerEntity player) {
        return WrapperPlayer.getWrapperFor(player);
    }

    public static PlayerEntity toExternal(WrapperPlayer player) {
        return player.player;
    }

    public static WrapperNBT toInternal(NbtCompound tag) {
        return new WrapperNBT(tag);
    }

    public static NbtCompound toExternal(WrapperNBT data) {
        return data.compound;
    }

    public static WrapperInventory toInternal(Inventory inventory) {
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
