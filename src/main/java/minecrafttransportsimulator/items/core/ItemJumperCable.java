package minecrafttransportsimulator.items.core;

import java.util.List;

import javax.annotation.Nullable;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.packets.general.PacketChat;
import minecrafttransportsimulator.packets.parts.PacketPartEngineLinked;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APart;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemJumperCable extends Item implements IItemVehicleInteractable{
	public static APartEngine<? extends EntityVehicleE_Powered> lastEngineClicked;
	
	public ItemJumperCable(){
		super();
		setFull3D();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltipLines, ITooltipFlag flagIn){
		for(byte i=1; i<=5; ++i){
			tooltipLines.add(I18n.format("info.item.jumpercable.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public void doVehicleInteraction(ItemStack stack, EntityVehicleE_Powered vehicle, APart<? extends EntityVehicleE_Powered> part, EntityPlayerMP player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(part instanceof APartEngine){
				APartEngine<? extends EntityVehicleE_Powered> engine = (APartEngine<? extends EntityVehicleE_Powered>) part;
				if(engine.linkedEngine == null){
					if(lastEngineClicked == null){
						lastEngineClicked = engine;
						MTS.MTSNet.sendTo(new PacketChat("interact.jumpercable.firstlink"), player);
					}else if(!lastEngineClicked.equals(engine)){
						if(lastEngineClicked.vehicle.equals(engine.vehicle)){
							MTS.MTSNet.sendTo(new PacketChat("interact.jumpercable.samevehicle"), player);
							lastEngineClicked = null;
						}else if(engine.partPos.distanceTo(lastEngineClicked.partPos) < 15){
							engine.linkedEngine = lastEngineClicked;
							lastEngineClicked.linkedEngine = engine;
							lastEngineClicked = null;
							MTS.MTSNet.sendToAll(new PacketPartEngineLinked(engine, engine.linkedEngine));
							MTS.MTSNet.sendTo(new PacketChat("interact.jumpercable.secondlink"), player);	
						}else{
							MTS.MTSNet.sendTo(new PacketChat("interact.jumpercable.toofar"), player);
							lastEngineClicked = null;
						}
					}
				}else{
					MTS.MTSNet.sendTo(new PacketChat("interact.jumpercable.alreadylinked"), player);
				}
			}
		}
	}
}
