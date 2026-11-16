package mcinterface1211.mixin.flashback;

import mcinterface1211.FlashbackCompatUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.moulberry.flashback.editor.ui.windows.VisualsWindow", remap = false)
public class FlashbackVisualsWindowMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/moulberry/flashback/editor/ui/ImGuiHelper;separatorWithText(Ljava/lang/String;)V",
                    ordinal = 2 //ImGuiHelper.separatorWithText(I18n.get("flashback.visuals.world"));
            ),
            remap = false,
            require = 0
    )
    private static void mts$renderWorld(CallbackInfo ci) {
        FlashbackCompatUI.renderVisualsWorld();
    }
}
