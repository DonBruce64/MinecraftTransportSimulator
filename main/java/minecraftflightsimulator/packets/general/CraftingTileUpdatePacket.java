package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.blocks.TileEntityCrafter;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class CraftingTileUpdatePacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private short propertyCode;

	public CraftingTileUpdatePacket() {}
	
	public CraftingTileUpdatePacket(TileEntityCrafter tile, short propertyCode){
		this.x = tile.xCoord;
		this.y = tile.yCoord;
		this.z = tile.zCoord;
		this.propertyCode = propertyCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.propertyCode=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeShort(this.propertyCode);
	}

	public static class CraftingTileUpdatePacketHandler implements IMessageHandler<CraftingTileUpdatePacket, CraftingTileUpdatePacket> {
		public CraftingTileUpdatePacket onMessage(CraftingTileUpdatePacket message, MessageContext ctx){
			TileEntityCrafter tile;
			if(ctx.side.isServer()){
				tile = (TileEntityCrafter) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.x, message.y, message.z);
			}else{
				tile = (TileEntityCrafter) Minecraft.getMinecraft().theWorld.getTileEntity(message.x, message.y, message.z);
			}
			
			if(tile != null){
				if(message.propertyCode > 0){
					tile.propertyCode = message.propertyCode;
				}else if(message.propertyCode < 0){
					if(tile.getStackInSlot(3) == null){
						tile.isOn = true;
						if(tile.timeLeft == 0){
							tile.timeLeft = -message.propertyCode;
						}
					}
				}else{
					tile.isOn = false;
					tile.timeLeft = 0;
				}
			}
			return ctx.side.isServer() ? message : null;
		}
	}	
}
