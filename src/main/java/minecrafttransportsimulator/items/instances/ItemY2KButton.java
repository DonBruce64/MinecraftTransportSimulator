package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehicleLightToggle;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

public class ItemY2KButton extends AItemBase{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		for(byte i=1; i<=4; ++i){
			tooltipLines.add(MasterLoader.coreInterface.translate("info.item.y2kbutton.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public boolean onUsed(IWrapperWorld world, IWrapperPlayer player){
		if(!world.isClient() && player.isOP()){
			for(AEntityBase entity : AEntityBase.createdServerEntities){
				if(entity instanceof EntityVehicleF_Physics){
					EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
					vehicle.throttle = 0;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlAnalog(vehicle, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, (byte) 0));
					vehicle.parkingBrakeOn = true;
					MasterLoader.networkInterface.sendToAllClients(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, true));
					for(PartEngine engine : vehicle.engines.values()){
						engine.setMagnetoStatus(false);
						MasterLoader.networkInterface.sendToAllClients(new PacketVehiclePartEngine(engine, Signal.MAGNETO_OFF));
					}
					Iterator<LightType> lightIterator = vehicle.lightsOn.iterator();
					while(lightIterator.hasNext()){
						LightType light = lightIterator.next();
						lightIterator.remove();
						MasterLoader.networkInterface.sendToAllClients(new PacketVehicleLightToggle(vehicle, light));
					}
				}
			}
		}
        return true;
    }
}
