package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.blocks.TileEntityPropellerBench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PropellerBenchUpdatePacket implements IMessage{
	private int x;
	private int y;
	private int z;
	private int playerID;
	private byte propellerType;
	private byte numberBlades;
	private byte pitch;
	private byte diameter;
	private long timeOperationFinished;

	public PropellerBenchUpdatePacket() {}
	
	public PropellerBenchUpdatePacket(TileEntityPropellerBench tile, EntityPlayer player){
		this.x = tile.getPos().getX();
		this.y = tile.getPos().getY();
		this.z = tile.getPos().getZ();
		this.playerID = player.getEntityId();
		this.propellerType = tile.propellerType;
		this.numberBlades = tile.numberBlades;
		this.pitch = tile.pitch;
		this.diameter = tile.diameter;
		this.timeOperationFinished = tile.timeOperationFinished;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.x=buf.readInt();
		this.y=buf.readInt();
		this.z=buf.readInt();
		this.playerID=buf.readInt();
		this.propellerType=buf.readByte();
		this.numberBlades=buf.readByte();
		this.pitch=buf.readByte();
		this.diameter=buf.readByte();
		this.timeOperationFinished=buf.readLong();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeInt(this.playerID);
		buf.writeByte(this.propellerType);
		buf.writeByte(this.numberBlades);
		buf.writeByte(this.pitch);
		buf.writeByte(this.diameter);
		buf.writeLong(this.timeOperationFinished);
	}

	public static class Handler implements IMessageHandler<PropellerBenchUpdatePacket, IMessage>{
		public IMessage onMessage(final PropellerBenchUpdatePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					TileEntityPropellerBench bench;
					if(ctx.side.isServer()){
						bench = (TileEntityPropellerBench) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(new BlockPos(message.x, message.y, message.z));
						if(message.timeOperationFinished != 0){
							EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.playerID);
							if(player == null){
								return;
							}else if(!player.capabilities.isCreativeMode){
								byte numberBladeMaterials = (byte) (message.diameter < 90 ? message.numberBlades : message.numberBlades*2);
								if(message.propellerType == 0){
									if(!player.inventory.hasItemStack(new ItemStack(Blocks.PLANKS, numberBladeMaterials)) || !player.inventory.hasItemStack(new ItemStack(Items.IRON_INGOT, 1)) || !player.inventory.hasItemStack(new ItemStack(Items.REDSTONE, 5))){
										return;
									}else{
										player.inventory.clearMatchingItems(Item.getItemFromBlock(Blocks.PLANKS), -1, numberBladeMaterials, null);
										player.inventory.clearMatchingItems(Items.IRON_INGOT, -1, 1, null);
										player.inventory.clearMatchingItems(Items.REDSTONE, -1, 5, null);
									}
								}else if(message.propellerType == 1){
									if(!player.inventory.hasItemStack(new ItemStack(Items.IRON_INGOT, 1 + numberBladeMaterials)) || !player.inventory.hasItemStack(new ItemStack(Items.REDSTONE, 5))){
										return;
									}else{
										player.inventory.clearMatchingItems(Items.IRON_INGOT, -1, 1 + numberBladeMaterials, null);
										player.inventory.clearMatchingItems(Items.REDSTONE, -1, 5, null);
									}
								}else if(message.propellerType == 2){
									if(!player.inventory.hasItemStack(new ItemStack(Blocks.OBSIDIAN, numberBladeMaterials)) || !player.inventory.hasItemStack(new ItemStack(Items.IRON_INGOT, 1)) || !player.inventory.hasItemStack(new ItemStack(Items.REDSTONE, 5))){
										return;
									}else{
										player.inventory.clearMatchingItems(Item.getItemFromBlock(Blocks.OBSIDIAN), -1, numberBladeMaterials, null);
										player.inventory.clearMatchingItems(Items.IRON_INGOT, -1, 1, null);
										player.inventory.clearMatchingItems(Items.REDSTONE, -1, 5, null);
									}
								}
							}
						}
					}else{
						bench = (TileEntityPropellerBench) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					}
					if(bench != null){
						bench.propellerType = message.propellerType;
						bench.numberBlades = message.numberBlades;
						bench.pitch = message.pitch;
						bench.diameter = message.diameter;
						bench.timeOperationFinished = message.timeOperationFinished;
					}
					if(ctx.side.isServer()){
						MTS.MTSNet.sendToAll(message);
					}
				}
			});
			return null;
		}
	}	
}
