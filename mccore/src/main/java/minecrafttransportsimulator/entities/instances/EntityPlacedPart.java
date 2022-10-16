package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
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
    private boolean needToFindGround = true;
    private final Point3D tempBoxPosition = new Point3D();

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
        if (parts.isEmpty()) {
            remove();
        } else {
            currentPart = parts.get(0);
            currentPart.placementDefinition.pos.y = currentPart.definition.generic.placedOffset;
            forceCollisionUpdateThisTick = currentPart.requiresDeltaUpdates();

            //Go down to find ground, if we haven't already.
            if (needToFindGround) {
                if (currentPart.definition.generic.fallsToGround) {
                    motion.y += (9.8 / 400) * ConfigSystem.settings.general.gravityFactor.value;
                    position.add(motion);
                    boolean clearedCache = false;
                    for (BoundingBox box : allInteractionBoxes) {
                        box.updateToEntity(this, null);
                        if (!box.collidesWithLiquids && world.checkForCollisions(box, tempBoxPosition, !clearedCache)) {
                            //Set position to the rounded Y value and halt.
                            position.y = Math.floor(position.y - 0.5) + 0.5;
                            motion.y = 0;
                            needToFindGround = false;
                            break;
                        }
                        clearedCache = true;
                    }
                    if (!needToFindGround) {
                        //Do final box update.
                        allInteractionBoxes.forEach(box -> box.updateToEntity(this, null));
                    }
                } else {
                    needToFindGround = false;
                }
            }
        }
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return super.requiresDeltaUpdates() || needToFindGround;
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
        return currentPart != null ? currentPart.shouldRenderBeams() : false;
    }

    @Override
    public boolean disableRendering(float partialTicks) {
        //Don't render the placed part entity.  Only render the part itself.
        return true;
    }
}
