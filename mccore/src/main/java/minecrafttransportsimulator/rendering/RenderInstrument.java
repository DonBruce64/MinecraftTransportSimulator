package minecrafttransportsimulator.rendering;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrument.JSONInstrumentComponent;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
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
    private static RenderableObject renderObject = null;
    private static final TransformationMatrix textTransform = new TransformationMatrix();
    private static final Point3D bottomLeft = new Point3D();
    private static final Point3D topLeft = new Point3D();
    private static final Point3D topRight = new Point3D();
    private static final Point3D bottomRight = new Point3D();
    private static final RotationMatrix helperRotation = new RotationMatrix();
    private static final RotationMatrix helperRotationMatrix = new RotationMatrix();
    private static final float[][] instrumentSingleComponentPoints = new float[6][8];

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
                    textTransform.applyTranslation(0, 0, i * 0.0001F);
                    double totalScaling = slotScale * component.scale;
                    textTransform.applyScaling(totalScaling, totalScaling, totalScaling);
                    int variablePartNumber = AEntityD_Definable.getVariableNumber(component.textObject.variableName);
                    final boolean addSuffix = variablePartNumber == -1 && ((component.textObject.variableName.startsWith("engine_") || component.textObject.variableName.startsWith("propeller_") || component.textObject.variableName.startsWith("gun_") || component.textObject.variableName.startsWith("seat_")));
                    if (addSuffix) {
                        String oldName = component.textObject.variableName;
                        component.textObject.variableName += "_" + partNumber;
                        RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, textTransform, component.textObject, true);
                        component.textObject.variableName = oldName;
                    } else {
                        RenderText.draw3DText(entity.getAnimatedTextVariableValue(component.textObject, partialTicks), entity, textTransform, component.textObject, true);
                    }
                } else {
                    //Init variables.
                    renderObject = entity.instrumentRenderables.get(slot).get(i);
                    renderObject.texture = "/assets/" + instrument.definition.packID + "/textures/" + instrument.definition.textureName;
                    renderObject.transform.set(transform);
                    renderObject.transform.applyTranslation(0.0, 0.0, i * 0.0001);
                    renderObject.transform.applyScaling(slotScale, slotScale, slotScale);
                    bottomLeft.set(-component.textureWidth / 2D, component.textureHeight / 2D, 0);
                    topLeft.set(-component.textureWidth / 2D, -component.textureHeight / 2D, 0);
                    topRight.set(component.textureWidth / 2D, -component.textureHeight / 2D, 0);
                    bottomRight.set(component.textureWidth / 2D, component.textureHeight / 2D, 0);

                    //Render if we don't have transforms, or of those transforms said we were good.
                    InstrumentSwitchbox switchbox = entity.instrumentComponentSwitchboxes.get(component);
                    if (switchbox == null || switchbox.runSwitchbox(partialTicks, true)) {
                        //Add the instrument UV-map offsets.
                        //These don't get added to the initial points to allow for rotation.
                        bottomLeft.add(component.textureXCenter, component.textureYCenter, 0);
                        topLeft.add(component.textureXCenter, component.textureYCenter, 0);
                        topRight.add(component.textureXCenter, component.textureYCenter, 0);
                        bottomRight.add(component.textureXCenter, component.textureYCenter, 0);

                        //Divide the Points by 1024.  This converts the points from pixels to the 0-1 UV values.
                        bottomLeft.scale(1D / 1024D);
                        topLeft.scale(1D / 1024D);
                        topRight.scale(1D / 1024D);
                        bottomRight.scale(1D / 1024D);

                        //Translate to the component.
                        renderObject.transform.applyTranslation(component.xCenter, -component.yCenter, 0);

                        //Scale to match definition.
                        renderObject.transform.applyScaling(component.scale, component.scale, component.scale);

                        //Set points to the variables here and render them.
                        //If the shape is lit, disable lighting for blending.
                        renderObject.disableLighting = component.lightUpTexture && lightsOn && ConfigSystem.client.renderingSettings.brightLights.value;
                        renderComponentFromState(component);
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
            int variablePartNumber = AEntityD_Definable.getVariableNumber(clock.animation.variable);
            final boolean addSuffix = variablePartNumber == -1 && !(entity instanceof APart) && (clock.animation.variable.startsWith("engine_") || clock.animation.variable.startsWith("propeller_") || clock.animation.variable.startsWith("gun_") || clock.animation.variable.startsWith("seat_"));
            if (partNumber == 0 && addSuffix) {
                partNumber = 1;
            }
            String oldVariable = clock.animation.variable;
            if (addSuffix) {
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
                    bottomLeft.x += xTranslation;
                    topLeft.x += xTranslation;
                } else if (clock.animation.axis.x > 0) {
                    topRight.x += xTranslation;
                    bottomRight.x += xTranslation;
                }
                if (clock.animation.axis.y < 0) {
                    bottomLeft.y += yTranslation;
                    bottomRight.y += yTranslation;
                } else if (clock.animation.axis.y > 0) {
                    topLeft.y += yTranslation;
                    topRight.y += yTranslation;
                }
            } else if (component.moveComponent) {
                //Translate the rather than adjust the window coords.
                renderObject.transform.applyTranslation(xTranslation, yTranslation, 0);
            } else {
                //Offset the window coords to the appropriate section of the texture sheet.
                //We don't want to do an OpenGL translation here as that would move the texture's
                //rendered position on the instrument rather than change what texture is rendered.
                if (clock.animation.axis.x != 0) {
                    bottomLeft.x += xTranslation;
                    topLeft.x += xTranslation;
                    topRight.x += xTranslation;
                    bottomRight.x += xTranslation;
                }
                if (clock.animation.axis.y != 0) {
                    bottomLeft.y += yTranslation;
                    topLeft.y += yTranslation;
                    topRight.y += yTranslation;
                    bottomRight.y += yTranslation;
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
                bottomLeft.add(clock.animation.centerPoint);
                topLeft.add(clock.animation.centerPoint);
                topRight.add(clock.animation.centerPoint);
                bottomRight.add(clock.animation.centerPoint);

                //Rotate the points by the rotation.
                helperRotation.angles.set(0, 0, variableValue);
                bottomLeft.rotate(helperRotation);
                topLeft.rotate(helperRotation);
                topRight.rotate(helperRotation);
                bottomRight.rotate(helperRotation);

                //Remove the rotation offsets.
                bottomLeft.subtract(clock.animation.centerPoint);
                topLeft.subtract(clock.animation.centerPoint);
                topRight.subtract(clock.animation.centerPoint);
                bottomRight.subtract(clock.animation.centerPoint);
            } else {
                renderObject.transform.applyTranslation((component.xCenter + clock.animation.centerPoint.x), -(component.yCenter + clock.animation.centerPoint.y), 0.0);
                helperRotationMatrix.setToAxisAngle(0, 0, 1, variableValue);
                renderObject.transform.applyRotation(helperRotationMatrix);
                renderObject.transform.applyTranslation(-(component.xCenter + clock.animation.centerPoint.x), (component.yCenter + clock.animation.centerPoint.y), 0.0);
            }
        }
    }

    /**
     * Helper method for setting points for rendering.
     */
    private static void renderComponentFromState(JSONInstrumentComponent component) {
        //Set X, Y, U, V, and normal Z.  All other values are 0.
        //Also invert V, as we're going off of pixel-coords here.
        for (int i = 0; i < instrumentSingleComponentPoints.length; ++i) {
            float[] vertex = instrumentSingleComponentPoints[i];
            switch (i) {
                case (0):
                case (3): {//Bottom-right
                    vertex[5] = component.textureWidth / 2;
                    vertex[6] = -component.textureHeight / 2;
                    vertex[3] = (float) bottomRight.x;
                    vertex[4] = (float) bottomRight.y;
                    break;
                }
                case (1): {//Top-right
                    vertex[5] = component.textureWidth / 2;
                    vertex[6] = component.textureHeight / 2;
                    vertex[3] = (float) topRight.x;
                    vertex[4] = (float) topRight.y;
                    break;
                }
                case (2):
                case (4): {//Top-left
                    vertex[5] = -component.textureWidth / 2;
                    vertex[6] = component.textureHeight / 2;
                    vertex[3] = (float) topLeft.x;
                    vertex[4] = (float) topLeft.y;
                    break;
                }
                case (5): {//Bottom-left
                    vertex[5] = -component.textureWidth / 2;
                    vertex[6] = -component.textureHeight / 2;
                    vertex[3] = (float) bottomLeft.x;
                    vertex[4] = (float) bottomLeft.y;
                    break;
                }
            }
            vertex[2] = 1.0F;
            renderObject.vertices.put(vertex);
        }
        renderObject.vertices.flip();
        renderObject.render();
    }
}