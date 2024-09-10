package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

/**
 * Now that we have an existing vehicle its time to add the ability to collide with it,
 * and for it to do collision with other entities in the world.  This is where collision
 * bounds are added, as well as the mass of the entity is calculated, as that's required
 * for collision physics forces.  We also add vectors here for the vehicle's orientation,
 * as those are required for us to know how the vehicle collided in the first place.
 *
 * @author don_bruce
 */
abstract class AEntityVehicleC_Colliding extends AEntityG_Towable<JSONVehicle> {

    //Internal states.
    public double currentMass;
    public double axialVelocity;
    public final Point3D headingVector = new Point3D();

    /**
     * Cached value for speedFactor.  Saves us from having to use the long form all over.
     */
    public final double speedFactor;

    public AEntityVehicleC_Colliding(AWrapperWorld world, IWrapperPlayer placingPlayer, ItemVehicle item, IWrapperNBT data) {
        super(world, placingPlayer, item, data);
        this.speedFactor = (definition.motorized.isAircraft ? ConfigSystem.settings.general.aircraftSpeedFactor.value : ConfigSystem.settings.general.carSpeedFactor.value) * ConfigSystem.settings.general.packSpeedFactors.value.get(definition.packID);
        double vehicleScale = ConfigSystem.settings.general.packVehicleScales.value.get(definition.packID);
        scale.set(vehicleScale, vehicleScale, vehicleScale);
    }

    @Override
    public void update() {
        super.update();
        world.beginProfiling("VehicleC_Level", true);

        //Set vectors to current velocity and orientation.
        headingVector.set(0D, 0D, 1D);
        headingVector.rotate(orientation);
        axialVelocity = Math.abs(motion.dotProduct(headingVector, false));

        //Update mass.
        currentMass = getMass();

        //Auto-close any open doors that should be closed.
        //Only do this once a second to prevent lag.
        if (!world.isClient() && velocity > 0.5 && ticksExisted % 20 == 0) {
            closeDoors();
        }

        world.endProfiling();
    }

    @Override
    public void destroy(BoundingBox box) {
        //Get drops from parts.  Vehicle just blows up.
        List<IWrapperItemStack> drops = new ArrayList<>();

        //Do part things before we call super, as that will remove the parts from this vehicle.
        IWrapperEntity controller = getController();
        Damage controllerCrashDamage = new Damage(ConfigSystem.settings.damage.crashDamageFactor.value * velocity * 20, null, this, null, LanguageSystem.DEATH_CRASH_NULL);
        LanguageEntry language = controller != null ? LanguageSystem.DEATH_CRASH_PLAYER : LanguageSystem.DEATH_CRASH_NULL;
        Damage passengerCrashDamage = new Damage(ConfigSystem.settings.damage.crashDamageFactor.value * velocity * 20, null, this, controller, language);

        //Damage riders.
        for (APart part : allParts) {
            if (part.rider != null) {
                if (part.rider == controller) {
                    part.rider.attack(controllerCrashDamage);
                } else {
                    part.rider.attack(passengerCrashDamage);
                }
            }
        }

        //Drop our parts as items.
        for (APart part : parts) {
            if (!part.isPermanent) {
                drops.add(part.getStack());
            }
        }

        //Now call super and spawn drops.
        super.destroy(box);
        drops.forEach(stack -> world.spawnItemStack(stack, box.globalCenter, null));
    }

    @Override
    public double getMass() {
        return super.getMass() + definition.motorized.emptyMass;
    }

    @Override
    public int getWorldLightValue() {
        //Offset position by 1 to prevent super darkness while being in the ground.
        ++position.y;
        int higherLight = InterfaceManager.renderingInterface.getLightingAtPosition(position);
        --position.y;
        return higherLight;
    }
}
