package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.PartEngine;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJumperCable extends Item implements IItemVehicleInteractable{
	public static PartEngine lastEngineClicked;
	
	public ItemJumperCable(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(BuilderGUI.translate("info.item.jumpercable.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(part instanceof PartEngine){
				PartEngine engine = (PartEngine) part;
				if(engine.linkedEngine == null){
					if(lastEngineClicked == null){
						lastEngineClicked = engine;
						if(vehicle.world.isClient()){
							player.displayChatMessage("interact.jumpercable.firstlink");
						}else{
							return CallbackType.ALL;
						}
					}else if(!lastEngineClicked.equals(engine)){
						if(lastEngineClicked.vehicle.equals(engine.vehicle)){
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.samevehicle");
							}else{
								return CallbackType.ALL;
							}
						}else if(engine.worldPos.distanceTo(lastEngineClicked.worldPos) < 15){
							engine.linkedEngine = lastEngineClicked;
							lastEngineClicked.linkedEngine = engine;
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.secondlink");
							}else{
								return CallbackType.ALL;
							}
						}else{
							lastEngineClicked = null;
							if(vehicle.world.isClient()){
								player.displayChatMessage("interact.jumpercable.toofar");
							}else{
								return CallbackType.ALL;
							}
						}
					}
				}else{
					if(vehicle.world.isClient()){
						player.displayChatMessage("interact.jumpercable.alreadylinked");
					}else{
						return CallbackType.ALL;
					}
				}
			}
		}
		return CallbackType.NONE;
	}
}
