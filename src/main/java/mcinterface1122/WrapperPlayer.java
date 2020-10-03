package mcinterface1122;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperTileEntity;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;

class WrapperPlayer extends WrapperEntity implements IWrapperPlayer{
	
	final EntityPlayer player;
	
	WrapperPlayer(EntityPlayer player){
		super(player);
		this.player = player;
	}
	
	@Override
	public double getSeatOffset(){
		//Player legs are 12 pixels.
		return -12D/16D;
	}
	
	@Override
	public float getHeadYaw(){
		//Player head yaw is their actual yaw.  Head is used for the camera.
		return getYaw();
	}
	
	@Override
	public void setHeadYaw(double yaw){
		setYaw(yaw);
	}
	
	@Override
	public String getUUID(){
		return EntityPlayer.getUUID(player.getGameProfile()).toString();
	}

	@Override
	public boolean isOP(){
		return player.getServer() == null || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
	}
	
	@Override
	public void displayChatMessage(String message){
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(MasterInterface.coreInterface.translate(message)));
	}
	
	@Override
	public boolean isCreative(){
		return player.capabilities.isCreativeMode;
	}
	
	@Override
	public boolean isSneaking(){
		return player.isSneaking();
	}
	
	@Override
	public WrapperEntity getLeashedEntity(){
		for(EntityLiving entityLiving : player.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))){
			if(entityLiving.getLeashed() && player.equals(entityLiving.getLeashHolder())){
				entityLiving.clearLeashed(true, !player.capabilities.isCreativeMode);
				return WrapperWorld.getWrapperFor(entityLiving.world).getWrapperFor(entityLiving);
			}
		}
		return null;
	}
	
	@Override
	public AItemBase getHeldItem(){
		return player.getHeldItemMainhand().getItem() instanceof BuilderItem ? ((BuilderItem) player.getHeldItemMainhand().getItem()).item : null;
	}
	
	@Override
	public IWrapperItemStack getHeldStack(){
		return new WrapperItemStack(player.getHeldItemMainhand());
	}
	
	@Override
	public IWrapperInventory getInventory(){
		return new WrapperInventory(player.inventory);
	}
	
	@Override
	public void sendPacket(APacketBase packet){
		MasterInterface.networkInterface.sendToPlayer(packet, (EntityPlayerMP) player);
	}
	
	@Override
	public void openCraftingGUI(){
		player.displayGui(new BlockWorkbench.InterfaceCraftingTable(player.world, null){
			@Override
			public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player){
	            return new ContainerWorkbench(playerInventory, player.world, player.getPosition()){
	            	@Override
	                public boolean canInteractWith(EntityPlayer playerIn){
	            		return true;
	                }
	            };
	        }
		});
	}
	
	@Override
	public void openTileEntityGUI(IWrapperTileEntity tile){
		if(((WrapperTileEntity) tile).tile instanceof IInventory){
			player.displayGUIChest((IInventory) ((WrapperTileEntity) tile).tile);
		}
	}
}