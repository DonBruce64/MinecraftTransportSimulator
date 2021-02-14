package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet sent to poles to change their states.  This gets sent when a player clicks a pole on the client.
 * Packet does server-side checks to see if the player could change the pole, and if so, it applies those
 * changes and sends packets out to all clients to have them apply those changes as well.  This can either
 * be fired from the player clicking the pole directly, or by them clicking the confirm button in the pole
 * GUI to set text.  If this packet is sent with a null component and text lines, it is assumed it's from
 * a sign GUI and the text needs to be applied to the sign.  If both the component and text is null, it's
 * assumed that the player is trying to open the sign GUI to start editing text.  The removal flag will
 * cause the component at the axis to be removed, so the component and textLines parameters may be null
 * as they are ignored.
 * 
 * @author don_bruce
 */
public class PacketTileEntityPoleChange extends APacketEntity<TileEntityPole>{
	private final Axis axis;
	private final ItemPoleComponent componentItem;
	private final WrapperNBT data;
	private final boolean removal;
	
	public PacketTileEntityPoleChange(TileEntityPole pole, Axis axis, ItemPoleComponent componentItem, WrapperNBT data, boolean removal){
		super(pole);
		this.axis = axis;
		this.componentItem = componentItem;
		this.data = data;
		this.removal = removal;
	}
	
	public PacketTileEntityPoleChange(ByteBuf buf){
		super(buf);
		this.axis = Axis.values()[buf.readByte()];
		if(buf.readBoolean()){
			this.componentItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf), readStringFromBuffer(buf));
		}else{
			this.componentItem = null;
		}
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
		}else{
			this.data = null;
		}
		this.removal = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(axis.ordinal());
		if(componentItem != null){
			buf.writeBoolean(true);
			writeStringToBuffer(componentItem.definition.packID, buf);
			writeStringToBuffer(componentItem.definition.systemName, buf);
			writeStringToBuffer(componentItem.subName, buf);
		}else{
			buf.writeBoolean(false);
		}
		if(data != null){
			buf.writeBoolean(true);
			writeDataToBuffer(data, buf);
		}else{
			buf.writeBoolean(false);
		}
		buf.writeBoolean(removal);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityPole pole){
		ATileEntityPole_Component component = pole.components.get(axis);
		
		//Check if we can do editing.
		if(world.isClient() || !ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()){
			if(removal){
				//Player clicked with a wrench, try to remove the component on the axis.
				if(pole.components.containsKey(axis)){
					WrapperNBT removedComponentData = new WrapperNBT();
					component.save(removedComponentData);
					if(world.isClient() || player.isCreative() || player.getInventory().addItem(component.getItem(), removedComponentData)){
						pole.components.remove(axis);
						pole.updateLightState();
						return true;
					}
				}
			}else if(componentItem == null){
				//Player didn't click with anything.  See if we can exit text.
				if(!pole.components.get(axis).text.isEmpty()){
					if(world.isClient()){
						InterfaceGUI.openGUI(new GUITextEditor(component));
					}else{
						//Player clicked a component  with editable text.  Fire back a packet ONLY to the player who sent this to have them open the sign GUI.
						player.sendPacket(new PacketTileEntityPoleChange(pole, axis, null, null, false));
					}
				}
				return false;
			}else if(componentItem != null && !pole.components.containsKey(axis)){
				//Player clicked with a component.  Add it.
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(pole, data);
				newComponent.variablesOn.add(LightType.UNLINKEDLIGHT.lowercaseName);
				newComponent.variablesOn.add(LightType.STREETLIGHT.lowercaseName);
				pole.components.put(axis, newComponent);
				pole.updateLightState();
				if(!player.isCreative()){
					player.getInventory().removeStack(player.getHeldStack(), 1);
				}
				return true;
			}
		}
		return false;
	}
}
