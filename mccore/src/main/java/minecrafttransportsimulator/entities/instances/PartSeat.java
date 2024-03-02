package minecrafttransportsimulator.entities.instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIHUD;
import minecrafttransportsimulator.guis.instances.GUIPanel;
import minecrafttransportsimulator.guis.instances.GUIRadio;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.items.instances.ItemPartSeat;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPotionEffect;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableSet;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import minecrafttransportsimulator.systems.LanguageSystem.LanguageEntry;

public final class PartSeat extends APart {
    public boolean canControlGuns;
    private boolean riderChangingSeats;
    public final Point3D riderScale = new Point3D();
    public ItemPartGun activeGunItem;
    public int gunSequenceCooldown;
    public int gunGroupIndex;
    public int gunIndex;
    public final HashMap<ItemPartGun, List<PartGun>> gunGroups = new LinkedHashMap<>();

    public PartSeat(AEntityF_Multipart<?> entityOn, IWrapperPlayer placingPlayer, JSONPartDefinition placementDefinition, ItemPartSeat item, IWrapperNBT data) {
        super(entityOn, placingPlayer, placementDefinition, item, data);
        if (data != null) {
            this.activeGunItem = PackParser.getItem(data.getString("activeGunPackID"), data.getString("activeGunSystemName"), data.getString("activeGunSubName"));
        }
    }

    @Override
    public void linkToEntity(AEntityF_Multipart<?> entityOn, JSONPartDefinition placementDefinition) {
        if (vehicleOn != null) {
            removeRiderFromVehicle();
        }
        super.linkToEntity(entityOn, placementDefinition);
    }

    @Override
    public boolean interact(IWrapperPlayer player) {
        //See if we can interact with the seats of this vehicle.
        //This can happen if the vehicle is not locked, or we're already inside a locked vehicle.
        if (isActive) {
            if (vehicleOn == null || !vehicleOn.lockedVar.isActive || masterEntity.allParts.contains(player.getEntityRiding())) {
                if (rider != null) {
                    //We already have a rider for this seat.  If it's not us, mark the seat as taken.
                    //If it's an entity that can be leashed, dismount the entity and leash it.
                    if (rider instanceof IWrapperPlayer) {
                        if (player != rider) {
                            player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_SEATTAKEN));
                        }
                    } else if (!rider.leashTo(player)) {
                        //Can't leash up this entity, so mark the seat as taken.
                        player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_SEATTAKEN));
                    }
                } else {
                    //Seat is free.  Either mount this seat, or if we have a leashed animal, set it in that seat.
                    IWrapperEntity leashedEntity = player.getLeashedEntity();
                    if (leashedEntity != null) {
                        setRider(leashedEntity, true);
                    } else {
                        //Didn't find an animal.  Just mount the player.
                        //Don't mount them if they are sneaking, however.  This will confuse MC.
                        if (!player.isSneaking()) {
                            //Check if the rider is riding something before adding them.
                            //If they are riding something, remove them from it first.
                            //If they are riding our vehicle, don't adjust their head position.
                            AEntityB_Existing entityPlayerRiding = player.getEntityRiding();
                            if (entityPlayerRiding != null) {
                                if(entityPlayerRiding instanceof PartSeat) {
                                   ((PartSeat) entityPlayerRiding).riderChangingSeats = true;
                                }
                                entityPlayerRiding.removeRider();
                            }
                            setRider(player, !(entityPlayerRiding instanceof PartSeat) || ((PartSeat) entityPlayerRiding).vehicleOn != vehicleOn);
                        }
                    }
                }
            } else {
                player.sendPacket(new PacketPlayerChatMessage(player, LanguageSystem.INTERACT_VEHICLE_LOCKED));
            }
        }
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!canControlGuns && (activeGunItem != null || placementDefinition.canDisableGun)) {
            canControlGuns = true;
        }
    }

    @Override
    public void updatePartList() {
        super.updatePartList();

        //Update gun list, this is grouped by the specific gun.
        List<PartGun> gunList = new ArrayList<>();
        addLinkedPartsToList(gunList, PartGun.class);
        for (APart part : parts) {
            if (part instanceof PartGun) {
                gunList.add((PartGun) part);
            }
        }
        if (entityOn instanceof PartGun) {
            gunList.add((PartGun) entityOn);
        }

        gunGroups.clear();
        for (PartGun gun : gunList) {
            ItemPartGun gunItem = (ItemPartGun) gun.cachedItem;
            if (!gunGroups.containsKey(gunItem)) {
                gunGroups.put(gunItem, new ArrayList<>());
            }
            gunGroups.get(gunItem).add(gun);
        }

        //Check if gun controlled is still valid.
        if (!gunGroups.containsKey(activeGunItem)) {
            activeGunItem = null;
            gunIndex = 0;
        }

        //Reset active index so that we don't risk going out of range.
        gunGroupIndex = 0;

        //Populate camera list.
        if (masterEntity.definition.rendering != null && masterEntity.definition.rendering.cameraObjects != null) {
            for (JSONCameraObject camera : masterEntity.definition.rendering.cameraObjects) {
                cameras.add(camera);
                cameraEntities.put(camera, masterEntity);
            }
        } else {
            for (APart part : masterEntity.allParts) {
                if (part.definition.rendering != null && part.definition.rendering.cameraObjects != null) {
                    for (JSONCameraObject camera : part.definition.rendering.cameraObjects) {
                        cameras.add(camera);
                        cameraEntities.put(camera, part);
                    }
                }
            }
        }
    }

    @Override
    public boolean setRider(IWrapperEntity rider, boolean facesForwards) {
        if (super.setRider(rider, facesForwards)) {
            //Set if vehicle has a controller or not.
            if (vehicleOn != null && placementDefinition.isController) {
                ++vehicleOn.controllerCount;
                //Set the vehicle creative status, if it's not true already.
                if (rider instanceof IWrapperPlayer && ((IWrapperPlayer) rider).isCreative() && !vehicleOn.isCreative) {
                    vehicleOn.isCreative = true;
                }
            }

            //If this seat can control a gun, and isn't controlling one, set it now.
            //This prevents the need to select a gun when initially mounting.
            //Only do this if we don't allow for no gun selection, or if the rider is a NPC..
            //If we do have an active gun, validate that it's still correct.
            if (activeGunItem == null) {
                if (!placementDefinition.canDisableGun || !(rider instanceof IWrapperPlayer)) {
                    setNextActiveGun();
                }
            }

            if (riderIsClient && vehicleOn != null) {
                //Open the HUD.  This will have been closed in the remove call.
                new GUIHUD(vehicleOn, this);

                //Auto-start the engines, if we have that config enabled and we can start them.
                if (placementDefinition.isController && ConfigSystem.client.controlSettings.autostartEng.value && vehicleOn.canPlayerStartEngines((IWrapperPlayer) rider) && !vehicleOn.definition.motorized.overrideAutoStart) {
                    vehicleOn.engines.forEach(engine -> {
                        if (!vehicleOn.definition.motorized.isAircraft) {
                            InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.shiftNeutralVar));
                        }
                        InterfaceManager.packetInterface.sendToServer(new PacketPartEngine(engine, Signal.AS_ON));
                    });
                    if (vehicleOn.parkingBrakeVar.isActive) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicleOn.parkingBrakeVar));
                    }
                }
            }

            //Auto-close doors for the rider in this seat, if such doors exist.
            if (!world.isClient() && placementDefinition.interactableVariables != null) {
                placementDefinition.interactableVariables.forEach(variableList -> variableList.forEach(variable -> entityOn.getVariable(variable).setTo(0, true)));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeRider() {
        if (vehicleOn != null) {
            removeRiderFromVehicle();
        }

        //De-select active gun if required.
        if (placementDefinition.canDisableGun) {
            activeGunItem = null;
        }

        //Get rid of any potion effects that were caused by the seat
        if (placementDefinition.seatEffects != null) {
            for (JSONPotionEffect effect : placementDefinition.seatEffects) {
                rider.removePotionEffect(effect);
            }
        }

        //If we, and we aren't changing seats to another of the same vehicle, handle things.
        if (!riderChangingSeats) {
            //Set the rider dismount position.
            //If we have a dismount position in the JSON.  Use it.
            //Otherwise, put us to the right or left of the seat depending on x-offset.
            //Make sure to take into the movement of the seat we were riding if it had moved.
            //This ensures the dismount moves with the seat.
            //Need to tell the rider to not ride us before we do this.
            //This'll get called in super again, but it won't hurt anything.
            rider.setRiding(null);
            if (placementDefinition.dismountPos != null) {
                rider.setPosition(placementDefinition.dismountPos.copy().rotate(entityOn.orientation).add(entityOn.position), false);
            } else if (vehicleOn != null) {
                Point3D dismountPosition = position.copy().subtract(vehicleOn.position).reOrigin(vehicleOn.orientation);
                if (dismountPosition.x < 0) {
                    dismountPosition.add(-2D, 0D, 0D).rotate(vehicleOn.orientation).add(vehicleOn.position);
                } else {
                    dismountPosition.add(2D, 0D, 0D).rotate(vehicleOn.orientation).add(vehicleOn.position);
                }
                rider.setPosition(dismountPosition, false);
            } else {
                rider.setPosition(position, false);
            }
            rider.setOrientation(orientation);
    
            //Auto-open doors for the rider in this seat, if such doors exist.
            if (!world.isClient() && placementDefinition.interactableVariables != null) {
                placementDefinition.interactableVariables.forEach(variableList -> variableList.forEach(variable -> entityOn.getVariable(variable).setTo(1, true)));
            }
        }
        riderChangingSeats = false;
        super.removeRider();
    }

    private void removeRiderFromVehicle() {
        //Check if we have another controller, and if they are creative.
        boolean otherController = false;
        boolean otherCreativeController = false;
        for (APart part : vehicleOn.allParts) {
            if (part != this && part.rider instanceof IWrapperPlayer && part.placementDefinition.isController) {
                otherController = true;
                if (rider instanceof IWrapperPlayer && ((IWrapperPlayer) rider).isCreative()) {
                    otherCreativeController = true;
                    break;
                }
            }
        }

        //Set creative to false if there are no other creative controllers.
        if (!otherCreativeController) {
            vehicleOn.isCreative = false;
        }

        //Remove controller from count, if we are one.
        if (placementDefinition.isController) {
            --vehicleOn.controllerCount;
        }

        if (riderIsClient) {
            //Client player is the one that left the vehicle.  Make sure they don't have their mouse locked or a GUI open.
            AGUIBase.closeIfOpen(GUIPanel.class);
            AGUIBase.closeIfOpen(GUIHUD.class);
            AGUIBase.closeIfOpen(GUIRadio.class);

            //Auto-stop engines if we have the config, and there aren't any other controllers in the vehicle, and we aren't changing seats, or this vehicle has the override.
            if (placementDefinition.isController && !otherController && ConfigSystem.client.controlSettings.autostartEng.value && !vehicleOn.definition.motorized.overrideAutoStart) {
                vehicleOn.engines.forEach(engine -> {
                    if (engine.magnetoVar.isActive) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.magnetoVar));
                    }
                    if (engine.electricStarterVar.isActive) {
                        InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(engine.electricStarterVar));
                    }
                });
                InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableSet(vehicleOn.brakeVar, 0));
                if (!vehicleOn.parkingBrakeVar.isActive) {
                    InterfaceManager.packetInterface.sendToServer(new PacketEntityVariableToggle(vehicleOn.parkingBrakeVar));
                }
            }
        }
    }

    @Override
    public boolean updateRider() {
        if (super.updateRider()) {
            //Update scale, need to not include main vehicle scaling since that doesn't affect player scale.
            if (vehicleOn != null) {
                riderScale.set(scale.x / vehicleOn.scale.x, scale.y / vehicleOn.scale.y, scale.z / vehicleOn.scale.z);
            } else {
                riderScale.set(scale);
            }

            //Add all seat-specific effects to the rider
            if (placementDefinition.seatEffects != null) {
                for (JSONPotionEffect effect : placementDefinition.seatEffects) {
                    rider.addPotionEffect(effect);
                }
            }

            //If we are on the client, and the rider is the main client player, check controls.
            //If the seat is a controller, and we have mouseYoke enabled, and our view is locked disable the mouse from MC.
            //We also need to make sure the player in this event is the actual client player.  If we are on a server,
            //another player could be getting us to this logic point, thus we'd be making their inputs in the vehicle.
            if (world.isClient() && !InterfaceManager.clientInterface.isChatOpen() && riderIsClient) {
                ControlSystem.controlMultipart(masterEntity, placementDefinition.isController);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public LanguageEntry checkForRemoval(IWrapperPlayer player) {
        if (rider != null) {
            return LanguageSystem.INTERACT_PARTREMOVE_RIDER;
        } else {
            return super.checkForRemoval(player);
        }
    }

    @Override
    public boolean canBeClicked() {
        //Don't block clicking of seats on clients if the player is a rider on one of the parts of the vehicle we are on.
        if (world.isClient() && vehicleOn != null) {
            IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
            for (APart part : vehicleOn.allParts) {
                if (player.equals(part.rider) && part != this) {
                    return true;
                }
            }
        }
        //Don't let us click ourselves if we are riding ourselves.
        if (riderIsClient) {
            return false;
        }
        return super.canBeClicked();
    }

    /**
     * Sets the next active gun for this seat.  Active guns are queried by checking guns to
     * see if this rider can control them.  If so, then the active gun is set to that gun type.
     */
    public void setNextActiveGun() {
        //If we don't have an active gun, just get the next possible unit.
        if (activeGunItem == null) {
            for (ItemPartGun gunItem : gunGroups.keySet()) {
                activeGunItem = gunItem;
                gunIndex = 0;
                return;
            }
        } else {
            //If we didn't find an active gun, try to get another one.
            //This will be our first gun, unless we had an active gun and we can disable our gun.
            //In this case, we will just set our active gun to null.
            activeGunItem = getNextActiveGun();
            gunGroupIndex = 0;
        }
    }

    /**
     * Helper method to get the next active gun in the gun listings.
     */
    private ItemPartGun getNextActiveGun() {
        boolean pastActiveGun = false;
        ItemPartGun firstPossibleGun = null;

        //Iterate over all the gun types, attempting to get the type after our selected type.
        for (ItemPartGun gunItem : gunGroups.keySet()) {
            //If we already found our active gun in our gun list, we use the next entry as our next gun.
            if (pastActiveGun) {
                return gunItem;
            } else {
                //Add the first possible gun in case we go all the way around.
                if (firstPossibleGun == null) {
                    firstPossibleGun = gunItem;
                }
                //If the gun type is the same as the active gun, check if it's set to fireSolo.
                //If we didn't group it and need to go to the next active gun with that type.
                if (gunItem == activeGunItem) {
                    pastActiveGun = true;
                    if (gunItem.definition.gun.fireSolo) {
                        if (gunGroups.get(gunItem).size() <= ++gunIndex) {
                            gunIndex = 0;
                            pastActiveGun = true;
                        } else {
                            return gunItem;
                        }
                    }
                }
            }
        }

        //Got down here.  Either we don't have a gun, or we need the first.
        //If our current gun is active, and we have the first, and we can disable guns,
        //return null.  This will make the guns inactive this cycle.
        return placementDefinition.canDisableGun && activeGunItem != null ? null : firstPossibleGun;
    }

    @Override
    public ComputedVariable createComputedVariable(String variable) {
        switch (variable) {
            case ("seat_occupied"):
                return new ComputedVariable(this, variable, partialTicks -> rider != null ? 1 : 0, false);
            case ("seat_occupied_client"):
                return new ComputedVariable(this, variable, partialTicks -> riderIsClient ? 1 : 0, false);
            case ("seat_rider_yaw"):
                return new ComputedVariable(this, variable, partialTicks -> rider != null ? (partialTicks != 0 ? prevRiderRelativeOrientation.angles.y + (riderRelativeOrientation.angles.y - prevRiderRelativeOrientation.angles.y) * partialTicks : riderRelativeOrientation.angles.y) : 0, true);
            case ("seat_rider_pitch"):
                return new ComputedVariable(this, variable, partialTicks -> rider != null ? (partialTicks != 0 ? prevRiderRelativeOrientation.angles.x + (riderRelativeOrientation.angles.x - prevRiderRelativeOrientation.angles.x) * partialTicks : riderRelativeOrientation.angles.x) : 0, true);
            default:
                return super.createComputedVariable(variable);
        }
    }

    @Override
    public IWrapperNBT save(IWrapperNBT data) {
        super.save(data);
        if (activeGunItem != null) {
            data.setString("activeGunPackID", activeGunItem.definition.packID);
            data.setString("activeGunSystemName", activeGunItem.definition.systemName);
            data.setString("activeGunSubName", activeGunItem.subDefinition.subName);
        }
        return data;
    }
}