package mcinterface261.mixin.common;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.SavedDataStorage;

@Mixin(SavedDataStorage.class)
public interface SavedDataStorageMixin {
    @Accessor("dataFolder")
    Path getDataFolder();
}
