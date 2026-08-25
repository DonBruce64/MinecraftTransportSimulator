package minecrafttransportsimulator.guis.components;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * HUD indicator shown while the player is holding the use button to install a part.
 * This is deliberately texture-less so it can be used by the permanent overlay even when
 * a custom camera overlay texture is not active.
 *
 * @author don_bruce
 */
public class GUIComponentPartInstallationProgress extends AGUIComponent {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_BORDER = 2;
    private static final int BAR_BOTTOM_OFFSET = 32;
    private static final int PROGRESS_Z = 650;

    private final RenderableData borderRenderable;
    private final RenderableData trackRenderable;
    private final RenderableData progressRenderable;
    private final Point3D labelPosition;
    private final int screenWidth;
    private final int barCenterY;

    public GUIComponentPartInstallationProgress(int screenWidth, int screenHeight) {
        super(0, 0, screenWidth, screenHeight);
        this.screenWidth = screenWidth;
        this.barCenterY = screenHeight - BAR_BOTTOM_OFFSET;
        this.labelPosition = new Point3D(screenWidth / 2D, -(barCenterY - 10), PROGRESS_Z + 1);
        this.text = LanguageSystem.GUI_PARTINSTALL_INSTALLING.getCurrentValue();

        RenderableVertices quadVertices = RenderableVertices.createSprite(1, null, null);
        borderRenderable = createRenderable(quadVertices, ColorRGB.BLACK, 0.75F);
        trackRenderable = createRenderable(quadVertices, ColorRGB.DARK_GRAY, 0.90F);
        progressRenderable = createRenderable(quadVertices, ColorRGB.WHITE, 1.0F);
    }

    private static RenderableData createRenderable(RenderableVertices vertices, ColorRGB color, float alpha) {
        RenderableData renderable = new RenderableData(vertices);
        renderable.setColor(color);
        renderable.setAlpha(alpha);
        renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        renderable.setTransucentOverride();
        return renderable;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!blendingEnabled || !ControlSystem.isPartInstallationInProgress()) {
            return;
        }

        renderBar(borderRenderable, screenWidth / 2D, BAR_WIDTH + BAR_BORDER * 2, BAR_HEIGHT + BAR_BORDER * 2, PROGRESS_Z);
        renderBar(trackRenderable, screenWidth / 2D, BAR_WIDTH, BAR_HEIGHT, PROGRESS_Z + 1);

        double progressWidth = BAR_WIDTH * ControlSystem.getPartInstallationProgress(partialTicks);
        if (progressWidth > 0) {
            double progressCenterX = (screenWidth - BAR_WIDTH) / 2D + progressWidth / 2D;
            renderBar(progressRenderable, progressCenterX, progressWidth, BAR_HEIGHT, PROGRESS_Z + 2);
        }
    }

    private void renderBar(RenderableData renderable, double centerX, double width, double height, double z) {
        renderable.transform.resetTransforms();
        renderable.transform.setTranslation(centerX, -barCenterY, z);
        renderable.transform.applyScaling(width, height, 1);
        renderable.render();
    }

    @Override
    public void renderText(boolean renderTextLit, int worldLightValue) {
        if (ControlSystem.isPartInstallationInProgress()) {
            text = LanguageSystem.GUI_PARTINSTALL_INSTALLING.getCurrentValue();
            RenderText.drawText(text, null, labelPosition, ColorRGB.WHITE, TextAlignment.CENTERED, 1.0F, false, 0, true, worldLightValue);
        }
    }
}
