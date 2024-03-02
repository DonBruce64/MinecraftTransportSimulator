package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONParticle {
    @JSONRequired
    @JSONDescription("Which type of particle to use.")
    public ParticleType type;

    @JSONDescription("Foces this particle to spawn every tick it is active.  Useful for constant particle flows, like smoke.")
    public boolean spawnEveryTick;

    @JSONDescription("If true, this particle will ignore lighting and will render bright at all times.  Useful for muzzle flashes and sparks.")
    public boolean isBright;

    @JSONDescription("Makes the particle stop all movement when it hits the ground.  This includes rotation.")
    public boolean stopsOnGround;

    @JSONDescription("Makes the particle ignore collision with all blocks.  Can and should be used on particles that don't need collision, since this takes up CPU cycles and can lead to odd behavior on occasion if particles are spawned inside blocks.")
    public boolean ignoreCollision;

    @JSONDescription("A random sound from this list of sounds will play when the particle stops on the ground when stopsOnGround is true.  Format for each entry is [packID:soundName]")
    public List<String> groundSounds;

    @JSONRequired
    @JSONDescription("The orientation this particle spawns about.")
    public ParticleSpawningOrientation spawningOrientation;

    @JSONRequired
    @JSONDescription("The orientation this particle rotates.")
    public ParticleRenderingOrientation renderingOrientation;

    @JSONDescription("How many of this particle to spawn at a time. Defaults to 1.")
    public int quantity;

    @JSONDescription("How far apart individual particles should spawn, in blocks.  Note that this overrides spawing rates and will be checked every frame.  Use sparingly!")
    public float distance;

    @JSONDescription("How long, in ticks, the particle should remain.  If not set on a defined type, the age will be auto-calculated the same way it would be for a Vanilla particle.  Defaults to 200 on any other types.")
    public int duration;

    @JSONDescription("If set, the particle will linearly change its speed from the intialVelocity, to 0, after this many ticks.  If the particle is still present after this, it will not move.  If the particle's duration is less than this value, then the particle will only slow down according to the linear interpolation and will never stop. Note that movementVelocity and terminalVelocity is still applied if applicable, so the velocity may not follow this exact value if those are present.")
    public int movementDuration;

    @JSONDescription("A number between 0.0 and 1.0 describing how transparent the particle should be.  If both this and toTransparency are not set, they are assumed to be 1.0 for both and no transparency changes will be performed.")
    public float transparency;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial transparency to this value.  If transparency is set and non-zero, then this defaults to 0.0.  Othwerise, it is used as-is.")
    public float toTransparency;

    @JSONDescription("The time, in ticks, for the particle to fade out at the end of its lifespan.  Particles fade out by multiplying their defined alpha times the relative time-value of this value.  So if fade-out time is 40, and the particle is 20 ticks from death, alpha will be half of what is defined (20/40).")
    public int fadeTransparencyTime;

    @JSONDescription("How big to spawn each particle.  A value of 1.0 will result in 1 pixel of the particle texture per 1 pixel in-game.  This is the default if this is not set.")
    public float scale;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial scale to this value.  Defaults to 1.0 if this and scale are not set.")
    public float toScale;

    @JSONDescription("The time, in ticks, for the particle to scale out at the end of its lifespan.  Particles scale out by multiplying their defined scale times the relative time-value of this value.  So if fade-out time is 40, and the particle is 20 ticks from death, scale will be half of what is defined (20/40).")
    public int fadeScaleTime;

    @JSONDescription("The size of the hitbox.  Defaults to 0.2 for all particles except break, which are 0.1, if not set.")
    public float hitboxSize;

    @JSONDescription("Normally particles use built-in textures.  However, one can specify a texture sheet to use if they wish.  Format is packID:path/to/texture.")
    public String texture;

    @JSONDescription("Normally particles use a 2D model to render as a flat texture.  However, one can specify a model to use here for full 3D objects.  Format is packID:path/to/model.")
    public String model;

    @JSONDescription("If you want your particle to have multiple textures, you can specify the texture PNG files to use here.  The delay between each texture segment is goverend by the textureDelay variable, if it is set.  If you delay past the last texture, the cycle repeats.")
    public List<String> textureList;

    @JSONDescription("A list of delays between cycling to the next texture.  If the end of this list is reached, the delay sequence will repeat from the start of the list.")
    public List<Integer> textureDelays;

    @JSONDescription("Normally, textureList starts with the first texture.  Setting this true starts from a random spot.  If textureDelays is null, then it'll just pick a random texture and stick with it.  Otherwise, it will cycle as normal.")
    public boolean randomTexture;

    @JSONDescription("If you want your particle to have multiple colors, you can specify the colors to use here.  The delay between each color is goverend by the colorDelay variable, if it is set.  If you delay past the last color, the cycle repeats.")
    public List<ColorRGB> colorList;

    @JSONDescription("A list of delays between cycling to the next color.  If the end of this list is reached, the delay sequence will repeat from the start of the list.")
    public List<Integer> colorDelays;

    @JSONDescription("Normally, colorList starts with the first color.  Setting this true starts from a random spot.  If colorDelays is null, then it'll just pick a random color and stick with it.  Otherwise, it will cycle as normal.")
    public boolean randomColor;

    @JSONDescription("A string in hexadecimal format representing the particle's color.  Defaults to white if not set, which essentially does no color modification.")
    public ColorRGB color;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial color to this value.  Defaults to be the same as the initial color.")
    public ColorRGB toColor;

    @JSONDescription("The position where this particle should be spawned relative to the spawning object.  May be left out if the particle should spawn at the same position.")
    public Point3D pos;

    @JSONDescription("The rotation to rotate this particle to.  Only has an effect for FIXED orientation particles, as do all other rotational parameters.")
    public RotationMatrix rot;

    @JSONDescription("These angles will be randomly added to rot, multipled by a random value between -1 and 1.")
    public Point3D rotationRandomness;

    @JSONDescription("The angles to rotate this particle by every tick.")
    public Point3D rotationVelocity;

    @JSONDescription("This velocity will be randomly added to the initialVelocity, multipled by a random value between -1 and 1.")
    public Point3D spreadRandomness;

    @JSONDescription("The factor of which to inherit the spawning velocity of the thing that is producing it, where +Z is straight ahead relative to the thing that is producing it.  If left out, no inherited velocity is assumed.")
    public Point3D relativeInheritedVelocityFactor;

    @JSONDescription("The initial velocity of the particle, where +Z is straight ahead relative to the thing that is producing it.  May be omitted to make a particle that doesn't spawn with any initial velocity except the velocity of the object spawning it.")
    public Point3D initialVelocity;

    @JSONDescription("The velocity to apply every tick to the particle.  This can be used to make smoke float up, oil drip down, etc.  If this and relativeMovementVelocity is not set, the default particle velocity is used.")
    public Point3D movementVelocity;

    @JSONDescription("The velocity to apply every tick to the particle, relative the the particle itself.  This differs from movementVelocity, which is relative to the world.")
    public Point3D relativeMovementVelocity;

    @JSONDescription("The max velocity this particle can have in any axis.  Used to prevent particles from going to fast if they move a long way.")
    public Point3D terminalVelocity;

    @JSONDescription("This is a list of animatedObjects that can be used to move the spawn position of this particle.")
    public List<JSONAnimationDefinition> spawningAnimations;

    @JSONRequired
    @JSONDescription("A listing of animation objects for determining if this particle should spawn.  Particles will only spawn when they first become active, unless spawnEveryTick is set.")
    public List<JSONAnimationDefinition> activeAnimations;

    @JSONDescription("A list of sub-particles this particle can spawn.  They will be spawned when their conditions are met.  Note that sub-particles do not reference spawningAnimations or activeAnimations.")
    public List<JSONSubParticle> subParticles;

    @Deprecated
    public Point3D velocityVector;

    @Deprecated
    public float spreadFactorHorizontal;

    @Deprecated
    public float spreadFactorVertical;

    @Deprecated
    public boolean axisAligned;

    public static class JSONSubParticle {

        @JSONDescription("The particle to spawn.")
        public JSONParticle particle;

        @JSONDescription("The time, in ticks, at which to spawn the particle.")
        public int time;
    }

    public enum ParticleSpawningOrientation {
        @JSONDescription("Particle spawns relative to the entity that spawned it.")
        ENTITY,
        @JSONDescription("Particle spawns relative to the world and ignores entity orientation.")
        WORLD,
        @JSONDescription("Particle spawns relative to to the block position where the bullet that spawned it hit.  If the particle is asked to spawn by anything but a bullet that hits a block, it will not be spawned.")
        BLOCK;
    }

    public enum ParticleRenderingOrientation {
        @JSONDescription("Particle does not rotate and orients as spawned.")
        FIXED,
        @JSONDescription("Particle rotates to always face the player.")
        PLAYER,
        @JSONDescription("Particle rotates to face the player, but only about the Y-axis.")
        YAXIS,
        @JSONDescription("Particle rotates to face its motion.  Think bullets.")
        MOTION;
    }

    public enum ParticleType {
        @JSONDescription("The standard smoke particle.")
        SMOKE,
        @JSONDescription("The standard (torch) flame particle.")
        FLAME,
        @Deprecated
        DRIP,
        @JSONDescription("The standard bubble particle.")
        BUBBLE,
        @JSONDescription("The standard block breakage particle. The block texture to use will always be the block below this particle when first spawned.")
        BREAK,
        @JSONDescription("A generic particle.  This has no movement by default, so you will have to specify it.")
        GENERIC
    }
}
