package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIFuelPump;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperItemStack;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteract;

/**Packet sent to pumps to allow dispensing of fluids to vehicles.  This will remove an item
 * from the player's inventory for the use of the pump if they have it, and will then allow
 * the pump to draw that much fluid into it for pumping.  However, if an amount is given, it is
 * assumed that this packet is changing the amount to pump, not requesting the pumping be started.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFuelPumpDispense extends APacketEntityInteract<TileEntityFuelPump, WrapperPlayer>{
	private final int slotClicked;
	private final int amountChangedTo;
	
	public PacketTileEntityFuelPumpDispense(TileEntityFuelPump pump, WrapperPlayer player, int slotClicked, int amountChangedTo){
		super(pump, player);
		this.slotClicked = slotClicked;
		this.amountChangedTo = amountChangedTo;
	}
	
	public PacketTileEntityFuelPumpDispense(TileEntityFuelPump pump, WrapperPlayer player, int slotClicked){
		super(pump, player);
		this.slotClicked = slotClicked;
		this.amountChangedTo = -1;
	}
	
	public PacketTileEntityFuelPumpDispense(ByteBuf buf){
		super(buf);
		this.slotClicked = buf.readInt();
		this.amountChangedTo = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(slotClicked);
		buf.writeInt(amountChangedTo);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, TileEntityFuelPump pump, WrapperPlayer player){
		if(amountChangedTo != -1){
			pump.fuelAmounts.set(slotClicked, amountChangedTo);
			return true;
		}else{
			WrapperItemStack stack = pump.fuelItems.getStack(slotClicked);
			if(player.getInventory().removeStack(stack, stack.getSize())){
				pump.fuelPurchasedRemaining += pump.fuelAmounts.get(slotClicked);
				if(world.isClient() && player.equals(InterfaceClient.getClientPlayer()) && AGUIBase.activeInputGUI instanceof GUIFuelPump){
					AGUIBase.activeInputGUI.close();
				}
				return true;
			}else{
				return false;
			}
		}
	}
}
