package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONInstrument.JSONInstrumentComponent;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableIncrement;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.rendering.RenderInstrument;
import minecrafttransportsimulator.rendering.RenderInstrument.InstrumentSwitchbox;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.systems.LanguageSystem;

/**
 * Base entity class containing riders and their positions on this entity.  Used for
 * entities that need to keep track of riders and their locations.  This also contains
 * various collision box lists for collision, as riders cannot interact and start riding
 * entities without collision boxes to click.
 *
 * @author don_bruce
 */
public abstract class AEntityE_Interactable<JSONDefinition extends AJSONInteractableEntity> extends AEntityD_Definable<JSONDefinition> {
    /**
     * Static helper matrix for transforming instrument positions.
     **/
    private static final TransformationMatrix instrumentTransform = new TransformationMatrix();
    private static final RotationMatrix INSTRUMENT_ROTATION_INVERSION = new RotationMatrix().setToAxisAngle(0, 1, 0, 180);

    /**
     * List of boxes generated from JSON.  These are stored here as objects to prevent the need
     * to create them every time we want to parse out hitboxes.  This allows parsing them into sub-sets,
     * and querying the entire list to find the hitbox for a given group and index.
     **/
    public final List<List<BoundingBox>> definitionCollisionBoxes = new ArrayList<>();
    public final Set<BoundingBox> collisionBoxes = new HashSet<>();
    private final Map<JSONCollisionGroup, AnimationSwitchbox> collisionSwitchboxes = new HashMap<>();

    /**
     * Box that encompasses all boxes on this entity.  This can be used as a pre-check for collision operations
     * to check a single large box rather than multiple small ones to save processing power.
     **/
    public final BoundingBox encompassingBox = new BoundingBox(new Point3D(), new Point3D(), 0, 0, 0, false, null);

    /**
     * Set of entities that this entity collided with this tick.  Any entity that is in this set
     * should NOT do collision checks with this entity, or infinite loops will occur.
     * This set should be cleared after all collisions have been checked.
     **/
    public final Set<AEntityE_Interactable<?>> collidedEntities = new HashSet<>();

    /**
     * List of instruments based on their slot in the JSON.  Note that this list is created on first construction
     * and will contain null elements for any instrument that isn't present in that slot.
     * Do NOT modify this list directly.  Instead, use the add/remove methods in this class.
     * This ensures proper animation component creation.
     **/
    public final List<ItemInstrument> instruments = new ArrayList<>();

    /**
     * Similar to {@link #instruments}, except this is the renderable bits for them.  There's one entry for each component,
     * with text being a null entry as text components render via the text rendering system.
     */
    public final List<List<RenderableData>> instrumentRenderables = new ArrayList<>();

    /**
     * Maps instrument components to their respective switchboxes.
     **/
    public final Map<JSONInstrumentComponent, InstrumentSwitchbox> instrumentComponentSwitchboxes = new LinkedHashMap<>();

    /**
     * Maps instrument slot transforms to their respective switchboxes.
     **/
    public final Map<JSONInstrumentDefinition, AnimationSwitchbox> instrumentSlotSwitchboxes = new LinkedHashMap<>();

    /**
     * The amount of damage on this entity.  This value is not necessarily used on all entities, but is put here
     * as damage is something that a good number of entities will have and that the base entity should track.
     **/
    @DerivedValue
    public double damageAmount;
    public static final String DAMAGE_VARIABLE = "damage";
    public boolean outOfHealth;

    /**
     * Internal variable to force collision box updates, even if we aren't normally a moveable entity.
     * Useful if we don't normally move and shouldn't run box updates, but can move on state-change.
     **/
    public boolean forceCollisionUpdateThisTick;

    protected final List<Integer> snapConnectionIndexes = new ArrayList<>();
    protected final Set<Integer> connectionGroupsIndexesInUse = new HashSet<>();
    protected int lastSnapConnectionTried = 0;
    protected boolean bypassConnectionPacket;

    public AEntityE_Interactable(AWrapperWorld world, IWrapperPlayer placingPlayer, AItemSubTyped<JSONDefinition> item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);
        
        //Parse variables out now to prevent variables from activating that use them.
        damageAmount = getVariable(DAMAGE_VARIABLE);
        outOfHealth = damageAmount == definition.general.health && definition.general.health != 0;

        //Load instruments, or create the default ones.
        if (definition.instruments != null) {
            //Need to init lists.
            for (int i = 0; i < definition.instruments.size(); ++i) {
                instruments.add(null);
                instrumentRenderables.add(null);
            }

            if (data != null) {
                for (int i = 0; i < definition.instruments.size(); ++i) {
                    String instrumentPackID = data.getString("instrument" + i + "_packID");
                    String instrumentSystemName = data.getString("instrument" + i + "_systemName");
                    if (!instrumentPackID.isEmpty()) {
                        ItemInstrument instrument = PackParser.getItem(instrumentPackID, instrumentSystemName);
                        //Check to prevent loading of faulty instruments due to updates.
                        if (instrument != null) {
                            addInstrument(instrument, i);
                        }
                    }
                }
            } else {
                for (JSONInstrumentDefinition packInstrument : definition.instruments) {
                    if (packInstrument.defaultInstrument != null) {
                        try {
                            String instrumentPackID = packInstrument.defaultInstrument.substring(0, packInstrument.defaultInstrument.indexOf(':'));
                            String instrumentSystemName = packInstrument.defaultInstrument.substring(packInstrument.defaultInstrument.indexOf(':') + 1);
                            try {
                                ItemInstrument instrument = PackParser.getItem(instrumentPackID, instrumentSystemName);
                                if (instrument != null) {
                                    addInstrument(instrument, definition.instruments.indexOf(packInstrument));
                                }
                            } catch (NullPointerException e) {
                                placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, LanguageSystem.SYSTEM_DEBUG, "Attempted to add defaultInstrument: " + instrumentPackID + ":" + instrumentSystemName + " to: " + definition.packID + ":" + definition.systemName + " but that instrument doesn't exist in the pack item registry."));
                            }
                        } catch (IndexOutOfBoundsException e) {
                            placingPlayer.sendPacket(new PacketPlayerChatMessage(placingPlayer, LanguageSystem.SYSTEM_DEBUG, "Could not parse defaultInstrument definition: " + packInstrument.defaultInstrument + ".  Format should be \"packId:instrumentName\""));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void initializeAnimations() {
        super.initializeAnimations();
        //Create collision boxes.
        if (definition.collisionGroups != null) {
            for (JSONCollisionGroup groupDef : definition.collisionGroups) {
                List<BoundingBox> boxes = new ArrayList<>();
                for (JSONCollisionBox boxDef : groupDef.collisions) {
                    boxes.add(new BoundingBox(boxDef, groupDef));
                }
                definitionCollisionBoxes.add(boxes);
                if (groupDef.animations != null || groupDef.applyAfter != null) {
                    List<JSONAnimationDefinition> animations = new ArrayList<>();
                    if (groupDef.animations != null) {
                        animations.addAll(groupDef.animations);
                    }
                    collisionSwitchboxes.put(groupDef, new AnimationSwitchbox(this, animations, groupDef.applyAfter));
                }
            }
        }

        //Create instrument animation clocks.
        if (definition.instruments != null) {
            for (JSONInstrumentDefinition packInstrument : definition.instruments) {
                if (packInstrument.animations != null) {
                    List<JSONAnimationDefinition> animations = new ArrayList<>(packInstrument.animations);
                    instrumentSlotSwitchboxes.put(packInstrument, new AnimationSwitchbox(this, animations, packInstrument.applyAfter));
                }
            }
        }

        //Check if we have snap connections.
        lastSnapConnectionTried = 0;
        if (definition.connectionGroups != null) {
            for (JSONConnectionGroup group : definition.connectionGroups) {
                if (group.isSnap && group.isHookup) {
                    snapConnectionIndexes.add(definition.connectionGroups.indexOf(group));
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();

        world.beginProfiling("EntityE_Level", true);
        //Update damage and locked value
        damageAmount = getVariable(DAMAGE_VARIABLE);
        outOfHealth = damageAmount == definition.general.health && definition.general.health != 0;

        world.endProfiling();
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return !collisionSwitchboxes.isEmpty() || forceCollisionUpdateThisTick;
    }

    @Override
    public double getRawVariableValue(String variable, float partialTicks) {
        if ("damage_percent".equals(variable)) {
            return damageAmount / definition.general.health;
        } else if ("damage_totaled".equals(variable)) {
            return outOfHealth ? 1 : 0;
        }

        //Not a towing variable, check others.
        return super.getRawVariableValue(variable, partialTicks);
    }

    /**
     * Updates the position of all collision boxes, and sets them in their appropriate maps based on their
     * properties, and animation state (if applicable).
     */
    protected void updateCollisionBoxes() {
        collisionBoxes.clear();
        if (definition.collisionGroups != null) {
            for (int i = 0; i < definition.collisionGroups.size(); ++i) {
                JSONCollisionGroup groupDef = definition.collisionGroups.get(i);
                List<BoundingBox> boxes = definitionCollisionBoxes.get(i);
                if (groupDef.health == 0 || getVariable("collision_" + (i + 1) + "_damage") < groupDef.health) {
                    AnimationSwitchbox switchBox = collisionSwitchboxes.get(groupDef);
                    if (switchBox != null) {
                        if (switchBox.runSwitchbox(0, false)) {
                            for (BoundingBox box : boxes) {
                                box.globalCenter.set(box.localCenter).transform(switchBox.netMatrix);
                                box.updateToEntity(this, box.globalCenter);
                            }
                        } else {
                            continue;
                        }
                    } else {
                        for (BoundingBox box : boxes) {
                            box.updateToEntity(this, null);
                        }
                    }
                    collisionBoxes.addAll(boxes);
                }
            }
        }
    }

    /**
     * Updates the encompassing box.  This has to run after {@link #updateCollisionBoxes()} to ensure
     * we get all boxes for the encompassing box.
     */
    protected void updateEncompassingBox() {
        encompassingBox.widthRadius = 0;
        encompassingBox.heightRadius = 0;
        encompassingBox.depthRadius = 0;
        for (BoundingBox box : collisionBoxes) {
            encompassingBox.widthRadius = (float) Math.max(encompassingBox.widthRadius, Math.abs(box.globalCenter.x - position.x) + box.widthRadius);
            encompassingBox.heightRadius = (float) Math.max(encompassingBox.heightRadius, Math.abs(box.globalCenter.y - position.y) + box.heightRadius);
            encompassingBox.depthRadius = (float) Math.max(encompassingBox.depthRadius, Math.abs(box.globalCenter.z - position.z) + box.depthRadius);
        }
        encompassingBox.updateToEntity(this, null);
    }

    /**
     * Applies damage to the collision group the passed-in box is a part of.
     * The box MUST have a {@link BoundingBox#groupDef} defined or this method will crash.
     * Only call this method on the server: clients will update via variable packets.
     */
    public void damageCollisionBox(BoundingBox box, double damageAmount) {
        String variableName = "collision_" + (definition.collisionGroups.indexOf(box.groupDef) + 1) + "_damage";
        double currentDamage = getVariable(variableName) + damageAmount;
        if (currentDamage > box.groupDef.health) {
            double amountActuallyNeeded = damageAmount - (currentDamage - box.groupDef.health);
            currentDamage = box.groupDef.health;
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, variableName, amountActuallyNeeded));
        } else {
            InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, variableName, damageAmount));
        }
        setVariable(variableName, currentDamage);
    }

    @Override
    public void doPostUpdateLogic() {
        super.doPostUpdateLogic();
        if (requiresDeltaUpdates()) {
            //Update collision boxes to new position.
            world.beginProfiling("CollisionBoxUpdates", true);
            updateCollisionBoxes();
            updateEncompassingBox();
            world.endProfiling();

            //Reset collision override flag.
            forceCollisionUpdateThisTick = false;
        }
    }

    /**
     * Adds the instrument to the specified slot.
     */
    public void addInstrument(ItemInstrument instrument, int slot) {
        instruments.set(slot, instrument);
        List<RenderableData> renderables = new ArrayList<>();
        for (JSONInstrumentComponent component : instrument.definition.components) {
            if (component.textObject != null) {
                renderables.add(null);
            } else {
                renderables.add(new RenderableData(RenderableVertices.createSprite(1, null, null)));
            }
            if (component.animations != null) {
                instrumentComponentSwitchboxes.put(component, new InstrumentSwitchbox(this, component));
            }
        }
        instrumentRenderables.set(slot, renderables);
    }

    /**
     * Removes the instrument from the specified slot.
     */
    public void removeIntrument(int slot) {
        ItemInstrument removedInstrument = instruments.set(slot, null);
        if (removedInstrument != null) {
            for (JSONInstrumentComponent component : removedInstrument.definition.components) {
                instrumentComponentSwitchboxes.remove(component);
            }
            instrumentRenderables.set(slot, null);
        }
    }

    /**
     * Called when the entity is attacked.
     * This should ONLY be called on the server; clients will sync via packets.
     * If calling this method in a loop, make sure to check if this entity is valid.
     * as this function may be called multiple times in a single tick for multiple damage
     * applications, which means one of those may have made this entity invalid.
     */
    public void attack(Damage damage) {
        if (!damage.isWater) {
            if (!outOfHealth) {
                damageAmount += damage.amount;
                if (damageAmount > definition.general.health) {
                    damageAmount = definition.general.health;
                    outOfHealth = true;
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableSet(this, DAMAGE_VARIABLE, damageAmount));
                } else {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketEntityVariableIncrement(this, DAMAGE_VARIABLE, damage.amount));
                }
                setVariable(DAMAGE_VARIABLE, damageAmount);
            }
        }
    }

    @Override
    public void renderBoundingBoxes(TransformationMatrix transform) {
        collisionBoxes.forEach(box -> box.renderWireframe(this, transform, null, null));
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        super.renderModel(transform, blendingEnabled, partialTicks);

        //Renders all instruments on the entity.  Uses the instrument's render code.
        //We only apply the appropriate translation and rotation.
        //Normalization is required here, as otherwise the normals get scaled with the
        //scaling operations, and shading gets applied funny.
        if (definition.instruments != null) {
            world.beginProfiling("Instruments", true);
            for (int i = 0; i < definition.instruments.size(); ++i) {
                ItemInstrument instrument = instruments.get(i);
                if (instrument != null) {
                    JSONInstrumentDefinition packInstrument = definition.instruments.get(i);

                    //Set initial transform.
                    instrumentTransform.set(transform);

                    //Do transforms if required and render if allowed.
                    AnimationSwitchbox switchbox = instrumentSlotSwitchboxes.get(packInstrument);
                    if (switchbox == null || switchbox.runSwitchbox(partialTicks, false)) {
                        if (switchbox != null) {
                            instrumentTransform.multiply(switchbox.netMatrix);
                        }

                        //Now that animations have adjusted us, apply final transforms.
                        //Note that instruments with rotation of Y=0 face backwards, which is opposite of normal rendering.
                        //To compensate, we rotate them 180 here.
                        instrumentTransform.applyTranslation(packInstrument.pos);
                        if (packInstrument.rot != null) {
                            instrumentTransform.applyRotation(packInstrument.rot);
                        }
                        instrumentTransform.applyRotation(INSTRUMENT_ROTATION_INVERSION);

                        //Instruments render with 1 unit being 1 pixel, not 1 block, so scale by 1/16.
                        instrumentTransform.applyScaling(1 / 16F, 1 / 16F, 1 / 16F);
                        RenderInstrument.drawInstrument(this, instrumentTransform, i, false, blendingEnabled, partialTicks);
                    }
                }
            }
            world.endProfiling();
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (definition.instruments != null) {
            for (int i = 0; i < definition.instruments.size(); ++i) {
                ItemInstrument instrument = instruments.get(i);
                if (instrument != null) {
                    data.setString("instrument" + i + "_packID", instrument.definition.packID);
                    data.setString("instrument" + i + "_systemName", instrument.definition.systemName);
                }
            }
        }
        return data;
    }
}
