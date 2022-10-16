package minecrafttransportsimulator.entities.components;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Base class for entities that are rendered in the world in 3D.
 * This level adds various rendering methods and functions for said rendering.
 *
 * @author don_bruce
 */
public abstract class AEntityC_Renderable extends AEntityB_Existing {
    private static final Point3D interpolatedPositionHolder = new Point3D();
    private static final RotationMatrix interpolatedOrientationHolder = new RotationMatrix();
    private static final Point3D interpolatedScaleHolder = new Point3D();
    private static final TransformationMatrix translatedMatrix = new TransformationMatrix();
    private static final TransformationMatrix rotatedMatrix = new TransformationMatrix();

    /**
     * The scale of this entity, in X/Y/Z components.
     */
    public final Point3D scale = new Point3D(1, 1, 1);

    /**
     * The previous scale of this entity.
     */
    public final Point3D prevScale = new Point3D(1, 1, 1);

    /**
     * Constructor for synced entities
     **/
    public AEntityC_Renderable(AWrapperWorld world, IWrapperPlayer placingPlayer, IWrapperNBT data) {
        super(world, placingPlayer, data);
    }

    /**
     * Constructor for un-synced entities.  Allows for specification of position/motion/angles.
     **/
    public AEntityC_Renderable(AWrapperWorld world, Point3D position, Point3D motion, Point3D angles) {
        super(world, position, motion, angles);
    }

    @Override
    public void update() {
        super.update();
        prevScale.set(scale);
    }

    /**
     * Called to render this entity.  This is the setup method that sets states to the appropriate values.
     * After this, the main model rendering method is called.
     */
    public final void render(boolean blendingEnabled, float partialTicks) {
        //If we need to render, do so now.
        world.beginProfiling("RenderSetup", true);
        if (!disableRendering(partialTicks)) {
            //Get the render offset.
            //This is the interpolated movement, plus the prior position.
            if (requiresDeltaUpdates()) {
                interpolatedPositionHolder.set(prevPosition);
                interpolatedPositionHolder.interpolate(position, partialTicks);
            } else {
                interpolatedPositionHolder.set(position);
            }

            //Subtract the entity's position by the render entity position to get the delta for translating.
            interpolatedPositionHolder.subtract(InterfaceManager.clientInterface.getRenderViewEntity().getRenderedPosition(partialTicks));

            //Get interpolated orientation if required.
            if (requiresDeltaUpdates()) {
                getInterpolatedOrientation(interpolatedOrientationHolder, partialTicks);
            } else {
                interpolatedOrientationHolder.set(orientation);
            }

            //Set up lighting.
            InterfaceManager.renderingInterface.setLightingToPosition(position);

            //Set up matrixes.
            translatedMatrix.resetTransforms();
            translatedMatrix.setTranslation(interpolatedPositionHolder);
            rotatedMatrix.set(translatedMatrix);
            rotatedMatrix.applyRotation(interpolatedOrientationHolder);
            interpolatedScaleHolder.set(scale).subtract(prevScale).scale(partialTicks).add(prevScale);
            rotatedMatrix.applyScaling(interpolatedScaleHolder);

            //Render the main model.
            world.endProfiling();
            renderModel(rotatedMatrix, blendingEnabled, partialTicks);

            //End rotation render matrix.
            //Render holoboxes.
            if (blendingEnabled) {
                renderHolographicBoxes(translatedMatrix);
            }

            //Render bounding boxes.
            if (!blendingEnabled && InterfaceManager.renderingInterface.shouldRenderBoundingBoxes()) {
                world.beginProfiling("BoundingBoxes", true);
                renderBoundingBoxes(translatedMatrix);
                world.endProfiling();
            }

            //Handle sounds.  These will be partial-tick only ones.
            //Normal sounds are handled on the main tick loop.
            world.beginProfiling("Sounds", true);
            updateSounds(partialTicks);
        }
        world.endProfiling();
    }

    /**
     * If rendering needs to be skipped for any reason, return true here.
     */
    protected boolean disableRendering(float partialTicks) {
        //Don't render on the first tick, as we might have not created some variables yet.
        return ticksExisted == 0;
    }

    /**
     * Called to render the main model.  At this point the matrix state will be aligned
     * to the position and rotation of the entity relative to the player-camera.
     */
    protected abstract void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks);

    /**
     * Called to render holdgraphic boxes.  These shouldn't rotate with the model, so rotation is not present here.
     * However, at this point the transforms will be set to the entity position, as it is assumed everything will
     * at least be relative to it.
     * Also, this method is only called when blending is enabled, because holographic stuff ain't solid.
     */
    protected void renderHolographicBoxes(TransformationMatrix transform) {
    }

    /**
     * Renders the bounding boxes for the entity, if any are present.
     * At this point, the translation and rotation done for the rendering
     * will be un-done, as boxes need to be rendered according to their world state.
     * The passed-in transform is between the player and the entity.
     */
    public void renderBoundingBoxes(TransformationMatrix transform) {
        boundingBox.renderWireframe(this, transform, null, null);
    }
}
