package minecrafttransportsimulator.blocks.tileentities.components;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;

/**
 * Base Tile Entity class.  In essence, this class holds the data and state of a Tile Entity in the world.
 * All TileEntities are used for making pack-based blocks, so they have JSON parameters
 * attached to them.
 * <br><br>
 * Note that this constructor is called on the server when first creating the TE or loading it from disk,
 * but on the client this is called after the server sends over the saved data, not when the player first clicks.
 * Because of this, there there may be a slight delay in the TE showing up from when the block is first clicked.
 * Also note that the position of the TE is set by the constructor.  This is because TEs have their positions
 * set when they are created by the setting of a block.
 *
 * @author don_bruce
 */
public abstract class ATileEntityBase<JSONDefinition extends AJSONMultiModelProvider> extends AEntityD_Definable<JSONDefinition> {

    private float lastLightLevel;
    private final Point3D blockPosition;

    public ATileEntityBase(AWrapperWorld world, Point3D position, IWrapperPlayer placingPlayer, AItemSubTyped<JSONDefinition> item, IWrapperNBT data) {
        super(world, item, data);
        //Offset the position of this tile to be centered in the blocks 0->1 space.
        //This allows for better rotation code and simpler models.
        //We need to save the actual position though so we don't constantly offset.
        this.blockPosition = position.copy();
        this.position.set(position);
        this.position.add(0.5, 0, 0.5);
        boundingBox.globalCenter.set(this.position);

        //Set angles to placement rotation.
        //Make sure to set to 0 first though since we could be a re-placed decor.
        if (placingPlayer != null) {
            orientation.setToZero().rotateY(getPlacementRotation(placingPlayer));
        }
    }

    /**
     * Returns the rotation, in the Y-direction, that should be applied to newly-placed instances of this entity.
     * The player is passed-in as it is expected the rotation will depend on the player's rotation.
     */
    public double getPlacementRotation(IWrapperPlayer player) {
        int clampAngle = getRotationIncrement();
        return Math.round((player.getYaw() + 180) / clampAngle) * clampAngle % 360;
    }

    @Override
    public void update() {
        super.update();
        if (lastLightLevel != getLightProvided()) {
            lastLightLevel = getLightProvided();
            world.updateLightBrightness(position);
        }
    }

    @Override
    public boolean shouldLinkBoundsToPosition() {
        return false;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable, boolean createDefaultIfNotPresent) {
        switch (variable) {
            case ("redstone_active"):
                return new ComputedVariable(this, variable, partialTicks -> world.getRedstonePower(position) > 0 ? 1 : 0, false);
            case ("redstone_level"):
                return new ComputedVariable(this, variable, partialTicks -> world.getRedstonePower(position), false);
            default:
                return super.createComputedVariable(variable, createDefaultIfNotPresent);
        }
    }

    /**
     * Returns the rotation increment for this TE.  This will ensure the TE is
     * rotated only in this increment when placed into the world.  If no rotation
     * should be performed, return 360.
     */
    public int getRotationIncrement() {
        return 15;
    }

    /**
     * Called when the neighboring block of this TE changes.  This can either
     * be a block being added or removed or just updating state.
     * This is only called on the SERVER.
     */
    public void onNeighborChanged(Point3D otherPosition) {
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (shouldSavePosition()) {
            //Overwrite the position with the actual block position.
            data.setPoint3d("position", blockPosition);
        }
        return data;
    }
}
