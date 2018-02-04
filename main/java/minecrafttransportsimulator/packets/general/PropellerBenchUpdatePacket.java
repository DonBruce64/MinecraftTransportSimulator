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
					}else{
						bench = (TileEntityPropellerBench) Minecraft.getMinecraft().theWorld.getTileEntity(new BlockPos(message.x, message.y, message.z));
					}
					if(bench != null){
						if(ctx.side.isServer() && message.timeOperationFinished != 0){
							EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.playerID);
							if(player == null){
								return;
							}else if(!player.capabilities.isCreativeMode){
								int numberPlayerPlanks = 0;
								int numberPlayerIronIngots = 0;
								int numberPlayerObsidian = 0;
								int numberPlayerRedstone = 0;
								for(ItemStack stack : player.inventory.mainInventory){
									if(stack != null){
										if(stack.getItem().equals(Item.getItemFromBlock(Blocks.PLANKS))){
											numberPlayerPlanks+=stack.stackSize;
										}else if(stack.getItem().equals(Items.IRON_INGOT)){
											numberPlayerIronIngots+=stack.stackSize;
										}else if(stack.getItem().equals(Item.getItemFromBlock(Blocks.OBSIDIAN))){
											numberPlayerObsidian+=stack.stackSize;
										}else if(stack.getItem().equals(Items.REDSTONE)){
											numberPlayerRedstone+=stack.stackSize;
										}
									}
								}
								
								Item propellerMaterial;
								switch(message.propellerType){
									case(0): propellerMaterial = Item.getItemFromBlock(Blocks.PLANKS); break;
									case(1): propellerMaterial = Items.IRON_INGOT; break;
									case(2): propellerMaterial = Item.getItemFromBlock(Blocks.OBSIDIAN); break;
									default: propellerMaterial = null;
								}
								
								boolean hasMaterials = false;
								byte propellerMaterialQty = (byte) (message.diameter < 90 ? message.numberBlades : message.numberBlades*2);
								switch(message.propellerType){
									case(0): hasMaterials = numberPlayerPlanks >= propellerMaterialQty && numberPlayerIronIngots >= 1 && numberPlayerRedstone >= 5; break;
									case(1): hasMaterials = numberPlayerIronIngots >= propellerMaterialQty + 1 && numberPlayerRedstone >= 5; break;
									case(2): hasMaterials = numberPlayerObsidian >= propellerMaterialQty && numberPlayerIronIngots >= 1 && numberPlayerRedstone >= 5; break;
								}
								
								if(!hasMaterials){
									return;
								}else{
									player.inventory.clearMatchingItems(propellerMaterial, -1, propellerMaterialQty, null);
									player.inventory.clearMatchingItems(Items.IRON_INGOT, -1, 1, null);
									player.inventory.clearMatchingItems(Items.REDSTONE, -1, 5, null);
								}
							}
						}
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
