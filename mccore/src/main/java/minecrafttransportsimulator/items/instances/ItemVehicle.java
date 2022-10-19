package minecrafttransportsimulator.items.instances;

import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemVehicle extends AItemSubTyped<JSONVehicle> implements IItemEntityProvider {

    public ItemVehicle(JSONVehicle definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        if (!world.isClient()) {
            IWrapperItemStack heldStack = player.getHeldStack();
            //Make sure the definition is set in the NBT we will be giving to our new entity.
            IWrapperNBT data = heldStack.getData();
            populateDefaultData(data);
            boolean wasSaved = !data.getString("uniqueUUID").isEmpty();

            //First construct the class.
            //This takes into account all saved data in the stack, so the vehicle will re-load its data from it
            //as if it has been saved in the world rather than into an item.  If there's no data,
            //then we just make a blank, new instance.
            EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(world, player, data);
            vehicle.addPartsPostAddition(player, data);

            //If we have a default fuel, add it now as we SHOULD have an engine to tell
            //us what fuel type we will need to add.
            if (!wasSaved && vehicle.definition.motorized.defaultFuelQty > 0) {
                for (APart part : vehicle.parts) {
                    if (part instanceof PartEngine) {
                        //Get the most potent fuel for the vehicle from the fuel configs.
                        String mostPotentFluid = "";
                        for (String fluidName : ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).keySet()) {
                            if (mostPotentFluid.isEmpty() || ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(mostPotentFluid) < ConfigSystem.settings.fuel.fuels.get(part.definition.engine.fuelType).get(fluidName)) {
                                mostPotentFluid = fluidName;
                            }
                        }
                        vehicle.fuelTank.manuallySet(mostPotentFluid, vehicle.definition.motorized.defaultFuelQty);
                        break;
                    }
                }
                if (vehicle.fuelTank.getFluid().isEmpty()) {
                    player.sendPacket(new PacketPlayerChatMessage(player, "A defaultFuelQty was specified for: " + vehicle.definition.packID + ":" + vehicle.definition.systemName + ", but no engine was noted as a defaultPart, so we don't know what fuel to put in the vehicle.  Vehicle will be spawned without fuel and engine."));
                }
            }

            //Set position to the spot that was clicked by the player.
            //Add a -90 rotation offset so the vehicle is facing perpendicular.
            //Remove motion to prevent it if it was previously stored.
            //Makes placement easier and is less likely for players to get stuck.
            vehicle.position.set(position).add(0.5, 1, 0.5);
            vehicle.prevPosition.set(position);
            vehicle.orientation.setToAngles(new Point3D(0, player.getYaw() + 90, 0));
            vehicle.prevOrientation.set(vehicle.orientation);
            vehicle.motion.set(0, 0, 0);
            vehicle.prevMotion.set(vehicle.motion);

            //Entity is valid.  Spawn it into the world.
            vehicle.world.spawnEntity(vehicle);

            //Decrement stack if we are not in creative.
            if (!player.isCreative()) {
                player.getInventory().removeFromSlot(player.getHotbarIndex(), 1);
            }
        }
        return true;
    }

    @Override
    public void repair(IWrapperNBT data) {
        super.repair(data);
        data.setDouble("electricPower", 12);
    }

    @Override
    public void registerEntities(Map<String, IItemEntityFactory> entityMap) {
        entityMap.put(EntityVehicleF_Physics.class.getSimpleName(), (world, placingPlayer, data) -> new EntityVehicleF_Physics(world, placingPlayer, data));
    }
}
