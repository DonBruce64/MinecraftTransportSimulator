package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage;
import minecrafttransportsimulator.jsondefs.JSONConfigLanguage.LanguageEntry;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.systems.ConfigSystem;

import java.util.ArrayList;
import java.util.List;

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
    public int currentPitch;

    private final List<PartEngine> connectedEngines = new ArrayList<>();
    protected final Point3D propellerAxisVector = new Point3D();
    private final Point3D propellerForce = new Point3D();
    private final BoundingBox damageBounds;

    public static final int MIN_DYNAMIC_PITCH = 45;

    public PartPropeller(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, data);
        this.currentPitch = definition.propeller.pitch;

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
            } else if (damageAmount == definition.general.health) {
                if (ConfigSystem.settings.damage.explosions.value) {
                    world.spawnExplosion(position, 1F, true);
                } else {
                    world.spawnExplosion(position, 0F, false);
                }
                remove();
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
                    } else if (currentPitch < definition.propeller.pitch) {
                        increasePitch = currentConnectedEngine.rpm > currentConnectedEngine.definition.engine.maxSafeRPM * 0.85;
                    }
                }
            }

            //If we are a dynamic-pitch propeller or rotor, adjust ourselves to the speed of the engine.
            if (vehicleOn != null) {
                if (definition.propeller.isRotor) {
                    double throttlePitchSetting = currentRPM != 0 ? (vehicleOn.throttle * 1.35 - 0.35) * definition.propeller.pitch : 0;
                    if (throttlePitchSetting < currentPitch) {
                        --currentPitch;
                    } else if (throttlePitchSetting > currentPitch) {
                        ++currentPitch;
                    }
                } else if (definition.propeller.isDynamicPitch) {
                    if (decreasePitch || (vehicleOn.reverseThrust && currentPitch > -MIN_DYNAMIC_PITCH)) {
                        --currentPitch;
                    } else if (increasePitch || (!vehicleOn.reverseThrust && currentPitch < MIN_DYNAMIC_PITCH)) {
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

        //Damage propeller or entities if required.
        if (!world.isClient() && currentRPM >= 100) {
            //Expand the bounding box bounds, and send off the attack.
            boundingBox.widthRadius += 0.2;
            boundingBox.heightRadius += 0.2;
            boundingBox.depthRadius += 0.2;
            IWrapperEntity controller = vehicleOn.getController();
            LanguageEntry language = controller != null ? JSONConfigLanguage.DEATH_PROPELLER_PLAYER : JSONConfigLanguage.DEATH_PROPELLER_NULL;
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
    public double getRawVariableValue(String variable, float partialTicks) {
        switch (variable) {
            case ("propeller_pitch_deg"):
                return Math.toDegrees(Math.atan(currentPitch / (definition.propeller.diameter * 0.75D * Math.PI)));
            case ("propeller_pitch_in"):
                return currentPitch;
            case ("propeller_pitch_percent"):
                return 1D * (currentPitch - PartPropeller.MIN_DYNAMIC_PITCH) / (definition.propeller.pitch - PartPropeller.MIN_DYNAMIC_PITCH);
            case ("propeller_rotation"):
                return (angularPosition + angularVelocity * partialTicks) * 360D;
        }

        return super.getRawVariableValue(variable, partialTicks);
    }

    public void addToForceOutput(Point3D force, Point3D torque) {
        propellerAxisVector.set(0, 0, 1).rotate(orientation);
        if (currentRPM != 0) {
            //Get the current linear velocity of the propeller, based on our axial velocity.
            //This is is meters per second.
            double currentLinearVelocity = 20D * vehicleOn.motion.dotProduct(propellerAxisVector, false);
            //Get the desired linear velocity of the propeller, based on the current RPM and pitch.
            //We add to the desired linear velocity by a small factor.  This is because the actual cruising speed of aircraft
            //is based off of engine max RPM equating exactly to ideal linear speed of the propeller.  I'm sure there are nuances
            //here, like perhaps the propeller manufactures reporting the prop pitch to match cruise, but for physics, that don't work,
            //because the propeller never reaches that speed during cruise due to drag.  So we add a small addition here to compensate.
            double desiredLinearVelocity = 0.0254D * (currentPitch + 20) * 20D * angularVelocity;

            if (desiredLinearVelocity != 0) {
                //Thrust produced by the propeller is the difference between the desired linear velocity and the current linear velocity.
                //This gets the magnitude of the initial thrust force.
                double thrust = (desiredLinearVelocity - currentLinearVelocity);
                //Multiply the thrust difference by the area of the propeller.  This accounts for the force-area defined by it.
                thrust *= Math.PI * Math.pow(0.0254 * definition.propeller.diameter / 2D, 2);
                //Finally, multiply by the air density, and a constant.  Less dense air causes less thrust force.
                thrust *= vehicleOn.airDensity / 25D * 1.5D;

                //Get the angle of attack of the propeller.
                //Note pitch velocity is in linear in meters per second, 
                //This means we need to convert it to meters per revolution before we can move on.
                //This gets the angle as a ratio of forward pitch to propeller circumference.
                //If the angle of attack is greater than 25 degrees (or a ratio of 0.4663), sap power off the propeller for stalling.
                double angleOfAttack = ((desiredLinearVelocity - currentLinearVelocity) / (currentRPM / 60D)) / (definition.propeller.diameter * Math.PI * 0.0254D);
                if (Math.abs(angleOfAttack) > 0.4663D) {
                    thrust *= 0.4663D / Math.abs(angleOfAttack);
                }

                //If the propeller is in the water, increase thrust.
                if (isInLiquid()) {
                    thrust *= 50;
                }

                //Add propeller force to total engine force as a vector.
                //Depends on propeller orientation, as upward propellers provide upwards thrust.
                propellerForce.set(propellerAxisVector).scale(thrust);
                force.add(propellerForce);
                propellerForce.reOrigin(vehicleOn.orientation);
                torque.y -= propellerForce.z * localOffset.x;
                torque.z += propellerForce.y * localOffset.x;
            }
        }
    }
}
