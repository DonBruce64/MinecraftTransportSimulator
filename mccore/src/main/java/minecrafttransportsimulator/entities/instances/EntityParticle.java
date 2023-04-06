package minecrafttransportsimulator.entities.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.JSONSubParticle;
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
    private static final TransformationMatrix helperTransform = new TransformationMatrix();
    private static final Point3D helperOffset = new Point3D();

    //Constant properties.
    private final AEntityC_Renderable entitySpawning;
    private final JSONParticle definition;
    private final int maxAge;
    private final Point3D initialVelocity;
    private final IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();

    private final ColorRGB startColor;
    private final ColorRGB endColor;
    private final ColorRGB staticColor;
    private final RenderableObject renderable;

    //Runtime variables.
    private boolean touchingBlocks;
    private float timeOfNextTexture;
    private int textureIndex;
    private int textureDelayIndex;
    private List<String> textureList;

    public EntityParticle(AEntityC_Renderable entitySpawning, JSONParticle definition) {
        super(entitySpawning.world, entitySpawning.position, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);

        if (definition.axisAligned) {
            orientation.set(entitySpawning.orientation);
            orientation.multiply(definition.rot);
            prevOrientation.set(orientation);
        }

        helperTransform.resetTransforms().set(entitySpawning.orientation);
        if (definition.pos != null) {
            helperOffset.set(definition.pos).multiply(entitySpawning.scale);
        } else {
            helperOffset.set(0, 0, 0);
        }
        helperOffset.transform(helperTransform);
        position.add(helperOffset);

        if (definition.initialVelocity != null) {
            Point3D adjustedVelocity = definition.initialVelocity.copy();
            adjustedVelocity.y += (Math.random() - 0.5F) * definition.spreadFactorVertical;
            adjustedVelocity.rotate(helperTransform);

            if (definition.spreadFactorHorizontal != 0) {
                motion.add(adjustedVelocity).scale(1D / 10D);
                RotationMatrix spreadRotation = new RotationMatrix();
                spreadRotation.angles.set((Math.random() - 0.5F) * definition.spreadFactorHorizontal, (Math.random() - 0.5F) * definition.spreadFactorHorizontal, 0);
                motion.rotate(spreadRotation);
            } else {
                //Add some basic randomness so particles don't all go in a line.
                motion.x += adjustedVelocity.x / 10D + 0.02 - Math.random() * 0.04;
                motion.y += adjustedVelocity.y / 10D + 0.02 - Math.random() * 0.04;
                motion.z += adjustedVelocity.z / 10D + 0.02 - Math.random() * 0.04;
            }
        }
        initialVelocity = motion.copy();

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
        final String texture;
        if (definition.texture != null) {
            texture = definition.texture;
        } else if (definition.type == ParticleType.BREAK) {
            texture = RenderableObject.GLOBAL_TEXTURE_NAME;
        } else if (definition.textureList != null) {
            //Set initial texture delay and texture.
            if (definition.randomTexture) {
                textureIndex = new Random().nextInt(definition.textureList.size());
            }
            textureList = definition.textureList;
            texture = textureList.get(textureIndex);
            if (definition.textureDelays != null) {
                timeOfNextTexture = definition.textureDelays.get(textureDelayIndex);
            }
        } else {
            if (definition.type == ParticleType.SMOKE) {
                textureList = new ArrayList<String>();
                for (int i = 0; i <= 11; ++i) {
                    textureList.add("mts:textures/particles/big_smoke_" + i + ".png");
                }
                texture = textureList.get(0);
                timeOfNextTexture = maxAge / 12F;
            } else {
                texture = "mts:textures/particles/" + definition.type.name().toLowerCase() + ".png";
            }
        }
        this.renderable = new RenderableObject("particle", texture, staticColor != null ? staticColor : new ColorRGB(), buffer, false);

        renderable.disableLighting = definition.type.equals(ParticleType.FLAME) || definition.isBright;
        renderable.ignoreWorldShading = true;
        renderable.isTranslucent = true;
        if (definition.type == ParticleType.BREAK) {
            float[] uvPoints = InterfaceManager.renderingInterface.getBlockBreakTexture(world, position);
            setParticleTextureBounds(uvPoints[0], uvPoints[1], uvPoints[2], uvPoints[3]);
        } else {
            setParticleTextureBounds(0, 1, 0, 1);
        }
    }

    @Override
    public void update() {
        super.update();
        //Set movement.
        if (definition.movementDuration != 0) {
            if (ticksExisted <= definition.movementDuration) {
                Point3D velocityLastTick = initialVelocity.copy().scale((definition.movementDuration - (ticksExisted - 1)) / (float) definition.movementDuration);
                Point3D velocityThisTick = initialVelocity.copy().scale((definition.movementDuration - ticksExisted) / (float) definition.movementDuration);
                motion.add(velocityThisTick).subtract(velocityLastTick);
            }
        }

        if (definition.movementVelocity != null) {
            motion.add(definition.movementVelocity);
            if (definition.terminalVelocity != null) {
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
                default: {
                    //No default movement for generic particles.
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
        if (ticksExisted == maxAge) {
            remove();
        }

        //Update bounds as we might have changed size.
        boundingBox.widthRadius = getSize() / 2D;
        boundingBox.heightRadius = boundingBox.widthRadius;
        boundingBox.depthRadius = boundingBox.widthRadius;

        //Update orientation to always face the player.
        if (!definition.axisAligned) {
            orientation.setToVector(clientPlayer.getEyePosition().copy().subtract(position), true);
        }

        //Check if we need to change textures.
        if (definition.textureDelays != null && timeOfNextTexture <= ticksExisted) {
            if (++textureIndex == textureList.size()) {
                textureIndex = 0;
            }
            renderable.texture = textureList.get(textureIndex);
            if (definition.textureDelays != null) {
                if (++textureDelayIndex == definition.textureDelays.size()) {
                    textureDelayIndex = 0;
                }
                timeOfNextTexture += definition.textureDelays.get(textureDelayIndex);
            } else {
                //Assume internal smoke, so use constant delay.
                timeOfNextTexture += maxAge / 12F;
            }
        }

        //Check for sub particles.
        if (definition.subParticles != null) {
            for (JSONSubParticle subDef : definition.subParticles) {
                if (subDef.particle.spawnEveryTick ? subDef.time >= ticksExisted : subDef.time == ticksExisted) {
                    world.addEntity(new EntityParticle(this, subDef.particle));
                }
            }
        }
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
                renderable.color.red = interpolate(startColor.red, endColor.red, true, partialTicks);
                renderable.color.green = interpolate(startColor.green, endColor.green, true, partialTicks);
                renderable.color.blue = interpolate(startColor.blue, endColor.blue, true, partialTicks);
            }
            renderable.alpha = interpolate(definition.transparency, definition.toTransparency, true, partialTicks);
            renderable.transform.set(transform);
            float scale = (definition.type == ParticleType.FLAME && definition.scale == 0 && definition.toScale == 0) ? (float) (1.0F - Math.pow((ticksExisted + partialTicks) / maxAge, 2) / 2F) : interpolate(definition.scale, definition.toScale, false, partialTicks);
            double totalScale = getSize() * scale;
            renderable.transform.applyScaling(totalScale * entitySpawning.scale.x, totalScale * entitySpawning.scale.y, totalScale * entitySpawning.scale.z);
            renderable.worldLightValue = worldLightValue;
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
                    return 33;
                case BUBBLE:
                case FLAME:
                    return (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
                case BREAK:
                    return (int) (4.0D / (Math.random() * 0.9D + 0.1D));
                default://Generic
                    return (int) (8.0D / (Math.random() * 0.8D + 0.2D));
            }
        }
    }

    /**
     * Gets the current size of the particle.  This parameter
     * is used for the particle's bounding box for collision, and
     * does not necessarily need to take the scale of the particle into account.
     */
    private float getSize() {
        return definition.type.equals(ParticleType.BREAK) ? 0.1F : 0.2F;
    }

    private float interpolate(float start, float end, boolean clamp, float partialTicks) {
        if (start != 0) {
            if (end != 0) {
                float value = start + (end - start) * (ticksExisted + partialTicks) / maxAge;
                return clamp ? value > 1.0F ? 1.0F : (value < 0.0F ? 0.0F : value) : value;
            } else {
                return start;
            }
        } else {
            return 1.0F;
        }
    }

    private void setParticleTextureBounds(float u, float U, float v, float V) {
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
     * to have a size of 1x1 with UV-spanning 0->1.
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
