package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.rendering.components.ITextProvider;
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
public class PacketTileEntityPoleChange extends APacketTileEntity<TileEntityPole>{
	private final Axis axis;
	private final ItemPoleComponent componentItem;
	private final List<String> textLines;
	private final boolean removal;
	
	public PacketTileEntityPoleChange(TileEntityPole pole, Axis axis, ItemPoleComponent componentItem, List<String> textLines, boolean removal){
		super(pole);
		this.axis = axis;
		this.componentItem = componentItem;
		this.textLines = textLines;
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
		if(componentItem != null){
			buf.writeBoolean(true);
			writeStringToBuffer(componentItem.definition.packID, buf);
			writeStringToBuffer(componentItem.definition.systemName, buf);
			writeStringToBuffer(componentItem.subName, buf);
		}else{
			buf.writeBoolean(false);
		}
		if(textLines != null){
			buf.writeBoolean(true);
			buf.writeByte(textLines.size());
			for(String textLine : textLines){
				writeStringToBuffer(textLine, buf);
			}
		}else{
			buf.writeBoolean(false);
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
					WrapperNBT data = null;
					if(component instanceof ITextProvider && component.definition.rendering != null && component.definition.rendering.textObjects != null){
						data = new WrapperNBT();
						int lineNumber = 0;
						for(String textLine : ((ITextProvider) component).getText().values()){
							data.setString("textLine" + lineNumber++, textLine);
						}
					}
					if(world.isClient() || player.isCreative() || player.getInventory().addItem(component.item, data)){
						pole.components.remove(axis);
						pole.updateLightState();
						return true;
					}
				}
			}else if(componentItem == null && textLines == null){
				if(pole.components.get(axis) instanceof ITextProvider && pole.components.get(axis).definition.rendering != null && pole.components.get(axis).definition.rendering.textObjects != null){
					if(world.isClient()){
						InterfaceGUI.openGUI(new GUITextEditor(pole, axis));
					}else{
						//Player clicked a component  with editable text.  Fire back a packet ONLY to the player who sent this to have them open the sign GUI.
						player.sendPacket(new PacketTileEntityPoleChange(pole, axis, null, null, false));
					}
				}
				return false;
			}if(componentItem == null && textLines != null){
				//This is a packet attempting to change component text.  Do so now.
				if(pole.components.containsKey(axis)){
					ATileEntityPole_Component component = pole.components.get(axis);
					if(component instanceof ITextProvider){
						int linesChecked = 0;
						for(Entry<JSONText, String> textEntry : ((ITextProvider) component).getText().entrySet()){
							textEntry.setValue(textLines.get(linesChecked));
							++linesChecked;
						}
					}
					return true;
				}
			}else if(componentItem != null && !pole.components.containsKey(axis)){
				//Player clicked with a component.  Add it.
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(pole, componentItem);
				pole.components.put(axis, newComponent);
				if(textLines != null && newComponent instanceof ITextProvider){
					int linesChecked = 0;
					for(Entry<JSONText, String> textEntry : ((ITextProvider) newComponent).getText().entrySet()){
						textEntry.setValue(textLines.get(linesChecked));
						++linesChecked;
					}
				}
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
