package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
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
    private boolean riderPresentLastCheck;
    private boolean riderPresentThisCheck;
    private boolean needToFindGround = true;

    public EntityPlacedPart(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
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
            currentPart = parts.get(0);
            if (currentPart != null) {
                currentPart.placementDefinition.pos.y = currentPart.definition.generic.placedOffset;
                forceCollisionUpdateThisTick = currentPart.requiresDeltaUpdates();
                //Seat checks are needed to allow the seat to update interactable boxes when rider state changes.
                riderPresentLastCheck = riderPresentThisCheck;
                riderPresentThisCheck = currentPart.rider != null;

                //Go down to find ground, if we haven't already.
                if (needToFindGround) {
                    if (currentPart.definition.generic.fallsToGround) {
                        //Don't check on the first tick, since we won't be updated yet.
                        if (ticksExisted > 1) {
                            if (motion.y > -3.9) {
                                motion.y += -0.08;
                            }

                            //We can only go down 1 block at a time to ensure proper collision checks.
                            //Too fast and we skip them.
                            Point3D motionApplied = new Point3D();
                            while (needToFindGround && motionApplied.y > motion.y) {
                                motionApplied.y -= 1;
                                if (motionApplied.y < motion.y) {
                                    motionApplied.y = motion.y;
                                }
                                encompassingBox.globalCenter.set(position).add(motionApplied);
                                world.updateBoundingBoxCollisions(encompassingBox, motionApplied, true);

                                if (encompassingBox.currentCollisionDepth.y != 0) {
                                    position.add(motionApplied).subtract(encompassingBox.currentCollisionDepth);
                                    motion.y = 0;
                                    needToFindGround = false;
                                    //Do final box update.
                                    allInteractionBoxes.forEach(box -> box.updateToEntity(this, null));
                                }
                            }
                            position.add(motion);
                        }
                    } else {
                        needToFindGround = false;
                    }
                }
            }
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return super.requiresDeltaUpdates() || needToFindGround || (riderPresentLastCheck != riderPresentThisCheck);
    }

    @Override
    protected void updateEncompassingBox() {
        super.updateEncompassingBox();
        //Need to move all interaction boxes into collision boxes to make players collide with us.
        allEntityCollisionBoxes.addAll(allInteractionBoxes);
        allEntityCollisionBoxes.removeAll(allPartSlotBoxes.keySet());
    }

    @Override
    public boolean shouldRenderBeams() {
        return ConfigSystem.client.renderingSettings.vehicleBeams.value;
    }

    @Override
    public boolean disableRendering(float partialTicks) {
        //Don't render the placed part entity.  Only render the part itself.
        return true;
    }
}
