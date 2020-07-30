package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import mcinterface.BuilderGUI;
import mcinterface.InterfaceNetwork;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
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
	public void doVehicleInteraction(ItemStack stack, EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(part instanceof PartEngine){
				PartEngine engine = (PartEngine) part;
				if(engine.linkedEngine == null){
					if(lastEngineClicked == null){
						lastEngineClicked = engine;
						player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.firstlink"));
					}else if(!lastEngineClicked.equals(engine)){
						if(lastEngineClicked.vehicle.equals(engine.vehicle)){
							player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.samevehicle"));
							lastEngineClicked = null;
						}else if(engine.worldPos.distanceTo(lastEngineClicked.worldPos) < 15){
							engine.linkedEngine = lastEngineClicked;
							lastEngineClicked.linkedEngine = engine;
							lastEngineClicked = null;
							InterfaceNetwork.sendToClientsTracking(new PacketVehiclePartEngine(engine, engine.linkedEngine.vehicle.uniqueID, engine.linkedEngine.placementOffset), engine.vehicle);
							player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.secondlink"));
						}else{
							player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.toofar"));
							lastEngineClicked = null;
						}
					}
				}else{
					player.sendPacket(new PacketPlayerChatMessage("interact.jumpercable.alreadylinked"));
				}
			}
		}
	}
}
