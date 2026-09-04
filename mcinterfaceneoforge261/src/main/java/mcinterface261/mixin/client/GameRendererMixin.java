package mcinterface261.mixin.client;

import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import mcinterface261.InterfaceRender;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Mixin to inject MTS GUI rendering after the vanilla GuiRenderer has submitted its
 * draw calls to the GPU.  At that point the main render target is still active and
 * RenderType.draw() works correctly — matching the approach used in LevelRendererMixin
 * for world-space entity rendering.
 *
 * Background: in MC 1.21.5 / NeoForge 26.1 the GUI pipeline became two-phase:
 *   1. extractGui()  → builds a deferred GuiRenderState; fires RenderGuiLayerEvent
 *   2. guiRenderer.render() → submits that state to the GPU
 *
 * Our MultiBufferSource.immediate / RenderType.draw() path must run in phase 2.
 *
 * Item rendering fix: items were submitted to GuiRenderState via mcGUI.item() during
 * extract phase and rendered by the first guiRenderer.render(), then covered by MTS
 * GUI backgrounds drawn in drawPendingGUI().  We now defer item submission: items are
 * submitted after drawPendingGUI() and rendered via a second guiRenderer.render() call,
 * so they appear on top of MTS backgrounds.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Redirect the guiRenderer.render(fogBuffer) call inside GameRenderer.render().
     * Order of operations:
     *   1. guiRenderer.render(fogBuffer) — renders vanilla GUI (HUD, screens).
     *      GuiRenderState is reset/empty after this call.
     *   2. drawPendingGUI()              — flushes MTS mesh backgrounds via RenderType.draw().
     *   3. renderItemsAfterGUI()         — submits deferred item stacks to the now-empty
     *                                     GuiRenderState, then calls guiRenderer.render()
     *                                     a second time so items appear on top.
     */
    @Redirect(
        method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
        )
    )
    private void redirect_renderGuiRenderer(GuiRenderer guiRenderer, GpuBufferSlice fogBuffer) {
        guiRenderer.render(fogBuffer);
        if (InterfaceRender.pendingGUIRender) {
            //GuiRenderer.draw() applies a dynamicTransforms of translate(0,0,-11000) when submitting
            //its own GUI elements, placing them within the ortho view range [1000, 11000].
            //We must apply the same translation on the model-view stack so our vertex Z coords
            //land in the same depth range when RenderType.draw() reads getModelViewMatrix().
            Matrix4fStack mvStack = RenderSystem.getModelViewStack();
            mvStack.pushMatrix();
            mvStack.translate(0.0F, 0.0F, -11000.0F);
            InterfaceRender.drawPendingGUI();
            mvStack.popMatrix();
            InterfaceRender.renderItemsAfterGUI(guiRenderer, fogBuffer);
            mvStack.pushMatrix();
            mvStack.translate(0.0F, 0.0F, -11000.0F);
            InterfaceRender.drawPendingTooltips();
            mvStack.popMatrix();
        }
    }
}
