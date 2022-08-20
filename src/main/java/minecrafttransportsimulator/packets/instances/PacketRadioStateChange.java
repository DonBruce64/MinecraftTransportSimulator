package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**
 * This packet is sent to servers when a radio changes state. It sets the state on the server.  After this,
 * the radio will send out packets to all clients to update their states.  This allows song changes to be
 * on both servers and clients.
 *
 * @author don_bruce
 */
public class PacketRadioStateChange extends APacketEntity<EntityRadio> {
    private final RadioSources source;
    private final int volume;
    private final int preset;
    private final boolean randomOrder;
    private final String currentURL;

    /**
     * Source change constructor
     **/
    public PacketRadioStateChange(EntityRadio radio, RadioSources source) {
        super(radio);
        this.source = source;
        this.volume = radio.volume;
        this.preset = radio.preset;
        this.randomOrder = radio.randomOrder;
        this.currentURL = radio.currentURL;
    }

    /**
     * Volume change constructor
     **/
    public PacketRadioStateChange(EntityRadio radio, int volume) {
        super(radio);
        this.source = radio.getSource();
        this.volume = volume;
        this.preset = radio.preset;
        this.randomOrder = radio.randomOrder;
        this.currentURL = radio.currentURL;
    }

    /**
     * Local playback start constructor
     **/
    public PacketRadioStateChange(EntityRadio radio, int preset, boolean randomOrder) {
        super(radio);
        this.source = radio.getSource();
        this.volume = radio.volume;
        this.preset = preset;
        this.randomOrder = randomOrder;
        this.currentURL = radio.currentURL;
    }

    /**
     * Internet playback start constructor
     **/
    public PacketRadioStateChange(EntityRadio radio, int preset, String currentURL) {
        super(radio);
        this.source = radio.getSource();
        this.volume = radio.volume;
        this.preset = preset;
        this.randomOrder = radio.randomOrder;
        this.currentURL = currentURL;
    }

    /**
     * Stop playback start constructor
     **/
    public PacketRadioStateChange(EntityRadio radio) {
        super(radio);
        this.source = radio.getSource();
        this.volume = radio.volume;
        this.preset = 0;
        this.randomOrder = radio.randomOrder;
        this.currentURL = radio.currentURL;
    }

    public PacketRadioStateChange(ByteBuf buf) {
        super(buf);
        this.source = RadioSources.values()[buf.readByte()];
        this.volume = buf.readByte();
        this.preset = buf.readByte();
        this.randomOrder = buf.readBoolean();
        this.currentURL = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(source.ordinal());
        buf.writeByte(volume);
        buf.writeByte(preset);
        buf.writeBoolean(randomOrder);
        writeStringToBuffer(currentURL, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, EntityRadio radio) {
        if (radio != null) {
            if (world.isClient()) {
                if (!radio.getSource().equals(source)) {
                    radio.changeSource(source);
                } else if (radio.volume != volume) {
                    radio.changeVolume(volume);
                } else if (preset == 0) {
                    radio.stop();
                } else if (radio.getSource().equals(RadioSources.INTERNET)) {
                    radio.startInternetPlayback(currentURL, preset);
                } else if (radio.getSource().equals(RadioSources.LOCAL)) {
                    radio.startLocalPlayback(preset, randomOrder);
                }
            } else {
                radio.setProperties(source, volume, preset, randomOrder, currentURL);
            }
            return true;
        } else {
            return false;
        }
    }
}
