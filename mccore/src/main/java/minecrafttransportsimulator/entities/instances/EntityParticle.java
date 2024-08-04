package minecrafttransportsimulator.entities.instances;

import java.nio.Buffer;
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
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityC_Renderable;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.JSONSubParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleSpawningOrientation;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.RenderableData;
import minecrafttransportsimulator.rendering.RenderableData.LightingMode;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Basic particle class.  This mimic's MC's particle logic, except we can manually set
 * movement logic.
 *
 * @author don_bruce
 */
public class EntityParticle extends AEntityC_Renderable {
    private static final RenderableVertices STANDARD_PARTICLE_SPRITE = RenderableVertices.createSprite(1, null, null);
    private static final TransformationMatrix helperTransform = new TransformationMatrix();
    private static final RotationMatrix helperRotation = new RotationMatrix();
    private static final Point3D helperPoint = new Point3D();
    private static final ColorRGB helperColor = new ColorRGB();
    private static final Map<String, RenderableVertices> parsedParticleModels = new HashMap<>();
    private static final Random particleRandom = new Random();

    //Constant properties.
    private final AEntityC_Renderable entitySpawning;
    private final AnimationSwitchbox spawningSwitchbox;
    private final JSONParticle definition;
    private final int maxAge;
    private final Point3D initialVelocity;
    private final IWrapperPlayer clientPlayer = InterfaceManager.clientInterface.getClientPlayer();

    private ColorRGB startColor;
    private ColorRGB endColor;
    private final ColorRGB staticColor;
    private final String model;
    private final RenderableData renderable;

    //Runtime variables.
    private final boolean killBadParticle;
    private boolean touchingBlocks;
    private float timeOfNextTexture;
    private int textureIndex;
    private int textureDelayIndex;
    private List<String> textureList;

    private int timeOfCurrentColor;
    private int timeOfNextColor;
    private int colorIndex;
    private int colorDelayIndex;

    public EntityParticle(AEntityC_Renderable entitySpawning, JSONParticle definition, Point3D spawingPosition, AnimationSwitchbox spawningSwitchbox) {
        super(entitySpawning.world, spawingPosition, ZERO_FOR_CONSTRUCTOR, ZERO_FOR_CONSTRUCTOR);
        this.entitySpawning = entitySpawning;
        this.definition = definition;
        this.spawningSwitchbox = spawningSwitchbox;
        this.maxAge = generateMaxAge();
        boundingBox.widthRadius = definition.hitboxSize / 2D;
        boundingBox.heightRadius = boundingBox.widthRadius;
        boundingBox.depthRadius = boundingBox.widthRadius;

        //Set transforms based on type.
        helperTransform.resetTransforms();
        switch (definition.spawningOrientation) {
            case ENTITY:
            case ATTACHED: {
                orientation.set(entitySpawning.orientation);
                helperTransform.set(orientation);
                break;
            }
            case FACING: {
                if (entitySpawning instanceof EntityBullet) {
                    EntityBullet bullet = (EntityBullet) entitySpawning;
                    if (bullet.sideHit != Axis.NONE) {
                        helperRotation.setToZero().rotateX(-90);
                        orientation.set(bullet.sideHit.facingRotation).multiplyTranspose(helperRotation);
                        helperTransform.set(orientation);
                    } else {
                        //Nothing for bullet to hit, block spawning.
                        this.initialVelocity = null;
                        this.staticColor = null;
                        this.renderable = null;
                        this.model = null;
                        this.killBadParticle = true;
                        return;
                    }
                }
                break;
            }
            case WORLD: {
                //Do nothing, world doesn't touch position/orientation.
                break;
            }
        }

        //Set position.
        setPositionToSpawn();
        prevPosition.set(position);

        //Now that position is set, check to make sure we aren't an invalid particle.
        if (definition.type == ParticleType.BREAK) {
            if (world.isAir(position)) {
                //Don't spawn break particles in the air, they're null textures.
                this.staticColor = null;
                this.renderable = null;
                this.model = null;
                this.initialVelocity = null;
                this.killBadParticle = true;
                return;
            }
        }

        //Get block position for particle properties.  This changes from our actual position to calculated depending on properties.
        Point3D blockCheckPosition;
        if (definition.getBlockPropertiesFromGround) {
            //Center of block for safety of FPEs.
            blockCheckPosition = position.copy().add(0, -world.getHeight(position) - 0.5, 0);
        } else {
            //Use spawning position here since block properties for particles are usually from bullets, which are slightly in the block.
            blockCheckPosition = spawingPosition;
        }

        //Set orientation.
        setOrientationToSpawn();
        prevOrientation.set(orientation);

        //Get initial motion.
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
            helperPoint.set(entitySpawning.motion);
            if (entitySpawning instanceof EntityVehicleF_Physics) {
                helperPoint.scale(((EntityVehicleF_Physics) entitySpawning).speedFactor);
            } else if (entitySpawning instanceof APart) {
                APart partSpawning = (APart) entitySpawning;
                if (partSpawning.vehicleOn != null) {
                    helperPoint.scale(partSpawning.vehicleOn.speedFactor);
                }
            }
            helperRotation.setToVector(entitySpawning.motion, true);
            helperPoint.reOrigin(helperRotation).multiply(definition.relativeInheritedVelocityFactor).rotate(helperRotation);
            motion.add(helperPoint);
        }
        initialVelocity = motion.copy();
        updateOrientation();

        //Set model and texture.
        String model = definition.model;
        final String texture;
        if (definition.texture != null) {
            texture = definition.texture;
        } else if (definition.type == ParticleType.BREAK) {
            texture = RenderableData.GLOBAL_TEXTURE_NAME;
        } else if (definition.type == ParticleType.CASING) {
            texture = ((PartGun) entitySpawning).lastLoadedBullet.definition.bullet.casingTexture;
            model = ((PartGun) entitySpawning).lastLoadedBullet.definition.bullet.casingModel;
            if (texture == null) {
                //Not supposed to be spawning any casings for this bullet.
                this.staticColor = null;
                this.renderable = null;
                this.model = null;
                this.killBadParticle = true;
                return;
            }
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

        this.model = model;
        if (this.model != null) {
            RenderableVertices parsedModel = parsedParticleModels.computeIfAbsent(this.model, k -> {
                String modelDomain = this.model.substring(0, this.model.indexOf(':'));
                String modelPath = this.model.substring(modelDomain.length() + 1);
                List<RenderableVertices> parsedObjects = AModelParser.parseModel("/assets/" + modelDomain + "/" + modelPath, true);
                int totalVertices = 0;
                for (RenderableVertices parsedObject : parsedObjects) {
                    totalVertices += parsedObject.vertices.capacity();
                }
                FloatBuffer totalBuffer = FloatBuffer.allocate(totalVertices);
                for (RenderableVertices parsedObject : parsedObjects) {
                    totalBuffer.put(parsedObject.vertices);
                }
                ((Buffer) totalBuffer).flip();
                return new RenderableVertices("PARTICLE_3D", totalBuffer, false);
            });
            this.renderable = new RenderableData(parsedModel, texture);
        } else if (definition.type == ParticleType.BREAK) {
            //Need to generate a new vertex buffer since break particles have varying UVs.
            RenderableVertices vertexObject = RenderableVertices.createSprite(1, null, null);
            this.renderable = new RenderableData(vertexObject, texture);
            float[] uvPoints = InterfaceManager.renderingInterface.getBlockBreakTexture(world, blockCheckPosition);
            vertexObject.setTextureBounds(uvPoints[0], uvPoints[1], uvPoints[2], uvPoints[3]);
        } else {
            //Basic particle, use standard buffer.
            this.renderable = new RenderableData(STANDARD_PARTICLE_SPRITE, RenderableData.GLOBAL_TEXTURE_NAME);
        }
        renderable.setTexture(texture);

        //Set color.
        if(definition.useBlockColor) {
        	this.staticColor = world.getBlockColor(blockCheckPosition);
        }else if (definition.color != null) {
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
        if(staticColor != null) {
            renderable.setColor(staticColor);
        }

        //Set alpha.
        if (definition.transparency != 0 || definition.toTransparency != 0) {
            renderable.setAlpha(definition.transparency);
        }

        //Set lighting mode.
        if (definition.type.equals(ParticleType.FLAME) || definition.isBright) {
            renderable.setLightMode(LightingMode.IGNORE_ALL_LIGHTING);
        } else if (model == null) {
            renderable.setLightMode(LightingMode.IGNORE_ORIENTATION_LIGHTING);
        }
        if (definition.isBlended) {
            renderable.setBlending(ConfigSystem.client.renderingSettings.blendedLights.value);
        }

        this.killBadParticle = false;
    }

    private void setPositionToSpawn() {
        //Apply transforms to get position.
        if (definition.pos != null) {
            helperPoint.set(definition.pos).multiply(entitySpawning.scale);
        } else {
            helperPoint.set(0, 0, 0);
        }
        if (spawningSwitchbox != null) {
            spawningSwitchbox.runSwitchbox(0, false);
            helperTransform.multiply(spawningSwitchbox.netMatrix);
        }
        helperPoint.transform(helperTransform);
        position.add(helperPoint);
    }

    private void setOrientationToSpawn() {
        //Apply transforms to get orientation.
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
    }

    @Override
    public EntityAutoUpdateTime getUpdateTime() {
        //Sync with our spawning entity in case we depend on their variables.
        if (entitySpawning instanceof APart) {
            return ((APart) entitySpawning).masterEntity.getUpdateTime();
        } else {
            return entitySpawning.getUpdateTime();
        }
    }

    @Override
    public void update() {
        super.update();
        //Check age to see if we are on our last tick or if we're a bad particle.
        if (ticksExisted == maxAge || killBadParticle) {
            remove();
            return;
        }

        //Set movement.
        if (!definition.stopsOnGround || !touchingBlocks) {
            if(definition.spawningOrientation == ParticleSpawningOrientation.ATTACHED) {
                position.set(entitySpawning.position);
                orientation.set(entitySpawning.orientation);
                helperTransform.set(orientation);
                setPositionToSpawn();
                setOrientationToSpawn();
            }
            
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
        //First set alpha, then check translucent.
        //We could change it this update cycle.
        if (definition.toTransparency != 0) {
            renderable.setAlpha(interpolate(definition.transparency, definition.toTransparency, (ticksExisted + partialTicks) / maxAge, true, partialTicks));
        } else {
            renderable.setAlpha(definition.transparency != 0 ? definition.transparency : 1.0F);
        }
        if (definition.fadeTransparencyTime > maxAge - ticksExisted) {
            renderable.setAlpha(renderable.alpha * (maxAge - ticksExisted) / definition.fadeTransparencyTime);
        }

        if (renderable.isTranslucent == blendingEnabled) {
            if (staticColor == null) {
                float colorDelta = (ticksExisted + partialTicks - timeOfCurrentColor) / (timeOfNextColor - timeOfCurrentColor);
                helperColor.red = interpolate(startColor.red, endColor.red, colorDelta, true, partialTicks);
                helperColor.green = interpolate(startColor.green, endColor.green, colorDelta, true, partialTicks);
                helperColor.blue = interpolate(startColor.blue, endColor.blue, colorDelta, true, partialTicks);
                renderable.setColor(helperColor);
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
            renderable.setLightValue(worldLightValue);
            renderable.render();
        }
    }

    @Override
    public boolean disableRendering() {
        return killBadParticle || super.disableRendering();
    }

    private void updateOrientation() {
        switch (definition.renderingOrientation) {
            case FIXED: {
                //No update since we never change.
                break;
            }
            case PLAYER: {
                helperPoint.set(InterfaceManager.clientInterface.getCameraPosition()).subtract(position);
                orientation.setToVector(helperPoint, true);
                break;
            }
            case YAXIS: {
                helperPoint.set(InterfaceManager.clientInterface.getCameraPosition()).subtract(position);
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
}
