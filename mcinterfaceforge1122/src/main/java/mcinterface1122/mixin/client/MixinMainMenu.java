package mcinterface1122.mixin.client;

import mcinterface1122.GuiButtonCustom;
import mcinterface1122.GuiButtonCustomLanguage;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.gui.NotificationModUpdateScreen;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinMainMenu extends GuiScreen {
    private static final ResourceLocation mainMenuImage = new ResourceLocation("mts:textures/splash/splashscreen.png");
    private final Minecraft minecraft = Minecraft.getMinecraft();
    @Shadow
    private int openGLWarning2Width;
    @Shadow
    private int openGLWarningX1;
    @Shadow
    private int openGLWarningY1;
    @Shadow
    private int openGLWarningX2;
    @Shadow
    private int openGLWarningY2;
    @Shadow
    private String openGLWarning1;
    @Shadow
    private String openGLWarning2;
    @Shadow
    private GuiScreen realmsNotification;
    @Shadow
    private int widthCopyright;
    @Shadow
    private int widthCopyrightRest;
    @Shadow
    private GuiButton realmsButton;
    @Shadow(remap = false)
    private GuiButton modButton;
    @Unique
    private NotificationModUpdateScreen updateNotification;

    @Shadow
    protected abstract boolean areRealmsNotificationsEnabled();

    @Inject(at = @At("RETURN"), method = "initGui")
    public void getModUpdateScreen(CallbackInfo ci) {
        updateNotification = net.minecraftforge.client.gui.NotificationModUpdateScreen.init((GuiMainMenu) ((Object) this), modButton);
    }

    @Redirect(at = @At(value = "NEW", target = "net/minecraft/client/gui/GuiButton"), method = "initGui")
    public GuiButton customMenuButtons(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        return ConfigSystem.client.renderingSettings.customMainMenu.value ? new GuiButtonCustom(buttonId, x, y, widthIn, heightIn, buttonText) : new GuiButton(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Redirect(at = @At(value = "NEW", target = "net/minecraft/client/gui/GuiButtonLanguage"), method = "initGui")
    public GuiButtonLanguage customLangButton(int buttonID, int xPos, int yPos) {
        return ConfigSystem.client.renderingSettings.customMainMenu.value ? new GuiButtonCustomLanguage(buttonID, xPos, yPos) : new GuiButtonLanguage(buttonID, xPos, yPos);
    }

    @Inject(at = @At("HEAD"), method = "addSingleplayerMultiplayerButtons", cancellable = true)
    public void addCustomSpMpButtons(int p_73969_1_, int p_73969_2_, CallbackInfo ci) {
        if (ConfigSystem.client.renderingSettings.customMainMenu.value) {
            ci.cancel();
            this.buttonList.add(new GuiButtonCustom(1, this.width / 2 - 100, p_73969_1_, I18n.format("menu.singleplayer")));
            this.buttonList.add(new GuiButtonCustom(2, this.width / 2 - 100, p_73969_1_ + p_73969_2_, I18n.format("menu.multiplayer")));
            this.realmsButton = this.addButton(new GuiButtonCustom(14, this.width / 2 + 2, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("menu.online").replace("Minecraft", "").trim()));
            this.buttonList.add(modButton = new GuiButtonCustom(6, this.width / 2 - 100, p_73969_1_ + p_73969_2_ * 2, 98, 20, I18n.format("fml.menu.mods")));
        }
    }

    @Inject(at = @At("HEAD"), method = "drawScreen", cancellable = true)
    public void drawScreenInject(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ConfigSystem.client.renderingSettings.customMainMenu.value) {
            ci.cancel();
            this.minecraft.getTextureManager().bindTexture(mainMenuImage);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawModalRectWithCustomSizedTexture(0, 0, 0, 0, 1700, 750, this.width, this.height);

            java.util.List<String> branding = com.google.common.collect.Lists.reverse(net.minecraftforge.fml.common.FMLCommonHandler.instance().getBrandings(true));
            for (int brdline = 0; brdline < branding.size(); brdline++) {
                String brd = branding.get(brdline);
                if (!com.google.common.base.Strings.isNullOrEmpty(brd)) {
                    this.drawString(this.fontRenderer, brd, 2, this.height - (10 + brdline * (this.fontRenderer.FONT_HEIGHT + 1)), 16777215);
                }
            }

            this.drawString(this.fontRenderer, "Copyright Mojang AB. Do not distribute!", this.widthCopyrightRest, this.height - 10, -1);

            if (mouseX > this.widthCopyrightRest && mouseX < this.widthCopyrightRest + this.widthCopyright && mouseY > this.height - 10 && mouseY < this.height && Mouse.isInsideWindow()) {
                drawRect(this.widthCopyrightRest, this.height - 1, this.widthCopyrightRest + this.widthCopyright, this.height, -1);
            }

            if (this.openGLWarning1 != null && !this.openGLWarning1.isEmpty()) {
                drawRect(this.openGLWarningX1 - 2, this.openGLWarningY1 - 2, this.openGLWarningX2 + 2, this.openGLWarningY2 - 1, 1428160512);
                this.drawString(this.fontRenderer, this.openGLWarning1, this.openGLWarningX1, this.openGLWarningY1, -1);
                this.drawString(this.fontRenderer, this.openGLWarning2, (this.width - this.openGLWarning2Width) / 2, (this.buttonList.get(0)).y - 12, -1);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);

            if (this.areRealmsNotificationsEnabled()) {
                this.realmsNotification.drawScreen(mouseX, mouseY, partialTicks);
            }

            updateNotification.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
