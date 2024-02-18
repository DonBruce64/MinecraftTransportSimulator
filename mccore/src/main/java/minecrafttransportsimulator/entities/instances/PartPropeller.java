package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.items.instances.ItemPartPropeller;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public class PartPropeller extends APart {
    private double currentRPM;
    /**
     * In revolutions, or 1/360th a degree.
     **/
    private double angularPosition;
    /**
     * In revolutions per tick
     **/
    private double angularVelocity;
    /**
     * In inches per rotation (360 degrees).
     **/
    public int currentPitch;
    /**
     * In meters per second.
     **/
    public double airstreamLinearVelocity;
    /**
     * In meters per second.
     **/
    public double desiredLinearVelocity;

    private final List<PartEngine> connectedEngines = new ArrayList<>();
    protected final Point3D propellerAxisVector = new Point3D();
    private final Point3D propellerForce = new Point3D();
    private double propellerForceValue;
    private final BoundingBox damageBounds;

    public static final int MIN_DYNAMIC_PITCH = 45;

    public PartPropeller(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartPropeller item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        this.currentPitch = data != null ? data.getInteger("currentPitch") : definition.propeller.pitch;

        //Rotors need different collision box bounds as they are pointed upwards.
        double propellerRadius = definition.propeller.diameter * 0.0254D / 2D;
        if (definition.propeller.isRotor) {
            this.damageBounds = new BoundingBox(position, propellerRadius, 0.25D, propellerRadius);
        } else {
            this.damageBounds = new BoundingBox(position, propellerRadius, propellerRadius, propellerRadius);
        }
    }

    @Override
    public void attack(Damage damage) {
        super.attack(damage);
        if (!damage.isWater) {
            if (damage.entityResponsible instanceof IWrapperPlayer && ((IWrapperPlayer) damage.entityResponsible).getHeldStack().isEmpty()) {
                //Don't let players sitting in the vehicle hand-start the propeller.  Lazy bums...
                if (!masterEntity.allParts.contains(damage.entityResponsible.getEntityRiding())) {
                    connectedEngines.forEach(connectedEngine -> {
                        connectedEngine.handStartEngine();
                        InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(connectedEngine, Signal.HS_ON));
                    });
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();
        //Get the running engine with the fastest RPM, and have us follow it.
        //This needs to account for the gearbox ratio as well.
        currentRPM = 0;
        if (isActive) {
            boolean increasePitch = false;
            boolean decreasePitch = false;

            //If we have an engine connected, adjust speed and pitch to it.
            if (!connectedEngines.isEmpty()) {
                //Get engine with highest running RPM, or highest total RPM if none are running.
                double highestPossibleRPM = 0;
                double highestRunningRPM = 0;
                PartEngine currentConnectedEngine = null;
                for (PartEngine connectedEngine : connectedEngines) {
                    if (connectedEngine.propellerGearboxRatio != 0) {
                        double engineDrivenRPM = connectedEngine.rpm / connectedEngine.propellerGearboxRatio;
                        if (currentConnectedEngine == null || engineDrivenRPM > highestPossibleRPM) {
                            highestPossibleRPM = engineDrivenRPM;
                            currentConnectedEngine = connectedEngine;
                        }
                        if (connectedEngine.running && engineDrivenRPM > highestRunningRPM) {
                            highestRunningRPM = engineDrivenRPM;
                            currentConnectedEngine = connectedEngine;
                        }
                    }
                }
                currentRPM = highestRunningRPM > 0 ? highestRunningRPM : highestPossibleRPM;

                //Ensure we don't over-speed the engine if we are a dynamic propeller by requesting a pitch adjustment later.
                if (currentConnectedEngine != null && definition.propeller.isDynamicPitch) {
                    if (currentPitch > MIN_DYNAMIC_PITCH) {
                        decreasePitch = currentConnectedEngine.rpm < currentConnectedEngine.definition.engine.maxSafeRPM * 0.60;
                    }
                    if (currentPitch < definition.propeller.pitch) {
                        increasePitch = currentConnectedEngine.rpm > currentConnectedEngine.definition.engine.maxSafeRPM * 0.85;
                    }
                }
            }

            //If we are a dynamic-pitch propeller or rotor, adjust ourselves to the speed of the engine.
            if (vehicleOn != null) {
                if (definition.propeller.isRotor) {
                    double throttlePitchSetting = (vehicleOn.throttleVar.currentValue * 1.35 - 0.35) * definition.propeller.pitch;
                    if (throttlePitchSetting < currentPitch) {
                        --currentPitch;
                    } else if (throttlePitchSetting > currentPitch) {
                        ++currentPitch;
                    }
                } else if (definition.propeller.isDynamicPitch) {
                    if (decreasePitch || (vehicleOn.reverseThrustVar.isActive && currentPitch > -MIN_DYNAMIC_PITCH)) {
                        --currentPitch;
                    } else if (increasePitch || (!vehicleOn.reverseThrustVar.isActive && currentPitch < MIN_DYNAMIC_PITCH)) {
                        ++currentPitch;
                    }
                }
            }
        }

        //Adjust angular position and velocity.
        if (currentRPM != 0) {
            angularVelocity = (float) (currentRPM / 60F / 20F);
        } else if (angularVelocity > .01) {
            angularVelocity -= 0.01;
        } else if (angularVelocity < -.01) {
            angularVelocity += 0.01;
        } else {
            angularVelocity = 0;
        }
        angularPosition += angularVelocity;

        //Check for high values.  These get less accurate and cause funky rendering.
        if (angularPosition > 3600000) {
            angularPosition -= 3600000;
            angularPosition -= 3600000;
        } else if (angularPosition < -3600000) {
            angularPosition += 3600000;
            angularPosition += 3600000;
        }


        //Get the linear velocity of the air around the propeller, based on our axial velocity.
        airstreamLinearVelocity = 20D * masterEntity.motion.dotProduct(propellerAxisVector, false);
        //Get the desired linear velocity of the propeller, based on the current RPM and pitch.
        //We add to the desired linear velocity by a small factor.  This is because the actual cruising speed of aircraft
        //is based off of engine max RPM equating exactly to ideal linear speed of the propeller.  I'm sure there are nuances
        //here, like perhaps the propeller manufactures reporting the prop pitch to match cruise, but for physics, that don't work,
        //because the propeller never reaches that speed during cruise due to drag.  So we add a small addition here to compensate.
        desiredLinearVelocity = 0.0254D * (currentPitch + 20) * 20D * angularVelocity;

        //Damage propeller or entities if required.
        if (!world.isClient() && currentRPM >= 100) {
            //Expand the bounding box bounds, and send off the attack.
            boundingBox.widthRadius += 0.2;
            boundingBox.heightRadius += 0.2;
            boundingBox.depthRadius += 0.2;
            IWrapperEntity controller = vehicleOn.getController();
            LanguageEntry language = controller != null ? LanguageSystem.DEATH_PROPELLER_PLAYER : LanguageSystem.DEATH_PROPELLER_NULL;
            Damage propellerDamage = new Damage(ConfigSystem.settings.damage.propellerDamageFactor.value * currentRPM / 500F, damageBounds, this, controller, language);
            world.attackEntities(propellerDamage, null, false);
            boundingBox.widthRadius -= 0.2;
            boundingBox.heightRadius -= 0.2;
            boundingBox.depthRadius -= 0.2;
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        connectedEngines.clear();
        if (entityOn instanceof PartEngine) {
            connectedEngines.add((PartEngine) entityOn);
        }
        addLinkedPartsToList(connectedEngines, PartEngine.class);
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("propeller_pitch_deg"):
                return new ComputedVariable(this, variable, partialTicks -> Math.toDegrees(Math.atan(currentPitch / (definition.propeller.diameter * 0.75D * Math.PI))), false);
            case ("propeller_pitch_in"):
                return new ComputedVariable(this, variable, partialTicks -> currentPitch, false);
            case ("propeller_pitch_percent"):
                return new ComputedVariable(this, variable, partialTicks -> 1D * (currentPitch - PartPropeller.MIN_DYNAMIC_PITCH) / (definition.propeller.pitch - PartPropeller.MIN_DYNAMIC_PITCH), false);
            case ("propeller_rotation"):
                return new ComputedVariable(this, variable, partialTicks -> (partialTicks != 0 ? (angularPosition - (angularVelocity * (1 - partialTicks))) : angularPosition) * 360D, true);
            case ("propeller_rpm"):
                return new ComputedVariable(this, variable, partialTicks -> currentRPM, false);
            default: {
                return super.createComputedVariable(variable);
            }
        }
    }

    public double addToForceOutput(Point3D force, Point3D torque) {
        propellerForceValue = 0;
        propellerAxisVector.set(0, 0, 1).rotate(orientation);
        if (currentRPM != 0 && desiredLinearVelocity != 0) {
            //Thrust produced by the propeller is the difference between the desired linear velocity and the airstream linear velocity.
            //This gets the magnitude of the initial thrust force.
            double thrust = (desiredLinearVelocity - airstreamLinearVelocity);

            //Multiply the thrust difference by the area of the propeller.  This accounts for the force-area defined by it.
            thrust *= Math.PI * Math.pow(0.0254 * definition.propeller.diameter / 2D, 2);
            //Finally, multiply by the air density, and a constant.  Less dense air causes less thrust force.
            thrust *= vehicleOn.airDensity / 25D * 1.5D;

            //Get the angle of attack of the propeller.
            //Note pitch velocity is in linear in meters per second, 
            //This means we need to convert it to meters per revolution before we can move on.
            //This gets the angle as a ratio of forward pitch to propeller circumference.
            //If the angle of attack is greater than 25 degrees (or a ratio of 0.4663), sap power off the propeller for stalling.
            double angleOfAttack = ((desiredLinearVelocity - airstreamLinearVelocity) / (currentRPM / 60D)) / (definition.propeller.diameter * Math.PI * 0.0254D);
            if (Math.abs(angleOfAttack) > 0.4663D) {
                thrust *= 0.4663D / Math.abs(angleOfAttack);
            }

            //If the propeller is in the water, increase thrust.
            if (isInLiquid()) {
                thrust *= 50;
            }

            //Add propeller force to total engine force as a vector.
            //Depends on propeller orientation, as upward propellers provide upwards thrust.
            propellerForceValue += thrust;
            propellerForce.set(propellerAxisVector).scale(thrust);
            force.add(propellerForce);
            propellerForce.reOrigin(vehicleOn.orientation);
            torque.add(localOffset.crossProduct(propellerForce));
        }
        return propellerForceValue;
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        data.setInteger("currentPitch", currentPitch);
        return data;
    }
}
