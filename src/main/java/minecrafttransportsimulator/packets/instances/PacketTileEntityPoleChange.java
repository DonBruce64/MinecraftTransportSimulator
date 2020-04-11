package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.instances.GUISign;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.ItemPole;
import minecrafttransportsimulator.items.packs.ItemPoleComponent;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.wrappers.WrapperGUI;
import minecrafttransportsimulator.wrappers.WrapperNBT;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.item.ItemStack;

/**Packet sent to poles to change their states.  This gets sent when a player clicks a pole on the client.
 * Packet does server-side checks to see if the player could change the pole, and if so, it applies those
 * changes and sends packets out to all clients to have them apply those changes as well.  This can either
 * be fired from the player clicking the pole directly, or by them clicking the confirm button in the pole
 * GUI to set text.  In the case of the former, text should be null.  In the case of the latter, text should
 * be a list of the text to apply to the sign.
 * 
 * @author don_bruce
 */
public class PacketTileEntityPoleChange extends APacketTileEntity<TileEntityPole>{
	private final Axis axis;
	private final List<String> textLines;
	
	public PacketTileEntityPoleChange(TileEntityPole pole, Axis axis, List<String> textLines){
		super(pole);
		this.axis = axis;
		this.textLines = textLines;
	}
	
	public PacketTileEntityPoleChange(ByteBuf buf){
		super(buf);
		this.axis = Axis.values()[buf.readByte()];
		if(buf.readBoolean()){
			byte textLineCount = buf.readByte();
			this.textLines = new ArrayList<String>();
			for(byte i=0; i<textLineCount; ++i){
				textLines.add(readStringFromBuffer(buf));
			}
		}else{
			this.textLines = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(axis.ordinal());
		buf.writeBoolean(textLines != null);
		if(textLines != null){
			buf.writeByte(textLines.size());
			for(String textLine : textLines){
				writeStringToBuffer(textLine, buf);
			}
		}
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityPole pole){
		//Check if we can do editing.
		if(world.isClient() || !ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()){
			if(textLines != null){
				//This is a packet attempting to change sign text.  Do so now.
				if(pole.components.containsKey(axis)){
					((TileEntityPole_Sign) pole.components.get(axis)).textLines.clear();
					((TileEntityPole_Sign) pole.components.get(axis)).textLines.addAll(textLines);
					return true;
				}else{
					return false;
				}
			}else if(player.isHoldingItem(ItemWrench.class)){
				//If the player clicked with a wrench, try to remove the component on the axis.
				if(pole.components.containsKey(axis)){
					ATileEntityPole_Component component = pole.components.get(axis);
					ItemStack poleStack = new ItemStack(MTSRegistry.packItemMap.get(component.definition.packID).get(component.definition.systemName));
					if(component.definition.general.textLines != null){
						WrapperNBT data = new WrapperNBT(poleStack);
						data.setStrings("textLines", ((TileEntityPole_Sign) component).textLines);
					}
					if(world.isClient() || player.isCreative() || player.addItem(poleStack)){
						pole.components.remove(axis);
						return true;
					}
				}
			}else if(player.isHoldingItem(ItemPoleComponent.class) && !player.isHoldingItem(ItemPole.class) && !pole.components.containsKey(axis)){
				//Player clicked with a component.  Add it.
				ItemPoleComponent component = ((ItemPoleComponent) player.getHeldStack().getItem());
				pole.components.put(axis, TileEntityPole.createComponent(component.definition));
				if(player.getHeldStack().hasTagCompound() && component.definition.general.textLines != null){
					//Sign.  Restore text.
					((TileEntityPole_Sign) pole.components.get(axis)).textLines.clear();
					((TileEntityPole_Sign) pole.components.get(axis)).textLines.addAll(new WrapperNBT(player.getHeldStack()).getStrings("textLines", component.definition.general.textLines.length));
				}
				return true;
			}else if(pole.components.get(axis) instanceof TileEntityPole_Sign && pole.components.get(axis).definition.general.textLines != null && !world.isClient()){
				//Player clicked a sign with editable text.  Fire back a packet ONLY to the player who sent this to have them open the sign GUI.
				player.sendPacket(new PacketTileEntityPoleChange(pole, axis, textLines));
				return false;
			}
		}
		
		//Check if we are getting a callback packet to open the sign GUI.
		if(world.isClient() && pole.components.get(axis) instanceof TileEntityPole_Sign){
			WrapperGUI.openGUI(new GUISign(pole, axis));
		}
		return false;
	}
}
