package minecrafttransportsimulator.rendering;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartGroundDevice;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.GIFParser.ParsedGIF;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * This class represents an object that can be rendered from a model.  This object is a set of
 * faces that are rendered during the main rendering routine.  Various transforms may be performed on
 * this object via the various rendering classes.  These transforms are applied to the mesh prior
 * to rendering, either manipulating the mesh directly, or manipulating the OpenGL state.
 *
 * @author don_bruce
 */
public class RenderableModelObject {
    public final RenderableData renderable;
    private final boolean isWindow;
    private final boolean isOnlineTexture;
    private final JSONAnimatedObject objectDef;
    private final JSONLight lightDef;
    private final AnimationSwitchbox switchbox;
    private final RenderableData interiorWindowRenderable;
    private final RenderableData colorRenderable;
    private final RenderableData flareRenderable;
    private final RenderableData beamRenderable;
    private final RenderableData coverRenderable;
    private final List<Double[]> treadPoints;

    private static final TransformationMatrix treadPathBaseTransform = new TransformationMatrix();
    private static final RotationMatrix treadRotation = new RotationMatrix();
    private static final float COLOR_OFFSET = RenderableVertices.Z_BUFFER_OFFSET;
    private static final float FLARE_OFFSET = COLOR_OFFSET + RenderableVertices.Z_BUFFER_OFFSET;
    private static final float COVER_OFFSET = FLARE_OFFSET + RenderableVertices.Z_BUFFER_OFFSET;

    private static final Set<String> downloadingTextures = new HashSet<>();
    private static final Set<String> downloadedTextures = new HashSet<>();
    private static final String ERROR_TEXTURE_NAME = "ERROR";
    private static final Map<String, String> erroredTextures = new HashMap<>();
    private static boolean errorTextureBound;

    public RenderableModelObject(AEntityD_Definable<?> entity, RenderableVertices vertexObject) {
        super();
        this.isWindow = vertexObject.name.toLowerCase(Locale.ROOT).contains(AModelParser.WINDOW_OBJECT_NAME);
        this.isOnlineTexture = vertexObject.name.toLowerCase(Locale.ROOT).startsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME) || vertexObject.name.toLowerCase(Locale.ROOT).endsWith(AModelParser.ONLINE_TEXTURE_OBJECT_NAME);
        this.objectDef = entity.animatedObjectDefinitions.get(vertexObject.name);
        this.lightDef = entity.lightObjectDefinitions.get(vertexObject.name);
        this.switchbox = entity.animatedObjectSwitchboxes.get(vertexObject.name);

        //If we are a window, split the model into two parts.  The first will be the exterior which will
        //be our normal model, the second will be a new, inverted, interior model.
        if (isWindow) {
            this.renderable = new RenderableData(vertexObject, "mts:textures/rendering/glass.png");
            renderable.vertexObject.setTextureBounds(0, 1, 0, 1);
            this.interiorWindowRenderable = new RenderableData(vertexObject.createBackface(), "mts:textures/rendering/glass.png");
        } else {
            this.renderable = new RenderableData(vertexObject);
            this.interiorWindowRenderable = null;
        }

        //Create light objects.
        if (lightDef != null) {
            if (lightDef.emissive) {
                this.colorRenderable = new RenderableData(vertexObject.createOverlay(COLOR_OFFSET), "mts:textures/rendering/light.png");
            } else {
                this.colorRenderable = null;
            }
            if (lightDef.isBeam) {
                renderable.setTransucentOverride();
            }
            if (lightDef.blendableComponents != null && !lightDef.blendableComponents.isEmpty()) {
                List<JSONLightBlendableComponent> flareDefs = new ArrayList<>();
                List<JSONLightBlendableComponent> beamDefs = new ArrayList<>();
                for (JSONLightBlendableComponent component : lightDef.blendableComponents) {
                    if (component.flareHeight > 0) {
                        flareDefs.add(component);
                    }
                    if (component.beamDiameter > 0) {
                        beamDefs.add(component);
                    }
                }
                if (!flareDefs.isEmpty()) {
                    List<TransformationMatrix> flareTransforms = new ArrayList<>();
                    List<Point3D> flareNormals = new ArrayList<>();
                    for (JSONLightBlendableComponent flareDef : flareDefs) {
                        //Get the matrix  that is needed to rotate points to the normalized vector.
                        TransformationMatrix transform = new TransformationMatrix();
                        transform.applyTranslation(flareDef.axis.copy().scale(FLARE_OFFSET).add(flareDef.pos));
                        transform.applyRotation(new RotationMatrix().setToVector(flareDef.axis, false));
                        transform.applyScaling(flareDef.flareWidth, flareDef.flareHeight, 1);
                        flareTransforms.add(transform);
                        flareNormals.add(flareDef.axis);
                    }
                    this.flareRenderable = new RenderableData(RenderableVertices.createSprite(flareDefs.size(), flareTransforms, flareNormals), "mts:textures/rendering/lensflare.png");
                    flareRenderable.setTransucentOverride();
                } else {
                    this.flareRenderable = null;
                }
                if (!beamDefs.isEmpty()) {
                    this.beamRenderable = new RenderableData(RenderableVertices.createLightBeams(beamDefs), "mts:textures/rendering/lightbeam.png");
                    beamRenderable.setTransucentOverride();
                } else {
                    this.beamRenderable = null;
                }
            } else {
                this.flareRenderable = null;
                this.beamRenderable = null;
            }
            if (lightDef.covered) {
                this.coverRenderable = new RenderableData(renderable.vertexObject.createOverlay(COVER_OFFSET), "mts:textures/rendering/glass.png");
            } else {
                this.coverRenderable = null;
            }
        } else {
            this.colorRenderable = null;
            this.flareRenderable = null;
            this.beamRenderable = null;
            this.coverRenderable = null;
        }

        //If we are a tread, create tread points.
        if (entity instanceof PartGroundDevice && ((PartGroundDevice) entity).definition.ground.isTread && !((PartGroundDevice) entity).isSpare) {
            this.treadPoints = generateTreads((PartGroundDevice) entity);
        } else {
            this.treadPoints = null;
        }

        //Bind the error texture if we haven't already.
        if (!errorTextureBound) {
            InterfaceManager.renderingInterface.bindURLTexture(ERROR_TEXTURE_NAME, null);
            errorTextureBound = true;
        }
    }

    /**
     * Renders this object, applying any transforms that need to happen.  This method also
     * renders any objects that depend on this object's transforms after rendering.
     */
    public void render(AEntityD_Definable<?> entity, TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        //Do pre-render checks based on the object we are rendering.
        //This may block rendering if there are false visibility transforms or the wrong render pass.
        if (shouldRender(entity, blendingEnabled, partialTicks)) {
            //If we are a online texture, bind that one rather than our own.
            //We do this first since we don't need to calculate other stuff if we aren't rendering.
            if (isOnlineTexture) {
                //Get the texture from the text objects of the entity.
                //If we don't have anything set, we just use the existing texture.
                for (Entry<JSONText, String> textEntry : entity.text.entrySet()) {
                    JSONText textDef = textEntry.getKey();
                    if (textDef.fieldName != null && renderable.vertexObject.name.contains(textDef.fieldName)) {
                        String textValue = entity.text.get(textDef);
                        if (erroredTextures.containsKey(textValue)) {
                            //Error in texture downloading, set fault data before continuing.
                            textEntry.setValue(erroredTextures.get(textValue));
                        }
                        if (textValue.startsWith(ERROR_TEXTURE_NAME)) {
                            //Texture didn't download, set to error texture.
                            renderable.setTexture(ERROR_TEXTURE_NAME);
                        } else if (downloadedTextures.contains(textValue)) {
                            //Good to render, set texture to object and go.
                            renderable.setTexture(textValue);
                        } else if (downloadingTextures.contains(textValue)) {
                            //Still downloading, skip rendering.
                            return;
                        } else if (textValue.isEmpty()) {
                            //Don't render since we don't have any text bound here.
                            return;
                        } else {
                            //No data at all.  Need to queue up a downloader for this texture.  Do so and skip rendering until it completes.
                            new ConnectorThread(textValue).run();
                            downloadingTextures.add(textValue);
                            return;
                        }
                        break;
                    }
                }
            } else if (!isWindow) {
                //Set our standard texture, provided we're not a window.
                //This allows the entity to dynamically change its texture.
                renderable.setTexture(entity.getTexture());
            }

            //Now set dynamic alpha if we have it, since this dictates translucent state.
            if (objectDef != null && objectDef.blendedAnimations && switchbox != null && switchbox.lastVisibilityClock != null) {
                if (switchbox.lastVisibilityValue < switchbox.lastVisibilityClock.animation.clampMin) {
                    renderable.setAlpha(0);
                } else if (switchbox.lastVisibilityValue >= switchbox.lastVisibilityClock.animation.clampMax) {
                    //Need >= here instead of above for things where min/max clamps are equal.
                    renderable.setAlpha(1);
                } else {
                    renderable.setAlpha((float) ((switchbox.lastVisibilityValue - switchbox.lastVisibilityClock.animation.clampMin) / (switchbox.lastVisibilityClock.animation.clampMax - switchbox.lastVisibilityClock.animation.clampMin)));
                }
            }

            //If we aren't on the right pass for our main object, and we don't have lights, skip further calcs.
            if (renderable.isTranslucent != blendingEnabled && lightDef == null) {
                return;
            }

            //If we are a light, get lightLevel for later.
            float lightLevel;
            if (lightDef != null) {
                lightLevel = entity.lightBrightnessValues.get(lightDef);
                AEntityD_Definable<?> masterEntity = entity;
                if (masterEntity instanceof APart) {
                    masterEntity = ((APart) masterEntity).masterEntity;
                }
                if (lightDef.isElectric && masterEntity instanceof EntityVehicleF_Physics) {
                    //Light start dimming at 10V, then go dark at 3V.
                    double electricPower = ((EntityVehicleF_Physics) masterEntity).electricPower;
                    if (electricPower < 3) {
                        lightLevel = 0;
                    } else if (electricPower < 10) {
                        lightLevel *= (electricPower - 3) / 7D;
                    }
                }
            } else {
                lightLevel = 0;
            }

            //Apply transforms.
            renderable.transform.set(transform);
            if (switchbox != null) {
                renderable.transform.multiply(switchbox.netMatrix);
            }

            //Do main rendering based on object properties.
            if (treadPoints != null) {
                //Active tread.  Do tread-path rendering instead of normal model.
                renderable.setLightValue(entity.worldLightValue);
                doTreadRendering((PartGroundDevice) entity, partialTicks);
            } else {
                //Set object states and render.
                if (renderable.isTranslucent == blendingEnabled) {
                    if (lightDef != null && lightDef.isBeam) {
                        //Model that's actually a beam, render it with beam lighting/blending. 
                        renderable.setLightValue(entity.worldLightValue);
                        renderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.NORMAL);
                        renderable.setBlending(ConfigSystem.client.renderingSettings.blendedLights.value);
                        renderable.setAlpha(Math.min((1 - entity.world.getLightBrightness(entity.position, false)) * lightLevel, 1));
                        renderable.render();
                    } else {
                        //Do normal rendering.
                        renderable.setLightValue(entity.worldLightValue);
                        renderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value && lightLevel > 0 && !lightDef.emissive && !lightDef.isBeam ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.NORMAL);
                        renderable.render();

                        //Render interior window if we have one.
                        if (interiorWindowRenderable != null && ConfigSystem.client.renderingSettings.innerWindows.value) {
                            interiorWindowRenderable.setLightValue(renderable.worldLightValue);
                            interiorWindowRenderable.transform.set(renderable.transform);
                            interiorWindowRenderable.render();
                        }
                    }
                }
            }

            //Check if we are a light that's not a beam.  If so, do light-specific rendering.
            if (lightDef != null && !lightDef.isBeam) {
                ColorRGB color = entity.lightColorValues.get(lightDef);
                if (colorRenderable != null && lightLevel > 0) {
                    //Color renderable might or might not be translucent depending on current alpha state.
                    colorRenderable.setAlpha(lightLevel);
                    if (blendingEnabled == colorRenderable.isTranslucent) {
                        colorRenderable.setLightValue(renderable.worldLightValue);
                        colorRenderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.IGNORE_ORIENTATION_LIGHTING);
                        colorRenderable.setColor(color);
                        colorRenderable.transform.set(renderable.transform);
                        colorRenderable.render();
                    }
                }

                //Flares and beams are always rendered on the blended pass since they need to do alpha blending.
                if (blendingEnabled && lightLevel > 0) {
                    //Light flares or beams detected on blended render pass.
                    //First render all flares, then render all beams.
                    float blendableBrightness = Math.min((1 - entity.world.getLightBrightness(entity.position, false)) * lightLevel, 1);
                    if (blendableBrightness > 0) {
                        if (flareRenderable != null) {
                            flareRenderable.setLightValue(renderable.worldLightValue);
                            flareRenderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.NORMAL);
                            flareRenderable.setColor(color);
                            flareRenderable.setAlpha(blendableBrightness);
                            flareRenderable.transform.set(renderable.transform);
                            flareRenderable.render();
                        }
                        if (beamRenderable != null && entity.shouldRenderBeams()) {
                            beamRenderable.setLightValue(renderable.worldLightValue);
                            beamRenderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.NORMAL);
                            beamRenderable.setBlending(ConfigSystem.client.renderingSettings.blendedLights.value);
                            beamRenderable.setColor(color);
                            beamRenderable.setAlpha(blendableBrightness);
                            beamRenderable.transform.set(renderable.transform);
                            beamRenderable.render();
                        }
                    }
                }
                if (!blendingEnabled && coverRenderable != null) {
                    //Light cover detected on solid render pass.
                    coverRenderable.setLightValue(renderable.worldLightValue);
                    coverRenderable.setLightMode(ConfigSystem.client.renderingSettings.brightLights.value && lightLevel > 0 ? LightingMode.IGNORE_ALL_LIGHTING : LightingMode.NORMAL);
                    coverRenderable.transform.set(renderable.transform);
                    coverRenderable.render();
                }
            }

            //Render text on this object.  Only do this on the solid pass.
            if (!blendingEnabled) {
                for (Entry<JSONText, String> textEntry : entity.text.entrySet()) {
                    JSONText textDef = textEntry.getKey();
                    if (renderable.vertexObject.name.equals(textDef.attachedTo)) {
                        RenderText.draw3DText(textEntry.getValue(), entity, renderable.transform, textDef, false);
                    }
                }
            }
        }
    }

    /**
     * Call to destroy this renderable object.  This should be done prior to re-parsing the model
     * as it allows for the freeing of OpenGL resources.
     */
    public void destroy() {
        renderable.destroy();
    }

    private boolean shouldRender(AEntityD_Definable<?> entity, boolean blendingEnabled, float partialTicks) {
        //Block windows if we have them disabled.
        if (isWindow && !ConfigSystem.client.renderingSettings.renderWindows.value) {
            return false;
        }
        //If we have a switchbox, run it once, and if it returns false for a non-blended object, don't render.
        if (switchbox != null) {
            if (objectDef.blendedAnimations) {
                switchbox.runSwitchbox(partialTicks, false);
            } else {
                return switchbox.runSwitchbox(partialTicks, false);
            }
        }
        //No false conditions, return true.
        return true;
    }

    private void doTreadRendering(PartGroundDevice tread, float partialTicks) {
        //Render the treads along their points.
        //We manually set point 0 here due to the fact it's a joint between two differing angles.
        //We also need to translate to that point to start rendering as we're currently at 0,0,0.
        //For each remaining point, we only translate the delta of the point.
        float treadLinearPosition = (float) (tread.getOrCreateVariable("ground_rotation").computeValue(partialTicks) / 360D);
        float treadMovementPercentage = (treadLinearPosition % tread.definition.ground.spacing) / tread.definition.ground.spacing;
        if (treadMovementPercentage < 0) {
            ++treadMovementPercentage;
        }
        Double[] point;
        Double[] nextPoint;
        double yDelta;
        double zDelta;
        double angleDelta;

        //Tread rendering is done via the thing the tread is on, which will assume the part is centered at 0, 0, 0.
        //We need to undo the offset of the tread part for this routine.
        if (!(tread.entityOn instanceof APart)) {
            renderable.transform.applyTranslation(0, -tread.localOffset.y / tread.entityOn.scale.y, -tread.localOffset.z / tread.entityOn.scale.z);
        }

        //Add initial translation for the first point
        point = treadPoints.get(0);
        renderable.transform.applyTranslation(0, point[0], point[1]);

        //Get cycle index for later.
        boolean[] renderIndexes = null;
        if (tread.definition.ground.treadOrder != null) {
            int treadCycleCount = tread.definition.ground.treadOrder.size();
            double treadCycleTotalDistance = treadCycleCount * tread.definition.ground.spacing;
            int treadCycleIndex = (int) Math.floor(treadCycleCount * ((treadLinearPosition % treadCycleTotalDistance) / treadCycleTotalDistance));
            if (treadCycleIndex < 0) {
                //Need to handle negatives if we only go backwards.
                treadCycleIndex += treadCycleCount;
            }
            renderIndexes = new boolean[treadCycleCount];
            for (int i = 0; i < treadCycleCount; ++i) {
                String treadObject = tread.definition.ground.treadOrder.get(i);
                renderIndexes[(i + treadCycleIndex) % treadCycleCount] = treadObject.equals(renderable.vertexObject.name);
            }
        }

        //Now transform all points.
        for (int i = 0; i < treadPoints.size() - 1; ++i) {
            //Update variables.
            //If we're at the last point, set the next point to the first point.
            //Also adjust angle delta, as it'll likely be almost 360 and needs to be adjusted for this.
            point = treadPoints.get(i);
            if (i == treadPoints.size() - 1) {
                nextPoint = treadPoints.get(0);
                angleDelta = (nextPoint[2] + 360) - point[2];
            } else {
                nextPoint = treadPoints.get(i + 1);
                angleDelta = nextPoint[2] - point[2];
            }
            yDelta = nextPoint[0] - point[0];
            zDelta = nextPoint[1] - point[1];

            //If our angle delta is greater than 180, we can assume that we're inverted.
            //This happens when we cross the 360 degree rotation barrier.
            if (angleDelta > 180) {
                angleDelta -= 360;
            } else if (angleDelta < -180) {
                angleDelta += 360;
            }

            //Check if we should render this object as a link in this position.
            //This is normally true, but for patterns we need to only render in specific spots.
            if (renderIndexes != null && !renderIndexes[i % renderIndexes.length]) {
                renderable.transform.applyTranslation(0, yDelta, zDelta);
                continue;
            }

            //Translate to the current position of the tread based on the percent it has moved.
            //This is determined by partial ticks and actual tread position.
            //Once there, render the tread.  Then translate the remainder of the way to prepare
            //to render the next tread.
            renderable.transform.applyTranslation(0, yDelta * treadMovementPercentage, zDelta * treadMovementPercentage);

            //If there's no rotation to the point, and no delta between points, don't do rotation.  That's just extra math.
            //Do note that the model needs to be rotated 180 on the X-axis due to all our points
            //assuming a YZ coordinate system with 0 degrees rotation being in +Y (just how the math comes out).
            //This is why 180 is added to all points cached in the operations above.
            if (point[2] != 0 || angleDelta != 0) {
                //We can't use a running rotation here as we'll end up translating in the rotated
                //coordinate system.  To combat this, we translate like normal, but then push a
                //stack and rotate prior to rendering.  This keeps us from having to do another
                //rotation to get the old coordinate system back.
                treadPathBaseTransform.set(renderable.transform);
                treadRotation.setToAxisAngle(1, 0, 0, point[2] + angleDelta * treadMovementPercentage);
                renderable.transform.applyRotation(treadRotation);
                renderable.render();
                renderable.transform.set(treadPathBaseTransform);
            } else {
                //Just render as normal as we didn't rotate.
                renderable.render();
            }

            //Add remaining translation.
            renderable.transform.applyTranslation(0, yDelta * (1 - treadMovementPercentage), zDelta * (1 - treadMovementPercentage));
        }
    }

    private static <TreadEntity extends AEntityD_Definable<?>> List<Double[]> generateTreads(PartGroundDevice tread) {
        //If we don't have the deltas, calculate them based on the points of the rollers defined in the JSON.			
        //Search through rotatable parts on the model and grab the rollers.
        List<RenderableVertices> parsedModel = AModelParser.parseModel(tread.entityOn.definition.getModelLocation(tread.entityOn.definition.definitions.get(0)), true);
        List<TreadRoller> rollers = new ArrayList<>();
        if (tread.placementDefinition.treadPath == null) {
            throw new IllegalArgumentException("No tread path found for part slot on " + tread.entityOn + "!");
        }
        for (String rollerName : tread.placementDefinition.treadPath) {
            boolean foundRoller = false;
            for (RenderableVertices modelObject : parsedModel) {
                if (modelObject.name.equals(rollerName)) {
                    rollers.add(new TreadRoller(modelObject));
                    foundRoller = true;
                    break;
                }
            }
            if (!foundRoller) {
                throw new IllegalArgumentException("Could not create tread path for " + tread.entityOn + " Due to missing roller " + rollerName + " in the model!");
            }
        }

        //Now that we have all the rollers, we can start calculating points.
        //First calculate the endpoints on the rollers by calling the calculation method.
        for (int i = 0; i < rollers.size(); ++i) {
            if (i < rollers.size() - 1) {
                rollers.get(i).calculateEndpoints(rollers.get(i + 1));
            } else {
                rollers.get(i).calculateEndpoints(rollers.get(0));
            }
        }

        //We need to ensure the endpoints are all angle-aligned.
        //It's possible to have a start angle of -181 and end angle of
        //181, which is really just 2 degress of angle (179-181).
        //To do this, we set the start angle of roller 1 to be 180, 
        //or downward-facing.  From there, we add angles to align things.
        //At the end, we should have a total angle of 540, or 180 + 360.
        rollers.get(0).setEndAngle(180);
        for (int i = 1; i < rollers.size(); ++i) {
            TreadRoller roller = rollers.get(i);
            TreadRoller priorRoller = rollers.get(i - 1);

            //Set the start angle to the end angle of the prior roller, then check for validity.
            roller.startAngle = i == 1 ? 180 : priorRoller.endAngle;

            //Roller angle delta  should be within -30-330 degrees.
            //Positive angles are standard, but negative are possible for concave routing points.
            while (roller.endAngle < roller.startAngle - 30) {
                roller.endAngle += 360;
            }
            while (roller.endAngle > roller.startAngle + 330) {
                roller.endAngle -= 360;
            }

            if (roller.endAngle < roller.startAngle) {
                //We have a concave roller.  Set our start and end angle to the midpoint of their current values.
                //This aligns the point on the roller to the center of concavity.
                double midPoint = roller.endAngle + (roller.startAngle - roller.endAngle) / 2D;
                roller.startAngle = midPoint;
                roller.endAngle = midPoint;
            }

            //Roller angles are bound.  Set our start and end angle values.
            roller.setStartAngle(roller.startAngle);
            roller.setEndAngle(roller.endAngle);
        }
        //Set the start angle to match the end angle of the last roller, rather than the 180 we set.
        rollers.get(0).setStartAngle(rollers.get(rollers.size() - 1).endAngle);

        //Now that the endpoints are set, we can calculate the path.
        //Do this by following the start and end points at small increments.
        //First calculate the total path length, and determine the optimum spacing.
        //This is the closest value to the definition's tread spacing.
        double totalPathLength = 0;
        for (int i = 0; i < rollers.size(); ++i) {
            //Get roller and add roller path contribution.
            TreadRoller roller = rollers.get(i);
            double angleDelta = roller.endAngle - roller.startAngle;
            if (i == 0) {
                //Need to add 360 rev for angle delta, as this will be this way from the tread going around the path.
                angleDelta += 360;
            }
            totalPathLength += 2 * Math.PI * roller.radius * angleDelta / 360D;

            //Get next roller and add distance path contribution.
            //For points that start and end at an angle of around 0 (top of rollers) we add droop.
            //This is a hyperbolic function, so we need to calculate the integral value to account for the path.
            TreadRoller nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
            double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
            if (tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle % 360 < 10 || roller.endAngle % 360 > 350) && (nextRoller.startAngle % 360 < 10 || nextRoller.startAngle % 360 > 350)) {
                //Catenary path length is a*singh(x/a), a is droop constant, x will be 1/2 total catenary distance due to symmetry, multiply this distance by 2 for total droop.
                totalPathLength += 2D * tread.placementDefinition.treadDroopConstant * Math.sinh((straightPathLength / 2D) / tread.placementDefinition.treadDroopConstant);
            } else {
                totalPathLength += straightPathLength;
            }
        }

        double deltaDist = tread.definition.ground.spacing + (totalPathLength % tread.definition.ground.spacing) / (totalPathLength / tread.definition.ground.spacing);
        double leftoverPathLength = 0;
        double yPoint = 0;
        double zPoint = 0;
        List<Double[]> points = new ArrayList<>();
        for (int i = 0; i < rollers.size(); ++i) {
            TreadRoller roller = rollers.get(i);
            //Follow the curve of the roller from the start and end point.
            //Do this until we don't have enough roller path left to make a point.
            //If we have any remaining path from a prior operation, we
            //need to offset our first point on the roller path to account for it.
            //It can very well be that this remainder will be more than the path length
            //of the roller.  If so, we just skip the roller entirely.
            //For the first roller we need to do some special math, as the angles will be inverted
            //For start and end due to the tread making a full 360 path.
            double currentAngle = roller.startAngle;
            double angleDelta = roller.endAngle - roller.startAngle;
            if (i == 0) {
                //Need to add 360 rev for angle delta, as this will be this way from the tread going around the path.
                angleDelta += 360;
            }
            double rollerPathLength = 2 * Math.PI * roller.radius * angleDelta / 360D;

            //Add the first point here, and add more as we follow the path.
            if (i == 0) {
                yPoint = roller.centerPoint.y + roller.radius * Math.cos(Math.toRadians(currentAngle));
                zPoint = roller.centerPoint.z + roller.radius * Math.sin(Math.toRadians(currentAngle));
                points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
            }

            //If we have any leftover straight path, account for it here to keep spacing consistent.
            //We will need to interpolate the point that the straight path would have gone to, but
            //take our rotation angle into account.  Only do this if we have enough of a path to do so.
            //If not, we should just skip this roller as we can't put any points on it.
            if (deltaDist - leftoverPathLength < rollerPathLength) {
                if (leftoverPathLength > 0) {
                    //Go backwards on the roller so when we do our next operation, we align with a new point.
                    //This ensures the new point will be closer to the start of the roller than normal.
                    //Make a new point that's a specific amount of path-movement along this roller.
                    //Then increment currentAngle to account for the new point made.
                    //We use the circumference of the roller and the remaining path to find out the amount to adjust.
                    currentAngle -= 360D * leftoverPathLength / roller.circumference;
                    rollerPathLength += leftoverPathLength;
                    leftoverPathLength = 0;
                }

                while (rollerPathLength > deltaDist) {
                    //Go to and add the next point on the roller path.
                    rollerPathLength -= deltaDist;
                    currentAngle += 360D * (deltaDist / roller.circumference);
                    yPoint = roller.centerPoint.y + roller.radius * Math.cos(Math.toRadians(currentAngle));
                    zPoint = roller.centerPoint.z + roller.radius * Math.sin(Math.toRadians(currentAngle));
                    points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
                }
            }

            //Done following roller, set angle to roller end angle to prevent slight FPEs.
            currentAngle = roller.endAngle;

            //If we have any leftover roller path, account for it here to keep spacing consistent.
            //We may also have leftover straight path length if we didn't do anything on a roller.
            //If we have roller length, make sure to offset it to account for the curvature of the roller.
            //If we don't do this, the line won't start at the end of the prior roller.
            //If we are on the last roller, we need to get the first roller to complete the loop.
            //For points that start and end at an angle of around 0 (top of rollers) we add droop.
            TreadRoller nextRoller = i == rollers.size() - 1 ? rollers.get(0) : rollers.get(i + 1);
            double straightPathLength = Math.hypot(nextRoller.startY - roller.endY, nextRoller.startZ - roller.endZ);
            double extraPathLength = rollerPathLength + leftoverPathLength;
            if (tread.placementDefinition.treadDroopConstant > 0 && (roller.endAngle % 360 < 10 || roller.endAngle % 360 > 350) && (nextRoller.startAngle % 360 < 10 || nextRoller.startAngle % 360 > 350)) {
                //Catenary path length is a*singh(x/a), a is droop constant, x will be 1/2 total catenary distance due to symmetry, multiply this distance by 2 for total droop.
                double catenaryPathLength = 2D * tread.placementDefinition.treadDroopConstant * Math.sinh((straightPathLength / 2D) / tread.placementDefinition.treadDroopConstant);

                //Get the top point in Y for the tips of the catenary (1/2 the span).  We will translate the droop path down this far to make the ends line up at Y=0.
                //We then offset this value to the rollers for the actual point position.
                final double catenaryPathEdgeY = tread.placementDefinition.treadDroopConstant * Math.cosh((straightPathLength / 2D) / tread.placementDefinition.treadDroopConstant);

                double catenaryFunctionCurrent = -catenaryPathLength / 2F;
                double catenaryPointZ;
                double catenaryPointY;
                double startingCatenaryPathLength = catenaryPathLength;
                double yDelta = nextRoller.startY - roller.endY;
                while (catenaryPathLength + extraPathLength > deltaDist) {
                    //Go to and add the next point on the catenary path.
                    if (extraPathLength > 0) {
                        catenaryFunctionCurrent += (deltaDist - extraPathLength);
                        catenaryPathLength -= (deltaDist - extraPathLength);
                        extraPathLength = 0;
                    } else {
                        catenaryFunctionCurrent += deltaDist;
                        catenaryPathLength -= deltaDist;
                    }
                    double value = catenaryFunctionCurrent / tread.placementDefinition.treadDroopConstant;
                    double arcSin = catenaryFunctionCurrent == 0.0 ? 0 : Math.log(value + Math.sqrt(value * value + 1.0));
                    double catenaryFunctionPercent = (catenaryFunctionCurrent + startingCatenaryPathLength / 2) / startingCatenaryPathLength;
                    catenaryPointZ = tread.placementDefinition.treadDroopConstant * arcSin;
                    catenaryPointY = tread.placementDefinition.treadDroopConstant * Math.cosh(catenaryPointZ / tread.placementDefinition.treadDroopConstant);
                    yPoint = roller.endY + yDelta * catenaryFunctionPercent + catenaryPointY - catenaryPathEdgeY;
                    zPoint = roller.endZ + catenaryPointZ + straightPathLength / 2D;
                    points.add(new Double[]{yPoint, zPoint, currentAngle + 180 - Math.toDegrees(Math.asin(catenaryFunctionCurrent / tread.placementDefinition.treadDroopConstant))});
                }
                leftoverPathLength = catenaryPathLength;
            } else {
                double normalizedY = (nextRoller.startY - roller.endY) / straightPathLength;
                double normalizedZ = (nextRoller.startZ - roller.endZ) / straightPathLength;
                while (straightPathLength + extraPathLength > deltaDist) {
                    //Go to and add the next point on the straight path.
                    if (extraPathLength > 0) {
                        yPoint = roller.endY + normalizedY * (deltaDist - extraPathLength);
                        zPoint = roller.endZ + normalizedZ * (deltaDist - extraPathLength);
                        straightPathLength -= (deltaDist - extraPathLength);
                        extraPathLength = 0;
                    } else {
                        yPoint += normalizedY * deltaDist;
                        zPoint += normalizedZ * deltaDist;
                        straightPathLength -= deltaDist;
                    }
                    points.add(new Double[]{yPoint, zPoint, currentAngle + 180});
                }
                leftoverPathLength = straightPathLength;
            }
        }
        return points;
    }

    /**
     * Custom URL downloader class to prevent blocking of the main thread when downloading textures
     * and to give more time for the downloader to run.
     *
     * @author don_bruce
     */
    private static class ConnectorThread extends Thread {
        private final String urlString;

        public ConnectorThread(String urlString) {
            this.urlString = urlString;
        }

        @Override
        public void run() {
            //Parse the texture out into an InputStream, if possible, and bind it.
            //FAR less jank than using MC's resource system.
            //We try a few times here since sources can do dumb things.
            int tryCount = 0;
            String errorString = null;
            do {
                try {
                    URL urlObject = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
                    try {
                        connection.connect();
                        String contentType = connection.getContentType();
                        String[] typeParams = contentType.split("/");
                        if (typeParams[0].equals("text")) {
                            errorString = "ERROR: Found only text at the URL.  This is not a direct image link, or you don't have permission to view this image (hosted behind a login).";
                        } else {
                            Iterator<ImageReader> iterator = ImageIO.getImageReadersByFormatName(typeParams[1]);
                            if (iterator.hasNext()) {
                                ImageReader reader = iterator.next();
                                if (typeParams[1].equals("gif")) {
                                    ImageInputStream stream = ImageIO.createImageInputStream(connection.getInputStream());
                                    reader.setInput(stream);
                                    ParsedGIF gif = GIFParser.parseGIF(reader);
                                    if (gif != null) {
                                        if (InterfaceManager.renderingInterface.bindURLGIF(urlString, gif)) {
                                            downloadedTextures.add(urlString);
                                            downloadingTextures.remove(urlString);
                                            return;
                                        } else {
                                            errorString = "ERROR: Could not parse GIF due to an internal MC-system interface error.  Contact the mod author!";
                                        }
                                    } else {
                                        errorString = "ERROR: Could not parse GIF due to no frames being present.  Is this a real direct link or a fake one?";
                                    }
                                } else {
                                    if (InterfaceManager.renderingInterface.bindURLTexture(urlString, connection.getInputStream())) {
                                        downloadedTextures.add(urlString);
                                        downloadingTextures.remove(urlString);
                                        return;
                                    } else {
                                        errorString = "ERROR: Got a correct image type, but was missing data for the image?  Likely partial data sent by the server source, try again later.";
                                    }
                                }
                            } else {
                                errorString = "ERROR: Invalid content type found.  Found:" + contentType + ", but the only valid types are: ";
                                for (String imageSuffix : ImageIO.getReaderFileSuffixes()) {
                                    errorString += ("image/" + imageSuffix + ", ");
                                }
                            }
                        }
                    } catch (Exception e) {
                        errorString = "ERROR: Could not parse images.  Error was: " + e.getMessage();
                    }
                } catch (Exception e) {
                    errorString = "ERROR: Could not open URL for processing.  Error was: " + e.getMessage();
                }
            } while (++tryCount < 10);

            //Set missing texture if we failed to get anything.
            InterfaceManager.renderingInterface.bindURLTexture(urlString, null);
            erroredTextures.put(urlString, errorString);
            downloadingTextures.remove(urlString);
            downloadedTextures.add(urlString);
        }
    }
}
