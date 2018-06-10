package minecrafttransportsimulator.packets.crafting;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.core.TileEntityPropellerBench;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.parts.ItemPartPropeller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PropellerBenchUpdatePacket extends APacketCrafting{
	private long timeOperationFinished;
	private String selectedPropeller;
	private String propellerOnBench;

	public PropellerBenchUpdatePacket(){}
	
	public PropellerBenchUpdatePacket(TileEntityPropellerBench tile, EntityPlayer player){
		super(tile, player);
		this.timeOperationFinished = tile.timeOperationFinished;
		this.selectedPropeller = tile.selectedPropeller.partName;
		this.propellerOnBench = tile.propellerOnBench.partName;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.timeOperationFinished = buf.readLong();
		this.selectedPropeller = ByteBufUtils.readUTF8String(buf);
		this.propellerOnBench = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeLong(this.timeOperationFinished);
		ByteBufUtils.writeUTF8String(buf, this.selectedPropeller);
		ByteBufUtils.writeUTF8String(buf, this.propellerOnBench);
	}

	public static class Handler implements IMessageHandler<PropellerBenchUpdatePacket, IMessage>{
		public IMessage onMessage(final PropellerBenchUpdatePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityPropellerBench bench = (TileEntityPropellerBench) getTileEntity(message, ctx);
					EntityPlayer player = getPlayer(message, ctx);
					
					if(ctx.side.isServer() && message.timeOperationFinished != 0 && bench != null && player != null){
						//Start button was clicked.  Remove materials and start process.
						if(!doesPlayerHaveMaterials(player, message.selectedPropeller)){
							return;
						}
						removeMaterials(player, message.selectedPropeller);
						bench.selectedPropeller = (ItemPartPropeller) MTSRegistry.partItemMap.get(message.selectedPropeller);
						MTS.MTSNet.sendToAll(message);
					}else if(ctx.side.isClient() && bench != null){
						//If we are on the client side, update everything.
						bench.selectedPropeller = (ItemPartPropeller) MTSRegistry.partItemMap.get(message.selectedPropeller);
						bench.propellerOnBench = (ItemPartPropeller) MTSRegistry.partItemMap.get(message.propellerOnBench);
						bench.timeOperationFinished = message.timeOperationFinished;
					}
				}
			});
			return null;
		}
	}	
}
