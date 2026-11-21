package mcinterface1211.mixin.flashback;

import mcinterface1211.FlashbackCompatUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.moulberry.flashback.editor.ui.windows.MainMenuBar", remap = false)
public class FlashbackMainMenuBarMixin {
    @Inject(
            method = "renderInner",
            at = @At(
                    value = "INVOKE",
                    target = "Limgui/moulberry90/ImGui;menuItem(Ljava/lang/String;)Z",
                    ordinal = 8 //if (ImGui.menuItem(I18n.get("flashback.player_list") + "##PlayerList")) {
            ),
            remap = false,
            require = 0
    )
    private static void mts$menuItemPlayerList(CallbackInfo ci) {
        FlashbackCompatUI.renderMenuItemBeforePlayerList();
    }
}
