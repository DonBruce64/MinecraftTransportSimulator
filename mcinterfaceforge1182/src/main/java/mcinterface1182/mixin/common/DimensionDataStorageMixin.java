package mcinterface1182.mixin.common;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.DimensionDataStorage;

@Mixin(DimensionDataStorage.class)
public interface DimensionDataStorageMixin {
    @Accessor("dataFolder")
    File getDataFolder();
}
