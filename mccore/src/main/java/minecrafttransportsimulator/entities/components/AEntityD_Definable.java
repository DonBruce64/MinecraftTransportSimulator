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
import java.util.Set;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAction;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONRendering.ModelType;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityInteractGUI;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.DurationDelayClock;
import minecrafttransportsimulator.rendering.RenderText;
import minecrafttransportsimulator.rendering.RenderableModelObject;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions.
 *
 * @author don_bruce
 */
public abstract class AEntityD_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityC_Renderable {
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
     * Map containing text lines for saved text provided by this entity.
     **/
    public final Map<JSONText, String> text = new HashMap<>();

    /**
     * Map of computed variables.  These are computed using logic and need to be re-created on core entity makeup changes.
     **/
    protected final Map<String, ComputedVariable> computedVariables = new HashMap<>();

    private final List<JSONSound> allSoundDefs = new ArrayList<>();
    private final Map<JSONSound, AnimationSwitchbox> soundActiveSwitchboxes = new HashMap<>();
    private final Set<JSONSound> soundDefFalseLastCheck = new HashSet<>();
    private final Map<JSONSound, SoundSwitchbox> soundVolumeSwitchboxes = new HashMap<>();
    private final Map<JSONSound, SoundSwitchbox> soundPitchSwitchboxes = new HashMap<>();
    private final Map<JSONLight, LightSwitchbox> lightBrightnessSwitchboxes = new HashMap<>();
    private final Map<JSONParticle, AnimationSwitchbox> particleActiveSwitchboxes = new HashMap<>();
    private final Map<JSONParticle, AnimationSwitchbox> particleSpawningSwitchboxes = new HashMap<>();
    private final Map<JSONParticle, Long> lastTickParticleSpawned = new HashMap<>();
    private final Map<JSONParticle, Point3D> lastPositionParticleSpawned = new HashMap<>();
    private final Map<JSONVariableModifier, VariableModifierSwitchbox> variableModiferSwitchboxes = new LinkedHashMap<>();
    private long lastTickParticlesSpawned;
    private float lastPartialTickParticlesSpawned;

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
     * Maps light definitions to their current brightness.  This is updated every frame prior to rendering.
     **/
    public final Map<JSONLight, Float> lightBrightnessValues = new HashMap<>();

    /**
     * Maps light definitions to their current color.  This is updated every frame prior to rendering.
     **/
    public final Map<JSONLight, ColorRGB> lightColorValues = new HashMap<>();

    /**
     * Maps light (model) object names to their definitions.  This is created from the JSON definition to prevent the need to do loops.
     **/
    public final Map<String, JSONLight> lightObjectDefinitions = new HashMap<>();

    /**
     * Object lists for models parsed for this entity.
     **/
    private List<RenderableModelObject> objectList;

    /**
     * List of players interacting with this entity via a GUI.
     **/
    public final Set<IWrapperPlayer> playersInteracting = new HashSet<>();
    public boolean playerCraftedItem;

    /**
     * Cached item to prevent pack lookups each item request.  May not be used if this is extended for other mods.
     **/
    public AItemPack<JSONDefinition> cachedItem;

    //Radar lists.  Only updated once a tick.  Created when first requested via animations.
    public final List<EntityVehicleF_Physics> aircraftOnRadar = new ArrayList<>();
    public final List<EntityVehicleF_Physics> groundersOnRadar = new ArrayList<>();
    private final Comparator<AEntityB_Existing> entityComparator = new Comparator<AEntityB_Existing>() {
        @Override
        public int compare(AEntityB_Existing o1, AEntityB_Existing o2) {
            return position.isFirstCloserThanSecond(o1.position, o2.position) ? -1 : 1;
        }

    };

    /**
     * Constructor for synced entities
     **/
    public AEntityD_Definable(AWrapperWorld world, AItemSubTyped<JSONDefinition> item, IWrapperNBT data) {
        super(world, data);
        if (item != null) {
            this.definition = item.definition;
            updateSubDefinition(item.subDefinition.subName);
        } else {
            this.definition = generateDefaultDefinition();
            updateSubDefinition("");
        }

        //Load data, or use defaults.
        if (data != null) {
            //Load text.
            if (definition.rendering != null && definition.rendering.textObjects != null) {
                for (int i = 0; i < definition.rendering.textObjects.size(); ++i) {
                    JSONText textDef = definition.rendering.textObjects.get(i);
                    //Check for text value in case we added a text line after we created this entity.
                    text.put(textDef, data.hasKey("text" + textDef.fieldName) ? data.getString("text" + textDef.fieldName) : textDef.defaultText);
                }
            }

            //Load variables.
            //We can always have this variable replaced in a subclassed constructor, if required.
            //Just as long as we do the replacement before we set any references.
            for (String variableName : data.getStrings("variables")) {
                addVariable(new ComputedVariable(this, variableName, data));
            }
        } else {
            //Only set initial text/variables on initial placement.
            if (definition.rendering != null && definition.rendering.textObjects != null) {
                for (int i = 0; i < definition.rendering.textObjects.size(); ++i) {
                    JSONText textDef = definition.rendering.textObjects.get(i);
                    text.put(textDef, textDef.defaultText);
                }
            }

            if (definition.initialVariables != null) {
                definition.initialVariables.forEach(variable -> {
                    ComputedVariable newVariable = new ComputedVariable(this, variable, null);
                    newVariable.setTo(1, false);
                    addVariable(newVariable);
                });
            }
        }
        performCommonConstructionWork();
    }

    /**
     * Constructor for un-synced entities.  Allows for specification of position/motion/angles.
     **/
    public AEntityD_Definable(AWrapperWorld world, Point3D position, Point3D motion, Point3D angles, AItemSubTyped<JSONDefinition> item) {
        super(world, position, motion, angles);
        this.definition = item.definition;
        updateSubDefinition(item.subDefinition.subName);
        performCommonConstructionWork();
    }

    private void performCommonConstructionWork() {
        //Add constants. 
        if (definition.constantValues != null) {
            definition.constantValues.forEach((constantKey, constantValue) -> {
                ComputedVariable newVariable = new ComputedVariable(this, constantKey);
                newVariable.setTo(constantValue, false);
                addVariable(newVariable);
            });
        }

        //Add variable modifiers.
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
                if (modifier.animations != null) {
                    variableModiferSwitchboxes.put(modifier, new VariableModifierSwitchbox(this, modifier.animations));
                }
            }
        }

        if (definition.rendering != null) {
            if (definition.rendering.customVariables != null) {
                definition.rendering.customVariables.forEach(variable -> {
                    //Need to check if we have ourselves defined from data before we make ourselves new.
                    if (!containsVariable(variable)) {
                        addVariable(new ComputedVariable(this, variable, null));
                    }
                });
            }

            if (definition.rendering.sounds != null) {
                for (JSONSound soundDef : definition.rendering.sounds) {
                    allSoundDefs.add(soundDef);
                    soundActiveSwitchboxes.put(soundDef, new AnimationSwitchbox(this, soundDef.activeAnimations, null));

                    if (soundDef.volumeAnimations != null) {
                        soundVolumeSwitchboxes.put(soundDef, new SoundSwitchbox(this, soundDef.volumeAnimations));
                    }

                    if (soundDef.pitchAnimations != null) {
                        soundPitchSwitchboxes.put(soundDef, new SoundSwitchbox(this, soundDef.pitchAnimations));
                    }
                }
            }

            if (definition.rendering.lightObjects != null) {
                for (JSONLight lightDef : definition.rendering.lightObjects) {
                    lightObjectDefinitions.put(lightDef.objectName, lightDef);
                    if (lightDef.brightnessAnimations != null) {
                        lightBrightnessSwitchboxes.put(lightDef, new LightSwitchbox(this, lightDef.brightnessAnimations));
                    }
                    lightBrightnessValues.put(lightDef, 0F);
                    lightColorValues.put(lightDef, new ColorRGB());
                }
            }

            if (definition.rendering.particles != null) {
                for (JSONParticle particleDef : definition.rendering.particles) {
                    particleActiveSwitchboxes.put(particleDef, new AnimationSwitchbox(this, particleDef.activeAnimations, null));
                    if (particleDef.spawningAnimations != null) {
                        particleSpawningSwitchboxes.put(particleDef, new AnimationSwitchbox(this, particleDef.spawningAnimations, null));
                    }
                    lastTickParticleSpawned.put(particleDef, ticksExisted);
                }
            }

            if (definition.rendering.animatedObjects != null) {
                for (JSONAnimatedObject animatedDef : definition.rendering.animatedObjects) {
                    animatedObjectDefinitions.put(animatedDef.objectName, animatedDef);
                    if (animatedDef.animations != null) {
                        animatedObjectSwitchboxes.put(animatedDef.objectName, new AnimationSwitchbox(this, animatedDef.animations, animatedDef.applyAfter));
                    }
                }
            }

            if (definition.rendering.cameraObjects != null) {
                for (JSONCameraObject cameraDef : definition.rendering.cameraObjects) {
                    if (cameraDef.animations != null) {
                        cameraSwitchboxes.put(cameraDef, new AnimationSwitchbox(this, cameraDef.animations, null));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return definition.packID + ":" + definition.systemName + subDefinition.subName;
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("EntityD_Level", true);
        if (world.isClient()) {
            spawnParticles(0);
        }

        //Verify interacting players are still interacting.
        //Server checks if players are still valid, client checks if current player doesn't have a GUI open.
        //This handles players disconnecting or TPing away from this entity without closing their GUI first.
        if (!playersInteracting.isEmpty()) {
            if (world.isClient()) {
                IWrapperPlayer thisClient = InterfaceManager.clientInterface.getClientPlayer();
                if (playersInteracting.contains(thisClient) && !InterfaceManager.clientInterface.isGUIOpen()) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityInteractGUI(this, thisClient, false));
                    playersInteracting.remove(thisClient);
                }
            } else {
                for (IWrapperPlayer player : playersInteracting) {
                    if (!player.isValid() || !player.getWorld().equals(world)) {
                        InterfaceManager.packetInterface.sendToAllClients(new PacketEntityInteractGUI(this, player, false));
                        playersInteracting.remove(player);
                        break;
                    }
                }
            }
        }
        playerCraftedItem = false;

        //Only update radar once a second, and only if we requested it via variables.
        if (definition.general.radarRange > 0 && ticksExisted % 20 == 0) {
            Collection<EntityVehicleF_Physics> allVehicles = world.getEntitiesOfType(EntityVehicleF_Physics.class);
            aircraftOnRadar.clear();
            groundersOnRadar.clear();
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
                    if (!vehicle.radarsTracking.contains(this)) {
                        vehicle.radarsTracking.add(this);
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
                    String value = getRawTextVariableValue(textDef, 0);
                    if (value != null) {
                        value = String.format(textDef.variableFormat, value);
                    } else {
                        value = String.format(textDef.variableFormat, getOrCreateVariable(textDef.variableName).computeValue(0) * textDef.variableFactor + textDef.variableOffset);
                    }
                    textEntry.setValue(value);
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
            	//Set all existing constants to 0 since they aren't valid anymore.
            	//These can be here if we are repainting something.
            	if (subDefinition != null && subDefinition.constants != null) {
                    subDefinition.constants.forEach(constant -> getOrCreateVariable(constant).setTo(0, false));
                }
            	
            	//Set new sub def and constants, if applicable.
                subDefinition = testSubDef;
                if (subDefinition.constants != null) {
                    subDefinition.constants.forEach(constant -> {
                    	//Need to make sure this doesn't already exist, since other systems could be referencing it.
                    	if(containsVariable(constant)) {
                    		getOrCreateVariable(constant).setTo(1, false);
                    	}else {
                    		ComputedVariable newVariable = new ComputedVariable(this, constant);
                            newVariable.setTo(1, false);
                            addVariable(newVariable);	
                    	}
                    });
                }
                
                //Set cached item and re-init animations.
                cachedItem = PackParser.getItem(definition.packID, definition.systemName, subDefinition.subName);
                resetModelsAndAnimations();
                return;
            }
        }
        throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + newSubDefName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    }

    @Override
    public void remove() {
        if (isValid) {
            super.remove();
            //Clear radars.
            aircraftOnRadar.clear();
            groundersOnRadar.clear();
            
            //Clear rendering assignments.
            if (world.isClient()) {
                if (objectList != null) {
                    objectList.forEach(object -> object.destroy());
                }
            }
        }
    }

    /**
     * Returns the entity as an item stack.  This may or may not have NBT defined on it depending on implementation.
     * The default is to just return our item without data, but this is not assured if this function is overridden.
     */
    public IWrapperItemStack getStack() {
        return cachedItem.getNewStack(null);
    }

    /**
     * Generates the default definition for this entity. Used if the item can't be found.
     * This allows for internally-definable entities.
     */
    public JSONDefinition generateDefaultDefinition() {
        throw new IllegalArgumentException("Was asked to auto-generate a definition on an entity with one not defined.  This is NOT allowed.  The entity must be missing its item.  Perhaps a pack was removed with this entity still in the world?");
    }

    /**
     * Returns the texture that should be bound to this entity for rendering.
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
     * Called to update the text on this entity.  Normally just sets the text to the passed-in values,
     * but may do supplemental logic if desired.
     */
    public void updateText(String textKey, String textValue) {
        for (Entry<JSONText, String> textEntry : text.entrySet()) {
            if (textKey.equals(textEntry.getKey().fieldName)) {
                textEntry.setValue(textValue);
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
        for (Entry<JSONParticle, AnimationSwitchbox> particleEntry : particleActiveSwitchboxes.entrySet()) {
            //Check if the particle should be spawned this tick.
            JSONParticle particleDef = particleEntry.getKey();
            AnimationSwitchbox switchbox = particleEntry.getValue();
            boolean shouldParticleSpawn = switchbox.runSwitchbox(partialTicks, false);

            //Make the particle spawn if able.
            if (shouldParticleSpawn) {
                if (particleDef.distance > 0) {
                    Point3D lastParticlePosition = lastPositionParticleSpawned.get(particleDef);
                    if (lastParticlePosition == null) {
                        lastParticlePosition = position.copy();
                        lastPositionParticleSpawned.put(particleDef, lastParticlePosition);
                        continue;//First tick we are active, checks are assured to fail.
                    }
                    while (!lastParticlePosition.isDistanceToCloserThan(position, particleDef.distance)) {
                        double distanceFactor = particleDef.distance / position.distanceTo(lastParticlePosition);
                        Point3D spawningPosition = lastParticlePosition.copy().interpolate(position, distanceFactor);
                        for (int i = 0; i < particleDef.quantity; ++i) {
                            AnimationSwitchbox spawningSwitchbox = particleSpawningSwitchboxes.get(particleDef);
                            if (spawningSwitchbox != null) {
                                spawningSwitchbox.runSwitchbox(partialTicks, false);
                            }
                            world.addEntity(new EntityParticle(this, particleDef, spawningPosition, spawningSwitchbox));
                        }
                        lastParticlePosition.set(spawningPosition);
                    }
                } else {
                    //If we've never spawned the particle, or have waited a whole tick for constant-spawners, spawn one now.
                    Long particleSpawnTime = lastTickParticleSpawned.get(particleDef);
                    if (particleSpawnTime == null || (particleDef.spawnEveryTick && ticksExisted > particleSpawnTime)) {
                        for (int i = 0; i < particleDef.quantity; ++i) {
                            AnimationSwitchbox spawningSwitchbox = particleSpawningSwitchboxes.get(particleDef);
                            if (spawningSwitchbox != null) {
                                spawningSwitchbox.runSwitchbox(partialTicks, false);
                            }
                            world.addEntity(new EntityParticle(this, particleDef, position, spawningSwitchbox));
                        }
                        lastTickParticleSpawned.put(particleDef, ticksExisted);
                    }
                }
            } else {
                lastTickParticleSpawned.remove(particleDef);
                lastPositionParticleSpawned.remove(particleDef);
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
        if (definition.rendering != null && definition.rendering.lightObjects != null) {
            for (JSONLight lightDef : definition.rendering.lightObjects) {
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

                //Set color level.
                if (lightColor != null) {
                    lightColorValues.put(lightDef, lightColor);
                } else if (lightDef.color != null) {
                    lightColorValues.put(lightDef, lightDef.color);
                } else {
                    lightColorValues.put(lightDef, ColorRGB.WHITE);
                }
            }
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
        if (!allSoundDefs.isEmpty()) {
            AEntityF_Multipart<?> soundMasterEntity = this instanceof APart ? ((APart) this).masterEntity : (this instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) this : null);
            AEntityB_Existing entityRiding = InterfaceManager.clientInterface.getClientPlayer().getEntityRiding();
            AEntityF_Multipart<?> playerRidingMasterEntity = entityRiding instanceof APart ? ((APart) entityRiding).masterEntity : (entityRiding instanceof AEntityF_Multipart ? (AEntityF_Multipart<?>) entityRiding : null);
            boolean cameraIsInterior = InterfaceManager.clientInterface.getCameraMode() == CameraMode.FIRST_PERSON && (CameraSystem.activeCamera == null || CameraSystem.activeCamera.isInterior);
            boolean playerRidingThisEntity = playerRidingMasterEntity != null && (playerRidingMasterEntity.equals(this) || playerRidingMasterEntity.allParts.contains(this));
            boolean playerRidingExteriorSeat = entityRiding instanceof PartSeat && ((PartSeat) entityRiding).isExteriorVar.isActive;
            boolean weAreOpenTop = soundMasterEntity instanceof EntityVehicleF_Physics && ((EntityVehicleF_Physics) soundMasterEntity).openTopVar.isActive;
            boolean playerRidingClosedTop = playerRidingMasterEntity instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) playerRidingMasterEntity).openTopVar.isActive && cameraIsInterior && !playerRidingExteriorSeat;

            for (JSONSound soundDef : allSoundDefs) {
                if (soundDef.canPlayOnPartialTicks ^ partialTicks == 0) {
                    //Check if the sound should be playing before we try to update state.
                    //First check the animated conditionals, since those drive on/off state.
                    //Need to check if we are valid, since we tick when invalid for one tick at death and don't want to re-start any stopped looping sounds.
                    AnimationSwitchbox activeSwitchbox = soundActiveSwitchboxes.get(soundDef);
                    boolean shouldSoundStartPlaying = (!soundDef.looping || isValid) && activeSwitchbox.runSwitchbox(partialTicks, true);

                    //If we aren't a looping or repeating sound, check if we were true last check.
                    //If we were, then we shouldn't play, even if all states are true, as we'd start another sound.
                    if (!soundDef.looping && !soundDef.forceSound) {
                        if (shouldSoundStartPlaying) {
                            if (!soundDefFalseLastCheck.remove(soundDef)) {
                                shouldSoundStartPlaying = false;
                            }
                        } else {
                            soundDefFalseLastCheck.add(soundDef);
                        }
                    }
                    
                    //Now that we know if we are enabled, check if the player has the right viewpoint.
                    if (shouldSoundStartPlaying) {
                        if (!weAreOpenTop) {
                            if (soundDef.isInterior && (!playerRidingThisEntity || !cameraIsInterior || playerRidingExteriorSeat)) {
                                shouldSoundStartPlaying = false;
                            } else if (soundDef.isExterior && playerRidingThisEntity && cameraIsInterior && !playerRidingExteriorSeat) {
                                shouldSoundStartPlaying = false;
                            }
                        }
                    }

                    //Next, check the distance.
                    double distance = 0;
                    double conicalFactor = 1.0;
                    if (shouldSoundStartPlaying) {
                        Point3D soundPos = soundDef.pos != null ? soundDef.pos.copy().rotate(orientation).add(position) : position;
                        if (shouldSoundStartPlaying) {
                            distance = soundPos.distanceTo(InterfaceManager.clientInterface.getClientPlayer().getPosition());
                            if (soundDef.maxDistance != soundDef.minDistance) {
                                shouldSoundStartPlaying = distance < soundDef.maxDistance && distance >= soundDef.minDistance;
                            } else {
                                shouldSoundStartPlaying = distance < SoundInstance.DEFAULT_MAX_DISTANCE;
                            }
                        }

                        //Next, check if we have a conical restriction.
                        if (shouldSoundStartPlaying && soundDef.conicalVector != null) {
                            double conicalAngle = Math.toDegrees(Math.acos(soundDef.conicalVector.copy().rotate(orientation).dotProduct(InterfaceManager.clientInterface.getClientPlayer().getEyePosition().subtract(soundPos).normalize(), true)));
                            if (conicalAngle >= soundDef.conicalAngle || conicalAngle < 0) {
                                shouldSoundStartPlaying = false;
                            } else {
                                conicalFactor = (soundDef.conicalAngle - conicalAngle) / soundDef.conicalAngle;
                            }
                        }
                    }

                    //Finally, play the sound if all checks were true.
                    SoundInstance playingSound = null;
                    for (SoundInstance sound : sounds) {
                        if (sound.soundDef == soundDef) {
                            playingSound = sound;
                            break;
                        }
                    }
                    if (shouldSoundStartPlaying) {
                        //Sound should play.
                        //If we aren't playing, or are playing but aren't a looping sound, update.
                        if (playingSound == null || !soundDef.looping) {
                            InterfaceManager.soundInterface.playQuickSound(new SoundInstance(this, soundDef));
                        }
                    } else {
                        if (soundDef.looping && playingSound != null) {
                            //If sound is playing, stop it.
                            //Non-looping sounds are trigger-based and will stop on their own.
                            playingSound.stopSound = true;
                        }

                        //Go to the next soundDef.  No need to change properties on sounds that shouldn't play.
                        continue;
                    }

                    //Sound should be playing.  If it's part of the sound list, update properties.
                    //Sounds may not be in the list if they have just been queued and haven't started yet.
                    //Try to get the sound provided by this def, and update it. 
                    //Note that multiple sounds might be for the same def if they played close enough together.
                    for (SoundInstance sound : sounds) {
                        if (sound.soundDef == soundDef) {
                            //Adjust volume.
                            SoundSwitchbox volumeSwitchbox = soundVolumeSwitchboxes.get(soundDef);
                            boolean definedVolume = false;
                            if (volumeSwitchbox != null) {
                                volumeSwitchbox.runSound(partialTicks);
                                sound.volume = volumeSwitchbox.value;
                                definedVolume = volumeSwitchbox.definedValue;
                            }
                            if (!definedVolume) {
                                sound.volume = 1;
                            } else if (sound.volume < 0) {
                                sound.volume = 0;
                            }

                            //Adjust volume based on distance.
                            if (soundDef.minDistanceVolume == 0 && soundDef.middleDistanceVolume == 0 && soundDef.maxDistanceVolume == 0) {
                                //Default sound distance.
                                double maxDistance = soundDef.maxDistance != 0 ? soundDef.maxDistance : SoundInstance.DEFAULT_MAX_DISTANCE;
                                if (distance > maxDistance) {
                                    //Edge-case if we floating-point errors give us badmaths with the distance calcs.
                                    sound.volume = 0;
                                } else {
                                    sound.volume *= (maxDistance - distance) / (maxDistance);
                                }
                            } else if (soundDef.middleDistance != 0) {
                                //Middle interpolation.
                                if (distance < soundDef.middleDistance) {
                                    sound.volume *= (float) (soundDef.minDistanceVolume + (distance - soundDef.minDistance) / (soundDef.middleDistance - soundDef.minDistance) * (soundDef.middleDistanceVolume - soundDef.minDistanceVolume));
                                } else {
                                    sound.volume *= (float) (soundDef.middleDistanceVolume + (distance - soundDef.middleDistance) / (soundDef.maxDistance - soundDef.middleDistance) * (soundDef.maxDistanceVolume - soundDef.middleDistanceVolume));
                                }
                            } else {
                                //Min/max.
                                if (distance > soundDef.maxDistance) {
                                    //Edge-case if we floating-point errors give us badmaths with the distance calcs.
                                    sound.volume = 0;
                                } else {
                                    sound.volume *= (float) (soundDef.minDistanceVolume + (distance - soundDef.minDistance) / (soundDef.maxDistance - soundDef.minDistance) * (soundDef.maxDistanceVolume - soundDef.minDistanceVolume));
                                }
                            }

                            //Apply conical factor.
                            sound.volume *= conicalFactor;

                            //If the player is in an interior seat that isn't on this entity, dampen the sound
                            //Unless it's a radio, in which case don't do so.
                            if (entityRiding != null && sound.radio == null && !playerRidingThisEntity && playerRidingClosedTop) {
                                sound.volume *= 0.5F;
                            }

                            //Adjust pitch.
                            SoundSwitchbox pitchSwitchbox = soundPitchSwitchboxes.get(soundDef);
                            boolean definedPitch = false;
                            if (pitchSwitchbox != null) {
                                pitchSwitchbox.runSound(partialTicks);
                                sound.pitch = pitchSwitchbox.value;
                                definedPitch = pitchSwitchbox.definedValue;
                            }
                            if (!definedPitch) {
                                sound.pitch = 1;
                            } else if (sound.volume < 0) {
                                sound.pitch = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Custom sound switchbox class.
     */
    private static class SoundSwitchbox extends AnimationSwitchbox {
        private boolean definedValue = false;
        private float value = 0;

        private SoundSwitchbox(AEntityD_Definable<?> entity, List<JSONAnimationDefinition> animations) {
            super(entity, animations, null);
        }

        public boolean runSound(float partialTicks) {
            value = 0;
            definedValue = false;
            return runSwitchbox(partialTicks, true);
        }

        @Override
        public void runTranslation(DurationDelayClock clock, float partialTicks) {
            definedValue = true;
            value += entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks);
        }

        @Override
        public void runRotation(DurationDelayClock clock, float partialTicks) {
            definedValue = true;
            //Parobola is defined with parameter A being x, and H being z.
            double parabolaValue = entity.getAnimatedVariableValue(clock, clock.animation.axis.y, -clock.animation.offset, partialTicks);
            value += clock.animation.axis.x * Math.pow(parabolaValue - clock.animation.axis.z, 2) + clock.animation.offset;
        }
    }

    /**
     * Returns a new computed variable for the passed-in variable.  The default implementation is to just 
     * get the variable assuming it's a basic variable.  As such, super should always be called after any
     * overriding functions, since super will always return a value.  The only exception is if 
     * createDefaultIfNotPresent is set to false, in which case it will return null to indicate no variable
     * was able to be computed for the requested key.
     */
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("radio_active"):
                return new ComputedVariable(this, variable, partialTicks -> radio != null && radio.isPlaying() ? 1 : 0, false);
            case ("radio_volume"):
                return new ComputedVariable(this, variable, partialTicks -> radio != null ? radio.volume : 0, false);
            case ("radio_preset"):
                return new ComputedVariable(this, variable, partialTicks -> radio != null ? radio.preset : 0, false);
            case ("tick"):
                return new ComputedVariable(this, variable, partialTicks -> ticksExisted + partialTicks, true);
            case ("tick_sin"):
                return new ComputedVariable(this, variable, partialTicks -> Math.sin(Math.toRadians(ticksExisted + partialTicks)), true);
            case ("tick_cos"):
                return new ComputedVariable(this, variable, partialTicks -> Math.cos(Math.toRadians(ticksExisted + partialTicks)), true);
            case ("time"):
                return new ComputedVariable(this, variable, partialTicks -> world.getTime(), false);
            case ("random"):
                return new ComputedVariable(this, variable, partialTicks -> Math.random(), true);
            case ("random_flip"):
                return new ComputedVariable(this, variable, partialTicks -> Math.random() < 0.5 ? 0 : 1, true);
            case ("rain_strength"):
                return new ComputedVariable(this, variable, partialTicks -> (int) world.getRainStrength(position), false);
            case ("rain_sin"): {
                return new ComputedVariable(this, variable, partialTicks -> {
                    int rainStrength = (int) world.getRainStrength(position);
                    return rainStrength > 0 ? Math.sin(rainStrength * Math.toRadians(360 * (ticksExisted + partialTicks) / 20)) / 2D + 0.5 : 0;
                }, false);
            }
            case ("rain_cos"): {
                return new ComputedVariable(this, variable, partialTicks -> {
                    int rainStrength = (int) world.getRainStrength(position);
                    return rainStrength > 0 ? Math.cos(rainStrength * Math.toRadians(360 * (ticksExisted + partialTicks) / 20)) / 2D + 0.5 : 0;
                }, false);
            }
            case ("light_sunlight"):
                return new ComputedVariable(this, variable, partialTicks -> world.getLightBrightness(position, false), false);
            case ("light_total"):
                return new ComputedVariable(this, variable, partialTicks -> world.getLightBrightness(position, true), false);
            case ("terrain_distance"):
                return new ComputedVariable(this, variable, partialTicks -> world.getHeight(position), false);
            case ("posX"):
                return new ComputedVariable(this, variable, partialTicks -> position.x, false);
            case ("posY"):
                return new ComputedVariable(this, variable, partialTicks -> position.y, false);
            case ("posZ"):
                return new ComputedVariable(this, variable, partialTicks -> position.z, false);
            case ("inliquid"):
                return new ComputedVariable(this, variable, partialTicks -> world.isBlockLiquid(position) ? 1 : 0, false);
            case ("player_interacting"):
                return new ComputedVariable(this, variable, partialTicks -> !playersInteracting.isEmpty() ? 1 : 0, false);
            case ("player_crafteditem"):
                return new ComputedVariable(this, variable, partialTicks -> playerCraftedItem ? 1 : 0, false);
            case ("distance_client"):
                return new ComputedVariable(this, variable, partialTicks -> position.distanceTo(InterfaceManager.clientInterface.getClientPlayer().getPosition()), false);
            case ("config_simplethrottle"):
                return new ComputedVariable(this, variable, partialTicks -> ConfigSystem.client.controlSettings.simpleThrottle.value ? 1 : 0, false);
            case ("config_innerwindows"):
                return new ComputedVariable(this, variable, partialTicks -> ConfigSystem.client.renderingSettings.innerWindows.value ? 1 : 0, false);
            default: {
                if (variable.endsWith("_cycle")) {
                    String[] parsedVariable = variable.split("_");
                    final int offTime = Integer.parseInt(parsedVariable[0]);
                    final int onTime = Integer.parseInt(parsedVariable[1]);
                    final int totalTime = offTime + onTime + Integer.parseInt(parsedVariable[2]);
                    return new ComputedVariable(this, variable, partialTicks -> {
                        long timeInCycle = ticksExisted % totalTime;
                        return timeInCycle > offTime && timeInCycle - offTime < onTime ? 1 : 0;
                    }, false);
                } else if (variable.startsWith("text_") && variable.endsWith("_present")) {
                    if (definition.rendering != null && definition.rendering.textObjects != null) {
                        final int textIndex = Integer.parseInt(variable.substring("text_".length(), variable.length() - "_present".length())) - 1;
                        if (definition.rendering.textObjects.size() > textIndex) {
                            return new ComputedVariable(this, variable, partialTicks -> !text.get(definition.rendering.textObjects.get(textIndex)).isEmpty() ? 1 : 0, false);
                        } else {
                            return new ComputedVariable(false);
                        }
                    } else {
                        return new ComputedVariable(false);
                    }
                } else if (variable.startsWith("blockname_")) {
                    final String blockName = variable.substring("blockname_".length()).toLowerCase();
                    return new ComputedVariable(this, variable, partialTicks -> {
                        return world.getBlockName(position).equals(blockName) ? 1 : 0;
                    }, false);
                } else if (variable.startsWith("terrain_blockname_")) {
                    final String blockName = variable.substring("terrain_blockname_".length()).toLowerCase();
                    return new ComputedVariable(this, variable, partialTicks -> {
                        double height = world.getHeight(position) + 1;
                        position.y -= height;
                        String actualBlockName = world.getBlockName(position);
                        position.y += height;
                        return actualBlockName.equals(blockName) ? 1 : 0;
                    }, false);
                } else if (variable.startsWith("blockmaterial_")) {
                    final String materialName = variable.substring("blockmaterial_".length()).toUpperCase();
                    return new ComputedVariable(this, variable, partialTicks -> {
                        BlockMaterial material = world.getBlockMaterial(position);
                        if (material != null) {
                            return material.name().equals(materialName) ? 1 : 0;
                        } else {
                            return 0;
                        }
                    }, false);
                } else if (variable.startsWith("terrain_blockmaterial_")) {
                    final String materialName = variable.substring("terrain_blockmaterial_".length()).toUpperCase();
                    return new ComputedVariable(this, variable, partialTicks -> {
                        double height = world.getHeight(position) + 1;
                        position.y -= height;
                        BlockMaterial material = world.getBlockMaterial(position);
                        position.y += height;
                        if (material != null) {
                            return material.name().equals(materialName) ? 1 : 0;
                        } else {
                            return 0;
                        }
                    }, false);
                } else {
                    //Either a hard-coded value, or one we are wrapping.  No logic required.
                    //Double-check we don't already have this variable.  If we do, we don't need to re-create it.
                    ComputedVariable exsitingVariable = computedVariables.get(variable);
                    if (exsitingVariable != null) {
                        return exsitingVariable;
                    } else {
                        if (createDefaultIfNotPresent) {
                            ComputedVariable newVariable = new ComputedVariable(this, variable, null);
                            addVariable(newVariable);
                            return newVariable;
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a String for text-based parameters rather than a double.  If no match
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
        double value = getOrCreateVariable(clock.animation.variable).computeValue(partialTicks);
        return clock.clampAndScale(this, value, scaleFactor, offset, partialTicks);
    }

    /**
     * Short-hand version of {@link #getAnimatedVariableValue(DurationDelayClock, double, double, float)}
     * with an offset of 0.0.
     */
    public final double getAnimatedVariableValue(DurationDelayClock clock, double scaleFactor, float partialTicks) {
        return getAnimatedVariableValue(clock, scaleFactor, 0.0, partialTicks);
    }

    /**
     * Gets the requested variable, or creates it if it doesn't exist.
     */
    public ComputedVariable getOrCreateVariable(String variable) {
        ComputedVariable computedVar = computedVariables.get(variable);
        if (computedVar == null) {
            if (variable.startsWith(ComputedVariable.INVERTED_PREFIX)) {
                //Get the normal variable, and then reference the inverted internal variable instead.
                //First try to get the actual variable, just the inverted one.  If we don't have it, make it and use the inversion.
                String normalVariable = variable.substring(ComputedVariable.INVERTED_PREFIX.length());
                computedVar = computedVariables.get(normalVariable);
                if (computedVar == null) {
                    computedVar = createComputedVariable(normalVariable, true);
                    computedVariables.put(normalVariable, computedVar);
                }
                computedVar = computedVar.invertedVariable;
            } else {
                computedVar = createComputedVariable(variable, true);
            }
            computedVariables.put(variable, computedVar);
        }
        return computedVar;
    }
    
    public void addVariable(ComputedVariable variable) {
        computedVariables.put(variable.variableKey, variable);
        if (variable.invertedVariable != null) {
            computedVariables.put(variable.invertedVariable.variableKey, variable.invertedVariable);
        }
    }

    public void resetAllVariables() {
        computedVariables.entrySet().removeIf(entry -> entry.getValue().entity != this || entry.getValue().shouldReset);
    }

    public boolean containsVariable(String variable) {
        return computedVariables.containsKey(variable);
    }

    /**
     * Helper method for variable modification.
     */
    protected double adjustVariable(JSONVariableModifier modifier, double currentValue) {
        double modifiedValue = modifier.setValue != 0 ? modifier.setValue : currentValue + modifier.addValue;
        VariableModifierSwitchbox switchbox = variableModiferSwitchboxes.get(modifier);
        if (switchbox != null) {
            switchbox.modifiedValue = (float) modifiedValue;
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
     * Returns true if any of the variables in the passed-in list are true.
     */
    public boolean isVariableListTrue(List<List<String>> list) {
        if (list != null) {
            for (List<String> variableList : list) {
                boolean listIsTrue = false;
                for (String variableName : variableList) {
                    if (getOrCreateVariable(variableName).computeValue(0) > 0) {
                        listIsTrue = true;
                        break;
                    }
                }
                if (!listIsTrue) {
                    //List doesn't have any true variables, therefore the value is false.
                    return false;
                }
            }
            //No false lists were found for this collection, therefore the list is true.
        } //No lists found for this entry, therefore no variables are false.

        return true;
    }

    /**
     * Performs the requested action.  Only call this on servers!
     */
    public void performAction(JSONAction action, boolean conditionsTrue) {
        switch (action.action) {
            case BUTTON: {
                if (conditionsTrue) {
                    getOrCreateVariable(action.variable).setTo(action.value, true);
                } else {
                    getOrCreateVariable(action.variable).setTo(0, true);
                }
                break;
            }
            case INCREMENT:
                if (conditionsTrue) {
                    getOrCreateVariable(action.variable).increment(action.value, action.clampMin, action.clampMax, true);
                }
                break;
            case SET:
                if (conditionsTrue) {
                    getOrCreateVariable(action.variable).setTo(action.value, true);
                }
                break;
            case TOGGLE: {
                if (conditionsTrue) {
                    getOrCreateVariable(action.variable).toggle(true);
                }
                break;
            }
        }
    }

    /**
     * Special method to close all doors on this entity.
     */
    public final void closeDoors() {
        computedVariables.forEach((variableKey, variableValue) -> {
            if (variableKey.contains("door")) {
                variableValue.setTo(0, true);
            }
        });
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
                modifiedValue *= clock.animation.axis.y == 0 ? entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks) : Math.pow(entity.getAnimatedVariableValue(clock, clock.animation.axis.x, partialTicks), clock.animation.axis.y); //If the Y axis is zero, simply multiply. If it is not zero, multiply the variable raised to the power of Y.
            } else if (clock.animation.axis.y != 0) {
                modifiedValue += clock.animation.axis.z == 0 ? entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks) : Math.pow(entity.getAnimatedVariableValue(clock, clock.animation.axis.y, partialTicks), clock.animation.axis.z);
            } else {
                modifiedValue = (float) (entity.getAnimatedVariableValue(clock, clock.animation.axis.z, partialTicks));
            }
        }

        //When a rotation is used, it will return V * (Xsin(V+x) + Ycos(V+y) + Ztan(V+z)) where X, Y, Z is the axis, and x, y, z is the centerPoint. Adding the 'invert' tag will make these inverse trig functions.
        @Override
        public void runRotation(DurationDelayClock clock, float partialTicks) {
        	float trigValue = 0;
        	if (clock.animation.invert) {
        		if (clock.animation.axis.x != 0) {
        			trigValue += clock.animation.axis.x * Math.toDegrees(Math.asin(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.x));
        		}
        		if (clock.animation.axis.y != 0) {
	    			trigValue += clock.animation.axis.y * Math.toDegrees(Math.acos(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.y));
        		}
        		if (clock.animation.axis.z != 0) {
	    			trigValue += clock.animation.axis.z * Math.toDegrees(Math.atan(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.z));
        		}
        	} else {
        		if (clock.animation.axis.x != 0) {
        			trigValue += clock.animation.axis.x * Math.sin(Math.toRadians(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.x));
        		}
        		if (clock.animation.axis.y != 0) {
	    			trigValue += clock.animation.axis.y * Math.cos(Math.toRadians(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.y));
        		}
        		if (clock.animation.axis.z != 0) {
	    			trigValue += clock.animation.axis.z * Math.tan(Math.toRadians(entity.getAnimatedVariableValue(clock, 1, partialTicks) + clock.animation.centerPoint.z));
        		}
        	}
        	modifiedValue *= trigValue;
        }
    }

    /**
     * Called to set the default values of all variables.  Must be run before any other updates that could
     * affect these values.
     */
    public void setVariableDefaults() {
        //Nothing for this level.
    }

    /**
     * Called to update the variable modifiers for this entity.
     */
    public void updateVariableModifiers() {
        if (definition.variableModifiers != null) {
            for (JSONVariableModifier modifier : definition.variableModifiers) {
            	ComputedVariable variable = getOrCreateVariable(modifier.variable);
            	variable.setTo(adjustVariable(modifier, variable.currentValue), false);
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
        if (objectList == null) {
            objectList = AModelParser.generateRenderables(this);
        }

        //Render model object individually.
        objectList.forEach(modelObject -> modelObject.render(this, transform, blendingEnabled, partialTicks));

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
        //Handle particles.  Need to only do this once per frame-render.  Shaders may have us render multiple times.
        if (!InterfaceManager.clientInterface.isGamePaused() && !(ticksExisted == lastTickParticlesSpawned && partialTicks == lastPartialTickParticlesSpawned)) {
            world.beginProfiling("Particles", false);
            spawnParticles(partialTicks);
            lastTickParticlesSpawned = ticksExisted;
            lastPartialTickParticlesSpawned = partialTicks;
        }
        world.endProfiling();
    }

    @Override
    protected boolean disableRendering() {
        //Don't render if we don't have a model.
        return super.disableRendering() || definition.rendering.modelType.equals(ModelType.NONE);
    }

    /**
     * Called externally to reset all caches for all objects and animations on this entity.
     */
    public void resetModelsAndAnimations() {
    	if (definition.rendering.modelType != ModelType.NONE) {
            if (objectList != null) {
                objectList.forEach(object -> object.destroy());
                objectList = null;
            }
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setPackItem(definition, subDefinition.subName);
        text.entrySet().forEach(textEntry -> data.setString("text" + textEntry.getKey().fieldName, textEntry.getValue()));
        List<String> savedNames = new ArrayList<>();
        computedVariables.values().forEach(variable -> {
            if (variable.entity == this) {
                variable.saveToNBT(savedNames, data);
            }
        });
        if (!savedNames.isEmpty()) {
            //Don't want to save variables if we don't have any set since it prevents stacking.
            data.setStrings("variables", savedNames);
        }
        return data;
    }

    /**
     * Indicates that this field is able to be modified via variable modification
     * by the code in {@link AEntityD_Definable#updateVariableModifiers()},
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
    public @interface ModifiableValue {
    }
}
