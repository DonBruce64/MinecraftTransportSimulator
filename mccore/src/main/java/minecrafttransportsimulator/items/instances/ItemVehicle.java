package minecrafttransportsimulator.items.instances;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.components.IItemEntityProvider;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.systems.ConfigSystem;

public class ItemVehicle extends AItemSubTyped<JSONVehicle> implements IItemEntityProvider {

    public ItemVehicle(JSONVehicle definition, JSONSubDefinition subDefinition, String sourcePackID) {
        super(definition, subDefinition, sourcePackID);
    }

    @Override
    public boolean onBlockClicked(AWrapperWorld world, IWrapperPlayer player, Point3D position, Axis axis) {
        if (!world.isClient()) {
            //First construct the class.
            //This takes into account all saved data in the stack, so the vehicle will re-load its data from it
            //as if it has been saved in the world rather than into an item.  If there's no data,
            //then we just make a blank, new instance.
            IWrapperNBT data = player.getHeldStack().getData();
            if (data != null) {
                data.deleteAllUUIDTags(); //Do this just in case this is an older item.
            }
            EntityVehicleF_Physics vehicle = new EntityVehicleF_Physics(world, player, this, data);

            //Set position to the spot that was clicked by the player.
            //Add a -90 rotation offset so the vehicle is facing perpendicular.
            //Remove motion to prevent it if it was previously stored.
            //Makes placement easier and is less likely for players to get stuck.
            //Then spawn the vehicle into the world once these properties are set.
            vehicle.position.set(position).add(0.5, 1, 0.5);
            vehicle.prevPosition.set(position);
            vehicle.orientation.setToAngles(new Point3D(0, player.getYaw() + 90, 0));
            vehicle.prevOrientation.set(vehicle.orientation);
            vehicle.motion.set(0, 0, 0);
            vehicle.prevMotion.set(vehicle.motion);
            vehicle.world.spawnEntity(vehicle);

            //Now add the parts.  These have to be done after spawning the vehicle so they have the right tick order and position.
            vehicle.addPartsPostAddition(player, data);

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
    public void populateRepairMaterials(List<String> materials, int recipeIndex) {
        super.populateRepairMaterials(materials, recipeIndex);
        if (!ConfigSystem.settings.general.defaultVehicleRepairMaterials.value.isEmpty()) {
            materials.addAll(ConfigSystem.settings.general.defaultVehicleRepairMaterials.value);
        }
    }

    @Override
    public void registerEntities(Map<String, IItemEntityFactory> entityMap) {
        entityMap.put(EntityVehicleF_Physics.class.getSimpleName(), (world, data) -> new EntityVehicleF_Physics(world, null, data.getPackItem(), data));
    }
}
