package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.guis.instances.GUIFuelPump;
import minecrafttransportsimulator.guis.instances.GUIInstruments;
import minecrafttransportsimulator.guis.instances.GUIInventoryContainer;
import minecrafttransportsimulator.guis.instances.GUIPackExporter;
import minecrafttransportsimulator.guis.instances.GUIPaintGun;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.guis.instances.GUISignalController;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**
 * Packet sent to entities to request a GUI be opened on them.  The GUI to be sent is an enum
 * and is used to open the proper GUI.  This packet is sent from servers the specific clients
 * when they click on something to open a GUI.  We do this as it lets us do validation, and prevents
 * handling the request on multiple clients, where multiple GUIs may be opened.
 *
 * @author don_bruce
 */
public class PacketEntityGUIRequest extends APacketEntityInteract<AEntityB_Existing, IWrapperPlayer> {
    private final EntityGUIType guiRequested;

    public PacketEntityGUIRequest(AEntityB_Existing entity, IWrapperPlayer player, EntityGUIType guiRequested) {
        super(entity, player);
        this.guiRequested = guiRequested;
    }

    public PacketEntityGUIRequest(ByteBuf buf) {
        super(buf);
        this.guiRequested = EntityGUIType.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(guiRequested.ordinal());
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityB_Existing entity, IWrapperPlayer player) {
        switch (guiRequested) {
            case INSTRUMENTS:
                new GUIInstruments((EntityVehicleF_Physics) entity);
                break;
            case INVENTORY_CHEST:
                new GUIInventoryContainer(((TileEntityChest) entity).inventory, ((TileEntityChest) entity).definition.decor.inventoryTexture, false);
                break;
            case FUEL_PUMP:
                new GUIFuelPump((TileEntityFuelPump) entity, false);
                break;
            case FUEL_PUMP_CONFIG:
                new GUIFuelPump((TileEntityFuelPump) entity, true);
                break;
            case PACK_EXPORTER:
                new GUIPackExporter((EntityVehicleF_Physics) entity);
                break;
            case PAINT_GUN:
                new GUIPaintGun((AEntityD_Definable<?>) entity, player);
                break;
            case PART_BENCH:
                new GUIPartBench(((TileEntityDecor) entity).definition.decor.crafting);
                break;
            case RADIO:
                new GUIRadio((EntityRadio) entity);
                break;
            case SIGNAL_CONTROLLER:
                new GUISignalController((TileEntitySignalController) entity);
                break;
            case TEXT_EDITOR:
                new GUITextEditor((AEntityD_Definable<?>) entity);
                break;
        }
        return true;
    }

    public enum EntityGUIType {
        INSTRUMENTS,
        INVENTORY_CHEST,
        FUEL_PUMP,
        FUEL_PUMP_CONFIG,
        PACK_EXPORTER,
        PAINT_GUN,
        PART_BENCH,
        RADIO,
        SIGNAL_CONTROLLER,
        TEXT_EDITOR

    }
}
