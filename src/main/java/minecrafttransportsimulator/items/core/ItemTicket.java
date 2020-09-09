package minecrafttransportsimulator.items.core;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperEntity;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTicket extends Item implements IItemVehicleInteractable{
	public ItemTicket(){
		super();
		setFull3D();
		setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=3; ++i){
			tooltipLines.add(BuilderGUI.translate("info.item.ticket.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(player.isSneaking()){
				Iterator<WrapperEntity> iterator = vehicle.locationRiderMap.inverse().keySet().iterator();
				while(iterator.hasNext()){
					WrapperEntity entity = iterator.next();
					if(!(entity instanceof WrapperPlayer)){
						vehicle.removeRider(entity, iterator);
					}
				}
			}else{
				vehicle.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), vehicle);
			}
		}
		return CallbackType.NONE;
	}
}
