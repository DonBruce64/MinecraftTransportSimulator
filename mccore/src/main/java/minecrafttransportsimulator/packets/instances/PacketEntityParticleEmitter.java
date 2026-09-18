package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityParticleEmitter;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.packets.components.APacketBase;

/**
 * Start, correction, and stop packet for a generic long-range particle emitter.
 * Actual particles are deliberately never serialized.
 */
public class PacketEntityParticleEmitter extends APacketBase {
    private static final byte START = 0;
    private static final byte UPDATE = 1;
    private static final byte STOP = 2;

    private final byte action;
    private final UUID emitterUUID;
    private final String dimensionName;
    private final String packID;
    private final String systemName;
    private final String subName;
    private final int particleIndex;
    private final Point3D position;
    private final Point3D extrapolationMotion;
    private final Point3D inheritedMotion;
    private final Point3D orientationAngles;
    private final Point3D scale;

    /** Creates a start or periodic update packet. */
    public PacketEntityParticleEmitter(EntityParticleEmitter emitter, boolean start) {
        super(null);
        this.action = start ? START : UPDATE;
        this.emitterUUID = emitter.getEmitterUUID();
        this.dimensionName = emitter.world.getName();
        this.packID = start ? emitter.getPackID() : null;
        this.systemName = start ? emitter.getSystemName() : null;
        this.subName = start ? emitter.getSubName() : null;
        this.particleIndex = start ? emitter.getParticleIndex() : -1;
        this.position = emitter.position.copy();
        this.extrapolationMotion = emitter.getExtrapolationMotion().copy();
        this.inheritedMotion = emitter.motion.copy();
        this.orientationAngles = emitter.orientation.convertToAngles().copy();
        this.scale = emitter.scale.copy();
    }

    /** Creates a stop packet. */
    public PacketEntityParticleEmitter(EntityParticleEmitter emitter) {
        super(null);
        this.action = STOP;
        this.emitterUUID = emitter.getEmitterUUID();
        this.dimensionName = emitter.world.getName();
        this.packID = null;
        this.systemName = null;
        this.subName = null;
        this.particleIndex = -1;
        this.position = null;
        this.extrapolationMotion = null;
        this.inheritedMotion = null;
        this.orientationAngles = null;
        this.scale = null;
    }

    public PacketEntityParticleEmitter(ByteBuf buf) {
        super(buf);
        this.action = buf.readByte();
        this.emitterUUID = readUUIDFromBuffer(buf);
        this.dimensionName = readStringFromBuffer(buf);
        if (action == START) {
            this.packID = readStringFromBuffer(buf);
            this.systemName = readStringFromBuffer(buf);
            this.subName = readStringFromBuffer(buf);
            this.particleIndex = buf.readInt();
        } else {
            this.packID = null;
            this.systemName = null;
            this.subName = null;
            this.particleIndex = -1;
        }
        if (action != STOP) {
            this.position = readPoint3dFromBuffer(buf);
            this.extrapolationMotion = readFloatPoint(buf);
            this.inheritedMotion = readFloatPoint(buf);
            this.orientationAngles = readFloatPoint(buf);
            this.scale = readFloatPoint(buf);
        } else {
            this.position = null;
            this.extrapolationMotion = null;
            this.inheritedMotion = null;
            this.orientationAngles = null;
            this.scale = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(action);
        writeUUIDToBuffer(emitterUUID, buf);
        writeStringToBuffer(dimensionName, buf);
        if (action == START) {
            writeStringToBuffer(packID, buf);
            writeStringToBuffer(systemName, buf);
            writeStringToBuffer(subName, buf);
            buf.writeInt(particleIndex);
        }
        if (action != STOP) {
            writePoint3dToBuffer(position, buf);
            writeFloatPoint(extrapolationMotion, buf);
            writeFloatPoint(inheritedMotion, buf);
            writeFloatPoint(orientationAngles, buf);
            writeFloatPoint(scale, buf);
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (!world.isClient() || !dimensionName.equals(world.getName())) {
            return;
        }

        EntityParticleEmitter emitter = EntityParticleEmitter.getClientEmitter(world, emitterUUID);
        if (action == STOP) {
            if (emitter != null) {
                emitter.remove();
            }
            return;
        }

        if (action == START) {
            JSONParticle particleDefinition = getParticleDefinition();
            if (particleDefinition == null || !particleDefinition.renderAtLongRange) {
                return;
            }
            if (emitter == null) {
                emitter = new EntityParticleEmitter(world, emitterUUID, particleDefinition, packID, systemName, subName, particleIndex, position, extrapolationMotion, inheritedMotion, orientationAngles, scale);
                world.addEntity(emitter);
                emitter.spawnInitialParticles();
            } else {
                emitter.applyNetworkState(position, extrapolationMotion, inheritedMotion, orientationAngles, scale, false);
            }
        } else if (emitter != null) {
            emitter.applyNetworkState(position, extrapolationMotion, inheritedMotion, orientationAngles, scale, false);
        }
    }

    private JSONParticle getParticleDefinition() {
        AItemPack<?> item = PackParser.getItem(packID, systemName, subName);
        if (item == null || !(item.definition instanceof AJSONMultiModelProvider)) {
            return null;
        }
        AJSONMultiModelProvider definition = (AJSONMultiModelProvider) item.definition;
        if (definition.rendering == null || definition.rendering.particles == null || particleIndex < 0 || particleIndex >= definition.rendering.particles.size()) {
            return null;
        }
        return definition.rendering.particles.get(particleIndex);
    }

    private static void writeFloatPoint(Point3D point, ByteBuf buf) {
        buf.writeFloat((float) point.x);
        buf.writeFloat((float) point.y);
        buf.writeFloat((float) point.z);
    }

    private static Point3D readFloatPoint(ByteBuf buf) {
        return new Point3D(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }
}
