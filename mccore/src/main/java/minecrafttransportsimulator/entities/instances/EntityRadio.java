package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.sound.RadioManager;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;
import minecrafttransportsimulator.sound.RadioStation;
import minecrafttransportsimulator.sound.SoundInstance;

/**
 * Base class for radios.  Used to provide a common set of tools for all radio implementations.
 * This class keeps track of the radio station currently selected, as well as the
 * current volume for the source and equalization settings.  The actual station being planed
 * is in the {@link RadioStation} class, which is handled by the {@link RadioManager}.
 *
 * @author don_bruce
 */
public class EntityRadio extends AEntityB_Existing {

    //Public variables for modifying state.
    public boolean randomOrder;
    public int preset;
    public String currentURL;
    public int volume;
    public String displayText;
    public RadioStation currentStation;

    //Private runtime variables.
    private final AEntityB_Existing provider;
    private RadioSources currentSource;
    private SoundInstance currentSound;

    public EntityRadio(AEntityB_Existing provider, IWrapperNBT data) {
        super(provider.world, null, data);
        this.provider = provider;
        if (world.isClient()) {
            if (data.getBoolean("savedRadio")) {
                changeSource(RadioSources.values()[data.getInteger("currentSource")]);
                changeVolume(data.getInteger("volume"));
                this.preset = data.getInteger("preset");
                this.currentURL = data.getString("currentURL");
                if (preset > 0) {
                    if (currentSource.equals(RadioSources.LOCAL)) {
                        startLocalPlayback(preset, data.getBoolean("randomOrder"));
                    } else {
                        startInternetPlayback(currentURL, preset);
                    }
                }
            } else {
                changeSource(RadioSources.LOCAL);
                changeVolume(10);
            }
        } else {
            setProperties(RadioSources.values()[data.getInteger("currentSource")], data.getInteger("volume"), data.getInteger("preset"), data.getBoolean("randomOrder"), data.getString("currentURL"));
        }
    }

    @Override
    public boolean shouldSavePosition() {
        //Don't save positional data.  We don't care about that as that comes from our provider.
        return false;
    }

    @Override
    public void update() {
        super.update();
        position.set(provider.position);
        if (world.isClient() && currentSound != null) {
            double distance = position.distanceTo(InterfaceManager.clientInterface.getClientPlayer().getPosition());
            currentSound.volume = (float) (volume / 10F * (distance < SoundInstance.DEFAULT_MAX_DISTANCE ? (1 - distance / SoundInstance.DEFAULT_MAX_DISTANCE) : 0));
        }
    }

    @Override
    public EntityUpdateType getUpdateType() {
        //Radios get ticked from their spawning entities post-update.
        return EntityUpdateType.NONE;
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.RADIO));
        return true;
    }

    /**
     * Starts radio playback, making a new sound instance to do so.
     * This command comes from the currently-selected radio station when
     * it has connected and is ready to play sound.  Start volume at 0.
     * The {@link #update()} function will set the volume on the next call.
     */
    public void start() {
        currentSound = new SoundInstance(this, "Radio_" + uniqueUUID, null, this);
        currentSound.volume = 0;
    }

    /**
     * Stops radio playback, disconnecting it from its source.
     * This command comes from the stop button or the audio system if the
     * radio station has stopped.
     */
    public void stop() {
        if (currentStation != null) {
            currentStation.removeRadio(this);
            currentStation = null;
            if (currentSound != null) {
                currentSound.stopSound = true;
            }
            displayText = "Radio turned off.";
        }
        preset = 0;
    }

    /**
     * Changes the radio's source.
     */
    public void changeSource(RadioSources source) {
        stop();
        this.currentSource = source;
        switch (source) {
            case LOCAL:
                displayText = "Ready to play from files on your PC.\nPress a station number to start.\nFiles are in folders in the mts_music directory.";
                break;
            case SERVER:
                displayText = "Ready to play from files on the server.\nPress a station number to start.";
                break;
            case INTERNET:
                displayText = "Ready to play from internet streams.\nPress a station number to start.\nOr press SET to set a station URL.";
                break;
        }
    }

    /**
     * Gets the radio's source.
     */
    public RadioSources getSource() {
        return currentSource;
    }

    /**
     * Changes the volume of this radio.
     */
    public void changeVolume(int setVolume) {
        this.volume = setVolume == 0 ? 10 : setVolume;
    }

    /**
     * Returns true if the radio is currently playing.
     */
    public boolean isPlaying() {
        return currentSound != null && !currentSound.stopSound;
    }

    /**
     * Returns the sound the radio is currently playing, or null if it isn't playing anything.
     */
    public SoundInstance getPlayingSound() {
        return currentSound;
    }

    /**
     * Sets the station for this radio.  Station is responsible for starting playback of sounds.
     */
    public void startLocalPlayback(int index, boolean randomRequested) {
        stop();
        preset = index;
        randomOrder = randomRequested;
        currentStation = RadioManager.getLocalStation(preset - 1, randomOrder);
        currentStation.addRadio(this);
    }

    /**
     * NOT USED
     */
    public void startServerPlayback(int index) {
        stop();
        preset = index;
        displayText = "This method of playback is not supported .... yet!";
    }

    /**
     * Sets the currentURL for this radio and starts playback of the internet stream.
     * Note that the preset pressed is only for visual setting of the GUI, and is NOT used
     * to determine anything about the stream.
     */
    public void startInternetPlayback(String newURL, int presetPressed) {
        stop();
        currentURL = newURL;
        preset = presetPressed;
        currentStation = RadioManager.getInternetStation(currentURL);
        currentStation.addRadio(this);
    }

    /**
     * Sets the properties without doing any operations.  Used on servers to track state changes.
     */
    public void setProperties(RadioSources source, int volume, int preset, boolean randomOrder, String currentURL) {
        this.currentSource = source;
        this.volume = volume;
        this.preset = preset;
        this.randomOrder = randomOrder;
        this.currentURL = currentURL;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setInteger("currentSource", currentSource.ordinal());
        data.setInteger("volume", volume);
        data.setBoolean("savedRadio", true);
        data.setBoolean("randomOrder", randomOrder);
        data.setInteger("preset", preset);
        data.setString("currentURL", currentURL);
        return data;
    }
}