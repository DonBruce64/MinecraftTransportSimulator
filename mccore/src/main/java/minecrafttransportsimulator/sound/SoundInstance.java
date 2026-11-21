package minecrafttransportsimulator.sound;

import java.util.Random;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class that holds sound information.  One class is created for each sound that's playing
 * in the {@link InterfaceManager.soundInterface}.  This class holds data such as the current
 * source the sound is playing from, whether the sound is supposed to be looping or not, etc.
 * Setting {@link #stopSound} will stop this sound immediately, while {@link #streaming} tells
 * the audio system that this sound needs to be read in via chunks rather than all at once.
 *
 * @author don_bruce
 */
public class SoundInstance {
    private static final Random soundRandom = new Random();

    public final AEntityB_Existing entity;
    public final String soundName;
    public final String soundPlayingName;
    public final JSONSound soundDef;
    public final EntityRadio radio;
    public final Point3D position;
    private final boolean fixedPosition;

    //Runtime variables.
    public int sourceIndex;
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public boolean stopSound = false;

    public static final int DEFAULT_MAX_DISTANCE = 32;

    public SoundInstance(AEntityB_Existing entity, String soundName) {
        this(entity, soundName, null, null);
    }

    /**
     * Creates a one-shot sound at a fixed position, retaining the entity only for ownership.
     */
    public SoundInstance(AEntityB_Existing entity, String soundName, Point3D position) {
        this(entity, soundName, null, null, position);
    }

    public SoundInstance(AEntityB_Existing entity, JSONSound soundDef) {
        this(entity, soundDef.name, soundDef, null);
    }

    public SoundInstance(AEntityB_Existing entity, String soundName, JSONSound soundDef, EntityRadio radio) {
        this(entity, soundName, soundDef, radio, null);
    }

    private SoundInstance(AEntityB_Existing entity, String soundName, JSONSound soundDef, EntityRadio radio, Point3D position) {
        this.entity = entity;
        this.soundDef = soundDef;
        this.soundName = soundName;
        this.soundPlayingName = soundDef != null && soundDef.soundVariations != null ? soundDef.soundVariations.get(soundRandom.nextInt(soundDef.soundVariations.size())) : soundName;
        this.radio = radio;
        this.fixedPosition = position != null;
        this.position = fixedPosition ? position.copy() : new Point3D();
        updatePosition();
    }

    public void updatePosition() {
        if (fixedPosition) {
            return;
        }
        if (soundDef != null && soundDef.pos != null) {
            position.set(soundDef.pos).rotate(entity.orientation).add(entity.position);
        } else {
            position.set(entity.position);
        }
    }
}
