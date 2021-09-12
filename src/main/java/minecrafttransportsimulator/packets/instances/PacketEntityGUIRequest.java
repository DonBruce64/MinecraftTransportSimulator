package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
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
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet sent to entities to request a GUI be opened on them.  The GUI to be sent is an enum
 * and is used to open the proper GUI.  This packet is sent from servers the specific clients
 * when they click on something to open a GUI.  We do this as it lets us do validation, and prevents
 * handling the request on multiple clients, where multiple GUIs may be opened.
 * 
 * @author don_bruce
 */
public class PacketEntityGUIRequest extends APacketEntityInteract<AEntityB_Existing, WrapperPlayer>{
	private final EntityGUIType guiRequested;
	
	public PacketEntityGUIRequest(AEntityB_Existing entity, WrapperPlayer player, EntityGUIType guiRequested){
		super(entity, player);
		this.guiRequested = guiRequested;
	}
	
	public PacketEntityGUIRequest(ByteBuf buf){
		super(buf);
		this.guiRequested = EntityGUIType.values()[buf.readByte()];
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(guiRequested.ordinal());
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityB_Existing entity, WrapperPlayer player){
		switch(guiRequested){
			case INSTRUMENTS: InterfaceGUI.openGUI(new GUIInstruments((EntityVehicleF_Physics) entity)); break;
			case INVENTORY_CHEST: InterfaceGUI.openGUI(new GUIInventoryContainer(((TileEntityChest) entity).inventory, ((TileEntityChest) entity).definition.decor.inventoryTexture)); break;
			case FUEL_PUMP: InterfaceGUI.openGUI(new GUIFuelPump((TileEntityFuelPump) entity, false)); break;
			case FUEL_PUMP_CONFIG: InterfaceGUI.openGUI(new GUIFuelPump((TileEntityFuelPump) entity, true)); break;
			case PACK_EXPORTER: InterfaceGUI.openGUI(new GUIPackExporter((EntityVehicleF_Physics) entity));	break;
			case PAINT_GUN: InterfaceGUI.openGUI(new GUIPaintGun((AEntityC_Definable<?>) entity, player));	break;
			case PART_BENCH: InterfaceGUI.openGUI(new GUIPartBench(((TileEntityDecor) entity).definition.decor.crafting)); break;
			case RADIO: InterfaceGUI.openGUI(new GUIRadio(entity.radio)); break;
			case SIGNAL_CONTROLLER: InterfaceGUI.openGUI(new GUISignalController((TileEntitySignalController) entity)); break;
			case TEXT_EDITOR: InterfaceGUI.openGUI(new GUITextEditor((AEntityC_Definable<?>) entity)); break;
		}
		return true;
	}
	
	public static enum EntityGUIType{
		INSTRUMENTS,
		INVENTORY_CHEST,
		FUEL_PUMP,
		FUEL_PUMP_CONFIG,
		PACK_EXPORTER,
		PAINT_GUN,
		PART_BENCH,
		RADIO,
		SIGNAL_CONTROLLER,
		TEXT_EDITOR;
		
	}
}
