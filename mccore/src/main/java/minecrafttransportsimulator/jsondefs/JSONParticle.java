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

    @JSONDescription("Normally, particles align themselves to face the player.  Set this to true if you want them to not do this and just be aligned as spawned.")
    public boolean axisAligned;

    @JSONDescription("Normally, textureList starts with the first texture.  Setting this true starts from a random spot.  If textureDelays is null, then it'll just pick a random texture and stick with it.  Otherwise, it will cycle as normal.")
    public boolean randomTexture;

    @JSONDescription("How many of this particle to spawn at a time. Defaults to 1.")
    public int quantity;

    @JSONDescription("How long, in ticks, the particle should remain.  If not set on a defined type, the age will be auto-calculated the same way it would be for a Vanilla particle.  Defaults to 200 on any other types.")
    public int duration;

    @JSONDescription("If set, the particle will linearly change its speed from the intialVelocity, to 0, after this many ticks.  If the particle is still present after this, it will not move.  If the particle's duration is less than this value, then the particle will only slow down according to the linear interpolation and will never stop. Note that movementVelocity and terminalVelocity is still applied if applicable, so the velocity may not follow this exact value if those are present.")
    public int movementDuration;

    @JSONDescription("A number between 0.0 and 1.0 describing how transparent the particle should be.  If both this and toTransparency are not set, they are assumed to be 1.0 for both and no transparency changes will be performed.")
    public float transparency;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial transparency to this value.  If transparency is set and non-zero, then this defaults to 0.0.  Othwerise, it is used as-is.")
    public float toTransparency;

    @JSONDescription("How big to spawn each particle.  A value of 1.0 will result in 1 pixel of the particle texture per 1 pixel in-game.  This is the default if this is not set.")
    public float scale;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial scale to this value.  Defaults to 1.0 if this and scale are not set.")
    public float toScale;

    @JSONDescription("How much off-zero the particle will be when spawned, in the XZ plane.  0 is no spread, higher values have higher spread.  This is per-particle, so if quantity is 10 they will all have different spread.")
    public float spreadFactorHorizontal;

    @JSONDescription("Like spreadFactorHorizontal, just for the vertical (Y) component.")
    public float spreadFactorVertical;

    @JSONDescription("Normally particles use built-in textures.  However, one can specify a texture sheet to use if they wish.  Format is packID:path/to/texture.")
    public String texture;

    @JSONDescription("If you want your particle to have multiple textures, you can specify the texture PNG files to use here.  The delay between each texture segment is goverend by the textureDelay variable, if it is set.  If you delay past the last texture, the cycle repeats.")
    public List<String> textureList;

    @JSONDescription("A list of delays between cycling to the next texture.  If the end of this list is reached, the delay sequence will repeat from the start of the list.")
    public List<Integer> textureDelays;

    @JSONDescription("A string in hexadecimal format representing the particle's color.  Defaults to white if not set, which essentially does no color modification.")
    public ColorRGB color;

    @JSONDescription("Like above, but tells the particle to gradually change from its initial color to this value.  Defaults to be the same as the initial color.")
    public ColorRGB toColor;

    @JSONDescription("The position where this particle should be spawned relative to the spawning object.  May be left out if the particle should spawn at the same position.")
    public Point3D pos;

    @JSONRequired(dependentField = "textureList", dependentValues = "true")
    @JSONDescription("The rotation to rotate this particle to.  Has no effect if axisAligned is false, as particles normally rotate to face the player.")
    public RotationMatrix rot;

    @JSONDescription("The initial velocity of the particle, where +Z is straight ahead relative to the thing that is producing it.  May be omitted to make a particle that doesn't spawn with any initial velocity except the velocity of the object spawning it.")
    public Point3D initialVelocity;

    @JSONDescription("The velocity to apply every tick to the particle.  This can be used to make smoke float up, oil drip down, etc.  If not set, the default particle velocity is used.")
    public Point3D movementVelocity;

    @JSONDescription("The max velocity this particle can have in any axis.  Used to prevent particles from going to fast if they move a long way.")
    public Point3D terminalVelocity;

    @JSONRequired
    @JSONDescription("A condition group that determines if this particle should spawn.  Particles will only spawn when they first become active, unless spawnEveryTick is set.")
    public JSONConditionGroup activeConditions;

    @JSONDescription("A list of sub-particles this particle can spawn.  They will be spawned when their conditions are met.  Note that sub-particles do not reference spawningAnimations or activeAnimations.")
    public List<JSONSubParticle> subParticles;

    @Deprecated
    public Point3D velocityVector;
    @Deprecated
    public List<JSONAnimationDefinition> activeAnimations;
    public static class JSONSubParticle {

        @JSONDescription("The particle to spawn.")
        public JSONParticle particle;

        @JSONDescription("The time, in ticks, at which to spawn the particle.")
        public int time;
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
