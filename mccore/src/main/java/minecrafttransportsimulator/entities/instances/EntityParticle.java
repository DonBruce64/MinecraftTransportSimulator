package minecrafttransportsimulator.entities.instances;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import minecrafttransportsimulator.baseclasses.AnimationSwitchbox;
import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.JSONSubParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleSpawningOrientation;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.RenderableObject;
import minecrafttransportsimulator.sound.SoundInstance;

/**
 * Basic particle class.  This mimic's MC's particle logic, except we can manually set
 * movement logic.
 *
 * @author don_bruce
 */
public class EntityParticle extends AEntityC_Renderable {
    private static final FloatBuffer STANDARD_RENDER_BUFFER = generateStandardBuffer();
    private static final TransformationMatrix helperTransform = new TransformationMatrix();
    private static final RotationMatrix helperRotation = new RotationMatrix();
    private static final Point3D helperPoint = new Point3D();
    private static final Map<String, FloatBuffer> parsedParticleBuffers = new HashMap<>();
    private static final Random particleRandom = new Random();

    //Constant properties.
    private final AEntityC_Renderable entitySpawning;
    private final JSONParticle definition;
    private final boolean textureIsTranslucent;
    private final int maxAge;
    private final Point3D initialVelocity;
    private final IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();

    private ColorRGB startColor;
    private ColorRGB endColor;
    private final ColorRGB staticColor;
    private final RenderableObject renderable;

    //Runtime variables.
    private boolean touchingBlocks;
    private float timeOfNextTexture;
    private int textureIndex;
    private int textureDelayIndex;
    private List<String> textureList;

    private int timeOfCurrentColor;
    private int timeOfNextColor;
    private int colorIndex;
    private int colorDelayIndex;

    public EntityParticle(AEntityC_Renderable entitySpawning, JSONParticle definition, Point3D spawingPosition, AnimationSwitchbox switchbox) {
        super(entitySpawning.world, spawingPosition, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);

        helperTransform.resetTransforms();
        if (definition.spawningOrientation == ParticleSpawningOrientation.ENTITY) {
            orientation.set(entitySpawning.orientation);
            helperTransform.set(entitySpawning.orientation);
        }
        if (switchbox != null) {
            helperTransform.multiply(switchbox.netMatrix);
        }

        if (definition.rot != null) {
            orientation.multiply(definition.rot);
        }
        if (definition.rotationRandomness != null) {
            helperPoint.set(definition.rotationRandomness);
            helperPoint.x = (2 * Math.random() - 1) * helperPoint.x;
            helperPoint.y = (2 * Math.random() - 1) * helperPoint.y;
            helperPoint.z = (2 * Math.random() - 1) * helperPoint.z;
            helperRotation.setToAngles(helperPoint);
            orientation.multiply(helperRotation);
        }
        prevOrientation.set(orientation);

        if (definition.pos != null) {
            helperPoint.set(definition.pos).multiply(entitySpawning.scale);
        } else {
            helperPoint.set(0, 0, 0);
        }
        helperPoint.transform(helperTransform);
        position.add(helperPoint);
        if (definition.initialVelocity != null) {
            if (definition.spreadRandomness != null) {
                motion.x = 2 * definition.spreadRandomness.x * Math.random() - definition.spreadRandomness.x;
                motion.y = 2 * definition.spreadRandomness.y * Math.random() - definition.spreadRandomness.y;
                motion.z = 2 * definition.spreadRandomness.z * Math.random() - definition.spreadRandomness.z;
                motion.add(definition.initialVelocity);
            } else {
                //Add some basic randomness so particles don't all go in a line.
                motion.x = definition.initialVelocity.x + 0.2 - Math.random() * 0.4;
                motion.y = definition.initialVelocity.y + 0.2 - Math.random() * 0.4;
                motion.z = definition.initialVelocity.z + 0.2 - Math.random() * 0.4;
            }
            //Scale down by 10 since most of the time we go too fast.
            motion.scale(1D / 10D);
            motion.rotate(helperTransform);
        }
        if (definition.relativeInheritedVelocityFactor != null) {
            helperRotation.setToVector(entitySpawning.motion, true);
            helperPoint.set(entitySpawning.motion);
            if (entitySpawning instanceof EntityVehicleF_Physics) {
                helperPoint.scale(((EntityVehicleF_Physics) entitySpawning).speedFactor);
            } else if (entitySpawning instanceof APart) {
                APart partSpawning = (APart) entitySpawning;
                if (partSpawning.vehicleOn != null) {
                    helperPoint.scale(partSpawning.vehicleOn.speedFactor);
                }
            }
            helperPoint.reOrigin(helperRotation).multiply(definition.relativeInheritedVelocityFactor).rotate(helperRotation);
            motion.add(helperPoint);
        }
        initialVelocity = motion.copy();

        this.entitySpawning = entitySpawning;
        this.definition = definition;
        boundingBox.widthRadius = definition.hitboxSize / 2D;
        boundingBox.heightRadius = boundingBox.widthRadius;
        boundingBox.depthRadius = boundingBox.widthRadius;
        this.maxAge = generateMaxAge();
        if (definition.color != null) {
            if (definition.toColor != null) {
                this.startColor = definition.color;
                this.endColor = definition.toColor;
                this.timeOfNextColor = maxAge;
                this.staticColor = null;
            } else {
                this.staticColor = definition.color;
            }
        } else {
            if (definition.colorList != null) {
                if (definition.randomColor) {
                    colorIndex = particleRandom.nextInt(definition.colorList.size());
                }
                startColor = definition.colorList.get(colorIndex);
                if (colorIndex + 1 < definition.colorList.size()) {
                    endColor = definition.colorList.get(colorIndex + 1);
                } else {
                    endColor = definition.colorList.get(0);
                }

                if (definition.colorDelays != null) {
                    timeOfNextColor = definition.colorDelays.get(colorDelayIndex);
                } else {
                    timeOfNextColor = maxAge;
                }
                this.staticColor = null;
            } else {
                this.staticColor = ColorRGB.WHITE;
            }
        }

        final String texture;
        if (definition.texture != null) {
            texture = definition.texture;
        } else if (definition.type == ParticleType.BREAK) {
            texture = RenderableObject.GLOBAL_TEXTURE_NAME;
        } else if (definition.type == ParticleType.SMOKE) {
            textureList = new ArrayList<String>();
            for (int i = 0; i <= 11; ++i) {
                textureList.add("mts:textures/particles/big_smoke_" + i + ".png");
            }
            texture = textureList.get(0);
            timeOfNextTexture = (int) (maxAge / 12F);
        } else if (definition.textureList != null) {
            //Set initial texture delay and texture.
            textureList = definition.textureList;
            if (definition.randomTexture) {
                textureIndex = particleRandom.nextInt(textureList.size());
            }
            texture = textureList.get(textureIndex);
            if (definition.textureDelays != null) {
                timeOfNextTexture = definition.textureDelays.get(textureDelayIndex);
            } else {
                timeOfNextTexture = maxAge;
            }
        } else if (definition.type == ParticleType.SMOKE) {
            textureList = new ArrayList<String>();
            for (int i = 0; i <= 11; ++i) {
                textureList.add("mts:textures/particles/big_smoke_" + i + ".png");
            }
            texture = textureList.get(0);
            timeOfNextTexture = maxAge / 12F;
        } else {
            texture = "mts:textures/particles/" + definition.type.name().toLowerCase(Locale.ROOT) + ".png";
        }
        this.textureIsTranslucent = texture.toLowerCase(Locale.ROOT).contains(AModelParser.TRANSLUCENT_OBJECT_NAME);

        FloatBuffer buffer;
        if (definition.model != null) {
            FloatBuffer totalModel = parsedParticleBuffers.get(definition.model);
            if (totalModel == null) {
                String modelDomain = definition.model.substring(0, definition.model.indexOf(':'));
                String modelPath = definition.model.substring(modelDomain.length() + 1);
                List<RenderableObject> parsedObjects = AModelParser.parseModel("/assets/" + modelDomain + "/" + modelPath);
                int totalVertices = 0;
                for (RenderableObject parsedObject : parsedObjects) {
                    totalVertices += parsedObject.vertices.capacity();
                }
                totalModel = FloatBuffer.allocate(totalVertices);
                for (RenderableObject parsedObject : parsedObjects) {
                    totalModel.put(parsedObject.vertices);
                }
                totalModel.flip();
                parsedParticleBuffers.put(definition.model, totalModel);
            }
            buffer = totalModel;
        } else {
            buffer = FloatBuffer.allocate(STANDARD_RENDER_BUFFER.capacity());
            buffer.put(STANDARD_RENDER_BUFFER);
            STANDARD_RENDER_BUFFER.rewind();
            buffer.flip();
        }
        this.renderable = new RenderableObject("particle", texture, staticColor != null ? staticColor : new ColorRGB(), buffer, false);
        if (definition.transparency != 0 || definition.toTransparency != 0) {
            renderable.alpha = definition.transparency;
        }

        renderable.disableLighting = definition.type.equals(ParticleType.FLAME) || definition.isBright;
        renderable.ignoreWorldShading = definition.model == null || definition.isBright;
        if (definition.type == ParticleType.BREAK) {
            if (world.isAir(position)) {
                //Don't spawn break particles in the air, they're null textures.
                remove();
                return;
            } else {
                float[] uvPoints = InterfaceManager.renderingInterface.getBlockBreakTexture(world, position);
                setParticleTextureBounds(uvPoints[0], uvPoints[1], uvPoints[2], uvPoints[3]);
            }
        } else if (definition.model == null) {
            setParticleTextureBounds(0, 1, 0, 1);
        }
        updateOrientation();
    }

    @Override
    public void update() {
        super.update();
        //Check age to see if we are on our last tick.
        if (ticksExisted == maxAge) {
            remove();
            return;
        }

        //Set movement.
        if (!definition.stopsOnGround || !touchingBlocks) {
            if (definition.movementDuration != 0) {
                if (ticksExisted <= definition.movementDuration) {
                    Point3D velocityLastTick = initialVelocity.copy().scale((definition.movementDuration - (ticksExisted - 1)) / (float) definition.movementDuration);
                    Point3D velocityThisTick = initialVelocity.copy().scale((definition.movementDuration - ticksExisted) / (float) definition.movementDuration);
                    motion.add(velocityThisTick).subtract(velocityLastTick);
                }
            }

            if (definition.movementVelocity != null) {
                motion.add(definition.movementVelocity);
            }
            if (definition.relativeMovementVelocity != null) {
                helperRotation.setToVector(motion, true);
                helperPoint.set(definition.relativeMovementVelocity).rotate(helperRotation);
                motion.add(helperPoint);
            }
            if (definition.movementVelocity == null && definition.relativeMovementVelocity == null) {
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

            //Check collision movement.  If we hit a block, don't move.
            if (!definition.ignoreCollision) {
                touchingBlocks = boundingBox.updateCollisions(world, motion, true);
                if (touchingBlocks) {
                    motion.subtract(boundingBox.currentCollisionDepth);
                    if (definition.stopsOnGround && definition.groundSounds != null) {
                        double distance = position.distanceTo(clientPlayer.getPosition());
                        if (distance < SoundInstance.DEFAULT_MAX_DISTANCE) {
                            SoundInstance sound = new SoundInstance(this, definition.groundSounds.get(particleRandom.nextInt(definition.groundSounds.size())));
                            sound.volume = (float) (1 - distance / SoundInstance.DEFAULT_MAX_DISTANCE);
                            InterfaceManager.soundInterface.playQuickSound(sound);
                        }
                    }
                }
            }
            position.add(motion);

            //Update orientation.
            updateOrientation();
            if (definition.rotationVelocity != null) {
                helperRotation.setToAngles(definition.rotationVelocity);
                orientation.multiply(helperRotation);
            }
        }

        //Check if we need to change textures or colors.
        if (textureList != null && timeOfNextTexture <= ticksExisted) {
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
        if (definition.colorDelays != null && timeOfNextColor == ticksExisted) {
            if (++colorIndex == definition.colorList.size()) {
                colorIndex = 0;
            }
            startColor = definition.colorList.get(colorIndex);
            if (colorIndex + 1 < definition.colorList.size()) {
                endColor = definition.colorList.get(colorIndex + 1);
            } else {
                endColor = definition.colorList.get(0);
            }

            if (++colorDelayIndex == definition.colorDelays.size()) {
                colorDelayIndex = 0;
            }
            timeOfCurrentColor = timeOfNextColor;
            timeOfNextColor += definition.colorDelays.get(colorDelayIndex);
        }

        //Check for sub particles.
        if (definition.subParticles != null) {
            for (JSONSubParticle subDef : definition.subParticles) {
                if (subDef.particle.spawnEveryTick ? subDef.time >= ticksExisted : subDef.time == ticksExisted) {
                    world.addEntity(new EntityParticle(this, subDef.particle, position, null));
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
        if (definition.toTransparency != 0) {
            renderable.alpha = interpolate(definition.transparency, definition.toTransparency, (ticksExisted + partialTicks) / maxAge, true, partialTicks);
        } else {
            renderable.alpha = definition.transparency != 0 ? definition.transparency : 1.0F;
        }
        if (definition.fadeTransparencyTime > maxAge - ticksExisted) {
            renderable.alpha *= (maxAge - ticksExisted) / (float) definition.fadeTransparencyTime;
        }
        if (!((definition.model == null || textureIsTranslucent || renderable.alpha < 1.0) ^ blendingEnabled)) {
            renderable.isTranslucent = blendingEnabled;
            if (staticColor == null) {
                float colorDelta = (ticksExisted + partialTicks - timeOfCurrentColor) / (timeOfNextColor - timeOfCurrentColor);
                renderable.color.red = interpolate(startColor.red, endColor.red, colorDelta, true, partialTicks);
                renderable.color.green = interpolate(startColor.green, endColor.green, colorDelta, true, partialTicks);
                renderable.color.blue = interpolate(startColor.blue, endColor.blue, colorDelta, true, partialTicks);
            }
            renderable.transform.set(transform);
            double totalScale;
            if (definition.type == ParticleType.FLAME && definition.scale == 0 && definition.toScale == 0) {
                totalScale = 1.0F - Math.pow((ticksExisted + partialTicks) / maxAge, 2) / 2F;
            } else if (definition.toScale != 0) {
                totalScale = interpolate(definition.scale, definition.toScale, (ticksExisted + partialTicks) / maxAge, false, partialTicks);
            } else if (definition.scale != 0) {
                totalScale = definition.scale;
            } else {
                totalScale = 1.0;
            }
            if (definition.fadeScaleTime > maxAge - ticksExisted) {
                totalScale *= (maxAge - ticksExisted) / (float) definition.fadeScaleTime;
            }
            renderable.transform.applyScaling(totalScale * entitySpawning.scale.x, totalScale * entitySpawning.scale.y, totalScale * entitySpawning.scale.z);
            renderable.worldLightValue = worldLightValue;
            renderable.render();
        }
    }

    private void updateOrientation() {
        switch (definition.renderingOrientation) {
            case FIXED: {
                //No update since we never change.
                break;
            }
            case PLAYER: {
                helperPoint.set(clientPlayer.getEyePosition()).subtract(position);
                orientation.setToVector(helperPoint, true);
                break;
            }
            case YAXIS: {
                helperPoint.set(clientPlayer.getEyePosition()).subtract(position);
                helperPoint.y = 0;
                orientation.setToVector(helperPoint, true);
                break;
            }
            case MOTION: {
                orientation.setToVector(motion, true);
                break;
            }
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

    private float interpolate(float start, float end, float factor, boolean clamp, float partialTicks) {
        float value = start + (end - start) * factor;
        return clamp ? value > 1.0F ? 1.0F : (value < 0.0F ? 0.0F : value) : value;
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
