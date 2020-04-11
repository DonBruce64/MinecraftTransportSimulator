package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole_Sign;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.guis.GUISign;
import minecrafttransportsimulator.items.core.ItemWrench;
import minecrafttransportsimulator.items.packs.ItemPole;
import minecrafttransportsimulator.items.packs.ItemPoleComponent;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.wrappers.WrapperPlayer;
import minecrafttransportsimulator.wrappers.WrapperWorld;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**Packet sent to poles to change their states.  This gets sent when a player clicks a pole on the client.
 * Packet does server-side checks to see if the player could change the pole, and if so, it applies those
 * changes and sends packets out to all clients to have them apply those changes as well.  This can either
 * be fired from the player clicking the pole directly, or by them clicking the confirm button in the pole
 * GUI to set text.  In the case of the former, text should be null.  In the case of the latter, text should
 * be an empty list.
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
			//If the player clicked with a wrench, try to remove the component on the axis.
			if(player.isHoldingItem(ItemWrench.class)){
				if(pole.components.containsKey(axis)){
					ATileEntityPole_Component component = pole.components.get(axis);
					if(world.isClient() || player.isCreative() || player.addItem(new ItemStack(MTSRegistry.packItemMap.get(component.definition.packID).get(component.definition.systemName)))){
						pole.components.remove(axis);
						return true;
					}
				}
			}else if(player.isHoldingItem(ItemPoleComponent.class) && !player.isHoldingItem(ItemPole.class) && !pole.components.containsKey(axis)){
				//Player clicked with a component.  Add it.
				pole.components.put(axis, TileEntityPole.createComponent(((ItemPoleComponent) player.getHeldStack().getItem()).definition));
				return true;
			}else if(pole.components.get(axis) instanceof TileEntityPole_Sign){
				//Player clicked a sign.  Fire back a packet ONLY to the player who sent this to have them open the sign GUI.
				player.sendPacket(new PacketTileEntityPoleChange(pole, axis, textLines));
				return false;
			}
		}
		
		//Check if we are getting a callback packet to open the sign GUI.
		if(world.isClient() && pole.components.get(axis) instanceof TileEntityPole_Sign){
			//TODO this needs to get done with the generic GUI system.
			FMLCommonHandler.instance().showGuiScreen(new GUISign((TileEntityPole_Sign) pole.components.get(axis)));
		}
		return false;
	}
}
