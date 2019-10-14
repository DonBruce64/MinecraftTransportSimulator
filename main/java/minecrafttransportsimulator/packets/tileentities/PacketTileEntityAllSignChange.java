package minecrafttransportsimulator.packets.tileentities;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.blocks.pole.TileEntityPoleSign;
import minecrafttransportsimulator.mcinterface.MTSNetwork;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.nbt.NBTTagCompound;


/**This packet is sent to servers when a player changes a sign.
 * The sign the player changed it to, as well as any text they
 * set, is sent.  If the change is valid, this packet is sent
 * to all clients.
 * 
 * @author don_bruce
 */
public class PacketTileEntityAllSignChange extends APacketTileEntity{
	private String definition;
	private List<String> text = new ArrayList<String>();
	private int playerID;

	public PacketTileEntityAllSignChange(){}
	
	public PacketTileEntityAllSignChange(TileEntityPoleSign tile, String definition, List<String> text, int playerID){
		super(tile);
		this.definition = definition;
		this.text = text;
		this.playerID = playerID;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		super.parseFromNBT(tag);
		definition = tag.getString("definition");
		if(PackParserSystem.getSign(definition).general.textLines != null){
			for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
				text.add(tag.getString("text_" + i));
			}
		}
		playerID = tag.getInteger("playerID");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		super.convertToNBT(tag);
		tag.setString("definition", definition);
		if(PackParserSystem.getSign(definition).general.textLines != null){
			for(byte i=0; i<PackParserSystem.getSign(definition).general.textLines.length; ++i){
				tag.setString("text_" + i, text.get(i));
			}
		}
		tag.setInteger("playerID", playerID);
	}

	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		TileEntityPoleSign sign = (TileEntityPoleSign) getTileEntity(world);
		if(ConfigSystem.getBooleanConfig("OPSignEditingOnly")){
			if(!(new MTSPlayerInterface(world.getEntity(playerID))).isOP()){
				return;
			}
		}
		if(sign != null){
			sign.definition = definition;
			sign.text = text;
			if(onServer){
				MTSNetwork.sendPacketToClients(this);
			}
		}
	}
}
