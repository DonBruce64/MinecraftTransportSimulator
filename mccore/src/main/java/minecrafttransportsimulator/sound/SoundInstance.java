package minecrafttransportsimulator.sound;

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
    public final AEntityB_Existing entity;
    public final String soundName;
    public final JSONSound soundDef;
    public final EntityRadio radio;
    public final Point3D position;

    //Runtime variables.
    public int sourceIndex;
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public boolean stopSound = false;

    public SoundInstance(AEntityB_Existing entity, String soundName) {
        this(entity, soundName, null, null);
    }

    public SoundInstance(AEntityB_Existing entity, JSONSound soundDef) {
        this(entity, soundDef.name, soundDef, null);
    }

    public SoundInstance(AEntityB_Existing entity, String soundName, JSONSound soundDef, EntityRadio radio) {
        this.entity = entity;
        this.soundName = soundName;
        this.soundDef = soundDef;
        this.radio = radio;
        this.position = entity.position.copy();
    }

    public void updatePosition() {
        if (soundDef != null && soundDef.pos != null) {
            position.set(soundDef.pos).rotate(entity.orientation).add(entity.position);
        } else {
            position.set(entity.position);
        }
    }
}
