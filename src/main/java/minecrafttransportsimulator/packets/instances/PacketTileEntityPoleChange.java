package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import mcinterface.BuilderGUI;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.ConfigSystem;

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
public class PacketTileEntityPoleChange extends APacketTileEntity<TileEntityPole>{
	private final Axis axis;
	private final String packID;
	private final String systemName;
	private final List<String> textLines;
	private final boolean removal;
	
	public PacketTileEntityPoleChange(TileEntityPole pole, Axis axis, ItemPoleComponent component, List<String> textLines, boolean removal){
		super(pole);
		this.axis = axis;
		this.packID = component != null ? component.definition.packID : "";
		this.systemName = component != null ? component.definition.systemName : "";
		this.textLines = textLines;
		this.removal = removal;
	}
	
	public PacketTileEntityPoleChange(ByteBuf buf){
		super(buf);
		this.axis = Axis.values()[buf.readByte()];
		this.packID = readStringFromBuffer(buf);
		this.systemName = readStringFromBuffer(buf);
		if(buf.readBoolean()){
			byte textLineCount = buf.readByte();
			this.textLines = new ArrayList<String>();
			for(byte i=0; i<textLineCount; ++i){
				textLines.add(readStringFromBuffer(buf));
			}
		}else{
			this.textLines = null;
		}
		this.removal = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(axis.ordinal());
		writeStringToBuffer(packID, buf);
		writeStringToBuffer(systemName, buf);
		buf.writeBoolean(textLines != null);
		if(textLines != null){
			buf.writeByte(textLines.size());
			for(String textLine : textLines){
				writeStringToBuffer(textLine, buf);
			}
		}
		buf.writeBoolean(removal);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityPole pole){
		//Check if we can do editing.
		if(world.isClient() || !ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()){
			if(removal){
				//Player clicked with a wrench, try to remove the component on the axis.
				if(pole.components.containsKey(axis)){
					ATileEntityPole_Component component = pole.components.get(axis);
					AItemBase item = MTSRegistry.packItemMap.get(component.definition.packID).get(component.definition.systemName);
					WrapperNBT data = null;
					if(component.getTextLines() != null){
						data = new WrapperNBT();
						data.setStrings("textLines", component.getTextLines());
					}
					if(world.isClient() || player.isCreative() || player.addItem(item, data)){
						pole.components.remove(axis);
						pole.updateLightState();
						return true;
					}
				}
			}else if(packID.isEmpty() && textLines == null){
				if(pole.components.get(axis).getTextLines() != null){
					if(world.isClient()){
						BuilderGUI.openGUI(new GUITextEditor(pole, axis));
					}else{
						//Player clicked a component  with editable text.  Fire back a packet ONLY to the player who sent this to have them open the sign GUI.
						player.sendPacket(new PacketTileEntityPoleChange(pole, axis, null, null, false));
					}
				}
				return false;
			}if(packID.isEmpty() && textLines != null){
				//This is a packet attempting to change component text.  Do so now.
				if(pole.components.containsKey(axis)){
					pole.components.get(axis).setTextLines(textLines);
					return true;
				}
			}else if(!packID.isEmpty() && !pole.components.containsKey(axis)){
				//Player clicked with a component.  Add it.
				ItemPoleComponent componentItem = (ItemPoleComponent) MTSRegistry.packItemMap.get(packID).get(systemName);
				ATileEntityPole_Component newComponent = TileEntityPole.createComponent(componentItem.definition);
				pole.components.put(axis, newComponent);
				if(textLines != null && newComponent.getTextLines() != null){
					newComponent.setTextLines(textLines);
				}
				pole.updateLightState();
				player.removeStack(player.getHeldStack(), 1);
				return true;
			} 
		}
		return false;
	}
}
