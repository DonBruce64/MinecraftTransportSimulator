package mcinterface1182;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.mcinterface.IInterfaceSound;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.OGGDecoder;
import minecrafttransportsimulator.sound.RadioStation;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Interface for the sound system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Dist.CLIENT)
public class InterfaceSound implements IInterfaceSound {
    /**
     * Flag for game paused state.  Gets set when the game is paused.
     **/
    private static boolean isSystemPaused;

    /**
     * Map of String-based file-names to Integer pointers to buffer locations.  Used for loading sounds into
     * memory to prevent the need to load them every time they are played.
     **/
    private static final Map<String, Integer> dataSourceBuffers = new HashMap<>();

    /**
     * List of sounds currently playing.  Queued for updates every tick.
     **/
    private static final Set<SoundInstance> playingSounds = new HashSet<>();

    /**
     * List of playing {@link RadioStation} objects.
     **/
    private static final List<RadioStation> playingStations = new ArrayList<>();

    /**
     * List of sounds to start playing next update.  Split from playing sounds to avoid CMEs and odd states.
     **/
    private static final List<SoundInstance> queuedSounds = new ArrayList<>();
    /**
     * List of radios paused.  Needs to be separate from normal paused sound this those get re-added to the sound set.
     **/
    private static final List<SoundInstance> pausedRadioSounds = new ArrayList<>();

    /**
     * This gets incremented whenever we try to get a source and fail.  If we get to 10, the sound system
     * will stop attempting to play sounds.  Used for when mods take all the sources.
     **/
    private static byte sourceGetFailures = 0;

    /**
     * Main update loop.  Call every tick to update playing sounds,
     * as well as queue up sounds that aren't playing yet but need to.
     */
    public static void update() {
        if (ALC.getFunctionProvider() == null) {
            //Don't go any further if OpenAL isn't ready.
            return;
        }

        //Handle pause state logic.
        if (InterfaceManager.clientInterface.isGamePaused()) {
            if (!isSystemPaused) {
                for (SoundInstance sound : playingSounds) {
                    AL10.alSourcePause(sound.sourceIndex);
                }
                isSystemPaused = true;
            } else {
                for (SoundInstance sound : playingSounds) {
                    //Stop playing sounds when paused as they can get corrupted.
                    if (sound.radio != null) {
                        pausedRadioSounds.add(sound);
                        sound.radio.currentStation.removeRadio(sound.radio);
                    } else {
                        sound.stopSound = true;
                    }
                }
                playingSounds.removeAll(pausedRadioSounds);
            }
            return;
        } else if (isSystemPaused) {
            for (SoundInstance sound : playingSounds) {
                AL10.alSourcePlay(sound.sourceIndex);
            }
            for (SoundInstance sound : pausedRadioSounds) {
                sound.radio.currentStation.addRadio(sound.radio);
            }
            pausedRadioSounds.clear();
            isSystemPaused = false;
        }

        //Get the player for further calculations.
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        //If the client world is null, or we don't have a player we need to stop all sounds.
        if (InterfaceManager.clientInterface.getClientWorld() == null || player == null) {
            queuedSounds.clear();
            for (SoundInstance sound : playingSounds) {
                sound.stopSound = true;
            }
        }

        //Start playing all queued sounds.
        if (!queuedSounds.isEmpty()) {
            for (SoundInstance sound : queuedSounds) {
                AL10.alSourcePlay(sound.sourceIndex);
                playingSounds.add(sound);
            }
            queuedSounds.clear();
        }

        //Update playing sounds.
        boolean soundSystemReset = false;
        Iterator<SoundInstance> iterator = playingSounds.iterator();
        while (iterator.hasNext()) {
            SoundInstance sound = iterator.next();
            AL10.alGetError();
            int state = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_SOURCE_STATE);
            //If we are an invalid name, it means the sound system was reset.
            if (AL10.alGetError() == AL10.AL_INVALID_NAME) {
                soundSystemReset = true;
                break;
            }

            if (state == AL10.AL_PLAYING) {
                if (sound.stopSound) {
                    AL10.alSourceStop(sound.sourceIndex);
                } else {
                    //Update position and volume, and block rolloff.
                    sound.updatePosition();
                    AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.position.x, (float) sound.position.y, (float) sound.position.z);
                    if (sound.radio == null) {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume * ConfigSystem.client.controlSettings.soundVolume.value);
                    } else {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume * ConfigSystem.client.controlSettings.radioVolume.value);
                    }
                    AL10.alSourcef(sound.sourceIndex, AL10.AL_ROLLOFF_FACTOR, 0);

                    //If the sound is looping, and the player isn't riding the source, calculate doppler pitch effect.
                    //Otherwise, set pitch as normal.
                    if (sound.soundDef != null && sound.soundDef.looping && !sound.soundDef.blockDoppler && !sound.entity.equals(player.getEntityRiding())) {
                        Point3D playerVelocity = player.getVelocity();
                        playerVelocity.y = 0;
                        double initalDelta = player.getPosition().subtract(sound.entity.position).length();
                        double finalDelta = player.getPosition().add(playerVelocity).subtract(sound.entity.position).add(-sound.entity.motion.x, 0D, -sound.entity.motion.z).length();
                        float dopplerFactor = (float) (initalDelta > finalDelta ? 1 + 0.25 * (initalDelta - finalDelta) / initalDelta : 1 - 0.25 * (finalDelta - initalDelta) / finalDelta);
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch * dopplerFactor);
                    } else {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch);
                    }
                }
            } else {
                //We are a stopped sound.  Un-bind and delete any sources and buffers we are using.
                if (sound.radio == null) {
                    //Normal sound. Un-bind buffer and make sure we're flagged as stopped.
                    //We could have just reached the end of the sound.
                    AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
                    sound.stopSound = true;
                } else if (sound.stopSound) {
                    //Radio with stop command.  Un-bind all radio buffers.
                    int boundBuffers = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED);
                    if (boundBuffers > 0) {
                        IntBuffer buffers = BufferUtils.createIntBuffer(boundBuffers);
                        AL10.alSourceUnqueueBuffers(sound.sourceIndex, buffers);
                    }
                }
                if (sound.stopSound) {
                    //Sound was commanded to be stopped.  Delete sound source to free up slot.
                    IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
                    sourceBuffer.put(sound.sourceIndex).flip();
                    AL10.alDeleteSources(sourceBuffer);

                    //Delete from playing list, and entity that has this sound.
                    iterator.remove();
                    sound.entity.sounds.remove(sound);
                }
            }
        }

        //Now update radio stations.
        for (RadioStation station : playingStations) {
            station.update();
        }

        //If the sound system was reset, blow out all saved data points.
        if (soundSystemReset) {
            InterfaceManager.coreInterface.logError("Had an invalid sound name.  Was the sound system reset?  Clearing all sounds, playing or not!");
            dataSourceBuffers.clear();
            for (SoundInstance sound : playingSounds) {
                sound.entity.sounds.remove(sound);
            }
            playingSounds.clear();
            sourceGetFailures = 0;
        }
    }

    @Override
    public void playQuickSound(SoundInstance sound) {
        if (ALC.getFunctionProvider() != null && sourceGetFailures < 10) {
            //First get the IntBuffer pointer to where this sound data is stored.
            Integer dataBufferPointer = loadOGGJarSound(sound.soundPlayingName);
            if (dataBufferPointer != null) {
                //Set the sound's source buffer index.
                IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
                AL10.alGetError();
                AL10.alGenSources(sourceBuffer);
                if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                    AL10.alDeleteBuffers(dataBufferPointer);
                    if (++sourceGetFailures == 10) {
                        InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                        ///Kill off the sound that's furthest from the player to make room if we have a sound we can remove.
                        //This keeps the sounds going, even with limited slots.
                        if (!playingSounds.isEmpty()) {
                            SoundInstance furthestSound = null;
                            Point3D playerPosition = InterfaceManager.clientInterface.getClientPlayer().getPosition();
                            for (SoundInstance testSound : playingSounds) {
                                if (furthestSound == null || playerPosition.isFirstCloserThanSecond(testSound.position, furthestSound.position)) {
                                    furthestSound = testSound;
                                }
                            }
                            sourceGetFailures = 0;
                            //Manually stop sound and remove from iterator.
                            //This makes the source entity think that it's still playing and won't re-add it.
                            AL10.alSourcei(furthestSound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
                            sourceBuffer = BufferUtils.createIntBuffer(1);
                            sourceBuffer.put(furthestSound.sourceIndex).flip();
                            AL10.alDeleteSources(sourceBuffer);
                            playingSounds.remove(furthestSound);
                        }
                    }
                    return;
                }
                sound.sourceIndex = sourceBuffer.get(0);

                //Set properties and bind data buffer to source.
                AL10.alGetError();
                AL10.alSourcei(sound.sourceIndex, AL10.AL_LOOPING, sound.soundDef != null && sound.soundDef.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
                AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.entity.position.x, (float) sound.entity.position.y, (float) sound.entity.position.z);
                AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, dataBufferPointer);

                //Done setting up buffer.  Queue sound to start playing.
                queuedSounds.add(sound);
                sound.entity.sounds.add(sound);
            }
        }
    }

    @Override
    public void addRadioStation(RadioStation station) {
        playingStations.add(station);
    }

    @Override
    public void addRadioSound(SoundInstance sound, List<Integer> buffers) {
        if (ALC.getFunctionProvider() != null && sourceGetFailures < 10) {
            //Set the sound's source buffer index.
            IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
            AL10.alGetError();
            AL10.alGenSources(sourceBuffer);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                if (++sourceGetFailures == 10) {
                    InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                }
                return;
            }
            sound.sourceIndex = sourceBuffer.get(0);

            //Queue up the buffer sources to the source itself.
            for (int bufferIndex : buffers) {
                bindBuffer(sound, bufferIndex);
            }
            queuedSounds.add(sound);
        }
    }

    @Override
    public int createBuffer(ByteBuffer buffer, IStreamDecoder decoder) {
        IntBuffer newDataBuffer = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(newDataBuffer);
        AL10.alBufferData(newDataBuffer.get(0), AL10.AL_FORMAT_MONO16, buffer, decoder.getSampleRate());
        return newDataBuffer.get(0);
    }

    @Override
    public void deleteBuffer(int bufferIndex) {
        AL10.alDeleteBuffers(bufferIndex);
    }

    @Override
    public void bindBuffer(SoundInstance sound, int bufferIndex) {
        AL10.alSourceQueueBuffers(sound.sourceIndex, bufferIndex);
    }

    @Override
    public int getFreeStationBuffer(Set<EntityRadio> playingRadios) {
        boolean freeBuffer = true;
        EntityRadio badRadio = null;
        AL10.alGetError();
        for (EntityRadio radio : playingRadios) {
            SoundInstance sound = radio.getPlayingSound();
            if (AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED) == 0) {
                freeBuffer = false;
                break;
            }
            if (AL10.alGetError() == AL10.AL_INVALID_NAME) {
                badRadio = radio;
            }
        }
        if (badRadio != null) {
            badRadio.stop();
            return 0;
        } else if (freeBuffer) {
            //First get the old buffer index.
            int freeBufferIndex = 0;
            IntBuffer oldDataBuffer = BufferUtils.createIntBuffer(1);
            for (EntityRadio radio : playingRadios) {
                SoundInstance sound = radio.getPlayingSound();
                AL10.alSourceUnqueueBuffers(sound.sourceIndex, oldDataBuffer);
                if (freeBufferIndex == 0) {
                    freeBufferIndex = oldDataBuffer.get(0);
                } else if (freeBufferIndex != oldDataBuffer.get(0)) {
                    badRadio = radio;
                    break;
                }
            }
            if (badRadio != null) {
                badRadio.stop();
                return 0;
            } else {
                return freeBufferIndex;
            }
        } else {
            return 0;
        }
    }

    /**
     * Loads an OGG file in its entirety using the {@link InterfaceOGGDecoder}.
     * The sound is then stored in a dataBuffer keyed by soundName located in {@link #dataSourceBuffers}.
     * The pointer to the dataBuffer is returned for convenience as it allows for transparent sound caching.
     * If a sound with the same name is passed-in at a later time, it is assumed to be the same and rather
     * than re-parse the sound the system will simply return the same pointer index to be bound.
     */
    private static Integer loadOGGJarSound(String soundName) {
        if (dataSourceBuffers.containsKey(soundName)) {
            //Already parsed the data.  Return the buffer.
            return dataSourceBuffers.get(soundName);
        } else {
            //Need to parse the data.  Do so now.
            String soundDomain = soundName.substring(0, soundName.indexOf(':'));
            String soundPath = soundName.substring(soundDomain.length() + 1);
            InputStream soundStream = InterfaceManager.coreInterface.getPackResource("/assets/" + soundDomain + "/sounds/" + soundPath + ".ogg");
            if (soundStream != null) {
                //Create decoder and decode whole file.
                OGGDecoder decoder = new OGGDecoder(soundStream);
                ByteBuffer decodedData = ByteBuffer.allocateDirect(0);
                ByteBuffer blockRead;
                while ((blockRead = decoder.readBlock()) != null) {
                    decodedData = ByteBuffer.allocateDirect(decodedData.capacity() + blockRead.limit()).put(decodedData).put(blockRead);
                    decodedData.rewind();
                }

                //Generate an IntBuffer to store a pointer to the data buffer.
                IntBuffer dataBufferPointers = BufferUtils.createIntBuffer(1);
                AL10.alGenBuffers(dataBufferPointers);

                //Bind the decoder output buffer to the data buffer pointer.
                AL10.alBufferData(dataBufferPointers.get(0), AL10.AL_FORMAT_MONO16, decodedData, decoder.getSampleRate());

                //Done parsing.  Map the dataBuffer(s) to the soundName and return the index.
                dataSourceBuffers.put(soundName, dataBufferPointers.get(0));
                return dataSourceBuffers.get(soundName);
            } else {
                return null;
            }
        }
    }

    public static void stopAllSounds() {
        queuedSounds.clear();
        for (SoundInstance sound : playingSounds) {
            if (sound.radio != null) {
                sound.radio.stop();
            } else {
                sound.stopSound = true;
            }
        }

        //Mark world as un-paused and update sounds to stop the ones that were just removed.
        isSystemPaused = false;
        update();
    }

    /**
     * Update all sounds every client tick.
     */
    @SubscribeEvent
    public static void onIVClientTick(ClientTickEvent event) {
        //Only do updates at the end of a phase to prevent double-updates.
        if (event.phase.equals(Phase.END)) {
            //We put this into a try block as sound system reloads can cause the thread to get stopped mid-execution.
            try {
                update();
            } catch (Exception e) {
                e.printStackTrace();
                //Do nothing.  We only get exceptions here if OpenAL isn't ready.
            }
        }
    }

    /**
     * Stop all sounds when the world is unloaded.
     */
    @SubscribeEvent
    public static void onIVWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            queuedSounds.removeIf(soundInstance -> event.getWorld() == ((WrapperWorld) soundInstance.entity.world).world);
            for (SoundInstance sound : playingSounds) {
                if (event.getWorld() == ((WrapperWorld) sound.entity.world).world) {
                    if (sound.radio != null) {
                        sound.radio.stop();
                    } else {
                        sound.stopSound = true;
                    }
                }
            }

            //Mark world as un-paused and update sounds to stop the ones that were just removed.
            isSystemPaused = false;
            update();
        }
    }
}