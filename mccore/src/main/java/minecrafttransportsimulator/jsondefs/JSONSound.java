package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSound {
    @JSONRequired
    @JSONDescription("The name for this sound.  This tells MTS where to find it.  Format is packID:soundName.  All sounds should be located in the 'sounds' folder as the first folder underneath your main pack folder.")
    public String name;

    @JSONDescription("A list of sounds to play instead of the main sound.  If this is present, then one of these at random will be played each time this sound is played rather than the normal sound.  Note that a paramter for name is still required to allow the audio system to track this sound, and should be unique to the entity this is defined on.")
    public List<String> soundVariations;

    @JSONRequired
    @JSONDescription("A required condition group for determining if this sound is active.")
    public JSONConditionGroup activeConditions;

    @JSONDescription("A listing of value modifiers for the volume (default is full volume).\nVolume is internally clamped from 0.0 to 1.0; you do not need to worry about clamping here.")
    public List<JSONValueModifier> volumeValueModifiers;

    @JSONDescription("Like volumeModifiers, but for pitch.  Pitch has a lower clamp of 0.0, but can be as high as you want, though some audio cards may have issues at really high values.")
    public List<JSONValueModifier> pitchValueModifiers;

    @JSONDescription("If the sound should loop, set this to true.  Be aware that unless the volume is set to 0 or below, or the sound is blocked via visibility variables or inhibitors, then it will keep playing forever and take up a sound slot!")
    public boolean looping;

    @JSONDescription("Normally, sounds won't play if there's already a sound playing.  If you want this sound to be played every tick the conditions say that it should be active, set this to true.  Mainly used for sounds on engines and wheels that can occur in the same tick back-to-back, but may be used for other things.  Looping sounds is HIGHLY preferred to this if possible.")
    public boolean forceSound;

    @JSONDescription("Normally, sounds are only checked every tick for playing.  However, some sounds, like guns with firing rates of 1 tick or less, or engine cylinder cam-based sounds, will occur more than once a tick.  You may set this to have these sounds play more than once a tick, but be warned that it will result in a drop in FPS, so only set this if absolutely required!")
    public boolean canPlayOnPartialTicks;

    @JSONDescription("This causes the sound to only play if the player is riding this entity and is in first-person.")
    public boolean isInterior;

    @JSONDescription("Like isInterior, but blocks this sound if the player is in first-person and riding this entity.")
    public boolean isExterior;

    @JSONDescription("Normally, all looping sounds have a doppler effect applied.  Setting this to true will block this.")
    public boolean blockDoppler;

    @JSONDescription("The x, y, z position that this animation should be performed at.  If this is rotation, it is the rotation point.")
    public Point3D centerPoint;

    @JSONDescription("An entry of x, y, and z coordinates that define the center point of where this sound will be played relative to the center of the object.  May be omitted if you just want the sound to play at the center.")
    public Point3D pos;

    @JSONDescription("The minimum distance for where this sound can be heard.")
    public float minDistance;

    @JSONDescription("The volume of the sound at the minimum distance.")
    public float minDistanceVolume;

    @JSONDescription("The maximum distance as to where this sound can be heard.  If this and minDistance are left out, 0-32 is used.")
    public float maxDistance;

    @JSONDescription("The volume of the sound at the maximum distance.")
    public float maxDistanceVolume;

    @JSONDescription("A special distance that causes a middle calculation in the sound volume.  This allows for a triangular interpolation of sound volume.")
    public float middleDistance;

    @JSONDescription("The volume of the sound at the middle distance.")
    public float middleDistanceVolume;

    @Deprecated
    public List<JSONAnimationDefinition> activeAnimations;
    @Deprecated
    public List<JSONAnimationDefinition> volumeAnimations;
    @Deprecated
    public List<JSONAnimationDefinition> pitchAnimations;
}
