package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
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
    private static final Point3D textureMinCoords = new Point3D();
    private static final Point3D textureMaxCoords = new Point3D();
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

        //Finally, render the instrument based on the JSON instrument.definitions.
        //We cache up all the draw calls for this blend pass, and then render them all at once.
        //This is more efficient than rendering each one individually.
        for (int i = 0; i < instrument.definition.components.size(); ++i) {
            JSONInstrumentComponent component = instrument.definition.components.get(i);
            if (component.overlayTexture == blendingEnabled) {
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
                        if (ComputedVariable.isNumberedVariable(component.textObject.variableName) && (component.textObject.variableName.startsWith("engine_") || component.textObject.variableName.startsWith("propeller_") || component.textObject.variableName.startsWith("gun_") || component.textObject.variableName.startsWith("seat_"))) {
                            String oldName = component.textObject.variableName;
                            component.textObject.variableName += "_" + partNumber;
                            RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, textTransform, component.textObject, true);
                            component.textObject.variableName = oldName;
                        } else {
                            RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, textTransform, component.textObject, true);
                        }
                    }
                } else {
                    //Init variables.
                    renderable = entity.instrumentRenderables.get(slot).get(i);
                    renderable.setTexture("/assets/" + instrument.definition.packID + "/textures/" + instrument.definition.textureName);
                    renderable.transform.set(transform);
                    renderable.transform.applyTranslation(0.0, 0.0, i * RenderableVertices.Z_BUFFER_OFFSET);
                    renderable.transform.applyScaling(slotScale, slotScale, slotScale);
                    textureMinCoords.set(-component.textureWidth / 2D, -component.textureHeight / 2D, 0);
                    textureMaxCoords.set(component.textureWidth / 2D, component.textureHeight / 2D, 0);

                    //Render if we don't have transforms, or of those transforms said we were good.
                    InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
                    if (switchbox == null || switchbox.runSwitchbox(partialTicks, true)) {
                        //Add the instrument UV-map offsets.
                        //These don't get added to the initial points to allow for rotation.
                        textureMinCoords.add(component.textureXCenter, component.textureYCenter, 0);
                        textureMaxCoords.add(component.textureXCenter, component.textureYCenter, 0);

                        //Divide the Points by 1024.  This converts the points from pixels to the 0-1 UV values.
                        textureMinCoords.scale(1D / 1024D);
                        textureMaxCoords.scale(1D / 1024D);

                        //Translate to the component.
                        renderable.transform.applyTranslation(component.xCenter, -component.yCenter, 0);

                        //Scale to match definition.
                        renderable.transform.applyScaling(component.scale, component.scale, component.scale);

                        //Set points to the variables here and render them.
                        //If the shape is lit, disable lighting for blending.
                        renderable.setLightValue(entity.worldLightValue);
                        if (component.lightUpTexture && lightsOn && ConfigSystem.client.renderingSettings.brightLights.value) {
                            renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
                        } else {
                            renderable.setLightMode(onGUI ? LightingMode.IGNORE_ORIENTATION_LIGHTING : LightingMode.NORMAL);
                        }
                        
                        //Need to invert Y here since we're using pixel-based coords.
                        renderable.vertexObject.setSpriteProperties(0, -component.textureWidth / 2, component.textureHeight / 2, component.textureWidth, component.textureHeight, (float) textureMinCoords.x, (float) textureMinCoords.y, (float) textureMaxCoords.x, (float) textureMaxCoords.y);
                        if (component.overlayTexture) {
                            renderable.setTransucentOverride();
                        }
                        renderable.render();
                    }
                }
            }
        }
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

        private String convertAnimationPartNumber(DurationDelayClock clock) {
            //If the partNumber is non-zero, we need to check if we are applying a part-based animation.
            //If so, we need to let the animation system know by adding a suffix to the variable.
            //Otherwise, as we don't pass-in the part, it will assume it's an entity variable.
            //We also need to set the partNumber to 1 if we have a part number of 0 and we're
            //doing a part-specific animation.
            //Skip adding a suffix if one already exists.
            final boolean addSuffix = ComputedVariable.isNumberedVariable(clock.animation.variable) && !(entity instanceof APart) && (clock.animation.variable.startsWith("engine_") || clock.animation.variable.startsWith("propeller_") || clock.animation.variable.startsWith("gun_") || clock.animation.variable.startsWith("seat_"));
            if (partNumber == 0 && addSuffix) {
                partNumber = 1;
            }
            String oldVariable = clock.animation.variable;
            if (partNumber != 0) {
                clock.animation.variable += "_" + partNumber;
            }
            return oldVariable;
        }

        @Override
        public void runTranslation(DurationDelayClock clock, float partialTicks) {
            //Offset the coords based on the translated amount.
            //Adjust the window to either move or scale depending on settings.
            String oldVariable = convertAnimationPartNumber(clock);
            double xTranslation = entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks);
            double yTranslation = entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
            clock.animation.variable = oldVariable;

            if (component.extendWindow) {
                //We need to add to the edge of the window in this case rather than move the entire window.
                if (clock.animation.axis.x < 0) {
                    textureMinCoords.x += xTranslation;
                } else if (clock.animation.axis.x > 0) {
                    textureMaxCoords.x += xTranslation;
                }
                if (clock.animation.axis.y < 0) {
                    textureMinCoords.y += yTranslation;
                } else if (clock.animation.axis.y > 0) {
                    textureMaxCoords.y += yTranslation;
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
                    textureMinCoords.x += xTranslation;
                    textureMaxCoords.x += xTranslation;
                }
                if (clock.animation.axis.y != 0) {
                    textureMinCoords.y += yTranslation;
                    textureMaxCoords.y += yTranslation;
                }
            }
        }

        @Override
        public void runRotation(DurationDelayClock clock, float partialTicks) {
            String oldVariable = convertAnimationPartNumber(clock);
            double variableValue = -entity.getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks);
            clock.animation.variable = oldVariable;

            //Depending on what variables are set we do different rendering operations.
            //If we are rotating the window, but not the texture we should offset the texture points to that rotated point.
            //Otherwise, we apply an OpenGL rotation operation.
            if (component.rotateWindow) {
                //Add rotation offset to the points.
                textureMinCoords.add(clock.animation.centerPoint);
                textureMaxCoords.add(clock.animation.centerPoint);

                //Rotate the points by the rotation.
                helperRotation.angles.set(0, 0, variableValue);
                textureMinCoords.rotate(helperRotation);
                textureMaxCoords.rotate(helperRotation);

                //Remove the rotation offsets.
                textureMinCoords.subtract(clock.animation.centerPoint);
                textureMaxCoords.subtract(clock.animation.centerPoint);
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