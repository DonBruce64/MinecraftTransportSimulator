package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;
import minecrafttransportsimulator.packloading.PackParser;

/**
 * Packet used to change instruments on entities.  Sent to the server
 * to process the instrument change, and then sent to all clients if
 * the change is able to be made.  Does not check ownership as that's
 * done before {@link GUIInstruments} is opened by checking for
 * ownership in {@link ItemItem#doVehicleInteraction}.
 *
 * @author don_bruce
 */
public class PacketEntityInstrumentChange extends APacketEntityInteract<AEntityE_Interactable<?>, IWrapperPlayer> {
    private final int slot;
    private final String instrumentPackID;
    private final String instrumentSystemName;

    public PacketEntityInstrumentChange(AEntityE_Interactable<?> entity, IWrapperPlayer player, int slot, ItemInstrument instrument) {
        super(entity, player);
        this.slot = slot;
        if (instrument != null) {
            this.instrumentPackID = instrument.definition.packID;
            this.instrumentSystemName = instrument.definition.systemName;
        } else {
            this.instrumentPackID = "";
            this.instrumentSystemName = "";
        }
    }

    public PacketEntityInstrumentChange(ByteBuf buf) {
        super(buf);
        this.slot = buf.readInt();
        this.instrumentPackID = readStringFromBuffer(buf);
        this.instrumentSystemName = readStringFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeInt(slot);
        writeStringToBuffer(instrumentPackID, buf);
        writeStringToBuffer(instrumentSystemName, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, IWrapperPlayer player) {
        //Check to make sure the instrument can fit in survival player's inventories.
        //Only check this on the server, as adding things to the client doesn't do us any good.
        if (!world.isClient() && !player.isCreative() && entity.instruments.get(slot) != null) {
            if (!player.isCreative() && !player.getInventory().addStack(entity.instruments.get(slot).getNewStack(null))) {
                return false;
            }
        }

        //If we are removing the instrument, do so now.
        //Otherwise add the instrument.
        if (instrumentPackID.isEmpty()) {
            entity.removeIntrument(slot);
        } else {
            //Check to make sure player has the instrument they are trying to put in.
            //This is only done on the server, as checking on the client won't make any difference.
            ItemInstrument instrument = PackParser.getItem(instrumentPackID, instrumentSystemName);
            if (!world.isClient() && !player.isCreative()) {
                int stackIndex = player.getInventory().getSlotForStack(instrument.getNewStack(null));
                if (stackIndex != -1) {
                    player.getInventory().removeFromSlot(stackIndex, 1);
                } else {
                    return false;
                }
            }
            entity.addInstrument(instrument, slot);
        }
        return true;
    }
}
