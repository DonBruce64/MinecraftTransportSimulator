package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.JSONInstrumentComponent;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Main render class for instruments.  This class contains a main method that takes an instance of {@link ItemInstrument},
 * as well as the engine associated with that instrument and the vehicle the instrument is on.  This allows for an
 * instrument to be rendered a vehicle, GUI, or HUD.}.
 *
 * @author don_bruce
 */
public final class RenderInstrument {
    private static int partNumber = 0;
    private static RenderableData renderable = null;
    private static final TransformationMatrix textTransform = new TransformationMatrix();
    private static final Point3D textureCoord1 = new Point3D();
    private static final Point3D textureCoord2 = new Point3D();
    private static final Point3D textureCoord3 = new Point3D();
    private static final Point3D textureCoord4 = new Point3D();
    private static final RotationMatrix helperRotation = new RotationMatrix();
    private static final RotationMatrix helperRotationMatrix = new RotationMatrix();

    /**
     * Renders the passed-in instrument using the entity's current state.  Note that this method does NOT take any
     * entity JSON parameters into account as it does not know which instrument is being rendered.  This means that
     * any transformations that need to be applied for translation should be applied prior to calling this method.
     * Also note that the parameters in the JSON here are in png-texture space, so y is inverted.  Hence the various
     * negations in translation transforms.
     */
    public static void drawInstrument(AEntityE_Interactable<?> entity, TransformationMatrix transform, int slot, boolean onGUI, boolean blendingEnabled, float partialTicks) {
        //Get the item and slot definition here, as that's needed for future calls.
        ItemInstrument instrument = entity.instruments.get(slot);
        JSONInstrumentDefinition slotDefinition = entity.definition.instruments.get(slot);

        //Check if the lights are on.  If so, render the overlays and the text lit if requested.
        boolean lightsOn = entity.renderTextLit();

        //Get scale of the instrument, before component scaling.
        float slotScale = onGUI ? slotDefinition.hudScale : slotDefinition.scale;

        //Set the part number for switchbox reference.
        partNumber = slotDefinition.optionalPartNumber;
        if (partNumber == 0) {
            partNumber = 1;
        }

        //Finally, render the instrument based on the JSON instrument.definitions.
        //We cache up all the draw calls for this blend pass, and then render them all at once.
        //This is more efficient than rendering each one individually.
        for (int i = 0; i < instrument.definition.components.size(); ++i) {
            JSONInstrumentComponent component = instrument.definition.components.get(i);
            boolean renderLit = ((component.lightUpTexture && lightsOn) || component.alwaysLit) && ConfigSystem.client.renderingSettings.brightLights.value;
            if (component.overlayTexture && blendingEnabled || ((renderLit && !component.overlayTexture) ? (ConfigSystem.client.renderingSettings.lightsTransp.value == blendingEnabled) : (component.overlayTexture == blendingEnabled))) {
                if (component.overlayTexture) {
                    System.out.println(blendingEnabled);
                }
                //If we have text, do a text render.  Otherwise, do a normal instrument render.
                if (component.textObject != null) {
                    //Also translate slightly away from the instrument location to prevent clipping.
                    textTransform.set(transform);
                    textTransform.applyTranslation(0, 0, i * RenderableVertices.Z_BUFFER_OFFSET);
                    double totalScaling = slotScale * component.scale;
                    textTransform.applyScaling(totalScaling, totalScaling, totalScaling);

                    //Render if we don't have transforms, or of those transforms said we were good.
                    InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
                    if (switchbox == null || switchbox.runSwitchbox(partialTicks, true)) {
                        String value = entity.getRawTextVariableValue(component.textObject, partialTicks);
                        if (value != null) {
                            value = String.format(component.textObject.variableFormat, value);
                        } else {
                            value = String.format(component.textObject.variableFormat, getInstrumentVariableValue(entity, null, component.textObject.variableName, component.textObject.variableFactor, partialTicks) + component.textObject.variableOffset);
                        }
                        RenderText.draw3DText(value, entity, textTransform, component.textObject, true, renderLit);
                    }
                } else {
                    //Render if we don't have transforms, or of those transforms said we were good.
                    InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
                    if (switchbox == null || switchbox.runSwitchbox(partialTicks, true)) {
                        //Init variables.
                        renderable = entity.instrumentRenderables.get(slot).get(i);
                        renderable.setTexture("/assets/" + instrument.definition.packID + "/textures/" + instrument.definition.textureName);
                        renderable.transform.set(transform);
                        renderable.transform.applyTranslation(0.0, 0.0, i * RenderableVertices.Z_BUFFER_OFFSET);
                        renderable.transform.applyScaling(slotScale, slotScale, slotScale);
                        textureCoord1.set(-component.textureWidth / 2D, -component.textureHeight / 2D, 0);
                        textureCoord2.set(-component.textureWidth / 2D, component.textureHeight / 2D, 0);
                        textureCoord3.set(component.textureWidth / 2D, component.textureHeight / 2D, 0);
                        textureCoord4.set(component.textureWidth / 2D, -component.textureHeight / 2D, 0);

                        //Add the instrument UV-map offsets.
                        //These don't get added to the initial points to allow for rotation.
                        //Divide the Points by 1024.  This converts the points from pixels to the 0-1 UV values.
                        textureCoord1.add(component.textureXCenter, component.textureYCenter, 0).scale(1D / 1024D);
                        textureCoord2.add(component.textureXCenter, component.textureYCenter, 0).scale(1D / 1024D);
                        textureCoord3.add(component.textureXCenter, component.textureYCenter, 0).scale(1D / 1024D);
                        textureCoord4.add(component.textureXCenter, component.textureYCenter, 0).scale(1D / 1024D);

                        //Translate to the component.
                        renderable.transform.applyTranslation(component.xCenter, -component.yCenter, 0);

                        //Scale to match definition.
                        renderable.transform.applyScaling(component.scale, component.scale, component.scale);

                        //Set points to the variables here and render them.
                        //If the shape is lit, disable lighting for blending.
                        renderable.setLightValue(entity.worldLightValue);
                        if (component.overlayTexture || (renderLit && ConfigSystem.client.renderingSettings.lightsTransp.value)) {
                            renderable.setTransucentOverride();
                        } else {
                            renderable.clearTranslucentOverride();
                        }
                        if (renderLit) {
                            renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
                        } else {
                            renderable.setLightMode(onGUI ? LightingMode.IGNORE_ORIENTATION_LIGHTING : LightingMode.NORMAL);
                        }
                        
                        //Need to invert Y here since we're using pixel-based coords.
                        renderable.vertexObject.setSpritePropertiesAdvancedTexture(0, -component.textureWidth / 2, component.textureHeight / 2, component.textureWidth, component.textureHeight, (float) textureCoord1.x, (float) textureCoord1.y, (float) textureCoord2.x, (float) textureCoord2.y, (float) textureCoord3.x, (float) textureCoord3.y, (float) textureCoord4.x, (float) textureCoord4.y);
                        renderable.render();
                    }
                }
            }
        }
    }

    private static double getInstrumentVariableValue(AEntityD_Definable<?> entity, DurationDelayClock clock, String variable, double scaleFactor, float partialTicks) {
        double value;
        if (ComputedVariable.isNumberedVariable(variable)) {
            //Variable has a defined part index on it.  No modifications required.
            value = entity.getOrCreateVariable(variable).computeValue(partialTicks);
        } else {
            value = entity.getOrCreateVariable(variable + "_" + partNumber).computeValue(partialTicks);
            if (value == 0) {
                //Could be 0, or part might not exist and we need to check the main entity.  Use that one instead.
                value = entity.getOrCreateVariable(variable).computeValue(partialTicks);
            }
        }
        if (clock != null) {
            value = clock.clampAndScale(entity, value, scaleFactor, 0, partialTicks);
        } else {
            value *= scaleFactor;
        }
        return value;
    }

    /**
     * Custom instrument switchbox class.
     */
    public static class InstrumentSwitchbox extends AnimationSwitchbox {
        private final JSONInstrumentComponent component;

        public InstrumentSwitchbox(AEntityD_Definable<?> entity, JSONInstrumentComponent component) {
            super(entity, component.animations, null);
            this.component = component;
        }

        @Override
        public void runTranslation(DurationDelayClock clock, float partialTicks) {
            //Offset the coords based on the translated amount.
            //Adjust the window to either move or scale depending on settings.
            double xTranslation = getInstrumentVariableValue(entity, clock, clock.animation.variable, clock.animation.axis.x, partialTicks);
            double yTranslation = getInstrumentVariableValue(entity, clock, clock.animation.variable, clock.animation.axis.y, partialTicks);

            if (component.extendWindow) {
                //We need to add to the edge of the window in this case rather than move the entire window.
                if (clock.animation.axis.x < 0) {
                    textureCoord1.x += xTranslation;
                    textureCoord2.x += xTranslation;
                } else if (clock.animation.axis.x > 0) {
                    textureCoord3.x += xTranslation;
                    textureCoord4.x += xTranslation;
                }
                if (clock.animation.axis.y < 0) {
                    textureCoord1.y += yTranslation;
                    textureCoord4.y += yTranslation;
                } else if (clock.animation.axis.y > 0) {
                    textureCoord2.y += yTranslation;
                    textureCoord3.y += yTranslation;
                }
            } else if (component.moveComponent) {
                //Translate the rather than adjust the window coords.
                renderable.transform.applyTranslation(xTranslation, yTranslation, 0);
            } else if (component.textObject != null) {
                //Text object needs translating with basic operations.
                textTransform.applyTranslation(xTranslation, yTranslation, 0);
            } else {
                //Offset the window coords to the appropriate section of the texture sheet.
                //We don't want to do an OpenGL translation here as that would move the texture's
                //rendered position on the instrument rather than change what texture is rendered.
                if (clock.animation.axis.x != 0) {
                    textureCoord1.x += xTranslation;
                    textureCoord2.x += xTranslation;
                    textureCoord3.x += xTranslation;
                    textureCoord4.x += xTranslation;
                }
                if (clock.animation.axis.y != 0) {
                    textureCoord1.y += yTranslation;
                    textureCoord2.y += yTranslation;
                    textureCoord3.y += yTranslation;
                    textureCoord4.y += yTranslation;
                }
            }
        }

        @Override
        public void runRotation(DurationDelayClock clock, float partialTicks) {
            double variableValue = -getInstrumentVariableValue(entity, clock, clock.animation.variable, clock.animation.axis.z, partialTicks);

            //Depending on what variables are set we do different rendering operations.
            //If we are rotating the window, but not the texture we should offset the texture points to that rotated point.
            //Otherwise, we apply an OpenGL rotation operation.
            if (component.rotateWindow) {
                //Add rotation offset to the points.
                textureCoord1.add(clock.animation.centerPoint);
                textureCoord2.add(clock.animation.centerPoint);
                textureCoord3.add(clock.animation.centerPoint);
                textureCoord4.add(clock.animation.centerPoint);

                //Rotate the points by the rotation.
                helperRotation.angles.set(0, 0, variableValue);
                textureCoord1.rotate(helperRotation);
                textureCoord2.rotate(helperRotation);
                textureCoord3.rotate(helperRotation);
                textureCoord4.rotate(helperRotation);

                //Remove the rotation offsets.
                textureCoord1.subtract(clock.animation.centerPoint);
                textureCoord2.subtract(clock.animation.centerPoint);
                textureCoord3.subtract(clock.animation.centerPoint);
                textureCoord4.subtract(clock.animation.centerPoint);
            } else if (component.textObject != null) {
                textTransform.applyTranslation((component.xCenter + clock.animation.centerPoint.x), -(component.yCenter + clock.animation.centerPoint.y), 0.0);
                helperRotationMatrix.setToAxisAngle(0, 0, 1, variableValue);
                textTransform.applyRotation(helperRotationMatrix);
                textTransform.applyTranslation(-(component.xCenter + clock.animation.centerPoint.x), (component.yCenter + clock.animation.centerPoint.y), 0.0);
            } else {
                renderable.transform.applyTranslation((component.xCenter + clock.animation.centerPoint.x), -(component.yCenter + clock.animation.centerPoint.y), 0.0);
                helperRotationMatrix.setToAxisAngle(0, 0, 1, variableValue);
                renderable.transform.applyRotation(helperRotationMatrix);
                renderable.transform.applyTranslation(-(component.xCenter + clock.animation.centerPoint.x), (component.yCenter + clock.animation.centerPoint.y), 0.0);
            }
        }
    }
}