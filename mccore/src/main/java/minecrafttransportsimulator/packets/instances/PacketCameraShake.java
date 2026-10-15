package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.CameraSystem;

/**
 * Packet sent from the server to apply a camera shake impulse to a nearby player.
 * The strength has already been scaled by the server config and distance.  Duration is in ticks.
 * Intensity is an oscillation speed multiplier.
 *
 * @author dldev32
 */
public class PacketCameraShake extends APacketBase {
    private final double strength;
    private final int duration;
    private final double intensity;

    public PacketCameraShake(double strength, int duration, double intensity) {
        super(null);
        this.strength = strength;
        this.duration = duration;
        this.intensity = intensity;
    }

    public PacketCameraShake(ByteBuf buf) {
        super(buf);
        this.strength = buf.readDouble();
        this.duration = buf.readInt();
        this.intensity = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(strength);
        buf.writeInt(duration);
        buf.writeDouble(intensity);
    }

    @Override
    public void handle(AWrapperWorld world) {
        CameraSystem.startCameraShake(strength, duration, intensity);
    }
}
