package minecrafttransportsimulator.entities.instances;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.RenderableObject;

/**
 * Basic particle class.  This mimic's MC's particle logic, except we can manually set
 * movement logic.
 *
 * @author don_bruce
 */
public class EntityParticle extends AEntityC_Renderable {
    private static final FloatBuffer STANDARD_RENDER_BUFFER = generateStandardBuffer();
    private static final int PARTICLES_PER_ROWCOL = 16;
    private static final TransformationMatrix helperTransform = new TransformationMatrix();
    private static final Point3D helperOffset = new Point3D();

    //Constant properties.
    private final AEntityD_Definable<?> entitySpawning;
    private final JSONParticle definition;
    private final int maxAge;
    private final IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();

    private final ColorRGB startColor;
    private final ColorRGB endColor;
    private final ColorRGB staticColor;
    private final RenderableObject renderable;

    //Runtime variables.
    private boolean touchingBlocks;
    private int age;

    public EntityParticle(AEntityD_Definable<?> entitySpawning, JSONParticle definition, AnimationSwitchbox switchbox) {
        super(entitySpawning.world, entitySpawning.position, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);

        helperTransform.resetTransforms().set(entitySpawning.orientation);
        if (switchbox != null) {
            helperTransform.multiply(switchbox.netMatrix);
        }
        if (definition.pos != null) {
            helperOffset.set(definition.pos).multiply(entitySpawning.scale);
        } else {
            helperOffset.set(0, 0, 0);
        }
        helperOffset.transform(helperTransform);
        position.add(helperOffset);

        if (definition.initialVelocity != null) {
            //Set initial velocity, but add some randomness so particles don't all go in a line.
            Point3D adjustedVelocity = definition.initialVelocity.copy().rotate(helperTransform);
            motion.x += adjustedVelocity.x / 10D + 0.02 - Math.random() * 0.04;
            motion.y += adjustedVelocity.y / 10D + 0.02 - Math.random() * 0.04;
            motion.z += adjustedVelocity.z / 10D + 0.02 - Math.random() * 0.04;
        }

        this.entitySpawning = entitySpawning;
        this.definition = definition;
        boundingBox.widthRadius = getSize() / 2D;
        boundingBox.heightRadius = boundingBox.widthRadius;
        boundingBox.depthRadius = boundingBox.widthRadius;
        this.maxAge = generateMaxAge();
        if (definition.color != null) {
            if (definition.toColor != null) {
                this.startColor = definition.color;
                this.endColor = definition.toColor;
                this.staticColor = null;
            } else {
                this.startColor = null;
                this.endColor = null;
                this.staticColor = definition.color;
            }
        } else {
            this.startColor = null;
            this.endColor = null;
            this.staticColor = ColorRGB.WHITE;
        }

        //Generate the points for this particle's renderable and create it.
        FloatBuffer buffer = FloatBuffer.allocate(STANDARD_RENDER_BUFFER.capacity());
        buffer.put(STANDARD_RENDER_BUFFER);
        STANDARD_RENDER_BUFFER.rewind();
        buffer.flip();
        this.renderable = new RenderableObject("particle", definition.texture != null ? definition.texture : (definition.type.equals(ParticleType.BREAK) ? RenderableObject.GLOBAL_TEXTURE_NAME : RenderableObject.PARTICLE_TEXTURE_NAME), staticColor != null ? staticColor : new ColorRGB(), buffer, false);
        renderable.disableLighting = definition.type.equals(ParticleType.FLAME) || definition.isBright;
        renderable.ignoreWorldShading = true;
        if (definition.type.equals(ParticleType.BREAK)) {
            setParticleTextureBounds(0, 0);
        }
    }

    @Override
    public void update() {
        super.update();
        //Set movement.
        if (definition.movementVelocity != null) {
            motion.add(definition.movementVelocity);
            if (motion.x > definition.terminalVelocity.x) {
                motion.x = definition.terminalVelocity.x;
            }
            if (motion.x < -definition.terminalVelocity.x) {
                motion.x = -definition.terminalVelocity.x;
            }
            if (motion.y > definition.terminalVelocity.y) {
                motion.y = definition.terminalVelocity.y;
            }
            if (motion.y < -definition.terminalVelocity.y) {
                motion.y = -definition.terminalVelocity.y;
            }
            if (motion.z > definition.terminalVelocity.z) {
                motion.z = definition.terminalVelocity.z;
            }
            if (motion.z < -definition.terminalVelocity.z) {
                motion.z = -definition.terminalVelocity.z;
            }
        } else {
            switch (definition.type) {
                case SMOKE: {
                    //Update the motions to make the smoke float up.
                    motion.x *= 0.9;
                    motion.y += 0.004;
                    motion.z *= 0.9;
                    break;
                }
                case FLAME: {
                    //Flame just slowly drifts in the direction it was going.
                    motion.scale(0.96);
                    break;
                }
                case DRIP: {
                    //Keep moving until we touch a block, then stop.
                    if (!touchingBlocks) {
                        motion.scale(0.96).add(0D, -0.06D, 0D);
                    } else {
                        motion.scale(0.0);
                    }
                    break;
                }
                case BUBBLE: {
                    //Bubbles float up until they break the surface of the water, then they pop.
                    if (!world.isBlockLiquid(position)) {
                        remove();
                    } else {
                        motion.scale(0.85).add(0, 0.002D, 0);
                    }
                    break;
                }
                case BREAK: {
                    //Breaking just fall down quickly.
                    if (!touchingBlocks) {
                        motion.scale(0.98).add(0D, -0.04D, 0D);
                    } else {
                        motion.scale(0.0);
                    }
                    break;
                }
                case GENERIC: {
                    //Generic particles don't do any movement by default.
                    break;
                }
            }
        }

        //Check collision movement.  If we hit a block, don't move.
        touchingBlocks = boundingBox.updateMovingCollisions(world, motion);
        if (touchingBlocks) {
            motion.add(-boundingBox.currentCollisionDepth.x * Math.signum(motion.x), -boundingBox.currentCollisionDepth.y * Math.signum(motion.y), -boundingBox.currentCollisionDepth.z * Math.signum(motion.z));
        }
        position.add(motion);

        //Check age to see if we are on our last tick.
        if (++age == maxAge) {
            remove();
        }

        //Update bounds as we might have changed size.
        boundingBox.widthRadius = getSize() / 2D;
        boundingBox.heightRadius = boundingBox.widthRadius;
        boundingBox.depthRadius = boundingBox.widthRadius;

        //Update orientation to always face the player.
        orientation.setToVector(clientPlayer.getPosition().add(0, clientPlayer.getEyeHeight(), 0).add(InterfaceManager.clientInterface.getCameraPosition()).subtract(position), true);
    }

    @Override
    public boolean requiresDeltaUpdates() {
        return true;
    }

    @Override
    public boolean shouldSync() {
        return false;
    }

    @Override
    public boolean shouldSavePosition() {
        return false;
    }

    @Override
    protected void renderModel(TransformationMatrix transform, boolean blendingEnabled, float partialTicks) {
        if (blendingEnabled) {
            if (staticColor == null) {
                renderable.color.red = startColor.red + (endColor.red - startColor.red) * (age + partialTicks) / maxAge;
                renderable.color.green = startColor.green + (endColor.green - startColor.green) * (age + partialTicks) / maxAge;
                renderable.color.blue = startColor.blue + (endColor.blue - startColor.blue) * (age + partialTicks) / maxAge;
            }
            renderable.alpha = getAlpha(partialTicks);
            renderable.transform.set(transform);
            double totalScale = getSize() * getScale(partialTicks);
            renderable.transform.applyScaling(totalScale * entitySpawning.scale.x, totalScale * entitySpawning.scale.y, totalScale * entitySpawning.scale.z);

            switch (definition.type) {
                case SMOKE:
                    setParticleTextureBounds(7 - age * 8 / maxAge, 0);
                    break;//Smoke gets smaller as it ages.
                case FLAME:
                    setParticleTextureBounds(0, 3);
                    break;
                case DRIP:
                    setParticleTextureBounds(touchingBlocks ? 1 : 0, 7);
                    break;//Drips become flat when they hit the ground.
                case BUBBLE:
                    setParticleTextureBounds(0, 2);
                    break;
                case BREAK:
                    break;//Do nothing, this will have been set at construction.
                case GENERIC:
                    break;//Do nothing, this is the same as the default buffer.
            }
            renderable.render();
        }
    }

    /**
     * Gets the max age of the particle.  This tries to use the definition's
     * maxAge, but will use Vanilla values if not set.  This should only be
     * called once, as the Vanilla values have a random element that means
     * this function will return different values on each call for them.
     */
    private int generateMaxAge() {
        if (definition.duration != 0) {
            return definition.duration;
        } else {
            switch (definition.type) {
                case SMOKE:
                case BUBBLE:
                case GENERIC:
                    return (int) (8.0D / (Math.random() * 0.8D + 0.2D));
                case FLAME:
                    return (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
                case DRIP:
                    return (int) (64.0D / (Math.random() * 0.8D + 0.2D));
                case BREAK:
                    return (int) (4.0D / (Math.random() * 0.9D + 0.1D));
            }
            //We'll never get here, but it makes the compiler happy.
            return 0;
        }
    }

    /**
     * Gets the current size of the particle.  This parameter
     * is used for the particle's bounding box for collision, and
     * does not necessarily need to take the scale of the particle into account.
     */
    private float getSize() {
        return definition.type.equals(ParticleType.DRIP) || definition.type.equals(ParticleType.BREAK) ? 0.1F : 0.2F;
    }

    /**
     * Gets the current alpha value of the particle.  This parameter
     * is used to make the particle translucent.
     */
    private float getAlpha(float partialTicks) {
        if (definition.transparency != 0) {
            if (definition.toTransparency != 0) {
                return definition.transparency + (definition.toTransparency - definition.transparency) * (age + partialTicks) / maxAge;
            } else {
                return definition.transparency;
            }
        } else {
            return 1.0F;
        }
    }

    /**
     * Gets the current scale of the particle.
     * This is for rendering only; it does not affect collision.
     */
    private float getScale(float partialTicks) {
        if (definition.scale != 0) {
            if (definition.toScale != 0) {
                return definition.scale + (definition.toScale - definition.scale) * (age + partialTicks) / maxAge;
            } else {
                return definition.scale;
            }
        } else {
            switch (definition.type) {
                case FLAME:
                    return (float) (1.0F - Math.pow((age + partialTicks) / maxAge, 2) / 2F);
                case DRIP:
                    return touchingBlocks ? 3.0F : 1.0F;
                default:
                    return 1.0F;
            }
        }
    }

    private void setParticleTextureBounds(int uRow, int vCol) {
        float u;
        float U;
        float v;
        float V;
        if (definition.type.equals(ParticleType.BREAK)) {
            --position.y;
            float[] uvPoints = InterfaceManager.renderingInterface.getBlockBreakTexture(world, position);
            ++position.y;
            u = uvPoints[0];
            U = uvPoints[1];
            v = uvPoints[2];
            V = uvPoints[3];
        } else {
            u = uRow++ / (float) PARTICLES_PER_ROWCOL;
            U = uRow / (float) PARTICLES_PER_ROWCOL;
            v = vCol++ / (float) PARTICLES_PER_ROWCOL;
            V = vCol / (float) PARTICLES_PER_ROWCOL;
        }

        for (int i = 0; i < 6; ++i) {
            switch (i) {
                case (0):
                case (3): {//Bottom-right
                    renderable.vertices.put(i * 8 + 3, U);
                    renderable.vertices.put(i * 8 + 4, V);
                    break;
                }
                case (1): {//Top-right
                    renderable.vertices.put(i * 8 + 3, U);
                    renderable.vertices.put(i * 8 + 4, v);
                    break;
                }
                case (2):
                case (4): {//Top-left
                    renderable.vertices.put(i * 8 + 3, u);
                    renderable.vertices.put(i * 8 + 4, v);
                    break;
                }
                case (5): {//Bottom-left
                    renderable.vertices.put(i * 8 + 3, u);
                    renderable.vertices.put(i * 8 + 4, V);
                    break;
                }
            }
        }
    }

    /**
     * Helper method to generate a standard buffer to be used for all particles as a
     * starting buffer.  Saves computation when creating particles.  Particle is assumed
     * to have a size of 1x1 with UV-pamming of 0->1.
     */
    private static FloatBuffer generateStandardBuffer() {
        FloatBuffer buffer = FloatBuffer.allocate(6 * 8);
        for (int i = 0; i < 6; ++i) {
            //Normal is always 0, 0, 1.
            buffer.put(0);
            buffer.put(0);
            buffer.put(1);
            switch (i) {
                case (0):
                case (3): {//Bottom-right
                    buffer.put(1);
                    buffer.put(1);
                    buffer.put(0.5F);
                    buffer.put(-0.5F);
                    break;
                }
                case (1): {//Top-right
                    buffer.put(1);
                    buffer.put(0);
                    buffer.put(0.5F);
                    buffer.put(0.5F);
                    break;
                }
                case (2):
                case (4): {//Top-left
                    buffer.put(0);
                    buffer.put(0);
                    buffer.put(-0.5F);
                    buffer.put(0.5F);
                    break;
                }
                case (5): {//Bottom-left
                    buffer.put(0);
                    buffer.put(1);
                    buffer.put(-0.5F);
                    buffer.put(-0.5F);
                    break;
                }
            }
            //Z is always 0.
            buffer.put(0);
        }
        buffer.flip();
        return buffer;
    }
}
