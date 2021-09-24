package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityFluidTankProvider;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityFluidTank;
import minecrafttransportsimulator.entities.instances.EntityInventoryContainer;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.items.instances.ItemPartInteractable;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packets.instances.PacketTileEntityFuelPumpConnection;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;

public class TileEntityFuelPump extends TileEntityDecor implements ITileEntityFluidTankProvider{
	public EntityVehicleF_Physics connectedVehicle;
    private final EntityFluidTank tank;
    public final EntityInventoryContainer fuelItems;
    public final List<Integer> fuelAmounts = new ArrayList<Integer>();
    public int fuelPurchasedRemaining;
    public boolean isCreative;
	public String placingPlayerID;

    public TileEntityFuelPump(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		super(world, position, placingPlayer, data);
    	this.tank = new EntityFluidTank(world, data.getDataOrNew("tank"), definition.decor.fuelCapacity){
    		@Override
    		public double fill(String fluid, double maxAmount, boolean doFill){
    			if(!isCreative){
					//We are a pump with a set cost, ensure we have purchased fuel.
    				//Make sure to add a small amount to ensure that the pump displays the name of the fluid in it.
    				if(fuelPurchasedRemaining == 0 && connectedVehicle == null && getFluidLevel() == 0){
    					maxAmount = 1;
					}else if(maxAmount > fuelPurchasedRemaining){
						maxAmount = fuelPurchasedRemaining;
					}
					double amountFilled = super.fill(fluid, maxAmount, doFill);
					if(doFill && fuelPurchasedRemaining > 0){
						fuelPurchasedRemaining -= amountFilled;
					}
					return amountFilled;
				}
    			return super.fill(fluid, maxAmount, doFill);
    		}
    	};
    	this.fuelItems = new EntityInventoryContainer(world, data.getDataOrNew("inventory"), 10);
    	for(int i=0; i<fuelItems.getSize(); ++i){
    		this.fuelAmounts.add(data.getInteger("fuelAmount" + i));
    	}
    	this.fuelPurchasedRemaining = data.getInteger("fuelPurchasedRemaining");
    	this.placingPlayerID = placingPlayer != null ? placingPlayer.getID() : data.getString("placingPlayerID");
    }
	
	@Override
	public boolean update(){
		if(super.update()){
			//Update creative status.
			isCreative = true;
			for(int i=0; i<fuelItems.getSize(); ++i){
				if(!fuelItems.getStack(i).isEmpty()){
					isCreative = false;
				}
			}
			
			//Update text lines to the current tank status if required.
			//Only do this on clients, as servers don't render any text.
			if(world.isClient() && definition.rendering != null && definition.rendering.textObjects != null){
				text.clear();
				String fluidName = tank.getFluidLevel() > 0 ? InterfaceCore.getFluidName(tank.getFluid()).toUpperCase() : "";
				String fluidLevel = InterfaceCore.translate("tile.fuelpump.level") + String.format("%04.1f", tank.getFluidLevel()/1000F) + "b";
				String fluidDispensed = InterfaceCore.translate("tile.fuelpump.dispensed") + String.format("%04.1f", tank.getAmountDispensed()/1000F) + "b";
				for(int i=0; i<definition.rendering.textObjects.size(); ++i){
					switch(i%3){
						case(0) : text.put(definition.rendering.textObjects.get(i), fluidName); break;
						case(1) : text.put(definition.rendering.textObjects.get(i), fluidLevel); break;
						case(2) : text.put(definition.rendering.textObjects.get(i), fluidDispensed); break;
					}
				}
			}
			
			//Do fuel checks.  Fuel checks only occur on servers.  Clients get packets for state changes.
			if(connectedVehicle != null && !world.isClient()){
				//Don't fuel vehicles that don't exist.
				if(!connectedVehicle.isValid){
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					return false;
				}
				
				//Check distance to make sure the vehicle hasn't moved away.
				if(connectedVehicle.position.distanceTo(position) > 16){
					InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 25, 25, 25))){
						if(entity instanceof WrapperPlayer){
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.toofar"));
						}
					}
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					return false;
				}
				//If we have room for fuel, try to add it to the vehicle.
				if(tank.getFluidLevel() > 0){
					double amountToFill = connectedVehicle.fuelTank.fill(tank.getFluid(), definition.decor.pumpRate, false);
					if(amountToFill > 0){
						double amountToDrain = tank.drain(tank.getFluid(), amountToFill, false);
						connectedVehicle.fuelTank.fill(tank.getFluid(), amountToDrain, true);
						tank.drain(tank.getFluid(), amountToDrain, true);
					}else{
						//No more room in the vehicle.  Disconnect.
						InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
						connectedVehicle.beingFueled = false;
						connectedVehicle = null;
						for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
							if(entity instanceof WrapperPlayer){
								((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.complete"));
							}
						}
					}
				}else{
					//No more fuel.  Disconnect vehicle.
					InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
					connectedVehicle.beingFueled = false;
					connectedVehicle = null;
					for(WrapperEntity entity : world.getEntitiesWithin(new BoundingBox(position, 16, 16, 16))){
						if(entity instanceof WrapperPlayer){
							((WrapperPlayer) entity).sendPacket(new PacketPlayerChatMessage((WrapperPlayer) entity, "interact.fuelpump.empty"));
						}
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){		
		//If we are holding an item, interact with the pump.
		ItemStack stack = player.getHeldStack();
		AItemBase item = player.getHeldItem();
		if(tank.interactWith(player) > 0){
			return true;
		}
		
		//Check if the item is a jerrycan.
		if(item instanceof ItemPartInteractable){
			ItemPartInteractable interactable = (ItemPartInteractable) item;
			if(interactable.definition.interactable.interactionType.equals(InteractableComponentType.JERRYCAN)){
				WrapperNBT data = new WrapperNBT(stack);
				if(data.getString("jerrycanFluid").isEmpty()){
					if(tank.getFluidLevel() >= 1000){
						data.setString("jerrycanFluid", tank.getFluid());
						stack.setTagCompound(data.tag);
						tank.drain(tank.getFluid(), 1000, true);
					}
				}
				return true;
			}
		}
		
		//Check if the item is a wrench, and the player can configure this pump..
		AItemBase heldItem = player.getHeldItem();
		if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type.equals(ItemComponentType.WRENCH) && (player.getID().equals(placingPlayerID) || player.isOP())){
			player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP_CONFIG));
			return true;
		}
		
		//If we aren't a creative pump, and we don't have fuel, bring up the GUI so the player can buy some.
		if(!isCreative && fuelPurchasedRemaining == 0 && tank.getFluidLevel() <= 1){
			player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.FUEL_PUMP));
			return true;
		}
    	
		//We don't have a vehicle connected.  Try to connect one now.
		if(connectedVehicle == null){
			//Get the closest vehicle within a 16-block radius.
			EntityVehicleF_Physics nearestVehicle = null;
			double lowestDistance = 16D;
			for(AEntityA_Base entity : AEntityA_Base.getEntities(world)){
				if(entity instanceof EntityVehicleF_Physics){
					EntityVehicleF_Physics testVehicle = (EntityVehicleF_Physics) entity;
					double vehicleDistance = testVehicle.position.distanceTo(position);
					if(vehicleDistance < lowestDistance){
						lowestDistance = vehicleDistance;
						nearestVehicle = testVehicle;
					}
				}
			}
			
			//Have a vehicle, try to connect to it.
			if(nearestVehicle != null){
				if(tank.getFluidLevel() == 0){
					//No fuel in the pump.
					player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.nofuel"));
				}else{
    				//Check to make sure this vehicle can take this fuel pump's fuel type.
					if(!nearestVehicle.fuelTank.getFluid().isEmpty()){
						if(!tank.getFluid().equals(nearestVehicle.fuelTank.getFluid())){
							player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.wrongtype"));
							return true;
						}
					}
					
					//Fuel type can be taken by vehicle, check to make sure engines can take it.
					for(APart part : nearestVehicle.parts){
						if(part instanceof PartEngine){
							if(ConfigSystem.configObject.fuel.fuels.get(part.definition.engine.fuelType).containsKey(tank.getFluid())){
								connectedVehicle = nearestVehicle;
								connectedVehicle.beingFueled = true;
								tank.resetAmountDispensed();
								InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, true));
								player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.connect"));
	    						return true;
							}
						}
					}
					player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.wrongengines"));
				}
			}else{
				player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.toofar"));
			}
		}else{
			//Connected vehicle exists, disconnect it.
			InterfacePacket.sendToAllClients(new PacketTileEntityFuelPumpConnection(this, false));
			connectedVehicle.beingFueled = false;
			connectedVehicle = null;
			player.sendPacket(new PacketPlayerChatMessage(player, "interact.fuelpump.disconnect"));
		}
		return true;
	}
	
	@Override
	public EntityFluidTank getTank(){
		return tank;
	}
	
	@Override
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("fuelpump_active"): return connectedVehicle != null ? 1 : 0;	
			case("fuelpump_stored"): return getTank().getFluidLevel();
			case("fuelpump_dispensed"): return getTank().getAmountDispensed();
			case("fuelpump_free"): return isCreative ? 1 : 0;
			case("fuelpump_purchased"): return fuelPurchasedRemaining;
		}
		
		return super.getRawVariableValue(variable, partialTicks);
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setData("tank", tank.save(new WrapperNBT()));
		data.setData("inventory", fuelItems.save(new WrapperNBT()));
		for(int i=0; i<fuelItems.getSize(); ++i){
    		data.setInteger("fuelAmount" + i, fuelAmounts.get(i));
    	}
		data.setInteger("fuelPurchasedRemaining", fuelPurchasedRemaining);
		data.setString("placingPlayerID", placingPlayerID);
		return data;
	}
}
