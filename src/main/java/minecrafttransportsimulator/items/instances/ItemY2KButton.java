package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlAnalog;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine;
import minecrafttransportsimulator.packets.instances.PacketVehiclePartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketVehicleVariableToggle;
import minecrafttransportsimulator.rendering.components.LightType;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartEngine;

public class ItemY2KButton extends AItemBase{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		for(byte i=1; i<=4; ++i){
			tooltipLines.add(InterfaceCore.translate("info.item.y2kbutton.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public boolean onUsed(WrapperWorld world, WrapperPlayer player){
		if(!world.isClient() && player.isOP()){
			for(AEntityBase entity : AEntityBase.createdServerEntities){
				if(entity instanceof EntityVehicleF_Physics){
					EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) entity;
					vehicle.throttle = 0;
					InterfacePacket.sendToAllClients(new PacketVehicleControlAnalog(vehicle, PacketVehicleControlAnalog.Controls.THROTTLE, (short) 0, (byte) 0));
					vehicle.parkingBrakeOn = true;
					InterfacePacket.sendToAllClients(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.P_BRAKE, true));
					for(PartEngine engine : vehicle.engines.values()){
						engine.setMagnetoStatus(false);
						InterfacePacket.sendToAllClients(new PacketVehiclePartEngine(engine, Signal.MAGNETO_OFF));
					}
					Iterator<String> variableIterator = vehicle.variablesOn.iterator();
					while(variableIterator.hasNext()){
						String variableName = variableIterator.next();
						for(LightType light : LightType.values()){
							if(light.lowercaseName.equals(variableName)){
								InterfacePacket.sendToAllClients(new PacketVehicleVariableToggle(vehicle, variableName));
								variableIterator.remove();
								break;
							}
						}
					}
				}
			}
		}
        return true;
    }
}
