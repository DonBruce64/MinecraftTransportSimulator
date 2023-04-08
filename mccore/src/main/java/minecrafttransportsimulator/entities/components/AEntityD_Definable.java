package minecrafttransportsimulator.entities.components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONCondition;
import minecrafttransportsimulator.jsondefs.JSONConditionGroup;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONRendering.ModelType;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONValueModifier;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.DurationDelayClock;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderableModelObject;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions.
 *
 * @author don_bruce
 */
public abstract class AEntityD_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityC_Renderable {
    private static final Random random = new Random();

    /**
     * The pack definition for this entity.  May contain extra sections if the super-classes
     * have them in their respective JSONs.
     */
    public final JSONDefinition definition;

    /**
     * The current sub-definition for this entity.
     */
    public JSONSubDefinition subDefinition;

    /**
     * Variable for saving animation definition initialized state.  Is set true on the first tick, but may be set false afterwards to re-initialize animation definitions.
     */
    public boolean animationsInitialized;

    /**
     * Map containing text lines for saved text provided by this entity.
     **/
    public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<>();

    /**
     * Map of variables.  These are generic and can be interfaced with in the JSON.  Some names are hard-coded to specific variables.Used for animations/physics.
     **/
    protected final Map<String, Double> variables = new HashMap<>();

    private final Map<JSONConditionGroup, Long> conditionGroupTrueTick = new HashMap<>();
    private final Map<JSONConditionGroup, Long> conditionGroupFalseTick = new HashMap<>();
    private final Set<JSONSound> activeSounds = new HashSet<>();
    private final Set<JSONParticle> activeParticles = new HashSet<>();
    private final Map<JSONVariableModifier, VariableModifierSwitchbox> variableModiferSwitchboxes = new LinkedHashMap<>();

    /**
     * Maps animated (model) object names to their JSON bits for this entity.  Used for model lookups as the same model might be used on multiple JSONs,
     * and iterating through the entire rendering section of the JSON is time-consuming.
     **/
    public final Map<String, JSONAnimatedObject> animatedObjectDefinitions = new HashMap<>();

    /**
     * Maps animated (model) object names to their switchboxes.  This is created from the JSON definition as each entity has their own switchbox.
     **/
    public final Map<String, AnimationSwitchbox> animatedObjectSwitchboxes = new HashMap<>();

    /**
     * Maps cameras to their respective switchboxes.
     **/
    public final Map<JSONCameraObject, AnimationSwitchbox> cameraSwitchboxes = new LinkedHashMap<>();

    /**
     * Maps light objects  to their current states.  This is updated every frame prior to rendering.
     **/
    public final Map<String, LightState> lightStates = new HashMap<>();

    /**
     * Object lists for models parsed in for this class.  Maps are keyed by the model name.
     **/
    protected static final Map<String, List<RenderableModelObject>> objectLists = new HashMap<>();

    /**
     * Cached item to prevent pack lookups each item request.  May not be used if this is extended for other mods.
     **/
    private AItemPack<JSONDefinition> cachedItem;

    //Radar lists.  Only updated once a tick.  Created when first requested via animations.
    private List<EntityVehicleF_Physics> aircraftOnRadar;
    private List<EntityVehicleF_Physics> groundersOnRadar;
    private int radarRequestCooldown;
    private Comparator<AEntityB_Existing> entityComparator;

    /**
     * Constructor for synced entities
     **/
    public AEntityD_Definable(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
        String subName = data.getString("subName");
        AItemSubTyped<JSONDefinition> item = PackParser.getItem(data.getString("packID"), data.getString("systemName"), subName);
        this.definition = item != null ? item.definition : generateDefaultDefinition();
        updateSubDefinition(subName);

        //Load text.
        if (definition.rendering != null && definition.rendering.textObjects != null) {
            for (int i = 0; i < definition.rendering.textObjects.size(); ++i) {
                JSONText textDef = definition.rendering.textObjects.get(i);
                text.put(textDef, newlyCreated ? textDef.defaultText : data.getString("textLine" + i));
            }
        }

        //Load variables.
        for (String variableName : data.getStrings("variables")) {
            variables.put(variableName, data.getDouble(variableName));
        }
        if (newlyCreated && definition.initialVariables != null) {
            for (String variable : definition.initialVariables) {
                variables.put(variable, 1D);
            }
        }
    }

    /**
     * Constructor for un-synced entities.  Allows for specification of position/motion/angles.
     **/
    public AEntityD_Definable(AWrapperWorld world, Point3D position, Point3D motion, Point3D angles, AItemSubTyped<JSONDefinition> creatingItem) {
        super(world, position, motion, angles);
        this.definition = creatingItem.definition;
        updateSubDefinition(creatingItem.subDefinition.subName);
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("EntityD_Level", true);
        if (!animationsInitialized) {
            initializeAnimations();
            animationsInitialized = true;
        }
        //Only update radar once a second, and only if we requested it via variables.
        if (radarRequestCooldown > 0 && ticksExisted % 20 == 0) {
            if (entityComparator == null) {
                entityComparator = new Comparator<AEntityB_Existing>() {
                    @Override
                    public int compare(AEntityB_Existing o1, AEntityB_Existing o2) {
                        return position.isFirstCloserThanSecond(o1.position, o2.position) ? -1 : 1;
                    }

                };
            }

            Collection<EntityVehicleF_Physics> allVehicles = world.getEntitiesOfType(EntityVehicleF_Physics.class);
            if (aircraftOnRadar == null) {
                aircraftOnRadar = new ArrayList<EntityVehicleF_Physics>();
            } else {
                aircraftOnRadar.clear();
            }
            if (groundersOnRadar == null) {
                groundersOnRadar = new ArrayList<EntityVehicleF_Physics>();
            } else {
                groundersOnRadar.clear();
            }
            Point3D searchVector = new Point3D();
            Point3D LOSVector = new Point3D();
            for (EntityVehicleF_Physics vehicle : allVehicles) {
                searchVector.set(0, 0, definition.general.radarRange).rotate(orientation);
                LOSVector.set(vehicle.position).subtract(position).normalize();
                double coneAngle = definition.general.radarWidth;
                double angle = Math.abs(Math.toDegrees(Math.acos(searchVector.normalize().dotProduct(LOSVector, false))));
                if (!vehicle.outOfHealth && vehicle != this && (angle < coneAngle && vehicle.position.isDistanceToCloserThan(position, definition.general.radarRange))) {
                    if (vehicle.definition.motorized.isAircraft) {
                        aircraftOnRadar.add(vehicle);
                    } else {
                        groundersOnRadar.add(vehicle);
                    }
                }
            }
            aircraftOnRadar.sort(entityComparator);
            groundersOnRadar.sort(entityComparator);
        }
        world.endProfiling();
    }

    /**
     * Called to perform supplemental update logic on this entity.  This is called after the main {@link #update()}
     * loop, and is used to do updates that require the new state to be ready.  At this point, all "prior" values
     * and current values will be set to their current states.
     */
    public void doPostUpdateLogic() {
        //Update value-based text.  Only do this on clients as servers won't render this text.
        if (world.isClient() && !text.isEmpty()) {
            for (Entry<JSONText, String> textEntry : text.entrySet()) {
                JSONText textDef = textEntry.getKey();
                if (textDef.variableName != null) {
                    textEntry.setValue(getAnimatedTextVariableValue(textDef, 0));
                }
            }
        }
    }

    /**
     * Updates the subDefinition to match the one passed-in.  Used for paint guns to change the sub-def,
     * but should also be called on initial setting to ensure other state-based operations are performed.
     */
    public void updateSubDefinition(String newSubDefName) {
        for (JSONSubDefinition testSubDef : definition.definitions) {
            if (testSubDef.subName.equals(newSubDefName)) {
                //Remove existing constants, if we have them, then add them, if we have them.
                if (subDefinition != null && subDefinition.constants != null) {
                    variables.keySet().removeAll(subDefinition.constants);
                }
                if (testSubDef.constants != null) {
                    testSubDef.constants.forEach(var -> variables.put(var, 1D));
                }
                subDefinition = testSubDef;
                cachedItem = null;
                return;
            }
        }
        throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + newSubDefName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    }

    /**
     * Called the first update tick after this entity is first constructed, and when the definition on it is reset via hotloading.
     * This should create (and reset) all JSON clocks and other static objects that depend on the definition.
     */
    protected void initializeAnimations() {
        //Update subdef, in case this was modified.
        updateSubDefinition(subDefinition.subName);

        if (definition.rendering.sounds != null) {
            //Stop all sounds, in case we orphan one due to removal of its def.
            sounds.forEach(sound -> sound.stopSound = true);
            sounds.clear();
            activeSounds.clear();

            //Add all non-looping sounds as active.  This makes us not trigger them on first spawn.
            //Looping sounds we will need to start since they should be running.
            //We also need to set the true and false tick delays to their offsets, so those don't cause a trigger.
            for (JSONSound soundDef : definition.rendering.sounds) {
                if (!soundDef.looping) {
                    activeSounds.add(soundDef);
                    if (soundDef.activeConditions.onDelay != 0) {
                        conditionGroupTrueTick.put(soundDef.activeConditions, ticksExisted - soundDef.activeConditions.onDelay);
                    }
                    if (soundDef.activeConditions.offDelay != 0) {
                        conditionGroupFalseTick.put(soundDef.activeConditions, ticksExisted - soundDef.activeConditions.offDelay);
                    }
                }
            }
        }

        if (definition.rendering.lightObjects != null) {
            lightStates.clear();
            for (JSONLight lightDef : definition.rendering.lightObjects) {
                lightStates.put(lightDef.objectName, new LightState(lightDef));
            }
        }

        activeParticles.clear();

        if (definition.rendering.animatedObjects != null) {
            animatedObjectDefinitions.clear();
            animatedObjectSwitchboxes.clear();
            for (JSONAnimatedObject animatedDef : definition.rendering.animatedObjects) {
                animatedObjectDefinitions.put(animatedDef.objectName, animatedDef);
                if (animatedDef.animations != null) {
                    animatedObjectSwitchboxes.put(animatedDef.objectName, new AnimationSwitchbox(this, animatedDef.animations, animatedDef.applyAfter));
                }
            }
        }

        if (definition.rendering.cameraObjects != null) {
            cameraSwitchboxes.clear();
            for (JSONCameraObject cameraDef : definition.rendering.cameraObjects) {
                if (cameraDef.animations != null) {
                    cameraSwitchboxes.put(cameraDef, new AnimationSwitchbox(this, cameraDef.animations, null));
                }
            }
        }

        //Store text data if we have it, then reset it.
        if (definition.rendering.textObjects != null) {
            List<String> oldTextValues = new ArrayList<>(text.values());
            text.clear();

            for (int i = 0; i < definition.rendering.textObjects.size(); ++i) {
                if (i < oldTextValues.size()) {
                    text.put(definition.rendering.textObjects.get(i), oldTextValues.get(i));
                } else {
                    text.put(definition.rendering.textObjects.get(i), definition.rendering.textObjects.get(i).defaultText);
                }
            }
        }

        //Add variable modifiers.
        if (definition.variableModifiers != null) {
            variableModiferSwitchboxes.clear();
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                if (modifier.animations != null) {
                    variableModiferSwitchboxes.put(modifier, new VariableModifierSwitchbox(this, modifier.animations));
                }
            }

        }

        //Add constants.
        if (definition.constantValues != null) {
            variables.putAll(definition.constantValues);
        }
    }

    @Override
    public void remove() {
        if (isValid) {
            super.remove();
            //Stop all playing sounds.
            for (SoundInstance sound : sounds) {
                sound.stopSound = true;
            }
        }
    }

    /**
     * Returns the current item for this entity.  This is not a static value to allow for overriding by packs.
     */
    @SuppressWarnings("unchecked")
    public <ItemInstance extends AItemPack<JSONDefinition>> ItemInstance getItem() {
        if (cachedItem == null) {
            cachedItem = PackParser.getItem(definition.packID, definition.systemName, subDefinition.subName);
        }
        return (ItemInstance) cachedItem;
    }

    /**
     * Populates the passed-in list with item stacks that will drop when this entity is broken.
     * This is different than what is used for middle-clicking, as that will
     * return a stack that can re-create this entity, whereas drops may or may not allow for this.
     * An example is a vehicle that is broken in a crash versus picked up via a wrench.
     */
    public void addDropsToList(List<IWrapperItemStack> drops) {
        AItemPack<JSONDefinition> packItem = getItem();
        if (packItem != null) {
            drops.add(packItem.getNewStack(save(InterfaceManager.coreInterface.getNewNBTWrapper())));
        }
    }

    /**
     * Generates the default definition for this entity. Used if the item can't be found.
     * This allows for internally-definable entities.
     */
    public JSONDefinition generateDefaultDefinition() {
        throw new IllegalArgumentException("Was asked to auto-generate a definition on an entity with one not defined.  This is NOT allowed.  The entity must be missing its item.  Perhaps a pack was removed with this entity still in the world?");
    }

    /**
     * Returns the texture that should be bound to this entity for the passed-in object from the model.
     * This may change between render passes, but only ONE texture may be used for any given object render
     * operation!  By default this returns the JSON-defined texture, though the model parser may override this.
     */
    public String getTexture() {
        return definition.getTextureLocation(subDefinition);
    }

    /**
     * Returns true if this entity is lit up, and text should be rendered lit.
     * Note that what text is lit is dependent on the text's definition, so just
     * because text could be lit, does not mean it will be lit if the pack
     * author doesn't want it to be.
     */
    public boolean renderTextLit() {
        return ConfigSystem.client.renderingSettings.brightLights.value;
    }

    /**
     * Returns the color for the text on this entity.  This takes into account the passed-in index.
     * If a color exists at the index, it is returned.  If not, then the passed-in color is returned.
     */
    public ColorRGB getTextColor(int index, ColorRGB defaultColor) {
        if (index != 0) {
            if (subDefinition.secondaryTextColors != null && subDefinition.secondaryTextColors.size() >= index) {
                return subDefinition.secondaryTextColors.get(index - 1);
            } else {
                return defaultColor;
            }
        } else {
            return defaultColor;
        }
    }

    /**
     * Called to update the text on this entity.  Variable is a map with the key as a field name,
     * and the value as the value of that field.  Normally just sets the text to the passed-in values,
     * but may do supplemental logic if desired.
     */
    public void updateText(LinkedHashMap<String, String> textLines) {
        for (Entry<JSONText, String> textEntry : text.entrySet()) {
            String newLine = textLines.get(textEntry.getKey().fieldName);
            if (newLine != null) {
                textEntry.setValue(newLine);
            }
        }
    }

    /**
     * Spawns particles for this entity.  This is called after every render frame, so
     * watch your methods to prevent spam.  Note that this method is not called if the
     * game is paused, as particles are assumed to only be spawned during normal entity
     * updates.
     */
    public void spawnParticles(float partialTicks) {
        //Check all particle defs and update the existing particles accordingly.
        if (definition.rendering.particles != null) {
            for (JSONParticle particleDef : definition.rendering.particles) {
                boolean shouldSpawn = checkConditions(particleDef.activeConditions, partialTicks);

                //Do supplemental checks to make sure we didn't already spawn this particle.
                if (!particleDef.spawnEveryTick) {
                    if (shouldSpawn) {
                        if (activeParticles.contains(particleDef)) {
                            shouldSpawn = false;
                        } else {
                            activeParticles.add(particleDef);
                        }
                    } else {
                        activeParticles.remove(particleDef);
                    }
                }

                //If we truly should spawn, do so now.
                if (shouldSpawn) {
                    for (int i = 0; i < particleDef.quantity; ++i) {
                        world.addEntity(new EntityParticle(this, particleDef));
                    }
                }
            }
        }
    }

    /**
     * Updates the light brightness values contained in {@link #lightBrightnessValues}.  This is done
     * every frame for all light definitions to prevent excess calculations caused by multiple
     * lighting components for the light re-calculating the same value multiple times a frame.
     * An example of this is a light with a bean and flare component.
     */
    public void updateLightBrightness(float partialTicks) {
        for (LightState lightState : lightStates.values()) {
            lightState.brightness = 1.0F;
            if (lightState.definition.redColorValueModifiers != null) {
                lightState.color.red = (float) calculateModifiers(lightState.definition.redColorValueModifiers, 0, partialTicks);
            }
            if (lightState.definition.greenColorValueModifiers != null) {
                lightState.color.green = (float) calculateModifiers(lightState.definition.greenColorValueModifiers, 0, partialTicks);
            }
            if (lightState.definition.blueColorValueModifiers != null) {
                lightState.color.blue = (float) calculateModifiers(lightState.definition.blueColorValueModifiers, 0, partialTicks);
            }

            /*
            float lightBrightness = 1;
            ColorRGB lightColor = null;
            LightSwitchbox switchbox = lightBrightnessSwitchboxes.get(lightDef);
            if (switchbox != null) {
                if (!switchbox.runLight(partialTicks)) {
                    lightBrightness = 0;
                } else if (switchbox.definedBrightness) {
                    lightBrightness = switchbox.brightness;
                }
                if (lightBrightness < 0) {
                    lightBrightness = 0;
                }
                lightBrightnessValues.put(lightDef, lightBrightness);
                lightColor = switchbox.color;
            } else {
                lightBrightnessValues.put(lightDef, 1.0F);
            }
            
            
            if (lightState != null) {
                lightLevel = lightState.brightness;
                if (lightState.definition.isElectric && entity instanceof EntityVehicleF_Physics) {
                    //Light start dimming at 10V, then go dark at 3V.
                    double electricPower = ((EntityVehicleF_Physics) entity).electricPower;
                    if (electricPower < 3) {
                        lightLevel = 0;
                    } else if (electricPower < 10) {
                        lightLevel *= (electricPower - 3) / 7D;
                    }
                }
            }
            
            //Set color level.
            if (lightColor != null) {
                lightColorValues.put(lightDef, lightColor);
            } else if (lightDef.color != null) {
                lightColorValues.put(lightDef, lightDef.color);
            } else {
                lightColorValues.put(lightDef, ColorRGB.WHITE);
            }*/
        }
    }

    /**
     * Custom light switchbox class.
     */
    private static class LightSwitchbox extends AnimationSwitchbox {
        private boolean definedBrightness = false;
        private float brightness = 0;
        private ColorRGB color = null;

        private LightSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations) {
            super(entity, animations, null);
        }

        public boolean runLight(float partialTicks) {
            definedBrightness = false;
            brightness = 0;
            color = null;
            return runSwitchbox(partialTicks, true);
        }

        @Override
        public void runTranslation(DurationDelayClock clock, float partialTicks) {
            definedBrightness = true;
            if (clock.animation.axis.x != 0) {
                brightness *= entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks);
            } else if (clock.animation.axis.y != 0) {
                brightness += entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
            } else {
                brightness = (float) (entity.getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks));
            }
        }

        @Override
        public void runRotation(DurationDelayClock clock, float partialTicks) {
            double colorFactor = entity.getAnimatedVariableValue(clock, 1.0, -clock.animation.offset, partialTicks);
            double colorX;
            double colorY;
            double colorZ;
            if (color == null) {
                colorX = clock.animation.axis.x * colorFactor + clock.animation.offset;
                colorY = clock.animation.axis.y * colorFactor + clock.animation.offset;
                colorZ = clock.animation.axis.z * colorFactor + clock.animation.offset;
            } else {
                colorX = clock.animation.axis.x * colorFactor + clock.animation.offset + color.red;
                colorY = clock.animation.axis.y * colorFactor + clock.animation.offset + color.green;
                colorZ = clock.animation.axis.z * colorFactor + clock.animation.offset + color.blue;
            }
            if (colorX < 0)
                colorX = 0;
            if (colorY < 0)
                colorY = 0;
            if (colorZ < 0)
                colorZ = 0;
            if (colorX > 1)
                colorX = 1;
            if (colorY > 1)
                colorY = 1;
            if (colorZ > 1)
                colorZ = 1;
            color = new ColorRGB((float) colorX, (float) colorY, (float) colorZ, false);
        }
    }

    @Override
    public void updateSounds(float partialTicks) {
        super.updateSounds(partialTicks);

        //Check all sound defs and update the existing sounds accordingly.
        if (definition.rendering.sounds != null) {
            for (JSONSound soundDef : definition.rendering.sounds) {
                //Only check sounds on the proper tick-timeline for them.
                if (soundDef.canPlayOnPartialTicks ^ partialTicks == 0) {
                    //Check if we are in the right view to play.
                    AEntityB_Existing entityRiding = InterfaceManager.clientInterface.getClientPlayer().getEntityRiding();
                    AEntityF_Multipart<?> multipartTopLevel = entityRiding instanceof APart ? ((APart) entityRiding).masterEntity : (entityRiding instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) entityRiding : null);
                    boolean playerRidingThisEntity = multipartTopLevel != null && (multipartTopLevel.equals(this) || multipartTopLevel.allParts.contains(this));
                    boolean hasOpenTop = multipartTopLevel instanceof EntityVehicleF_Physics && ((EntityVehicleF_Physics) multipartTopLevel).definition.motorized.hasOpenTop;
                    boolean shouldSoundPlay = hasOpenTop ? true : (playerRidingThisEntity && InterfaceManager.clientInterface.inFirstPerson() && !CameraSystem.runningCustomCameras) ? !soundDef.isExterior : !soundDef.isInterior;

                    //Next, check the distance.
                    double distance = 0;
                    if (shouldSoundPlay) {
                        distance = position.distanceTo(InterfaceManager.clientInterface.getClientPlayer().getPosition());
                        if (soundDef.maxDistance != soundDef.minDistance) {
                            shouldSoundPlay = distance < soundDef.maxDistance && distance > soundDef.minDistance;
                        } else {
                            shouldSoundPlay = distance < SoundInstance.DEFAULT_MAX_DISTANCE;
                        }
                    }

                    //Next, check conditions.
                    if (shouldSoundPlay) {
                        shouldSoundPlay = checkConditions(soundDef.activeConditions, partialTicks);
                    }

                    if (shouldSoundPlay) {
                        //Need to start a new sound, do so now.
                        SoundInstance sound = null;
                        if (soundDef.forceSound || !activeSounds.contains(soundDef)) {
                            sound = new SoundInstance(this, soundDef);
                            if (!InterfaceManager.soundInterface.playQuickSound(sound)) {
                                //Sound failed to start, go to next sound instead of messing with this one.
                                continue;
                            }
                            activeSounds.add(soundDef);
                        } else {
                            //Get existing sound.
                            for (SoundInstance testSound : sounds) {
                                if (testSound.soundName.equals(soundDef.name)) {
                                    sound = testSound;
                                    break;
                                }
                            }
                        }

                        //If we have a valid sound running, do logic on it.
                        if (sound != null) {
                            //Adjust volume for modifiers, if they exist.
                            sound.volume = soundDef.volumeValueModifiers != null ? (float) calculateModifiers(soundDef.volumeValueModifiers, 0, partialTicks) : 1.0F;
                            if (sound.volume < 0) {
                                sound.volume = 0;
                            } else if (sound.volume > 1) {
                                sound.volume = 1;
                            }

                            //Adjust volume based on distance.
                            if (soundDef.minDistanceVolume == 0 && soundDef.middleDistanceVolume == 0 && soundDef.maxDistanceVolume == 0) {
                                //Default sound distance.
                                double maxDistance = soundDef.maxDistance != 0 ? soundDef.maxDistance : SoundInstance.DEFAULT_MAX_DISTANCE;
                                sound.volume *= (maxDistance - distance) / (maxDistance);
                            } else if (soundDef.middleDistance != 0) {
                                //Middle interpolation.
                                if (distance < soundDef.middleDistance) {
                                    sound.volume *= (float) (soundDef.minDistanceVolume + (distance - soundDef.minDistance) / (soundDef.middleDistance - soundDef.minDistance) * (soundDef.middleDistanceVolume - soundDef.minDistanceVolume));
                                } else {
                                    sound.volume *= (float) (soundDef.middleDistanceVolume + (distance - soundDef.middleDistance) / (soundDef.maxDistance - soundDef.middleDistance) * (soundDef.maxDistanceVolume - soundDef.middleDistanceVolume));
                                }
                            } else {
                                //Min/max.
                                sound.volume *= (float) (soundDef.minDistanceVolume + (distance - soundDef.minDistance) / (soundDef.maxDistance - soundDef.minDistance) * (soundDef.maxDistanceVolume - soundDef.minDistanceVolume));
                            }

                            //If the player is in a closed-top vehicle that isn't this one, dampen the sound
                            //Unless it's a radio, in which case don't do so.
                            if (!playerRidingThisEntity && sound.radio == null && !hasOpenTop && InterfaceManager.clientInterface.inFirstPerson() && !CameraSystem.runningCustomCameras) {
                                sound.volume *= 0.5F;
                            }

                            //Adjust pitch.
                            sound.pitch = soundDef.pitchValueModifiers != null ? (float) calculateModifiers(soundDef.pitchValueModifiers, 0, partialTicks) : 1.0F;
                            if (sound.pitch < 0) {
                                sound.pitch = 0;
                            }
                        }
                    } else {
                        if (activeSounds.remove(soundDef)) {
                            //If sound is playing, stop it.
                            //Non-looping sounds are trigger-based and will stop on their own.
                            for (SoundInstance sound : sounds) {
                                if (sound.soundName.equals(soundDef.name)) {
                                    if (soundDef.looping) {
                                        sound.stopSound = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the status of the conditions.
     */
    public final boolean checkConditions(JSONConditionGroup conditionGroup, float partialTicks) {
        if (conditionGroup != null) {
            //Get result.
            boolean result = true;
            for (JSONCondition condition : conditionGroup.conditions) {
                if (!checkCondition(condition, partialTicks)) {
                    result = false;
                }
            }

            //Apply condition delays.
            if (conditionGroup.onDelay != 0) {
                if (result) {
                    Long tickOn = conditionGroupTrueTick.get(conditionGroup);
                    if (tickOn == null) {
                        tickOn = ticksExisted;
                        conditionGroupTrueTick.put(conditionGroup, ticksExisted);
                    }
                    if (ticksExisted - tickOn < conditionGroup.onDelay) {
                        result = false;
                    }
                } else {
                    conditionGroupTrueTick.remove(conditionGroup);
                }
            } else if (conditionGroup.offDelay != 0) {
                if (result) {
                    conditionGroupFalseTick.remove(conditionGroup);
                } else {
                    Long tickOff = conditionGroupFalseTick.get(conditionGroup);
                    if (tickOff == null) {
                        tickOff = ticksExisted;
                        conditionGroupFalseTick.put(conditionGroup, ticksExisted);
                    }
                    if (ticksExisted - tickOff < conditionGroup.offDelay) {
                        result = true;
                    }
                }
            }
            return result;
        }
        return true;
    }

    private boolean checkCondition(JSONCondition condition, float partialTicks) {
        boolean result = false;
        switch (condition.type) {
            case ACTIVE: {
                result = getCleanRawVariableValue(condition.input, partialTicks) > 0;
                break;
            }
            case MATCH: {
                result = getCleanRawVariableValue(condition.input, partialTicks) == condition.parameter1;
                break;
            }
            case MATCH_VAR: {
                result = getCleanRawVariableValue(condition.input, partialTicks) == getCleanRawVariableValue(condition.variable1, partialTicks);
                break;
            }
            case GREATER: {
                result = getCleanRawVariableValue(condition.input, partialTicks) > condition.parameter1;
                break;
            }
            case GREATER_VAR: {
                result = getCleanRawVariableValue(condition.input, partialTicks) > getCleanRawVariableValue(condition.variable1, partialTicks);
                break;
            }
            case LESS: {
                result = getCleanRawVariableValue(condition.input, partialTicks) < condition.parameter1;
                break;
            }
            case LESS_VAR: {
                result = getCleanRawVariableValue(condition.input, partialTicks) < getCleanRawVariableValue(condition.variable1, partialTicks);
                break;
            }
            case BOUNDS: {
                double value = getCleanRawVariableValue(condition.input, partialTicks);
                result = value >= condition.parameter1 && value <= condition.parameter2;
                break;
            }
            case BOUNDS_VAR: {
                double value = getCleanRawVariableValue(condition.input, partialTicks);
                result = value >= getCleanRawVariableValue(condition.variable1, partialTicks) && value <= getCleanRawVariableValue(condition.variable2, partialTicks);
                break;
            }
            case CONDITIONS: {
                for (JSONCondition condition2 : condition.conditions) {
                    if (checkCondition(condition2, partialTicks)) {
                        result = true;
                        break;
                    }
                }
                break;
            }
        }
        return condition.invert ? !result : result;
    }

    /**
     * Returns the calculation of the modifier.
     */
    public double calculateModifiers(List<JSONValueModifier> modifiers, double rollingValue, float partialTicks) {
        for (JSONValueModifier modifier : modifiers) {
            if(modifier.type == JSONValueModifier.Type.CONDITIONS){
                if (checkConditions(modifier.conditions, partialTicks)) {
                    if(modifier.trueCode != null) {
                        rollingValue = calculateModifiers(modifier.trueCode, rollingValue, partialTicks);
                    }
                }else {
                    if(modifier.falseCode != null) {
                        rollingValue = calculateModifiers(modifier.falseCode, rollingValue, partialTicks);
                    }
                }
            }else {
                switch(modifier.type) {
                    case SET : {
                        rollingValue = modifier.parameter1;
                        break;
                    }
                    case SET_VAR: {
                        if (modifier.factor != 0) {
                            rollingValue = getCleanRawVariableValue(modifier.input, partialTicks) * modifier.factor;
                        } else {
                            rollingValue = getCleanRawVariableValue(modifier.input, partialTicks);
                        }
                        break;
                    }
                    case ADD : {
                        rollingValue += modifier.parameter1;
                        break;
                    }
                    case ADD_VAR: {
                        if (modifier.factor != 0) {
                            rollingValue += getCleanRawVariableValue(modifier.input, partialTicks) * modifier.factor;
                        } else {
                            rollingValue += getCleanRawVariableValue(modifier.input, partialTicks);
                        }
                        break;
                    }
                    case MULTIPLY : {
                        rollingValue *= modifier.parameter1;
                        break;
                    }
                    case MULTIPLY_VAR: {
                        if (modifier.factor != 0) {
                            rollingValue *= getCleanRawVariableValue(modifier.input, partialTicks) * modifier.factor;
                        } else {
                            rollingValue *= getCleanRawVariableValue(modifier.input, partialTicks);
                        }
                        break;
                    }
                    case LINEAR : {
                        rollingValue += getCleanRawVariableValue(modifier.input, partialTicks) * modifier.parameter1 + modifier.parameter2;
                        break;
                    }
                    case PARABOLIC: {
                        rollingValue += modifier.parameter1 * Math.pow(getCleanRawVariableValue(modifier.input, partialTicks) * modifier.parameter2 - modifier.parameter3, 2) + modifier.parameter4;
                        break;
                    }
                    case CLAMP: {
                        if (rollingValue < modifier.parameter1) {
                            rollingValue = modifier.parameter1;
                        } else if (rollingValue > modifier.parameter2) {
                            rollingValue = modifier.parameter2;
                        }
                        break;
                    }
                    case CONDITIONS: //We'll never get this one.
                }
            }
        }
        return rollingValue;
    }

    /**
     * Returns the raw value for the passed-in variable.  If the variable is not present, NaN
     * should be returned (calling functions need to account for this!).
     * This should be extended on all sub-classes for them to provide their own variables.
     * For all cases of this, the sub-classed variables should be checked first.  If none are
     * found, then the super() method should be called to return those as a default.
     */
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("tick"):
                return ticksExisted + partialTicks;
            case ("tick_sin"):
                return Math.sin(Math.toRadians(ticksExisted + partialTicks));
            case ("tick_cos"):
                return Math.cos(Math.toRadians(ticksExisted + partialTicks));
            case ("time"):
                return world.getTime();
            case ("random"):
                return Math.random();
            case ("rain_strength"):
                return (int) world.getRainStrength(position);
            case ("rain_sin"): {
                int rainStrength = (int) world.getRainStrength(position);
                return rainStrength > 0 ? Math.sin(rainStrength * Math.toRadians(360 * (ticksExisted + partialTicks) / 20)) / 2D + 0.5 : 0;
            }
            case ("rain_cos"): {
                int rainStrength = (int) world.getRainStrength(position);
                return rainStrength > 0 ? Math.cos(rainStrength * Math.toRadians(360 * (ticksExisted + partialTicks) / 20)) / 2D + 0.5 : 0;
            }
            case ("light_sunlight"):
                return world.getLightBrightness(position, false);
            case ("light_total"):
                return world.getLightBrightness(position, true);
            case ("terrain_distance"):
                return world.getHeight(position);
            case ("inliquid"):
                return world.isBlockLiquid(position) ? 1 : 0;
            case ("config_simplethrottle"):
                return ConfigSystem.client.controlSettings.simpleThrottle.value ? 1 : 0;
            case ("config_innerwindows"):
                return ConfigSystem.client.renderingSettings.innerWindows.value ? 1 : 0;
        }

        //Check if this is a cycle variable.
        if (variable.endsWith("_cycle")) {
            String[] parsedVariable = variable.split("_");
            int offTime = Integer.parseInt(parsedVariable[0]);
            int onTime = Integer.parseInt(parsedVariable[1]);
            int totalTime = offTime + onTime + Integer.parseInt(parsedVariable[2]);
            long timeInCycle = ticksExisted % totalTime;
            return timeInCycle > offTime && timeInCycle - offTime < onTime ? 1 : 0;
        }

        //Check if this is a text_x_ispresent variable.
        if (variable.startsWith("text_") && variable.endsWith("_present")) {
            if (definition.rendering != null && definition.rendering.textObjects != null) {
                int textIndex = Integer.parseInt(variable.substring("text_".length(), variable.length() - "_present".length())) - 1;
                if (definition.rendering.textObjects.size() > textIndex) {
                    return !text.get(definition.rendering.textObjects.get(textIndex)).isEmpty() ? 1 : 0;
                }
            }
            return 0;
        }

        //Check if this is a radar variable.
        if (variable.startsWith("radar_")) {
            if (radarRequestCooldown != 0 && entityComparator != null) {
                String[] parsedVariable = variable.split("_");
                List<? extends AEntityB_Existing> radarList;
                switch (parsedVariable[1]) {
                    case ("aircraft"):
                        radarList = aircraftOnRadar;
                        break;
                    case ("ground"):
                        radarList = groundersOnRadar;
                        break;
                    default:
                        //Can't continue, as we expect non-null.
                        return 0;
                }
                int index = Integer.parseInt(parsedVariable[2]);
                if (index < radarList.size()) {
                    AEntityB_Existing contact = radarList.get(index);
                    switch (parsedVariable[3]) {
                        case ("distance"):
                            return contact.position.distanceTo(position);
                        case ("direction"):
                            double delta = Math.toDegrees(Math.atan2(-contact.position.z + position.z, -contact.position.x + position.x)) + 90 + orientation.angles.y;
                        while (delta < -180)
                            delta += 360;
                        while (delta > 180)
                            delta -= 360;
                        return delta;
                        case ("speed"):
                            return contact.velocity;
                        case ("altitude"):
                            return contact.position.y;
                        case ("angle"):
                            return -Math.toDegrees(Math.atan2(-contact.position.y + position.y,Math.hypot(-contact.position.z + position.z,-contact.position.x + position.x))) + orientation.angles.x;
                    }
                }
            }
            radarRequestCooldown = 40;
            return 0;
        }

        //Check if this is a generic variable.  This contains lights in most cases.
        Double variableValue = variables.get(variable);
        if (variableValue != null) {
            return variableValue;
        }

        //Didn't find a variable.  Return NaN.
        return Double.NaN;
    }

    /**
     * Like {@link #getRawVariableValue(String, float)}, but returns 0 if not found
     * rather than NaN.  This is designed for getting variable values without animations.
     */
    public final double getCleanRawVariableValue(String variable, float partialTicks) {
        double value = getRawVariableValue(variable, partialTicks);
        return Double.isNaN(value) ? 0 : value;
    }

    /**
     * Similar to {@link #getRawVariableValue(String, float)}, but returns
     * a String for text-based parameters rather than a double.  If no match
     * is found, return null.  Otherwise, return the string.
     */
    public String getRawTextVariableValue(JSONText textDef, float partialTicks) {
        return null;
    }

    /**
     * Returns the value for the passed-in variable, subject to the clamping, and duration/delay requested in the
     * animation definition.  The passed-in offset is used to allow for stacking animations, and should be 0 if
     * this functionality is not required.  Note that the animation offset is applied AFTER the scaling performed by
     * the scale parameter as only the variable value should be scaled, not the offset..
     */
    public final double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, double offset, float partialTicks) {
        double value;
        if (clock.animation.variable.startsWith("!")) {
            value = getCleanRawVariableValue(clock.animation.variable.substring(1), partialTicks);
            value = value == 0 ? 1 : 0;
        } else {
            value = getRawVariableValue(clock.animation.variable, partialTicks);
            if (Double.isNaN(value)) {
                value = 0;
            }
        }
        if (!clock.isUseful) {
            return clampAndScale(value, clock.animation, scaleFactor, offset);
        } else {
            return clampAndScale(clock.getFactoredState(this, value, partialTicks), clock.animation, scaleFactor, offset);
        }
    }

    /**
     * Short-hand version of {@link #getAnimatedVariableValue(DurationDelayClock, double, double, float)}
     * with an offset of 0.0.
     */
    public final double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, float partialTicks) {
        return getAnimatedVariableValue(clock, scaleFactor, 0.0, partialTicks);
    }

    /**
     * Helper method to clamp and scale the passed-in variable value based on the passed-in animation,
     * returning it in the proper form.
     */
    private static double clampAndScale(double value, JSONAnimationDefinition animation, double scaleFactor, double offset) {
        if (animation.axis != null) {
            value = (animation.absolute ? Math.abs(value) : value) * scaleFactor + animation.offset + offset;
            if (animation.clampMin != 0 && value < animation.clampMin) {
                value = animation.clampMin;
            } else if (animation.clampMax != 0 && value > animation.clampMax) {
                value = animation.clampMax;
            }
            return value;
        } else {
            return (animation.absolute ? Math.abs(value) : value) * scaleFactor + animation.offset;
        }
    }

    /**
     * Returns the value for the passed-in variable, subject to the formatting and factoring in the
     * text definition.
     */
    public final String getAnimatedTextVariableValue(JSONText textDef, float partialTicks) {
        //Check text values first, then animated values.
        String value = getRawTextVariableValue(textDef, 0);
        if (value == null) {
            return String.format(textDef.variableFormat, getCleanRawVariableValue(textDef.variableName, 0) * textDef.variableFactor);
        } else {
            return String.format(textDef.variableFormat, value);
        }
    }

    /**
     * Helper method to toggle a variable for this entity.
     */
    public void toggleVariable(String variable) {
        //Try to remove the variable,this requires only one key-search operation, unlike a containsKey followed by a remove.
        if (variables.remove(variable) == null) {
            //No key was in this map prior, so this variable was off, set it on.
            variables.put(variable, 1D);
        }
    }

    /**
     * Helper method to set a variable for this entity.
     */
    public void setVariable(String variable, double value) {
        if (value == 0) {
            //Remove variable from the map so we don't have as many to deal with.
            variables.remove(variable);
        } else {
            variables.put(variable, value);
        }
    }

    /**
     * Helper method to increment a variable for this entity.
     * This will adjust the value between the clamps.  Returns
     * true if the value was changed.
     */
    public boolean incrementVariable(String variable, double incrementValue, double minValue, double maxValue) {
        double currentValue = getVariable(variable);
        double newValue = currentValue + incrementValue;
        if (minValue != 0 || maxValue != 0) {
            if (newValue < minValue) {
                newValue = minValue;
            } else if (newValue > maxValue) {
                newValue = maxValue;
            }
        }
        if (newValue != currentValue) {
            setVariable(variable, newValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper method to get get a variable for this entity.
     */
    public double getVariable(String variable) {
        Double value = variables.get(variable);
        if (value == null) {
            //Don't add the variable to the map, just return 0 here.
            return 0;
        } else {
            return value;
        }
    }

    /**
     * Helper method to check if a variable is non-zero.
     * This is a bit quicker than getting the value due to auto-boxing off the map.
     */
    public boolean isVariableActive(String variable) {
        return variables.containsKey(variable);
    }

    /**
     * Helper method for variable modification.
     */
    protected float adjustVariable(JSONVariableModifier modifier, float currentValue) {
        float modifiedValue = modifier.setValue != 0 ? modifier.setValue : currentValue + modifier.addValue;
        VariableModifierSwitchbox switchbox = variableModiferSwitchboxes.get(modifier);
        if (switchbox != null) {
            switchbox.modifiedValue = modifiedValue;
            if (switchbox.runSwitchbox(0, true)) {
                modifiedValue = switchbox.modifiedValue;
            } else {
                return currentValue;
            }
        }
        if (modifier.minValue != 0 || modifier.maxValue != 0) {
            if (modifiedValue < modifier.minValue) {
                return modifier.minValue;
            } else if (modifiedValue > modifier.maxValue) {
                return modifier.maxValue;
            }
        }
        return modifiedValue;
    }

    /**
     * Custom variable modifier switchbox class.
     */
    private static class VariableModifierSwitchbox extends AnimationSwitchbox {
        private float modifiedValue = 0;

        private VariableModifierSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations) {
            super(entity, animations, null);
        }

        @Override
        public void runTranslation(DurationDelayClock clock, float partialTicks) {
            if (clock.animation.axis.x != 0) {
                modifiedValue *= entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks);
            } else if (clock.animation.axis.y != 0) {
                modifiedValue += entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
            } else {
                modifiedValue = (float) (entity.getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks));
            }
        }
    }

    /**
     * Called to update the variable modifiers for this entity.
     * By default, this will get any variables that {@link #getVariable(String)}
     * returns, but can be extended to do other variables specific to the entity.
     */
    protected void updateVariableModifiers() {
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                setVariable(modifier.variable, adjustVariable(modifier, (float) getVariable(modifier.variable)));
            }
        }
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        //Update internal lighting states.
        world.beginProfiling("LightStateUpdates", true);
        updateLightBrightness(partialTicks);

        //Parse model if it hasn't been already.
        world.beginProfiling("MainModel", false);
        String modelLocation = definition.getModelLocation(subDefinition);
        if (!objectLists.containsKey(modelLocation)) {
            objectLists.put(modelLocation, AModelParser.generateRenderables(this));
        }

        //Render model object individually.
        for (RenderableModelObject modelObject : objectLists.get(modelLocation)) {
            modelObject.render(this, transform, blendingEnabled, partialTicks);
        }

        //Render any static text.
        world.beginProfiling("MainText", false);
        if (!blendingEnabled) {
            for (Entry<JSONText, String> textEntry : text.entrySet()) {
                JSONText textDef = textEntry.getKey();
                if (textDef.attachedTo == null) {
                    RenderText.draw3DText(textEntry.getValue(), this, transform, textDef, false);
                }
            }
        }
        //Handle particles.
        if (!InterfaceManager.clientInterface.isGamePaused() && !blendingEnabled) {
            world.beginProfiling("Particles", false);
            spawnParticles(partialTicks);
        }
        world.endProfiling();
    }

    @Override
    protected boolean disableRendering(float partialTicks) {
        //Don't render if we don't have a model.
        return super.disableRendering(partialTicks) || definition.rendering.modelType.equals(ModelType.NONE);
    }

    /**
     * Called externally to reset all caches for all renders.
     */
    public static void clearObjectCaches(AJSONMultiModelProvider definition) {
        for (JSONSubDefinition subDef : definition.definitions) {
            String modelLocation = definition.getModelLocation(subDef);
            List<RenderableModelObject> resetObjects = objectLists.remove(modelLocation);
            if (resetObjects != null) {
                for (RenderableModelObject modelObject : resetObjects) {
                    modelObject.destroy();
                }
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setString("packID", definition.packID);
        data.setString("systemName", definition.systemName);
        data.setString("subName", subDefinition.subName);
        int lineNumber = 0;
        for (String textLine : text.values()) {
            data.setString("textLine" + lineNumber++, textLine);
        }
        data.setStrings("variables", variables.keySet());
        for (String variableName : variables.keySet()) {
            data.setDouble(variableName, variables.get(variableName));
        }
        return data;
    }

    public static class LightState {
        public final JSONLight definition;
        public float brightness;
        public final ColorRGB color = new ColorRGB();

        public LightState(JSONLight definition) {
            this.definition = definition;
        }
    }

    /**
     * Indicates that this field is a derived value from
     * one of the variables in {@link AEntityD_Definable#variables}.
     * Variables that are derived are parsed from the map every update.
     * To modify them you will need to update their values in the respective
     * variable set via
     * {@link PacketEntityVariableToggle},
     * {@link PacketEntityVariableSet},
     * {@link PacketEntityVariableIncrement}
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
    public @interface DerivedValue {
    }

    /**
     * Indicates that this field is able to be modified via variable modification
     * by the code in {@link AEntityE_Interactable#updateVariableModifiers()},
     * This annotation is only for variables that are NOT derived from states
     * and annotated with {@link DerivedValue}, as those variables can inherently
     * be modified as they are derived from the variable states.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
    public @interface ModifiableValue {
    }

    /**
     * Indicates that this field is a modified version of a field annotated with
     * {@link ModifiableValue}.  This is done to prevent modifying the parsed
     * definition entry that contains the value, which is why it's stored
     * in a new variable that gets aligned every tick before updates.
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
    public @interface ModifiedValue {
    }
}
