package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONDummyPartProvider;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Entity class responsible for being a part placed in the world.
 * Doesn't do anything except be a data storage bank for the placed part
 * and give it a multipart to exist on.
 *
 * @author don_bruce
 */
public class EntityPlacedPart extends AEntityF_Multipart<JSONDummyPartProvider> {
    private static final List<String> allPartTypes = new ArrayList<>();

    public APart currentPart;
    private final Point3D motionApplied = new Point3D();
    private boolean riderPresentLastCheck;
    private boolean riderPresentThisCheck;

    public EntityPlacedPart(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, null, data);
    }

    @Override
    public JSONDummyPartProvider generateDefaultDefinition() {
        JSONDummyPartProvider defaultDefinition = JSONDummyPartProvider.generateDummy();

        if (allPartTypes.isEmpty()) {
            //Look though all part types and add them.
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem instanceof AItemPart) {
                    AItemPart partItem = (AItemPart) packItem;
                    if (!allPartTypes.contains(partItem.definition.generic.type)) {
                        allPartTypes.add(partItem.definition.generic.type);
                    }
                }
            }
        }
        defaultDefinition.parts.get(0).types.addAll(allPartTypes);

        return defaultDefinition;
    }

    @Override
    public void update() {
        super.update();
        //TODO will NOT be empty normally, but have to check due to syncing.  See F-class TODO for details.
        if (parts.isEmpty()) {
            if (ticksExisted > 100) {
                remove();
            }
        } else {
            if (currentPart != null) {
                //Seat checks are needed to allow the seat to update interactable boxes when rider state changes.
                riderPresentLastCheck = riderPresentThisCheck;
                riderPresentThisCheck = currentPart.rider != null;

                //Go down to find ground, if we haven't already.
                if (currentPart.definition.generic.fallsToGround) {
                    //Need this check to be here since if we put it in the check method we recurse since parts call super.
                    forceCollisionUpdateThisTick = currentPart.requiresDeltaUpdates();

                    //Don't check on the first tick, since we won't be updated yet.
                    if (ticksExisted > 1) {
                        if (motion.y > -3.9) {
                            motion.y += -0.08;
                        }

                        //We can only go down 1 block at a time to ensure proper collision checks.
                        //Too fast and we skip them
                        double maxCollisionDepth = 0;
                        motionApplied.set(0, 0, 0);
                        while (motionApplied.y > motion.y) {
                            motionApplied.y -= 1;
                            if (motionApplied.y < motion.y) {
                                motionApplied.y = motion.y;
                            }
                            for (BoundingBox box : allCollisionBoxes) {
                                if (box.collisionTypes.contains(CollisionType.BLOCK)) {
                                    box.updateCollisions(world, motionApplied, false);
                                    if (box.currentCollisionDepth.y <= maxCollisionDepth) {
                                        maxCollisionDepth = box.currentCollisionDepth.y;
                                    }
                                }
                            }
                        }
                        position.add(motion);
                        if(maxCollisionDepth != 0) {
                        	motion.y = 0;
                        	position.y -= maxCollisionDepth;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addPart(APart part, boolean sendPacket) {
        super.addPart(part, sendPacket);
        currentPart = part;
        currentPart.placementDefinition.pos.y = currentPart.definition.generic.placedOffset;
    }

    @Override
    public void removePart(APart part, boolean removeFromWorld, Iterator<APart> iterator) {
        super.removePart(part, removeFromWorld, iterator);
        currentPart = null;
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return currentPart != null && (super.requiresDeltaUpdates() || currentPart.definition.generic.fallsToGround || (riderPresentLastCheck != riderPresentThisCheck));
    }

    @Override
    public boolean shouldRenderBeams() {
        return ConfigSystem.client.renderingSettings.vehicleBeams.value;
    }

    @Override
    public boolean disableRendering() {
        //Don't render the placed part entity.  Only render the part itself.
        return true;
    }
}
