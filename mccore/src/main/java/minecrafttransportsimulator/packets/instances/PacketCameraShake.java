package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.CameraSystem;

/**
 * Packet sent from the server to apply a camera shake impulse to a nearby player.
 * The strength has already been scaled by the server config and distance.  Duration is in ticks.
 *
 * @author dldev32
 */
public class PacketCameraShake extends APacketBase {
    private final double strength;
    private final int duration;

    public PacketCameraShake(double strength) {
        this(strength, 10);
    }

    public PacketCameraShake(double strength, int duration) {
        super(null);
        this.strength = strength;
        this.duration = duration;
    }

    public PacketCameraShake(ByteBuf buf) {
        super(buf);
        this.strength = buf.readDouble();
        this.duration = buf.readInt();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(strength);
        buf.writeInt(duration);
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (world.isClient()) {
            CameraSystem.startCameraShake(strength, duration);
        }
    }
}
