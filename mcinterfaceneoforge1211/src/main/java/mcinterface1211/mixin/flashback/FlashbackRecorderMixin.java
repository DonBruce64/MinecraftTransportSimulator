package mcinterface1211.mixin.flashback;

import mcinterface1211.FlashbackCompat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public class FlashbackRecorderMixin {
    @Inject(
            method = "writeCustomSnapshot",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void mts$writeCustomSnapshot(Consumer<Packet<? super ClientGamePacketListener>> consumer, CallbackInfo ci) {
        FlashbackCompat.writeCustomSnapshot(consumer);
    }
}
