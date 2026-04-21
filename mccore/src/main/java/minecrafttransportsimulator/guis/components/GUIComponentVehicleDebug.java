package minecrafttransportsimulator.guis.components;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderText.TextAlignment;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.ConfigSystem;

public class GUIComponentVehicleDebug extends AGUIComponent {
    public static float panelScale = 0.5F;

    private static final int BASE_PANEL_WIDTH = 400;
    private static final int BASE_BAR_WIDTH = 150;
    private static final int BASE_BAR_HEIGHT = 12;
    private static final int BASE_MARGIN = 8;
    private static final double MAX_DISTANCE = 10.0;

    private final RenderableData bgRenderable;
    private final RenderableData barBgRenderable;
    private final RenderableData barFgRenderable;

    public GUIComponentVehicleDebug() {
        super(0, 0, 0, 0);

        // Use 3 quads for "slightly rounded" corners (top, middle, bottom)
        RenderableVertices bgVertices = RenderableVertices.createSprite(3, null, null);
        bgRenderable = new RenderableData(bgVertices, AGUIBase.STANDARD_TEXTURE_NAME);
        bgRenderable.setColor(ColorRGB.BLACK);
        bgRenderable.setAlpha(0.6F);
        bgRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        bgRenderable.isTranslucent = true;

        RenderableVertices barBgVertices = RenderableVertices.createSprite(3, null, null);
        barBgRenderable = new RenderableData(barBgVertices, AGUIBase.STANDARD_TEXTURE_NAME);
        barBgRenderable.setColor(ColorRGB.GRAY);
        barBgRenderable.setAlpha(0.8F);
        barBgRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        barBgRenderable.isTranslucent = true;

        RenderableVertices barFgVertices = RenderableVertices.createSprite(3, null, null);
        barFgRenderable = new RenderableData(barFgVertices, AGUIBase.STANDARD_TEXTURE_NAME);
        barFgRenderable.setColor(ColorRGB.GREEN);
        barFgRenderable.setAlpha(0.9F);
        barFgRenderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        barFgRenderable.isTranslucent = true;
    }

    @Override
    public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks) {
        if (!ConfigSystem.settings.general.devMode.value || !ConfigSystem.client.renderingSettings.vehicleDebugPanel.value) {
            return;
        }

        if (blendingEnabled) {
            return;
        }

        AWrapperWorld world = InterfaceManager.clientInterface.getClientWorld();
        if (world == null) return;
        
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null) return;

        int panelWidth = (int) (BASE_PANEL_WIDTH * panelScale);
        int barWidth = (int) (BASE_BAR_WIDTH * panelScale);
        int barHeight = (int) (BASE_BAR_HEIGHT * panelScale);
        int margin = (int) (BASE_MARGIN * panelScale);
        int bgRadius = (int) (4 * panelScale);
        int barRadius = (int) (2 * panelScale);
        
        if (bgRadius < 1) bgRadius = 1;
        if (barRadius < 1) barRadius = 1;
        float textScale = 0.8F * panelScale;
        float titleScale = 1.0F * panelScale;
        float percentScale = 0.7F * panelScale;

        for (EntityVehicleF_Physics vehicle : world.getEntitiesExtendingType(EntityVehicleF_Physics.class)) {
            if (vehicle.position.distanceTo(player.getPosition()) > MAX_DISTANCE) {
                continue;
            }

            Point3D topPos = vehicle.position.copy();
            topPos.y += vehicle.encompassingBox.heightRadius + 2.5;
            
            Point3D screen = InterfaceManager.clientInterface.projectToScreen(topPos, gui.screenWidth, gui.screenHeight);
            if (screen == null) continue;

            double screenX = screen.x;
            double screenY = screen.y;

            List<APart> parts = new ArrayList<>();
            for (APart part : vehicle.allParts) {
                if (part.definition.general.health > 0 && !part.definition.generic.type.toLowerCase().startsWith("seat")) {
                    parts.add(part);
                }
            }

            int numBars = (vehicle.definition.general.health > 0 ? 1 : 0) + parts.size();
            if (numBars == 0) continue;
            
            int panelHeight = margin * 2 + (int)(15 * titleScale) + (barHeight + margin) * numBars;

            int startX = (int) screenX - panelWidth / 2;
            int startY = (int) screenY - panelHeight / 2;

            float u = 7 / 256F;
            float v = 7 / 256F;
            float u2 = 8 / 256F;
            float v2 = 8 / 256F;
            
            setRoundedBoxSolid(bgRenderable.vertexObject, panelWidth, panelHeight, bgRadius, u, v, u2, v2);
            bgRenderable.transform.resetTransforms();
            bgRenderable.transform.setTranslation(startX, -startY, 300);
            bgRenderable.render();

            int currentY = startY + margin;

            String vehicleName = vehicle.cachedItem != null ? vehicle.cachedItem.getItemName() : vehicle.definition.systemName;
            InterfaceManager.renderingInterface.drawVanillaText(vehicleName, (int) screenX, -(currentY), ColorRGB.WHITE, TextAlignment.CENTERED, titleScale);
            currentY += (int)(15 * titleScale) + margin;
            
            int barX = startX + panelWidth - barWidth - margin;

            if (vehicle.definition.general.health > 0) {
                InterfaceManager.renderingInterface.drawVanillaText("VEHICLE: " + vehicleName, startX + margin, -(currentY + (int)(2 * panelScale)), ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, textScale);
                
                drawBar(vehicle.damageVar.currentValue, vehicle.definition.general.health, barX, currentY, barWidth, barHeight, barRadius, percentScale, u, v, u2, v2);
                currentY += barHeight + margin;
            }

            for (APart part : parts) {
                String partType = getTypeName(part.definition.generic.type).toUpperCase();
                String partName = part.cachedItem != null ? part.cachedItem.getItemName() : part.definition.systemName;
                
                InterfaceManager.renderingInterface.drawVanillaText(partType + ": " + partName, startX + margin, -(currentY + (int)(2 * panelScale)), ColorRGB.WHITE, TextAlignment.LEFT_ALIGNED, textScale);
                
                drawBar(part.damageVar.currentValue, part.definition.general.health, barX, currentY, barWidth, barHeight, barRadius, percentScale, u, v, u2, v2);
                currentY += barHeight + margin;
            }
        }
    }
    
    private void drawBar(double currentDamage, double maxHealth, int x, int y, int barWidth, int barHeight, int r, float textScale, float u, float v, float u2, float v2) {
        double healthRatio = 1.0 - (currentDamage / maxHealth);
        if (healthRatio < 0) healthRatio = 0;
        
        int fillWidth = (int) (barWidth * healthRatio);
        
        setRoundedBoxSolid(barBgRenderable.vertexObject, barWidth, barHeight, r, u, v, u2, v2);
        barBgRenderable.transform.resetTransforms();
        barBgRenderable.transform.setTranslation(x, -y, 301);
        barBgRenderable.render();
        
        if (fillWidth > 0) {
            if (healthRatio > 0.5) barFgRenderable.setColor(ColorRGB.GREEN);
            else if (healthRatio > 0.2) barFgRenderable.setColor(ColorRGB.YELLOW);
            else barFgRenderable.setColor(ColorRGB.RED);
            
            int activeR = Math.min(r, fillWidth / 2);
            setRoundedBoxSolid(barFgRenderable.vertexObject, fillWidth, barHeight, activeR, u, v, u2, v2);
            barFgRenderable.transform.resetTransforms();
            barFgRenderable.transform.setTranslation(x, -y, 302);
            barFgRenderable.render();
        }
        
        int percent = (int) (healthRatio * 100);
        InterfaceManager.renderingInterface.drawVanillaText(percent + "%", x + barWidth / 2, -(y + (int)(barHeight / 2 - 4 * textScale)), ColorRGB.WHITE, TextAlignment.CENTERED, textScale);
    }

    private void setRoundedBoxSolid(RenderableVertices vertices, int width, int height, int r, float u, float v, float u2, float v2) {
        if (r <= 0) {
            vertices.setSpriteProperties(0, 0, 0, width, height, u, v, u2, v2);
            vertices.setSpriteProperties(1, 0, 0, 0, 0, u, v, u2, v2);
            vertices.setSpriteProperties(2, 0, 0, 0, 0, u, v, u2, v2);
            return;
        }
        vertices.setSpriteProperties(0, r, 0, width - 2*r, r, u, v, u2, v2);
        vertices.setSpriteProperties(1, 0, -r, width, height - 2*r, u, v, u2, v2);
        vertices.setSpriteProperties(2, r, -(height - r), width - 2*r, r, u, v, u2, v2);
    }
    
    private String getTypeName(String type) {
        if (type == null) return "Unknown";
        String t = type.toLowerCase();
        if (t.startsWith("engine")) return "Engine";
        if (t.startsWith("ground")) return "Ground device";
        if (t.startsWith("propeller")) return "Propeller";
        if (t.startsWith("seat")) return "Seat";
        if (t.startsWith("gun")) return "Gun";
        if (t.startsWith("interactable")) return "Interactable";
        if (t.startsWith("effector")) return "Effector";
        if (t.startsWith("generic")) return "Generic";
        if (t.startsWith("custom")) return "Custom";
        
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase().replace('_', ' ');
    }
}