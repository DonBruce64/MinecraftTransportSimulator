package minecrafttransportsimulator.packets.crafting;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.ATileEntityRotatable;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class APacketCrafting implements IMessage{
	private BlockPos tileEntityPos;
	private int player;

	public APacketCrafting(){}
	
	public APacketCrafting(ATileEntityRotatable tile, EntityPlayer player){
		this.tileEntityPos = tile.getPos();
		this.player = player != null ? player.getEntityId() : -1;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.tileEntityPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.player = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.tileEntityPos.getX());
		buf.writeInt(this.tileEntityPos.getY());
		buf.writeInt(this.tileEntityPos.getZ());
		buf.writeInt(this.player);
	}

	protected static ATileEntityRotatable getTileEntity(APacketCrafting message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (ATileEntityRotatable) ctx.getServerHandler().playerEntity.worldObj.getTileEntity(message.tileEntityPos);
		}else{
			return (ATileEntityRotatable) Minecraft.getMinecraft().theWorld.getTileEntity(message.tileEntityPos);
		}
	}
	
	protected static EntityPlayer getPlayer(APacketCrafting message, MessageContext ctx){
		if(message.player != -1){
			if(ctx.side.isServer()){
				return (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
			}else{
				return (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.player);
			}
		}else{
			return null;
		}
	}
	
	protected static boolean doesPlayerHaveMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				int requiredMaterialCount = materialStack.stackSize;
				for(ItemStack stack : player.inventory.mainInventory){
					if(ItemStack.areItemsEqual(stack, materialStack)){
						requiredMaterialCount -= stack.stackSize;
					}
				}
				if(requiredMaterialCount > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	protected static void removeMaterials(EntityPlayer player, String partToCraft){
		if(!player.capabilities.isCreativeMode){
			for(ItemStack materialStack : PackParserSystem.getMaterials(partToCraft)){
				player.inventory.clearMatchingItems(materialStack.getItem(), materialStack.getMetadata(), materialStack.stackSize, null);
			}
		}
	}
}
