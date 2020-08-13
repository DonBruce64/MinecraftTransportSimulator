package minecrafttransportsimulator.items.core;

import net.minecraft.item.Item;

public class ItemTicket extends Item{
	public ItemTicket(){
		super();
		setFull3D();
		setMaxStackSize(1);
	}
	
	//TODO make tickets into a block that can load things.
	/*
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=4; ++i){
			tooltipLines.add(BuilderGUI.translate("info.item.ticket.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand){
		if(!world.isRemote && player.getRidingEntity() instanceof EntityVehicleF_Physics){
			EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) player.getRidingEntity();
			//Check to see if we're a controller.
			if(vehicle.getSeatForRider(player).vehicleDefinition.isController){
				//Check if the vehicle is empty and we need to load or unload all NPCs.
				boolean unloadMode = false;
				for(Entity passenger : vehicle.getPassengers()){
					if(passenger instanceof INpc){
						//We have NPCs, unload them.
						unloadMode = true;
						passenger.dismountRidingEntity();
					}
				}
				if(!unloadMode){
					//No passengers, try to load.
					for(EntityLiving entityliving : vehicle.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 15D, player.posY - 15D, player.posZ - 15D, player.posX + 15D, player.posY + 15D, player.posZ + 15D))){
						if(entityliving instanceof INpc){
							//Get the next free seat.
							for(APart part : vehicle.getVehicleParts()){
								if(part instanceof PartSeat && vehicle.getRiderForSeat((PartSeat) part) == null && !((PartSeat) part).vehicleDefinition.isController){
									vehicle.setRiderInSeat(entityliving, (PartSeat) part);
									break;
								}
							}
						}
					}
				}
			}
		}
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
    }
	
	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand){
		if(!player.world.isRemote && hand.equals(EnumHand.MAIN_HAND)){
			if(target instanceof EntityLiving && target instanceof INpc){
				//Set NBT to reflect the ID of the entity we just clicked.
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("entityID", target.getEntityId());
				//Don't use the passed-in stack here.  It's a copy.  Cause Mojang is dumb like that.
				player.getHeldItemMainhand().setTagCompound(tag);
				InterfaceNetwork.sendToPlayer(new PacketPlayerChatMessage("interact.ticket.linked"), (EntityPlayerMP) player);
			}else{
				InterfaceNetwork.sendToPlayer(new PacketPlayerChatMessage("interact.ticket.notnpc"), (EntityPlayerMP) player);
			}
		}
		return true;
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick && part instanceof PartSeat){
			PartSeat seat = (PartSeat) part;
			if(stack.hasTagCompound()){
				if(vehicle.getRiderForSeat(seat) != null){
					InterfaceNetwork.sendToPlayer(new PacketPlayerChatMessage("interact.failure.seattaken"), player);
				}else{
					//We are an assigned ticket, load the entity.
					EntityLiving entityLiving = (EntityLiving) vehicle.world.getEntityByID(stack.getTagCompound().getInteger("entityID"));
					if(entityLiving != null && seat.worldPos.distanceTo(new Point3d(entityLiving.posX, entityLiving.posY, entityLiving.posZ)) < 35){
						vehicle.setRiderInSeat(entityLiving, seat);
						stack.setTagCompound(null);
					}else{
						InterfaceNetwork.sendToPlayer(new PacketPlayerChatMessage("interact.ticket.toofar"), player);
					}
				}
			}else{
				if(vehicle.getRiderForSeat(seat) instanceof INpc){
					//Dismount seated NPC.
					vehicle.getRiderForSeat(seat).dismountRidingEntity();
				}
			}
		}
	}*/
}
