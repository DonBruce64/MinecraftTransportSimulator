package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send signals to guns.  This can be either to change the state of the gun,
 * or to re-load the gun with the specified bullets.  If we are doing state commands, then
 * this packet first gets sent to the server from the client who requested the command.  After this,
 * it is send to all players tracking the gun (if applicable).  If this packet is for re-loading bullets, then it will
 * only appear on clients after the server has verified the bullets can in fact be loaded.
 *
 * @author don_bruce
 */
public class PacketPartGun extends APacketEntity<PartGun> {
    private final Request stateRequest;
    private final ItemBullet bulletItem;

    public PacketPartGun(PartGun gun, Request stateRequest) {
        super(gun);
        this.stateRequest = stateRequest;
        this.bulletItem = null;
    }

    public PacketPartGun(PartGun gun, ItemBullet bullet) {
        super(gun);
        this.stateRequest = Request.RELOAD_ONCLIENT;
        this.bulletItem = bullet;
    }

    public PacketPartGun(ByteBuf buf) {
        super(buf);
        this.stateRequest = Request.values()[buf.readByte()];
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            this.bulletItem = readItemFromBuffer(buf);
        } else {
            this.bulletItem = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(stateRequest.ordinal());
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            writeItemToBuffer(bulletItem, buf);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGun gun) {
        switch (stateRequest) {
            case RELOAD_ONCLIENT: {
                gun.clientNextBullet = bulletItem;
                break;
            }
            case RELOAD_HAND_ON: {
                gun.isHandHeldGunReloadRequested = true;
                break;
            }
            case RELOAD_HAND_OFF: {
                gun.isHandHeldGunReloadRequested = false;
                break;
            }
            case TRIGGER_ON: {
                gun.playerHoldingTrigger = true;
                break;
            }
            case TRIGGER_OFF: {
                gun.playerHoldingTrigger = false;
                break;
            }
            case AIM_ON: {
                gun.isHandHeldGunAimed = true;
                break;
            }
            case AIM_OFF: {
                gun.isHandHeldGunAimed = false;
                break;
            }
            case BULLETS_OUT: {
                gun.bulletsPresentOnServer = false;
                break;
            }
            case BULLETS_PRESENT: {
                gun.bulletsPresentOnServer = true;
                break;
            }
            case KNOCKBACK: {
                gun.performGunKnockback();
                break;
            }
        }
        return stateRequest.sendToClients;
    }

    public static enum Request {
        RELOAD_ONCLIENT(false),
        RELOAD_HAND_ON(false),
        RELOAD_HAND_OFF(false),
        TRIGGER_ON(true),
        TRIGGER_OFF(true),
        AIM_ON(true),
        AIM_OFF(true),
        BULLETS_OUT(false),
        BULLETS_PRESENT(false),
        KNOCKBACK(true);

        private final boolean sendToClients;

        private Request(boolean sendToClients) {
            this.sendToClients = sendToClients;
        }
    }
}
