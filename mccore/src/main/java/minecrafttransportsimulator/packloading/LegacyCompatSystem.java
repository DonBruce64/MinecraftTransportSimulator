package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.components.ABlockBase.BlockMaterial;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.AJSONBase;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.AJSONPartProvider;
import minecrafttransportsimulator.jsondefs.JSONAction;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONCollisionBox;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup;
import minecrafttransportsimulator.jsondefs.JSONCollisionGroup.CollisionType;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONDecor.DecorComponentType;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.ItemComponentType;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONMuzzleGroup;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.CrafterComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.EngineType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.JSONSubParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleRenderingOrientation;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleSpawningOrientation;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import minecrafttransportsimulator.jsondefs.JSONRendering.ModelType;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.rendering.AModelParser;
import minecrafttransportsimulator.rendering.RenderableVertices;
import minecrafttransportsimulator.rendering.TreadRoller;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Class responsible for applying legacy compat code to JSONs.  All legacy compat code should
 * go here.  Once a definition calls methods in this class, it can be assumed to be in the most
 * modern form possible and ready for all the current systems.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public final class LegacyCompatSystem {

    public static final Map<String, String> variableChanges = new HashMap<>();

    static {
        variableChanges.put("engine_rpm_max", "maxRPM");
        variableChanges.put("engine_rpm_safe", "maxSafeRPM");
        variableChanges.put("engine_rpm_revlimit", "revlimitRPM");
        variableChanges.put("engine_rpm_revlimit_bounce", "revlimitBounce");
        variableChanges.put("engine_rpm_revresistance", "revResistance");
        variableChanges.put("engine_rpm_idle", "idleRPM");
        variableChanges.put("engine_rpm_start", "startRPM");
        variableChanges.put("engine_rpm_stall", "stallRPM");
        variableChanges.put("engine_starter_power", "starterPower");
        variableChanges.put("engine_fuel_consumption", "fuelConsumption");
        variableChanges.put("engine_heating_coefficient", "heatingCoefficient");
        variableChanges.put("engine_cooling_coefficient", "coolingCoefficient");
        variableChanges.put("engine_supercharger_fuel_consumption", "superchargerFuelConsumption");
        variableChanges.put("engine_supercharger_efficiency", "superchargerEfficiency");
        variableChanges.put("engine_gear_ratio", "gearRatio");
        variableChanges.put("engine_forceshift", "forceShift");
        variableChanges.put("engine_isautomatic", "isAutomatic");
        variableChanges.put("engine_wear_factor", "engineWearFactor");
        variableChanges.put("engine_winddown_rate", "engineWinddownRate");
        variableChanges.put("engine_jet_power_factor", "jetPowerFactor");
        variableChanges.put("engine_bypass_ratio", "bypassRatio");
    }

    public static void performLegacyCompats(AJSONBase definition) {
        if (definition instanceof AJSONItem) {
            AJSONItem item = (AJSONItem) definition;
            //Update materials to match new format.
            if (item.general.materials != null) {
                item.general.materialLists = new ArrayList<List<String>>();
                item.general.materialLists.add(item.general.materials);
                item.general.materials = null;
            } else if (item.general.materialLists == null) {
                item.general.materialLists = new ArrayList<List<String>>();
                item.general.materialLists.add(new ArrayList<>());
            }
            if (item.general.repairMaterials != null) {
                item.general.repairMaterialLists = new ArrayList<List<String>>();
                item.general.repairMaterialLists.add(item.general.repairMaterials);
                item.general.repairMaterials = null;
            }
        }

        if (definition instanceof AJSONMultiModelProvider) {
            AJSONMultiModelProvider provider = (AJSONMultiModelProvider) definition;
            //If we are a multi-model provider without a definition, add one so we don't crash on other systems.
            if (provider.definitions == null) {
                provider.definitions = new ArrayList<>();
                JSONSubDefinition subDef = new JSONSubDefinition();
                subDef.extraMaterialLists = new ArrayList<List<String>>();
                provider.general.materialLists.forEach(entry -> subDef.extraMaterialLists.add(new ArrayList<>()));
                subDef.name = provider.general.name;
                subDef.subName = "";
                provider.definitions.add(subDef);
            }

            //Add model name if we don't have one.
            if (provider.general.modelName != null) {
                for (JSONSubDefinition subDef : provider.definitions) {
                    subDef.modelName = provider.general.modelName;
                }
                provider.general.modelName = null;
            }

            //Check if the model needs a model type or has extraMaterials to convert..
            for (JSONSubDefinition subDef : provider.definitions) {
                if (subDef.extraMaterials != null) {
                    subDef.extraMaterialLists = new ArrayList<List<String>>();
                    subDef.extraMaterialLists.add(subDef.extraMaterials);
                    subDef.extraMaterials = null;
                }
            }
            if (provider.rendering == null) {
                provider.rendering = new JSONRendering();
            }
            if (provider.rendering.modelType == null) {
                provider.rendering.modelType = ModelType.OBJ;
            }

            //Move constants and initial variables to the main file and out of rendering.
            if (provider.rendering != null && provider.rendering.constants != null) {
                provider.constants = provider.rendering.constants;
                provider.rendering.constants = null;
            }
            if (provider.rendering != null && provider.rendering.initialVariables != null) {
                provider.initialVariables = provider.rendering.initialVariables;
                provider.rendering.initialVariables = null;
            }
            if (provider.constants != null) {
                provider.constantValues = new HashMap<>();
                provider.constants.forEach(key -> provider.constantValues.put(key, 1D));
                provider.constants = null;
            }

            //Move collision box-specific paramters to their groups.
            if (provider instanceof AJSONInteractableEntity) {
                AJSONInteractableEntity interactable = (AJSONInteractableEntity) provider;
                if (interactable.collisionGroups != null) {
                    for (JSONCollisionGroup collisionGroup : interactable.collisionGroups) {
                        for (JSONCollisionBox collisionBox : collisionGroup.collisions) {
                            if (collisionBox.armorThickness != 0) {
                                collisionGroup.armorThickness = collisionBox.armorThickness;
                                collisionBox.armorThickness = 0;
                            }
                            if (collisionBox.heatArmorThickness != 0) {
                                collisionGroup.heatArmorThickness = collisionBox.heatArmorThickness;
                                collisionBox.heatArmorThickness = 0;
                            }
                            if (collisionBox.damageMultiplier != 0) {
                                collisionGroup.damageMultiplier = collisionBox.damageMultiplier;
                                collisionBox.damageMultiplier = 0;
                            }
                        }
                    }
                }
            }

            //Update variable names for CV-changed variables.
            if (provider.variableModifiers != null) {
                provider.variableModifiers.forEach(modifier -> {
                    String newVariable = variableChanges.get(modifier.variable);
                    if (newVariable != null) {
                        modifier.variable = newVariable;
                    }
                    if (modifier.animations != null) {
                        modifier.animations.forEach(animation -> {
                            String newVariable2 = variableChanges.get(animation.variable);
                            if (newVariable2 != null) {
                                animation.variable = newVariable2;
                            }
                        });
                    }
                });
            }
            if (provider.rendering != null && provider.rendering.animatedObjects != null) {
                provider.rendering.animatedObjects.forEach(animatedObject -> {
                    if (animatedObject.animations != null) {
                        animatedObject.animations.forEach(animation -> {
                            String newVariable = variableChanges.get(animation.variable);
                            if (newVariable != null) {
                                animation.variable = newVariable;
                            }
                        });
                    }
                });
            }
        }

        //Do JSON-specific compats.
        if (definition instanceof JSONVehicle) {
            performVehicleLegacyCompats((JSONVehicle) definition);
        } else if (definition instanceof JSONPart) {
            performPartLegacyCompats((JSONPart) definition);
        } else if (definition instanceof JSONInstrument) {
            performInstrumentLegacyCompats((JSONInstrument) definition);
        } else if (definition instanceof JSONPoleComponent) {
            performPoleLegacyCompats((JSONPoleComponent) definition);
        } else if (definition instanceof JSONDecor) {
            performDecorLegacyCompats((JSONDecor) definition);
        } else if (definition instanceof JSONRoadComponent) {
            performRoadLegacyCompats((JSONRoadComponent) definition);
        } else if (definition instanceof JSONItem) {
            performItemLegacyCompats((JSONItem) definition);
        } else if (definition instanceof JSONSkin) {
            performSkinLegacyCompats((JSONSkin) definition);
        } else if (definition instanceof JSONBullet) {
            performBulletLegacyCompats((JSONBullet) definition);
        }

        if (definition instanceof AJSONMultiModelProvider) {
            //Parse the model and do LCs on it if we need to do so.
            //This happens after general parsing so we don't clobber anything with the model LCs.
            AJSONMultiModelProvider provider = (AJSONMultiModelProvider) definition;
            if (ConfigSystem.settings != null && ConfigSystem.settings.general.doLegacyLightCompats.value && !(definition instanceof JSONSkin) && provider.rendering.modelType.equals(ModelType.OBJ)) {
                performModelLegacyCompats((AJSONMultiModelProvider) definition);
            }


            //Check vehicle litVariable LCs, these have to run after model LCs since the model can set some of these.
            if (provider instanceof JSONVehicle) {
                JSONVehicle vehicleDef = (JSONVehicle) provider;
                if (vehicleDef.motorized.litVariable == null) {
                    if (vehicleDef.motorized.hasRunningLights) {
                        vehicleDef.motorized.litVariable = "running_light";
                    } else if (vehicleDef.motorized.hasHeadlights) {
                        vehicleDef.motorized.litVariable = "headlight";
                    } else if (vehicleDef.motorized.hasNavLights) {
                        vehicleDef.motorized.litVariable = "navigation_light";
                    } else if (vehicleDef.motorized.hasStrobeLights) {
                        vehicleDef.motorized.litVariable = "strobe_light";
                    } else if (vehicleDef.motorized.hasTaxiLights) {
                        vehicleDef.motorized.litVariable = "taxi_light";
                    } else if (vehicleDef.motorized.hasLandingLights) {
                        vehicleDef.motorized.litVariable = "landing_light";
                    } else {
                        //Probably a trailer, just use running lights.
                        vehicleDef.motorized.litVariable = "running_light";
                    }
                }
            }

            //Check particles.
            if (provider.rendering.particles != null) {
                for (JSONParticle particleDef : provider.rendering.particles) {
                    performParticleLegacyCompats(particleDef);
                }
            }
            
            //Convert old hitboxes.
            if(definition instanceof AJSONInteractableEntity) {
                AJSONInteractableEntity interactable = (AJSONInteractableEntity) provider;
                if(interactable.collisionGroups != null) {
                    for(JSONCollisionGroup collisionGroup : interactable.collisionGroups) {
                        if (collisionGroup.collisionTypes == null) {
                            collisionGroup.collisionTypes = new HashSet<>();
                            if (collisionGroup.isForBullets) {
                                collisionGroup.collisionTypes.add(CollisionType.BULLET);
                                collisionGroup.isForBullets = false;
                            } else if (collisionGroup.isInterior) {
                                collisionGroup.collisionTypes.add(CollisionType.ENTITY);
                                collisionGroup.collisionTypes.add(CollisionType.ATTACK);
                                collisionGroup.collisionTypes.add(CollisionType.CLICK);
                                collisionGroup.isInterior = false;
                            } else {
                                collisionGroup.collisionTypes.add(CollisionType.BLOCK);
                                collisionGroup.collisionTypes.add(CollisionType.ENTITY);
                                collisionGroup.collisionTypes.add(CollisionType.ATTACK);
                                collisionGroup.collisionTypes.add(CollisionType.CLICK);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void performVehicleLegacyCompats(JSONVehicle definition) {
        //Move vehicle parameters to the motorized section.
        if (definition.general.emptyMass > 0) {
            definition.motorized.isAircraft = definition.general.isAircraft;
            definition.general.isAircraft = false;
            definition.motorized.isBlimp = definition.general.isBlimp;
            definition.general.isBlimp = false;
            definition.motorized.hasOpenTop = definition.general.openTop;
            definition.general.openTop = false;
            definition.motorized.emptyMass = definition.general.emptyMass;
            definition.general.emptyMass = 0;
        }

        if (definition.car != null) {
            definition.motorized.isBigTruck = definition.car.isBigTruck;
            definition.motorized.isFrontWheelDrive = definition.car.isFrontWheelDrive;
            definition.motorized.isRearWheelDrive = definition.car.isRearWheelDrive;
            definition.motorized.hasCruiseControl = definition.car.hasCruiseControl;
            definition.motorized.axleRatio = definition.car.axleRatio;
            definition.motorized.dragCoefficient = definition.car.dragCoefficient;
            definition.motorized.hornSound = definition.car.hornSound;
            definition.car = null;
        }

        //If we still have the old type parameter and are an aircraft, set the flag to true.
        if (definition.general.type != null) {
            if (definition.general.type.equals("plane") || definition.general.type.equals("blimp") || definition.general.type.equals("helicopter")) {
                definition.motorized.isAircraft = true;
            }
            if (definition.general.type.equals("blimp")) {
                definition.motorized.isBlimp = true;
            }
            definition.general.type = null;
        }

        //Set default health.
        if (definition.general.health == 0) {
            definition.general.health = 100;
        }

        //Set default crash speed totaled if not set.
        if (definition.motorized.crashSpeedMax != 0 && definition.motorized.crashSpeedDestroyed == 0) {
            definition.motorized.crashSpeedDestroyed = definition.motorized.crashSpeedMax;
        }

        if (definition.plane != null) {
            definition.general.isAircraft = true;
            definition.motorized.hasFlaps = definition.plane.hasFlaps;
            definition.motorized.hasAutopilot = definition.plane.hasAutopilot;
            definition.motorized.wingSpan = definition.plane.wingSpan;
            definition.motorized.wingArea = definition.plane.wingArea;
            definition.motorized.tailDistance = definition.plane.tailDistance;
            definition.motorized.aileronArea = definition.plane.aileronArea;
            definition.motorized.elevatorArea = definition.plane.elevatorArea;
            definition.motorized.rudderArea = definition.plane.rudderArea;
            definition.plane = null;

            //If aileronArea is 0, we're a legacy plane and need to adjust.
            if (definition.motorized.aileronArea == 0) {
                definition.motorized.aileronArea = definition.motorized.wingArea / 5F;
            }
        }

        if (definition.blimp != null) {
            definition.general.isAircraft = true;
            definition.general.isBlimp = true;
            definition.motorized.isBlimp = true;
            definition.motorized.crossSectionalArea = definition.blimp.crossSectionalArea;
            definition.motorized.tailDistance = definition.blimp.tailDistance;
            definition.motorized.rudderArea = definition.blimp.rudderArea;
            definition.motorized.ballastVolume = definition.blimp.ballastVolume;
            definition.blimp = null;
        }

        //Set panel if we don't have one.
        if (definition.motorized.panel == null) {
            if (definition.motorized.isAircraft) {
                definition.motorized.panel = "mts:default_plane";
            } else {
                definition.motorized.panel = "mts:default_car";
            }
        }

        //Check for old hitches and hookups.
        if (definition.motorized.hitchPos != null) {
            definition.connections = new ArrayList<>();
            for (String hitchName : definition.motorized.hitchTypes) {
                JSONConnection connection = new JSONConnection();
                connection.hookup = false;
                connection.type = hitchName;
                connection.pos = definition.motorized.hitchPos;
                if (connection.mounted) {
                    connection.rot = new RotationMatrix();
                }
                definition.connections.add(connection);
            }
            definition.motorized.hitchPos = null;
            definition.motorized.hitchTypes = null;
        }
        if (definition.motorized.hookupPos != null) {
            if (definition.connections == null) {
                definition.connections = new ArrayList<>();
            }
            JSONConnection connection = new JSONConnection();
            connection.hookup = true;
            connection.type = definition.motorized.hookupType;
            connection.pos = definition.motorized.hookupPos;
            definition.connections.add(connection);
            definition.motorized.hookupType = null;
            definition.motorized.hookupPos = null;
        }

        //Check for old instrument references.
        if (definition.motorized.instruments != null) {
            definition.instruments = definition.motorized.instruments;
            definition.motorized.instruments = null;
            for (JSONInstrumentDefinition def : definition.instruments) {
                if (def.optionalPartNumber != 0) {
                    def.placeOnPanel = true;
                }
            }
        }

        //Check to make sure helicopters have area-factors.  Previously this wasn't needed so they could all be 0.
        if (definition.motorized.isAircraft && definition.motorized.aileronArea == 0 && definition.motorized.elevatorArea == 0 && definition.motorized.rudderArea == 0) {
            definition.motorized.aileronArea = 1.0F;
            definition.motorized.elevatorArea = 1.0F;
            definition.motorized.rudderArea = 1.0F;
        }

        //Check for old flaps.
        if (definition.motorized.hasFlaps) {
            definition.motorized.flapSpeed = 0.1F;
            definition.motorized.flapNotches = new ArrayList<>();
            for (int i = 0; i <= EntityVehicleF_Physics.MAX_FLAP_ANGLE_REFERENCE / 10 / 5; ++i) {
                definition.motorized.flapNotches.add((float) (i * 5));
            }
            definition.motorized.hasFlaps = false;
        }

        //Check if we didn't specify drag.
        if (definition.motorized.dragCoefficient == 0) {
            definition.motorized.dragCoefficient = definition.motorized.isAircraft || definition.motorized.isBlimp ? 0.03F : 2.0F;
        }

        //Check if we didn't specify a braking force.
        if (definition.motorized.brakingFactor == 0) {
            definition.motorized.brakingFactor = 1.0F;
        }

        //Check if we didn't specity an axleRatio.
        if (definition.motorized.axleRatio == 0) {
            definition.motorized.axleRatio = 3.55F;
        }

        //Move cruiseControl to autopilot.
        if (definition.motorized.hasCruiseControl) {
            definition.motorized.hasAutopilot = true;
            definition.motorized.hasCruiseControl = false;
        }

        //Change downForce's name to steeringForceFactor
        if (definition.motorized.downForce != 0) {
            definition.motorized.steeringForceFactor = definition.motorized.downForce;
            definition.motorized.downForce = 0;
        }

        //Add hookup variables if we are a trailer and don't have them.
        if (definition.motorized.isTrailer && definition.motorized.hookupVariables == null) {
            definition.motorized.hookupVariables = new ArrayList<>();
            definition.motorized.hookupVariables.add("electric_power");
            definition.motorized.hookupVariables.add("engine_gear_1");
            definition.motorized.hookupVariables.add("engines_on");
            definition.motorized.hookupVariables.add("right_turn_signal");
            definition.motorized.hookupVariables.add("left_turn_signal");
            definition.motorized.hookupVariables.add("runninglight");
            definition.motorized.hookupVariables.add("headlight");
            definition.motorized.hookupVariables.add("emergencylight");
        }

        //Update part defs.
        for (JSONPartDefinition partDef : definition.parts) {
            try {
                performVehiclePartDefLegacyCompats(partDef);
            } catch (Exception e) {
                e.printStackTrace();
                throw new NullPointerException("Could not perform Legacy Compats on part entry #" + (definition.parts.indexOf(partDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
            }
        }
        if (definition.parts != null) {
            performPartSlotListingLegacyCompats(definition.parts, definition.motorized.isFrontWheelDrive, definition.motorized.isRearWheelDrive);
        }

        //Remove FWD and RWD parameters and replace with engine linking.
        //Need to do this after we do part def and slot compats since those change the type-names.
        if (definition.motorized.isFrontWheelDrive || definition.motorized.isRearWheelDrive) {
            for (JSONPartDefinition partDef : definition.parts) {
                for (String partDefType : partDef.types) {
                    if (partDefType.startsWith("engine")) {
                        //Link all slots with wheels, or generic, to this engine.
                        if (partDef.linkedParts == null) {
                            partDef.linkedParts = new ArrayList<>();
                        }
                        for (JSONPartDefinition partDef2 : definition.parts) {
                            if ((definition.motorized.isFrontWheelDrive && partDef2.pos.z > 0) || definition.motorized.isRearWheelDrive && partDef2.pos.z <= 0) {
                                for (String partDefType2 : partDef2.types) {
                                    if (partDefType2.startsWith("ground") || partDefType2.startsWith("generic")) {
                                        if (!partDef.linkedParts.contains(definition.parts.indexOf(partDef2) + 1)) {
                                            partDef.linkedParts.add(definition.parts.indexOf(partDef2) + 1);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            definition.motorized.isFrontWheelDrive = false;
            definition.motorized.isRearWheelDrive = false;
        }

        performVehicleConnectionLegacyCompats(definition);
        performVehicleCollisionLegacyCompats(definition);

        //Do rendering compats.
        if (definition.rendering != null) {
            //Check for old HUD stuff.
            if (definition.rendering.hudTexture != null) {
                definition.motorized.hudTexture = definition.rendering.hudTexture;
                definition.rendering.hudTexture = null;
            }
            if (definition.rendering.panelTexture != null) {
                definition.motorized.panelTexture = definition.rendering.panelTexture;
                definition.rendering.panelTexture = null;
            }
            if (definition.rendering.panelTextColor != null) {
                definition.motorized.panelTextColor = definition.rendering.panelTextColor;
                definition.rendering.panelTextColor = null;
            }
            if (definition.rendering.panelLitTextColor != null) {
                definition.motorized.panelLitTextColor = definition.rendering.panelLitTextColor;
                definition.rendering.panelLitTextColor = null;
            }

            //Do compats for sounds.
            if (definition.rendering.sounds == null) {
                definition.rendering.sounds = new ArrayList<>();
                if (definition.motorized.hornSound != null) {
                    JSONSound hornSound = new JSONSound();
                    hornSound.name = definition.motorized.hornSound;
                    hornSound.looping = true;
                    hornSound.activeAnimations = new ArrayList<>();
                    JSONAnimationDefinition hornDef = new JSONAnimationDefinition();
                    hornDef.animationType = AnimationComponentType.VISIBILITY;
                    hornDef.variable = "horn";
                    hornDef.clampMin = 1.0F;
                    hornDef.clampMax = 1.0F;
                    hornSound.activeAnimations.add(hornDef);
                    definition.rendering.sounds.add(hornSound);
                    definition.motorized.hornSound = null;
                }
                if (definition.motorized.sirenSound != null) {
                    JSONSound sirenSound = new JSONSound();
                    sirenSound.name = definition.motorized.sirenSound;
                    sirenSound.looping = true;
                    sirenSound.activeAnimations = new ArrayList<>();
                    JSONAnimationDefinition sirenDef = new JSONAnimationDefinition();
                    sirenDef.animationType = AnimationComponentType.VISIBILITY;
                    sirenDef.variable = "siren";
                    sirenDef.clampMin = 1.0F;
                    sirenDef.clampMax = 1.0F;
                    sirenSound.activeAnimations.add(sirenDef);
                    definition.rendering.sounds.add(sirenSound);
                    if (definition.rendering.customVariables == null) {
                        definition.rendering.customVariables = new ArrayList<>();
                    }
                    definition.rendering.customVariables.add("siren");
                    definition.motorized.sirenSound = null;
                }
                if (definition.motorized.isBigTruck) {
                    JSONSound airbrakeSound = new JSONSound();
                    airbrakeSound.name = InterfaceManager.coreModID + ":air_brake_activating";
                    airbrakeSound.activeAnimations = new ArrayList<>();
                    JSONAnimationDefinition airbrakeDef = new JSONAnimationDefinition();
                    airbrakeDef.animationType = AnimationComponentType.VISIBILITY;
                    airbrakeDef.variable = "p_brake";
                    airbrakeDef.clampMin = 1.0F;
                    airbrakeDef.clampMax = 1.0F;
                    airbrakeSound.activeAnimations.add(airbrakeDef);
                    definition.rendering.sounds.add(airbrakeSound);

                    JSONSound backupBeeperSound = new JSONSound();
                    backupBeeperSound.name = InterfaceManager.coreModID + ":backup_beeper";
                    backupBeeperSound.looping = true;
                    backupBeeperSound.activeAnimations = new ArrayList<>();
                    JSONAnimationDefinition backupBeeperDef = new JSONAnimationDefinition();
                    backupBeeperDef.animationType = AnimationComponentType.VISIBILITY;
                    backupBeeperDef.variable = "engine_gear_1";
                    backupBeeperDef.clampMin = -10.0F;
                    backupBeeperDef.clampMax = -1.0F;
                    backupBeeperSound.activeAnimations.add(backupBeeperDef);
                    definition.rendering.sounds.add(backupBeeperSound);

                    definition.motorized.isBigTruck = false;
                }
            }

            //Do compats for particles.
            if (definition.rendering.particles == null) {
                definition.rendering.particles = new ArrayList<>();
            }
            int engineNumber = 0;
            for (JSONPartDefinition partDef : definition.parts) {
                if (partDef.particleObjects != null) {
                    ++engineNumber;
                    int pistonNumber = 0;
                    for (JSONParticle exhaustDef : partDef.particleObjects) {
                        ++pistonNumber;
                        exhaustDef.type = ParticleType.SMOKE;
                        exhaustDef.activeAnimations = new ArrayList<>();
                        exhaustDef.initialVelocity = exhaustDef.velocityVector;
                        exhaustDef.velocityVector = null;
                        JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
                        activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                        activeAnimation.variable = "engine_piston_" + pistonNumber + "_" + partDef.particleObjects.size() + "_cam_" + engineNumber;
                        activeAnimation.clampMin = 1.0F;
                        activeAnimation.clampMax = 1.0F;
                        exhaustDef.activeAnimations.add(activeAnimation);
                        definition.rendering.particles.add(exhaustDef);

                        JSONParticle backfireDef = new JSONParticle();
                        backfireDef.type = exhaustDef.type;
                        backfireDef.color = ColorRGB.BLACK;
                        backfireDef.scale = 2.5F;
                        backfireDef.quantity = 5;
                        backfireDef.pos = exhaustDef.pos;
                        backfireDef.initialVelocity = exhaustDef.initialVelocity;
                        backfireDef.activeAnimations = new ArrayList<>();
                        activeAnimation = new JSONAnimationDefinition();
                        activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                        activeAnimation.variable = "engine_backfired_" + engineNumber;
                        activeAnimation.clampMin = 1.0F;
                        activeAnimation.clampMax = 1.0F;
                        backfireDef.activeAnimations.add(activeAnimation);
                        definition.rendering.particles.add(backfireDef);
                    }
                    partDef.particleObjects = null;
                }
            }

            try {
                performAnimationLegacyCompats(definition.rendering);
            } catch (Exception e) {
                throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
            }
        }
    }

    private static void performPartLegacyCompats(JSONPart definition) {
        //Move general things to generic section.
        if (definition.general.type != null) {
            if (definition.generic == null) {
                definition.generic = new JSONPart.JSONPartGeneric();
            }
            definition.generic.type = definition.general.type;
            definition.general.type = null;
            definition.generic.customType = definition.general.customType;
            definition.general.customType = null;
            definition.generic.useVehicleTexture = definition.general.useVehicleTexture;
            definition.general.useVehicleTexture = false;
        }
        
        //Move vehicle texture to new section.
        if (definition.generic.useVehicleTexture) {
            for (JSONSubDefinition subDef : definition.definitions) {
                subDef.useVehicleTexture = true;
            }
            definition.general.useVehicleTexture = false;
        }

        //Move subParts to parts if we have them there.
        if (definition.subParts != null) {
            definition.parts = definition.subParts;
            definition.subParts = null;
        }

        //Check for old ground devices, crates, barrels, effectors, and customs.
        switch (definition.generic.type) {
            case ("wheel"): {
                definition.generic.type = "ground_" + definition.generic.type;
                definition.ground = new JSONPart.JSONPartGroundDevice();
                definition.ground.isWheel = true;
                definition.ground.width = definition.wheel.diameter / 2F;
                definition.ground.height = definition.wheel.diameter;
                definition.ground.lateralFriction = definition.wheel.lateralFriction;
                definition.ground.motiveFriction = definition.wheel.motiveFriction;
                definition.wheel = null;
                break;
            }
            case ("skid"): {
                definition.generic.type = "ground_" + definition.generic.type;
                definition.ground = new JSONPart.JSONPartGroundDevice();
                definition.ground.width = definition.skid.width;
                definition.ground.height = definition.skid.width;
                definition.ground.lateralFriction = definition.skid.lateralFriction;
                definition.skid = null;
                break;
            }
            case ("pontoon"): {
                definition.generic.type = "ground_" + definition.generic.type;
                definition.ground = new JSONPart.JSONPartGroundDevice();
                definition.ground.canFloat = true;
                definition.ground.width = definition.pontoon.width;
                definition.ground.height = definition.pontoon.width;
                definition.ground.lateralFriction = definition.pontoon.lateralFriction;
                definition.ground.extraCollisionBoxOffset = definition.pontoon.extraCollisionBoxOffset;
                definition.pontoon = null;
                break;
            }
            case ("tread"): {
                definition.generic.type = "ground_" + definition.generic.type;
                definition.ground = new JSONPart.JSONPartGroundDevice();
                definition.ground.isTread = true;
                definition.ground.width = definition.tread.width;
                definition.ground.height = definition.tread.width;
                definition.ground.lateralFriction = definition.tread.lateralFriction;
                definition.ground.motiveFriction = definition.tread.motiveFriction;
                definition.ground.extraCollisionBoxOffset = definition.tread.extraCollisionBoxOffset;
                definition.ground.spacing = definition.tread.spacing;
                definition.tread = null;
                break;
            }
            case ("crate"): {
                definition.generic.type = "interactable_crate";
                definition.interactable = new JSONPart.JSONPartInteractable();
                definition.interactable.interactionType = InteractableComponentType.CRATE;
                definition.interactable.inventoryUnits = 1;
                definition.interactable.feedsVehicles = true;
                break;
            }
            case ("barrel"): {
                definition.generic.type = "interactable_barrel";
                definition.interactable = new JSONPart.JSONPartInteractable();
                definition.interactable.interactionType = InteractableComponentType.BARREL;
                definition.interactable.inventoryUnits = 1;
                break;
            }
            case ("crafting_table"): {
                definition.generic.type = "interactable_crafting_table";
                definition.interactable = new JSONPart.JSONPartInteractable();
                definition.interactable.interactionType = InteractableComponentType.CRAFTING_TABLE;
                break;
            }
            case ("furnace"): {
                definition.generic.type = "interactable_furnace";
                definition.interactable = new JSONPart.JSONPartInteractable();
                definition.interactable.interactionType = InteractableComponentType.FURNACE;
                break;
            }
            case ("fertilizer"): {
                definition.generic.type = "effector_fertilizer";
                definition.effector = new JSONPart.JSONPartEffector();
                definition.effector.type = EffectorComponentType.FERTILIZER;
                break;
            }
            case ("harvester"): {
                definition.generic.type = "effector_harvester";
                definition.effector = new JSONPart.JSONPartEffector();
                definition.effector.type = EffectorComponentType.HARVESTER;
                break;
            }
            case ("planter"): {
                definition.generic.type = "effector_planter";
                definition.effector = new JSONPart.JSONPartEffector();
                definition.effector.type = EffectorComponentType.PLANTER;
                break;
            }
            case ("plow"): {
                definition.generic.type = "effector_plow";
                definition.effector = new JSONPart.JSONPartEffector();
                definition.effector.type = EffectorComponentType.PLOW;
                break;
            }
            case ("custom"): {
                definition.generic.type = "generic";
                definition.generic.height = definition.custom.height;
                definition.generic.width = definition.custom.width;
                definition.custom = null;
                break;
            }
        }

        //Set engine new parameters.
        if (definition.engine != null) {
            //Add engine type if it is missing.
            if (definition.engine.type == null) {
                definition.engine.type = JSONPart.EngineType.NORMAL;
            }

            //Add fuel type, if it is missing.
            if (definition.engine.type == EngineType.MAGIC) {
                definition.engine.fuelType = JSONConfigSettings.FuelDefaults.NOTHING.name().toLowerCase();
            }
            if (definition.engine.fuelType == null) {
                definition.engine.fuelType = JSONConfigSettings.FuelDefaults.DIESEL.name().toLowerCase();
            }

            //If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
            if (definition.generic.type.equals("engine_jet") && definition.engine.jetPowerFactor == 0) {
                definition.engine.jetPowerFactor = 1.0F;
                definition.engine.bypassRatio = definition.engine.gearRatios.get(0);
                definition.engine.gearRatios.set(0, 1.0F);
            }

            //If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
            if (definition.engine.gearRatios.size() == 1) {
                definition.engine.propellerRatio = 1 / definition.engine.gearRatios.get(0);
                definition.engine.gearRatios.clear();
                definition.engine.gearRatios.add(-1F);
                definition.engine.gearRatios.add(0F);
                definition.engine.gearRatios.add(1F);
            }

            //Check various engine parameters that shouldn't be 0 as they might not be set.
            if (definition.engine.shiftSpeed == 0) {
                definition.engine.shiftSpeed = 20;
            } else if (definition.engine.shiftSpeed == -1) {
                definition.engine.shiftSpeed = 0;
            }
            if (definition.engine.revResistance == 0) {
                definition.engine.revResistance = 10;
            }
            if (definition.engine.idleRPM == 0) {
                definition.engine.idleRPM = definition.engine.maxRPM < 15000 ? 500 : 2000;
            } else if (definition.engine.idleRPM == -1) {
                definition.engine.idleRPM = 0;
            }
            if (definition.engine.maxSafeRPM == 0) {
                definition.engine.maxSafeRPM = definition.engine.maxRPM < 15000 ? definition.engine.maxRPM - (definition.engine.maxRPM - 2500) / 2 : (int) (definition.engine.maxRPM / 1.1);
            }
            if (definition.engine.revlimitRPM == 0) {
                if (definition.engine.jetPowerFactor != 0 || definition.engine.gearRatios.size() == 3) {
                    definition.engine.revlimitRPM = -1;
                } else {
                    definition.engine.revlimitRPM = (int) (definition.engine.maxSafeRPM * 0.95);
                }
            }
            if (definition.engine.revlimitBounce == 0) {
                definition.engine.revlimitBounce = definition.engine.revResistance;
            }
            if (definition.engine.startRPM == 0) {
                definition.engine.startRPM = (int) (definition.engine.idleRPM * 1.2);
            } else if (definition.engine.startRPM == -1) {
                definition.engine.startRPM = 0;
            }
            if (definition.engine.stallRPM == 0) {
                definition.engine.stallRPM = (int) (definition.engine.idleRPM * 0.65);
            } else if (definition.engine.stallRPM == -1) {
                definition.engine.stallRPM = 0;
            }
            if (definition.engine.engineWinddownRate == 0) {
                definition.engine.engineWinddownRate = 10;
            }
            if (definition.engine.engineWearFactor == 0) {
                definition.engine.engineWearFactor = 1;
            }
            if (definition.engine.coolingCoefficient == 0) {
                definition.engine.coolingCoefficient = 1;
            }
            if (definition.engine.heatingCoefficient == 0) {
                definition.engine.heatingCoefficient = 1;
            }

            //If we don't have matching up-shift and down-shift numbers, we are an engine that came before multiple reverse gears.
            if (definition.engine.upShiftRPM != null) {
                while (definition.engine.upShiftRPM.size() < definition.engine.gearRatios.size()) {
                    definition.engine.upShiftRPM.add(0, 0);
                }
            }
            if (definition.engine.downShiftRPM != null) {
                while (definition.engine.downShiftRPM.size() < definition.engine.gearRatios.size()) {
                    definition.engine.downShiftRPM.add(0, 0);
                }
            }
        }

        //Do propeller compats.
        if (definition.propeller != null) {
            if (definition.propeller.pitchChangeRate == 0) {
                definition.propeller.pitchChangeRate = 1;
            }
        }

        //Do various ground device compats.
        if (definition.ground != null) {
            //Set flat height if it's not set.
            if (definition.ground.canGoFlat && definition.ground.flatHeight == 0) {
                definition.ground.flatHeight = definition.ground.height / 2F;
                definition.ground.canGoFlat = false;
            }
            //Set climb height if it's not set.
            if (definition.ground.climbHeight == 0) {
                definition.ground.climbHeight = 1.5F;
            }
            //Set friction modifiers.
            if (definition.ground.wetFrictionPenalty == 0 && !definition.ground.isTread) {
                definition.ground.wetFrictionPenalty = -0.1F;
            }
            if (definition.ground.frictionModifiers == null) {
                definition.ground.frictionModifiers = new LinkedHashMap<>();
                definition.ground.frictionModifiers.put(BlockMaterial.SNOW, -0.2F);
                definition.ground.frictionModifiers.put(BlockMaterial.ICE, -0.2F);
            } else {
                if (definition.ground.wetFrictionPenalty == 0 && definition.ground.frictionModifiers.containsKey(BlockMaterial.NORMAL) && definition.ground.frictionModifiers.containsKey(BlockMaterial.NORMAL_WET)) {
                    definition.ground.wetFrictionPenalty = definition.ground.frictionModifiers.get(BlockMaterial.NORMAL) - definition.ground.frictionModifiers.get(BlockMaterial.NORMAL_WET);
                }
                definition.ground.frictionModifiers.remove(BlockMaterial.DIRT_WET);
                definition.ground.frictionModifiers.remove(BlockMaterial.SAND_WET);
                definition.ground.frictionModifiers.remove(BlockMaterial.NORMAL_WET);
            }
        }

        //If the part is a seat, and doesn't have a seat sub-section, add one.
        if (definition.generic.type.startsWith("seat") && definition.seat == null) {
            definition.seat = new JSONPart.JSONPartSeat();
        }

        //If the part is a seat, and has player scaling, convert it.
        if (definition.seat != null) {
            if (definition.seat.widthScale != 0 || definition.seat.heightScale != 0) {
                if (definition.seat.widthScale == 0) {
                    definition.seat.widthScale = 1;
                }
                if (definition.seat.heightScale == 0) {
                    definition.seat.heightScale = 1;
                }
                definition.seat.playerScale = new Point3D(definition.seat.widthScale, definition.seat.heightScale, definition.seat.widthScale);
                definition.seat.widthScale = 0;
                definition.seat.heightScale = 0;
            }
        }

        //If the part is a gun, set yaw and pitch speed if not set.
        //Also set muzzle position.
        if (definition.gun != null) {
            if (definition.gun.yawSpeed == 0) {
                definition.gun.yawSpeed = 50 / definition.gun.diameter + 1 / definition.gun.length;
            }
            if (definition.gun.pitchSpeed == 0) {
                definition.gun.pitchSpeed = 50 / definition.gun.diameter + 1 / definition.gun.length;
            }
            if (definition.gun.muzzleGroups == null) {
                definition.gun.muzzleGroups = new ArrayList<>();
                JSONMuzzleGroup muzzleGroup = new JSONMuzzleGroup();
                muzzleGroup.muzzles = new ArrayList<>();
                JSONMuzzle muzzle = new JSONMuzzle();
                muzzle.pos = new Point3D(0, 0, definition.gun.length);
                muzzleGroup.muzzles.add(muzzle);
                definition.gun.muzzleGroups.add(muzzleGroup);
            }
            if (definition.gun.lockOnType == null) {
                definition.gun.lockOnType = JSONPart.LockOnType.DEFAULT;
            }
            if (definition.gun.targetType == null) {
                definition.gun.targetType = JSONPart.TargetType.ALL;
            }
            definition.gun.length = 0;
        }

        if (definition.interactable != null) {
            if (definition.interactable.interactionType == InteractableComponentType.FURNACE) {
                //Convert old furnaces.
                if (definition.interactable.crafterType == null) {
                    definition.interactable.crafterType = CrafterComponentType.STANDARD;
                    definition.interactable.crafterRate = 1.0F;
                    definition.interactable.crafterEfficiency = 1.0F;
                }

                //Convert furnaces to crafters.
                if (definition.interactable.furnaceType != null) {
                    definition.interactable.crafterType = definition.interactable.furnaceType;
                    definition.interactable.furnaceType = null;
                    definition.interactable.crafterRate = definition.interactable.furnaceRate;
                    definition.interactable.furnaceRate = 0;
                    definition.interactable.crafterEfficiency = definition.interactable.furnaceEfficiency;
                    definition.interactable.furnaceEfficiency = 0;
                }
            }
        }

        if (definition.effector != null) {
            //Convert old effector delay.
            if (definition.effector.placerDelay != 0) {
                definition.effector.operationDelay = definition.effector.placerDelay;
                definition.effector.placerDelay = 0;
            }

            //Convert old effector hitboxes.
            if (definition.collisionGroups != null) {
                for (JSONCollisionGroup collisionGroup : definition.collisionGroups) {
                    if (collisionGroup.collisionTypes == null) {
                        collisionGroup.collisionTypes = new HashSet<>();
                        collisionGroup.collisionTypes.add(CollisionType.EFFECTOR);
                    }
                }
            }
        }

        if (definition.parts != null) {
            for (JSONPartDefinition subPartDef : definition.parts) {
                try {
                    performVehiclePartDefLegacyCompats(subPartDef);
                } catch (Exception e) {
                    throw new NullPointerException("Could not perform Legacy Compats on sub-part entry #" + (definition.parts.indexOf(subPartDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
                }
            }
        }
        if (definition.parts != null) {
            performPartSlotListingLegacyCompats(definition.parts, false, false);
        }

        if (definition.rendering != null) {
            try {
                performAnimationLegacyCompats(definition.rendering);
            } catch (Exception e) {
                throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
            }
        }

        performVehicleConnectionLegacyCompats(definition);
        performVehicleCollisionLegacyCompats(definition);

        //Do compats for wheel rotation and mirroring.
        if (definition.ground != null && definition.ground.isWheel) {
            if (definition.generic.movementAnimations != null) {
                ///Check for old isMirroed LCed animations.  These will be wrong.
                boolean oldAnimationsDetected = false;
                for (JSONAnimationDefinition animation : definition.generic.movementAnimations) {
                    if (animation.variable.equals("part_ismirrored")) {
                        oldAnimationsDetected = true;
                        break;
                    }
                }
                if (oldAnimationsDetected) {
                    definition.generic.movementAnimations = null;
                }
            }

            if (definition.generic.movementAnimations == null) {
                definition.generic.movementAnimations = new ArrayList<>();

                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation = new JSONAnimationDefinition();
                animation.centerPoint = new Point3D(0, 0, 0);
                animation.axis = new Point3D(1, 0, 0);
                animation.animationType = AnimationComponentType.ROTATION;
                animation.variable = "ground_rotation";
                definition.generic.movementAnimations.add(animation);
            }
        }

        //Do compats for propeller rotation.
        if (definition.propeller != null) {
            if (definition.generic.movementAnimations == null) {
                definition.generic.movementAnimations = new ArrayList<>();

                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.centerPoint = new Point3D(0, 0, 0);
                animation.axis = new Point3D(0, 0, 1);
                animation.animationType = AnimationComponentType.ROTATION;
                animation.variable = "propeller_rotation";
                definition.generic.movementAnimations.add(animation);
            }
        }

        //Do compats for engine and gun sounds.
        if (definition.rendering == null || definition.rendering.sounds == null) {
            if (definition.engine != null) {
                if (definition.rendering == null) {
                    definition.rendering = new JSONRendering();
                }
                if (definition.rendering.sounds == null) {
                    definition.rendering.sounds = new ArrayList<>();
                }

                //Starting sound plays when engine goes from stopped to running.
                JSONSound startingSound = new JSONSound();
                startingSound.name = definition.packID + ":" + definition.systemName + "_starting";
                startingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition startingActiveDef = new JSONAnimationDefinition();
                startingActiveDef.animationType = AnimationComponentType.VISIBILITY;
                startingActiveDef.variable = "engine_running";
                startingActiveDef.clampMin = 1.0F;
                startingActiveDef.clampMax = 1.0F;
                startingSound.activeAnimations.add(startingActiveDef);
                definition.rendering.sounds.add(startingSound);

                //Stopping sound plays when engine goes from running to stopped.
                JSONSound stoppingSound = new JSONSound();
                stoppingSound.name = definition.packID + ":" + definition.systemName + "_stopping";
                stoppingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition stoppingActiveDef = new JSONAnimationDefinition();
                stoppingActiveDef.animationType = AnimationComponentType.VISIBILITY;
                stoppingActiveDef.variable = "engine_running";
                stoppingActiveDef.clampMin = 0.0F;
                stoppingActiveDef.clampMax = 0.0F;
                stoppingSound.activeAnimations.add(stoppingActiveDef);
                definition.rendering.sounds.add(stoppingSound);

                //Sputtering sound plays when engine backfires.
                JSONSound sputteringSound = new JSONSound();
                sputteringSound.name = definition.packID + ":" + definition.systemName + "_sputter";
                sputteringSound.forceSound = true;
                sputteringSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition sputteringActiveDef = new JSONAnimationDefinition();
                sputteringActiveDef.animationType = AnimationComponentType.VISIBILITY;
                sputteringActiveDef.variable = "engine_backfired";
                sputteringActiveDef.clampMin = 1.0F;
                sputteringActiveDef.clampMax = 1.0F;
                sputteringSound.activeAnimations.add(sputteringActiveDef);
                definition.rendering.sounds.add(sputteringSound);

                //Griding sound plays when engine has a bad shift.
                JSONSound grindingSound = new JSONSound();
                grindingSound.name = InterfaceManager.coreModID + ":engine_shifting_grinding";
                grindingSound.forceSound = true;
                grindingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition grindingActiveDef = new JSONAnimationDefinition();
                grindingActiveDef.animationType = AnimationComponentType.VISIBILITY;
                grindingActiveDef.variable = "engine_badshift";
                grindingActiveDef.clampMin = 1.0F;
                grindingActiveDef.clampMax = 1.0F;
                grindingSound.activeAnimations.add(grindingActiveDef);
                definition.rendering.sounds.add(grindingSound);

                //Cranking sound plays when engine starters are engaged.  May be pitch-shifted depending on state.
                JSONSound crankingSound = new JSONSound();
                crankingSound.name = definition.packID + ":" + definition.systemName + "_cranking";
                crankingSound.looping = true;
                crankingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition crankingActiveDef = new JSONAnimationDefinition();
                crankingActiveDef.animationType = AnimationComponentType.VISIBILITY;
                crankingActiveDef.variable = "engine_starter";
                crankingActiveDef.clampMin = 1.0F;
                crankingActiveDef.clampMax = 1.0F;
                crankingSound.activeAnimations.add(crankingActiveDef);

                crankingSound.pitchAnimations = new ArrayList<>();
                JSONAnimationDefinition crankingPitchDef = new JSONAnimationDefinition();
                if (!definition.engine.isCrankingNotPitched) {
                    crankingPitchDef.animationType = AnimationComponentType.TRANSLATION;
                    crankingPitchDef.variable = "electric_power";
                    crankingPitchDef.axis = new Point3D(0, 1D / 10D, 0);
                    crankingPitchDef.offset = 0.3F;
                    crankingPitchDef.clampMax = 1.0F;
                    definition.engine.isCrankingNotPitched = false;
                } else {
                    crankingPitchDef = new JSONAnimationDefinition();
                    crankingPitchDef.animationType = AnimationComponentType.TRANSLATION;
                    crankingPitchDef.variable = "engine_rpm";
                    crankingPitchDef.axis = new Point3D(0, 1D / (definition.engine.maxRPM < 15000 ? 500D : 2000D), 0);
                }
                crankingSound.pitchAnimations.add(crankingPitchDef);
                definition.rendering.sounds.add(crankingSound);

                //Running sound plays when engine is running, and pitch-shifts to match engine speed.
                if (definition.engine.customSoundset != null) {
                    for (minecrafttransportsimulator.jsondefs.JSONPart.JSONPartEngine.EngineSound customSound : definition.engine.customSoundset) {
                        JSONSound runningSound = new JSONSound();
                        runningSound.name = customSound.soundName;
                        runningSound.looping = true;
                        runningSound.activeAnimations = new ArrayList<>();
                        runningSound.volumeAnimations = new ArrayList<>();
                        runningSound.pitchAnimations = new ArrayList<>();

                        //Add default sound on/off variable.
                        JSONAnimationDefinition runningStartDef = new JSONAnimationDefinition();
                        runningStartDef.animationType = AnimationComponentType.VISIBILITY;
                        runningStartDef.variable = "engine_powered";
                        runningStartDef.clampMin = 1.0F;
                        runningStartDef.clampMax = 1.0F;
                        runningSound.activeAnimations.add(runningStartDef);

                        //Add volume variable.
                        JSONAnimationDefinition runningVolumeDef = new JSONAnimationDefinition();
                        if (customSound.volumeAdvanced) {
                            runningVolumeDef.animationType = AnimationComponentType.ROTATION;
                            runningVolumeDef.variable = "engine_rpm";
                            runningVolumeDef.centerPoint = new Point3D();
                            runningVolumeDef.axis = new Point3D(-0.000001 / (customSound.volumeLength / 1000), 0, customSound.volumeCenter);
                            runningVolumeDef.offset = (customSound.volumeLength / 20000) + 1;
                        } else {
                            runningVolumeDef.animationType = AnimationComponentType.TRANSLATION;
                            runningVolumeDef.variable = "engine_rpm_percent";
                            runningVolumeDef.axis = new Point3D(0, (customSound.volumeMax - customSound.volumeIdle), 0);
                            runningVolumeDef.offset = customSound.volumeIdle + (definition.engine.maxRPM < 15000 ? 500 : 2000) / definition.engine.maxRPM;
                        }
                        runningSound.volumeAnimations.add(runningVolumeDef);

                        //Add pitch variable.
                        JSONAnimationDefinition runningPitchDef = new JSONAnimationDefinition();
                        if (customSound.pitchAdvanced) {
                            runningPitchDef.animationType = AnimationComponentType.ROTATION;
                            runningPitchDef.variable = "engine_rpm";
                            runningPitchDef.centerPoint = new Point3D();
                            runningPitchDef.axis = new Point3D(-0.000001 / (customSound.pitchLength / 1000), 0, customSound.pitchCenter);
                            runningPitchDef.offset = (customSound.pitchLength / 20000) + 1;
                        } else {
                            runningPitchDef.animationType = AnimationComponentType.TRANSLATION;
                            runningPitchDef.variable = "engine_rpm_percent";
                            runningPitchDef.axis = new Point3D(0, (customSound.pitchMax - customSound.pitchIdle), 0);
                            runningPitchDef.offset = customSound.pitchIdle + (definition.engine.maxRPM < 15000 ? 500 : 2000) / definition.engine.maxRPM;
                        }
                        runningSound.pitchAnimations.add(runningPitchDef);

                        //Add the sound.
                        definition.rendering.sounds.add(runningSound);
                    }
                    definition.engine.customSoundset = null;
                } else {
                    JSONSound runningSound = new JSONSound();
                    runningSound.name = definition.packID + ":" + definition.systemName + "_running";
                    runningSound.looping = true;
                    runningSound.activeAnimations = new ArrayList<>();
                    JSONAnimationDefinition runningVolumeDef = new JSONAnimationDefinition();
                    runningVolumeDef.animationType = AnimationComponentType.VISIBILITY;
                    runningVolumeDef.variable = "engine_powered";
                    runningVolumeDef.clampMin = 1.0F;
                    runningVolumeDef.clampMax = 1.0F;
                    runningSound.activeAnimations.add(runningVolumeDef);

                    runningSound.pitchAnimations = new ArrayList<>();
                    JSONAnimationDefinition runningPitchDef = new JSONAnimationDefinition();
                    runningPitchDef.animationType = AnimationComponentType.TRANSLATION;
                    runningPitchDef.variable = "engine_rpm";
                    //Pitch should be 0.35 at idle, with a 0.35 increase for every 2500 RPM, or every 25000 RPM for jet (high-revving) engines by default.
                    runningPitchDef.axis = new Point3D(0, 0.35 / (definition.engine.maxRPM < 15000 ? 500 : 5000), 0);
                    runningPitchDef.offset = 0.35F;
                    runningSound.pitchAnimations.add(runningPitchDef);
                    definition.rendering.sounds.add(runningSound);
                }

            } else if (definition.gun != null) {
                if (definition.rendering == null) {
                    definition.rendering = new JSONRendering();
                }
                if (definition.rendering.sounds == null) {
                    definition.rendering.sounds = new ArrayList<>();
                }

                JSONSound firingSound = new JSONSound();
                firingSound.name = definition.packID + ":" + definition.systemName + "_firing";
                firingSound.forceSound = true;
                firingSound.canPlayOnPartialTicks = definition.gun.fireDelay < 2;
                firingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition firingDef = new JSONAnimationDefinition();
                firingDef.animationType = AnimationComponentType.VISIBILITY;
                firingDef.variable = "gun_fired";
                firingDef.clampMin = 1.0F;
                firingDef.clampMax = 1.0F;
                firingSound.activeAnimations.add(firingDef);
                definition.rendering.sounds.add(firingSound);

                JSONSound reloadingSound = new JSONSound();
                reloadingSound.name = definition.packID + ":" + definition.systemName + "_reloading";
                reloadingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition reloadingDef = new JSONAnimationDefinition();
                reloadingDef.animationType = AnimationComponentType.VISIBILITY;
                reloadingDef.variable = "gun_reload";
                reloadingDef.clampMin = 1.0F;
                reloadingDef.clampMax = 1.0F;
                reloadingSound.activeAnimations.add(reloadingDef);
                definition.rendering.sounds.add(reloadingSound);
            } else if (definition.ground != null) {
                if (definition.rendering == null) {
                    definition.rendering = new JSONRendering();
                }
                if (definition.rendering.sounds == null) {
                    definition.rendering.sounds = new ArrayList<>();
                }

                JSONSound blowoutSound = new JSONSound();
                blowoutSound.name = InterfaceManager.coreModID + ":wheel_blowout";
                blowoutSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition blowoutDef = new JSONAnimationDefinition();
                blowoutDef.animationType = AnimationComponentType.VISIBILITY;
                blowoutDef.variable = "ground_isflat";
                blowoutDef.clampMin = 1.0F;
                blowoutDef.clampMax = 1.0F;
                blowoutSound.activeAnimations.add(blowoutDef);
                definition.rendering.sounds.add(blowoutSound);

                JSONSound strikingSound = new JSONSound();
                strikingSound.name = InterfaceManager.coreModID + ":" + "wheel_striking";
                strikingSound.forceSound = true;
                strikingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition strikingDef = new JSONAnimationDefinition();
                strikingDef.animationType = AnimationComponentType.VISIBILITY;
                strikingDef.variable = "ground_contacted";
                strikingDef.clampMin = 1.0F;
                strikingDef.clampMax = 1.0F;
                strikingSound.activeAnimations.add(strikingDef);
                definition.rendering.sounds.add(strikingSound);

                JSONSound skiddingSound = new JSONSound();
                skiddingSound.name = InterfaceManager.coreModID + ":" + "wheel_skidding";
                skiddingSound.looping = true;
                skiddingSound.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition skiddingDef = new JSONAnimationDefinition();
                skiddingDef.animationType = AnimationComponentType.VISIBILITY;
                skiddingDef.variable = "ground_slipping";
                skiddingDef.clampMin = 1.0F;
                skiddingDef.clampMax = 1.0F;
                skiddingSound.activeAnimations.add(skiddingDef);
                definition.rendering.sounds.add(skiddingSound);
            }
        }

        //Do compats for particles.
        if (definition.rendering != null && definition.rendering.particles == null) {
            definition.rendering.particles = new ArrayList<>();
            if (definition.engine != null) {
                //Small overheat.
                JSONParticle particleDef = new JSONParticle();
                particleDef.type = ParticleType.SMOKE;
                particleDef.color = ColorRGB.BLACK;
                particleDef.spawnEveryTick = true;
                particleDef.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "engine_temp";
                activeAnimation.clampMin = PartEngine.OVERHEAT_TEMP_1;
                activeAnimation.clampMax = 999F;
                particleDef.activeAnimations.add(activeAnimation);
                definition.rendering.particles.add(particleDef);

                //Large overheat.
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.SMOKE;
                particleDef.color = ColorRGB.BLACK;
                particleDef.spawnEveryTick = true;
                particleDef.quantity = 1;
                particleDef.scale = 2.5F;
                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "engine_temp";
                activeAnimation.clampMin = PartEngine.OVERHEAT_TEMP_2;
                activeAnimation.clampMax = 999F;
                particleDef.activeAnimations.add(activeAnimation);
                definition.rendering.particles.add(particleDef);

                //Oil drip.
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.DRIP;
                particleDef.color = ColorRGB.BLACK;
                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "engine_oilleak";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "cycle_10_20";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);
                definition.rendering.particles.add(particleDef);

                //Fuel drip.
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.DRIP;
                particleDef.color = ColorRGB.RED;
                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "engine_fuelleak";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "cycle_10_20";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);
                definition.rendering.particles.add(particleDef);
            } else if (definition.ground != null) {
                //Contact smoke.
                JSONParticle particleDef = new JSONParticle();
                particleDef.type = ParticleType.SMOKE;
                particleDef.color = ColorRGB.WHITE;
                particleDef.quantity = 4;
                particleDef.initialVelocity = new Point3D(0, 0.15, 0);

                particleDef.activeAnimations = new ArrayList<>();
                JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "ground_contacted";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);

                particleDef.spawningAnimations = new ArrayList<>();
                JSONAnimationDefinition spawningAnimation = new JSONAnimationDefinition();
                spawningAnimation.animationType = AnimationComponentType.ROTATION;
                spawningAnimation.variable = "ground_rotation";
                spawningAnimation.centerPoint = new Point3D();
                spawningAnimation.axis = new Point3D(-1, 0, 0);
                particleDef.spawningAnimations.add(spawningAnimation);

                definition.rendering.particles.add(particleDef);

                //Burnout smoke.
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.SMOKE;
                particleDef.color = ColorRGB.WHITE;
                particleDef.quantity = 4;
                particleDef.spawnEveryTick = true;
                particleDef.pos = new Point3D(0, -definition.ground.height / 2, 0);
                particleDef.initialVelocity = new Point3D(0, 0.15, 0);

                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "ground_slipping";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);

                particleDef.spawningAnimations = new ArrayList<>();
                spawningAnimation = new JSONAnimationDefinition();
                spawningAnimation.animationType = AnimationComponentType.ROTATION;
                spawningAnimation.variable = "ground_rotation";
                spawningAnimation.centerPoint = new Point3D();
                spawningAnimation.axis = new Point3D(-1, 0, 0);
                particleDef.spawningAnimations.add(spawningAnimation);

                definition.rendering.particles.add(particleDef);

                //Wheel dirt for skidding (burnouts).
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.BREAK;
                particleDef.color = new ColorRGB("999999");
                particleDef.quantity = 4;
                particleDef.scale = 0.3F;
                particleDef.spawnEveryTick = true;
                particleDef.pos = new Point3D(0, -definition.ground.height / 2, 0);
                particleDef.initialVelocity = new Point3D(0, 1.5, -1.5);

                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "ground_skidding";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);

                particleDef.spawningAnimations = new ArrayList<>();
                spawningAnimation = new JSONAnimationDefinition();
                spawningAnimation.animationType = AnimationComponentType.ROTATION;
                spawningAnimation.variable = "ground_rotation";
                spawningAnimation.centerPoint = new Point3D();
                spawningAnimation.axis = new Point3D(-1, 0, 0);
                particleDef.spawningAnimations.add(spawningAnimation);

                definition.rendering.particles.add(particleDef);

                //Wheel dirt for slipping (turning too fast).
                particleDef = new JSONParticle();
                particleDef.type = ParticleType.BREAK;
                particleDef.color = new ColorRGB("999999");
                particleDef.quantity = 4;
                particleDef.scale = 0.3F;
                particleDef.spawnEveryTick = true;
                particleDef.pos = new Point3D(0, -definition.ground.height / 2, 0);
                particleDef.initialVelocity = new Point3D(0, 1.5, 0.0);

                particleDef.activeAnimations = new ArrayList<>();
                activeAnimation = new JSONAnimationDefinition();
                activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                activeAnimation.variable = "ground_slipping";
                activeAnimation.clampMin = 1.0F;
                activeAnimation.clampMax = 1.0F;
                particleDef.activeAnimations.add(activeAnimation);

                particleDef.spawningAnimations = new ArrayList<>();
                spawningAnimation = new JSONAnimationDefinition();
                spawningAnimation.animationType = AnimationComponentType.ROTATION;
                spawningAnimation.variable = "ground_rotation";
                spawningAnimation.centerPoint = new Point3D();
                spawningAnimation.axis = new Point3D(-1, 0, 0);
                particleDef.spawningAnimations.add(spawningAnimation);

                definition.rendering.particles.add(particleDef);
            }
        }
    }

    private static void performInstrumentLegacyCompats(JSONInstrument definition) {
        //Check if we have any old component definitions.  If so, we need
        //to make all textures light-up.
        boolean oldDefinition = false;
        for (JSONInstrument.JSONInstrumentComponent component : definition.components) {
            if (component.rotationVariable != null || component.translationVariable != null) {
                oldDefinition = true;
                break;
            }
        }
        //Convert any old component definitions to the new style.
        for (JSONInstrument.JSONInstrumentComponent component : definition.components) {
            if (oldDefinition) {
                component.lightUpTexture = true;
                component.overlayTexture = component.lightOverlay;
                component.lightOverlay = false;
            }
            if (component.rotationVariable != null) {
                component.animations = new ArrayList<>();
                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.animationType = AnimationComponentType.ROTATION;
                animation.variable = component.rotationVariable;
                animation.centerPoint = new Point3D();
                animation.axis = new Point3D(0, 0, component.rotationFactor);
                animation.offset = component.rotationOffset;
                animation.clampMin = component.rotationClampMin;
                animation.clampMax = component.rotationClampMax;
                animation.absolute = component.rotationAbsoluteValue;
                component.animations.add(animation);
                component.rotationVariable = null;
                component.rotationFactor = 0;
                component.rotationOffset = 0;
                component.rotationClampMin = 0;
                component.rotationClampMax = 0;
                component.rotationAbsoluteValue = false;
            }
            if (component.translationVariable != null) {
                if (component.animations == null) {
                    component.animations = new ArrayList<>();
                }
                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.animationType = AnimationComponentType.TRANSLATION;
                animation.variable = component.translationVariable;
                if (component.translateHorizontal) {
                    animation.axis = new Point3D(component.translationFactor, 0, 0);
                } else {
                    animation.axis = new Point3D(0, component.translationFactor, 0);
                }
                animation.clampMin = component.translationClampMin;
                animation.clampMax = component.translationClampMax;
                animation.absolute = component.translationAbsoluteValue;
                //If we were rotating the texture, and not the window, we need to do the translation first.
                //This is due to how the old animation system did rendering.
                if (component.rotateWindow) {
                    component.animations.add(animation);
                } else {
                    component.animations.add(0, animation);
                }
                component.translateHorizontal = false;
                component.translationVariable = null;
                component.translationFactor = 0;
                component.translationClampMin = 0;
                component.translationClampMax = 0;
                component.translationAbsoluteValue = false;
            }
            if (component.scale == 0.0) {
                component.scale = 1.0F;
            }
            //Move fieldName to variableName for consistency.
            if (component.textObject != null) {
                if (component.textObject.variableName == null) {
                    component.textObject.variableName = component.textObject.fieldName;
                    component.textObject.variableFormat = "%0" + component.textObject.maxLength + ".0f";
                    component.textObject.variableFactor = 1.0F;
                    component.textObject.fieldName = null;
                    component.textObject.maxLength = 0;
                }
            }
        }

        //Check if we are missing a texture name.
        if (definition.textureName == null) {
            definition.textureName = "instruments.png";
        }
    }

    private static void performPoleLegacyCompats(JSONPoleComponent definition) {
        //If we are a sign using the old textlines, update them.
        if (definition.general.textLines != null) {
            definition.general.textObjects = new ArrayList<>();
            for (minecrafttransportsimulator.jsondefs.AJSONItem.General.TextLine line : definition.general.textLines) {
                JSONText object = new JSONText();
                object.color = line.color;
                object.scale = line.scale;
                object.maxLength = line.characters;
                object.pos = new Point3D(line.xPos, line.yPos, line.zPos + 0.01D);
                object.fieldName = "TextLine #" + (definition.general.textObjects.size() + 1);
                definition.general.textObjects.add(object);
            }
            definition.general.textLines = null;
        }

        //If we are a sign using the old textObjects location, move it.
        if (definition.general.textObjects != null) {
            if (definition.rendering == null) {
                definition.rendering = new JSONRendering();
            }
            definition.rendering.textObjects = definition.general.textObjects;
            definition.general.textObjects = null;
        }

        //Set default text to blank for sign text objects.
        if (definition.rendering != null && definition.rendering.textObjects != null) {
            for (JSONText text : definition.rendering.textObjects) {
                if (text.defaultText == null) {
                    text.defaultText = "";
                }
            }
        }

        //Move pole general properties to new location.
        if (definition.general.type != null) {
            definition.pole = new JSONPoleComponent.JSONPoleGeneric();
            definition.pole.type = PoleComponentType.valueOf(definition.general.type.toUpperCase(Locale.ROOT));
            definition.general.type = null;
            definition.pole.radius = definition.general.radius;
            definition.general.radius = 0;
        }

        //Create a animation set for core poles if they don't have one and use the old auto-render systems.
        if (definition.pole.type.equals(PoleComponentType.CORE) && definition.rendering.animatedObjects == null) {
            definition.rendering.animatedObjects = new ArrayList<>();
            for (Axis axis : Axis.values()) {
                if (!axis.equals(Axis.NONE)) {
                    JSONAnimatedObject connectorModelObject = new JSONAnimatedObject();
                    connectorModelObject.objectName = axis.name().toLowerCase(Locale.ROOT);
                    connectorModelObject.animations = new ArrayList<>();
                    JSONAnimationDefinition connectorVisibilityInhibitor = new JSONAnimationDefinition();

                    connectorVisibilityInhibitor.animationType = AnimationComponentType.INHIBITOR;
                    connectorVisibilityInhibitor.variable = "solid_present_" + axis.name().toLowerCase(Locale.ROOT);
                    connectorVisibilityInhibitor.clampMin = 1.0F;
                    connectorVisibilityInhibitor.clampMax = 1.0F;
                    connectorModelObject.animations.add(connectorVisibilityInhibitor);

                    JSONAnimationDefinition connectorVisibility = new JSONAnimationDefinition();
                    connectorVisibility.animationType = AnimationComponentType.VISIBILITY;
                    connectorVisibility.variable = "neighbor_present_" + axis.name().toLowerCase(Locale.ROOT);
                    connectorVisibility.clampMin = 1.0F;
                    connectorVisibility.clampMax = 1.0F;
                    connectorModelObject.animations.add(connectorVisibility);
                    definition.rendering.animatedObjects.add(connectorModelObject);

                    JSONAnimatedObject solidModelObject = new JSONAnimatedObject();
                    solidModelObject.objectName = axis.name().toLowerCase(Locale.ROOT) + "_solid";
                    solidModelObject.animations = new ArrayList<>();
                    JSONAnimationDefinition solidVisibility = new JSONAnimationDefinition();
                    solidVisibility.animationType = AnimationComponentType.VISIBILITY;
                    solidVisibility.variable = "solid_present_" + axis.name().toLowerCase(Locale.ROOT);
                    solidVisibility.clampMin = 1.0F;
                    solidVisibility.clampMax = 1.0F;
                    solidModelObject.animations.add(solidVisibility);
                    definition.rendering.animatedObjects.add(solidModelObject);

                    if (axis.equals(Axis.UP) || axis.equals(Axis.DOWN)) {
                        JSONAnimatedObject slabModelObject = new JSONAnimatedObject();
                        slabModelObject.objectName = axis.name().toLowerCase(Locale.ROOT) + "_slab";
                        slabModelObject.animations = new ArrayList<>();
                        JSONAnimationDefinition slabVisibility = new JSONAnimationDefinition();
                        slabVisibility.animationType = AnimationComponentType.VISIBILITY;
                        slabVisibility.variable = "slab_present_" + axis.name().toLowerCase(Locale.ROOT);
                        slabVisibility.clampMin = 1.0F;
                        slabVisibility.clampMax = 1.0F;
                        slabModelObject.animations.add(slabVisibility);
                        definition.rendering.animatedObjects.add(slabModelObject);
                    }
                }
            }
        }
    }

    private static void performDecorLegacyCompats(JSONDecor definition) {
        //Move decor general properties to new location.
        if (definition.decor == null) {
            definition.decor = new JSONDecor.JSONDecorGeneric();
            if (definition.general.type != null) {
                definition.decor.type = DecorComponentType.valueOf(definition.general.type.toUpperCase(Locale.ROOT));
                definition.general.type = null;
            }
            definition.decor.width = definition.general.width;
            definition.general.width = 0;
            definition.decor.height = definition.general.height;
            definition.general.height = 0;
            definition.decor.depth = definition.general.depth;
            definition.general.depth = 0;
            definition.decor.itemTypes = definition.general.itemTypes;
            definition.general.itemTypes = null;
            definition.decor.partTypes = definition.general.partTypes;
            definition.general.partTypes = null;
            definition.decor.items = definition.general.items;
            definition.general.items = null;
        }

        //If we are a decor without a type, set us to generic.
        if (definition.decor.type == null) {
            definition.decor.type = DecorComponentType.GENERIC;
        }

        //If we are a decor with a type of item loader, then set defaults for invalid/missing values.
        if (definition.decor.type.equals(DecorComponentType.ITEM_LOADER) || definition.decor.type.equals(DecorComponentType.ITEM_UNLOADER)) {
            if (definition.decor.inventoryUnits == 0) {
                definition.decor.inventoryUnits = 1F / 9F;
            }
        }

        //If we are a decor with a type of fuel pump or fluid loader, then set defaults for invalid/missing values.
        if (definition.decor.type.equals(DecorComponentType.FUEL_PUMP) || definition.decor.type.equals(DecorComponentType.FLUID_LOADER) || definition.decor.type.equals(DecorComponentType.FLUID_UNLOADER)) {
            if (definition.decor.fuelCapacity == 0) {
                definition.decor.fuelCapacity = 15000;
            }

            if (definition.decor.pumpRate == 0) {
                definition.decor.pumpRate = 10;
            }
        }

        //If we are a decor using the old textlines, update them.
        if (definition.general.textLines != null) {
            definition.general.textObjects = new ArrayList<>();
            int lineNumber = 0;
            for (minecrafttransportsimulator.jsondefs.AJSONItem.General.TextLine line : definition.general.textLines) {
                JSONText object = new JSONText();
                object.lightsUp = true;
                object.color = line.color;
                object.scale = line.scale;
                if (lineNumber++ < 3) {
                    object.pos = new Point3D(line.xPos, line.yPos, line.zPos + 0.0001D);
                } else {
                    object.pos = new Point3D(line.xPos, line.yPos, line.zPos - 0.0001D);
                    object.rot = new RotationMatrix();
                    object.rot.setToAxisAngle(0, 1, 0, 180);
                }
                object.fieldName = "TextLine #" + (definition.general.textObjects.size() + 1);
                definition.general.textObjects.add(object);
            }
            definition.general.textLines = null;
        }

        //If we are a sign using the old textObjects location, move it.
        if (definition.general.textObjects != null) {
            if (definition.rendering == null) {
                definition.rendering = new JSONRendering();
            }
            definition.rendering.textObjects = definition.general.textObjects;
            definition.general.textObjects = null;
        }

        //Set default text to blank for decor text objects.
        if (definition.rendering != null && definition.rendering.textObjects != null) {
            for (JSONText text : definition.rendering.textObjects) {
                if (text.defaultText == null) {
                    text.defaultText = "";
                }
            }
        }

        //If we have crafting things in the decor, move them.
        if (definition.decor.items != null || definition.decor.itemTypes != null) {
            definition.decor.crafting = new JSONCraftingBench();
            definition.decor.crafting.itemTypes = definition.decor.itemTypes;
            definition.decor.itemTypes = null;
            definition.decor.crafting.partTypes = definition.decor.partTypes;
            definition.decor.partTypes = null;
            definition.decor.crafting.items = definition.decor.items;
            definition.decor.items = null;
        }

        //If we are a fuel pump with auto-generated text, set the rest of the properties.
        if (definition.decor.type.equals(DecorComponentType.FUEL_PUMP)) {
            if (definition.rendering != null && definition.rendering.textObjects != null) {
                for (int i = 0; i < definition.rendering.textObjects.size(); ++i) {
                    JSONText textDef = definition.rendering.textObjects.get(i);
                    if (textDef.variableName == null) {
                        switch (i % 3) {
                            case (0): {
                                textDef.variableName = "fuelpump_fluid";
                                textDef.variableFormat = "%s";
                                break;
                            }
                            case (1): {
                                textDef.variableName = "fuelpump_stored";
                                textDef.variableFactor = 0.001F;
                                textDef.variableFormat = "LVL:%04.1fb";
                                break;
                            }
                            case (2): {
                                textDef.variableName = "fuelpump_dispensed";
                                textDef.variableFactor = 0.001F;
                                textDef.variableFormat = "DISP:%04.1fb";
                                break;
                            }
                        }
                    }
                }
            }
        }

        //If we are a beacon with no text, add it as it's required.
        if (definition.decor.type.equals(DecorComponentType.BEACON)) {
            if (definition.rendering == null) {
                definition.rendering = new JSONRendering();
            }
            if (definition.rendering.textObjects == null) {
                definition.rendering.textObjects = new ArrayList<>();

                JSONText nameTextObject = new JSONText();
                nameTextObject.pos = new Point3D(0, -500, 0);
                nameTextObject.fieldName = TileEntityBeacon.BEACON_NAME_TEXT_KEY;
                nameTextObject.defaultText = "NONE";
                nameTextObject.maxLength = 5;
                nameTextObject.color = ColorRGB.WHITE;
                definition.rendering.textObjects.add(nameTextObject);

                JSONText glideslopeTextObject = new JSONText();
                glideslopeTextObject.pos = new Point3D(0, -500, 0);
                glideslopeTextObject.fieldName = TileEntityBeacon.BEACON_GLIDESLOPE_TEXT_KEY;
                glideslopeTextObject.defaultText = "10.0";
                glideslopeTextObject.maxLength = 5;
                glideslopeTextObject.color = ColorRGB.WHITE;
                definition.rendering.textObjects.add(glideslopeTextObject);

                JSONText bearingTextObject = new JSONText();
                bearingTextObject.pos = new Point3D(0, -500, 0);
                bearingTextObject.fieldName = TileEntityBeacon.BEACON_BEARING_TEXT_KEY;
                bearingTextObject.defaultText = "0.0";
                bearingTextObject.maxLength = 5;
                bearingTextObject.color = ColorRGB.WHITE;
                definition.rendering.textObjects.add(bearingTextObject);
            }
        }
    }

    private static void performRoadLegacyCompats(JSONRoadComponent definition) {
        //Make road segment length 1 if it's 0 and we are a dynamic road.
        if (definition.road.type.equals(RoadComponent.CORE_DYNAMIC) && definition.road.segmentLength == 0) {
            definition.road.segmentLength = 1.0F;
        }
    }

    private static void performItemLegacyCompats(JSONItem definition) {
        //Move item type if required.
        if (definition.item == null) {
            definition.item = new JSONItem.JSONItemGeneric();
            if (definition.general.type != null) {
                definition.item.type = ItemComponentType.valueOf(definition.general.type.toUpperCase(Locale.ROOT));
                definition.general.type = null;
            }
        }

        //Set item type to NONE if null.
        if (definition.item.type == null) {
            definition.item.type = ItemComponentType.NONE;
        }

        //Add blank fieldNames for booklets, as they shouldn't exist.
        if (definition.booklet != null) {
            for (JSONText text : definition.booklet.titleText) {
                text.fieldName = "";
            }
            for (BookletPage page : definition.booklet.pages) {
                for (JSONText text : page.pageText) {
                    text.fieldName = "";
                }
            }
        }
    }

    private static void performSkinLegacyCompats(JSONSkin definition) {
        //Move skin properties to new location, if we have them.
        if (definition.general.packID != null) {
            definition.skin = new JSONSkin.Skin();
            definition.skin.packID = definition.general.packID;
            definition.general.packID = null;
            definition.skin.systemName = definition.general.systemName;
            definition.general.systemName = null;
        }
    }

    private static void performBulletLegacyCompats(JSONBullet definition) {
        //If we are a bullet without a definition, add one so we don't crash on other systems.
        if (definition.definitions == null) {
            definition.definitions = new ArrayList<>();
            JSONSubDefinition subDefinition = new JSONSubDefinition();
            subDefinition.extraMaterials = new ArrayList<>();
            subDefinition.name = definition.general.name;
            subDefinition.subName = "";
            definition.definitions.add(subDefinition);
        }

        //Add damage value.
        if (definition.bullet.damage == 0) {
            definition.bullet.damage = definition.bullet.diameter / 5F;
        }
        //Make guided bullets default to active guidance
        if (definition.bullet.guidanceType == null && definition.bullet.turnRate > 0) {
            definition.bullet.guidanceType = JSONBullet.GuidanceType.ACTIVE;
        }
        if (definition.bullet.seekerMaxAngle == 0) {
            definition.bullet.seekerMaxAngle = 90;
        }
        if (definition.bullet.seekerRange == 0) {
            definition.bullet.seekerRange = 1024;
        }

        //Make rendering particle section for bullets for block hitting if it doesn't exist.
        if (definition.rendering == null || definition.rendering.particles == null) {
            if (definition.rendering == null) {
                definition.rendering = new JSONRendering();
            }
            if (definition.rendering.particles == null) {
                definition.rendering.particles = new ArrayList<>();
            }

            //Block hit particle.
            JSONParticle particleDef = new JSONParticle();
            particleDef.type = ParticleType.BREAK;
            particleDef.color = new ColorRGB("999999");
            particleDef.quantity = 4;
            particleDef.scale = 0.3F;
            particleDef.pos = new Point3D(0, 0.5, 0);
            particleDef.initialVelocity = new Point3D(0, 1.5, 0);
            particleDef.activeAnimations = new ArrayList<>();
            JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
            activeAnimation.animationType = AnimationComponentType.VISIBILITY;
            activeAnimation.variable = "bullet_hit_block";
            activeAnimation.clampMin = 1.0F;
            activeAnimation.clampMax = 1.0F;
            particleDef.activeAnimations.add(activeAnimation);
            definition.rendering.particles.add(particleDef);
        }
    }

    private static void performParticleLegacyCompats(JSONParticle particleDef) {
        if (particleDef.spreadFactorVertical != 0 || particleDef.spreadFactorHorizontal != 0) {
            particleDef.spreadRandomness = new Point3D(particleDef.initialVelocity.x * particleDef.spreadFactorHorizontal, particleDef.spreadFactorVertical / 2D, particleDef.initialVelocity.z * particleDef.spreadFactorHorizontal);
            particleDef.spreadFactorVertical = 0;
            particleDef.spreadFactorHorizontal = 0;
        }
        if (particleDef.renderingOrientation == null) {
            particleDef.renderingOrientation = particleDef.axisAligned ? ParticleRenderingOrientation.FIXED : ParticleRenderingOrientation.PLAYER;
        }
        if (particleDef.spawningOrientation == null) {
            particleDef.spawningOrientation = ParticleSpawningOrientation.ENTITY;
        }
        if (particleDef.subParticles != null) {
            for (JSONSubParticle subParticleDef : particleDef.subParticles) {
                performParticleLegacyCompats(subParticleDef.particle);
            }
        }
        if (particleDef.hitboxSize == 0) {
            particleDef.hitboxSize = particleDef.type == ParticleType.BREAK ? 0.1F : 0.2F;
            particleDef.scale *= particleDef.hitboxSize;
            particleDef.toScale *= particleDef.hitboxSize;
        }
    }

    private static void performPartSlotListingLegacyCompats(List<JSONPartDefinition> partDefs, boolean linkFrontWheels, boolean linkRearWheels) {
        //First move all additional parts into regular part slots.
        //We will need to apply LCs to block their placement unless the part is placed.
        for (int i = 0; i < partDefs.size(); ++i) {
            JSONPartDefinition partDef = partDefs.get(i);
            if (partDef.additionalParts != null) {
                for (JSONPartDefinition additionalDef : partDef.additionalParts) {
                    //If we are at the end of the list, just add this entry.
                    //Otherwise, insert it one ahead of the current entry.
                    if (i + 1 == partDefs.size()) {
                        partDefs.add(additionalDef);
                    } else {
                        partDefs.add(i + 1, additionalDef);
                    }

                    //Add interactable variable to block interaction based on part present.
                    //Then combine this with existing IVs since they should inherit from the "parent".
                    if (additionalDef.interactableVariables == null) {
                        additionalDef.interactableVariables = new ArrayList<>();
                    }
                    List<String> presenceList = new ArrayList<>();
                    presenceList.add("part_present_" + (i + 1));
                    additionalDef.interactableVariables.add(presenceList);
                    if (partDef.interactableVariables != null) {
                        //Need to deep copy these, if we just add the list then edits will foul things up.
                        for (List<String> variableList : partDef.interactableVariables) {
                            List<String> copiedList = new ArrayList<>(variableList);
                            additionalDef.interactableVariables.add(copiedList);
                        }
                    }
                }
                partDef.additionalParts = null;
            }
        }

        //Update linking for engines, guns, and effectors.
        boolean linkedPartsPresent = false;
        for (JSONPartDefinition partDef : partDefs) {
            if (partDef.linkedParts != null) {
                linkedPartsPresent = true;
                break;
            }
        }
        if (!linkedPartsPresent) {
            for (JSONPartDefinition partDef : partDefs) {
                if (partDef.linkedParts == null) {
                    for (String partDefType : partDef.types) {
                        if (partDefType.startsWith("engine")) {
                            //Engine should link to as least one part on this definition.  What's the point of it if it doesn't drive anything?
                            partDef.linkedParts = new ArrayList<>();
                            for (JSONPartDefinition partDef2 : partDefs) {
                                for (String partDefType2 : partDef2.types) {
                                    if (partDefType2.startsWith("propeller") || (partDefType2.startsWith("ground") && ((linkFrontWheels && partDef2.pos.z > 0) || linkRearWheels && partDef2.pos.z <= 0))) {
                                        if (!partDef.linkedParts.contains(partDefs.indexOf(partDef2) + 1)) {
                                            partDef.linkedParts.add(partDefs.indexOf(partDef2) + 1);
                                        }
                                        break;
                                    }
                                }
                            }
                        } else if (partDefType.startsWith("gun")) {
                            //If the gun requires a seat to be present to place, link it to that seat rather than the controller.
                            partDef.linkedParts = new ArrayList<>();
                            boolean foundLink = false;
                            if (partDef.interactableVariables != null) {
                                for (List<String> variables : partDef.interactableVariables) {
                                    for (String variable : variables) {
                                        if (variable.startsWith("part_present")) {
                                            if (!partDef.linkedParts.contains(Integer.parseInt(variable.substring("part_present_".length())))) {
                                                partDef.linkedParts.add(Integer.parseInt(variable.substring("part_present_".length())));
                                            }
                                            foundLink = true;
                                            break;
                                        }
                                    }
                                    if (foundLink) {
                                        break;
                                    }
                                }
                            }

                            if (!foundLink) {
                                //No required seat found, just link to controller(s).
                                for (JSONPartDefinition partDef2 : partDefs) {
                                    if (partDef2.isController) {
                                        for (String partDefType2 : partDef2.types) {
                                            if (partDefType2.startsWith("seat")) {
                                                if (!partDef.linkedParts.contains(partDefs.indexOf(partDef2) + 1)) {
                                                    partDef.linkedParts.add(partDefs.indexOf(partDef2) + 1);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (partDefType.startsWith("effector")) {
                            //Effectors link to interactables (i.e. crates).  We don't link to genrics though as that would let them pull from the wrong things.
                            partDef.linkedParts = new ArrayList<>();
                            for (JSONPartDefinition partDef2 : partDefs) {
                                for (String partDefType2 : partDef2.types) {
                                    if (partDefType2.startsWith("interactable")) {
                                        if (!partDef.linkedParts.contains(partDefs.indexOf(partDef2) + 1)) {
                                            partDef.linkedParts.add(partDefs.indexOf(partDef2) + 1);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void performVehiclePartDefLegacyCompats(JSONPartDefinition partDef) {
        if (partDef.additionalPart != null) {
            partDef.additionalParts = new ArrayList<>();
            partDef.additionalParts.add(partDef.additionalPart);
            partDef.additionalPart = null;
        }
        if (partDef.linkedDoor != null) {
            partDef.linkedDoors = new ArrayList<>();
            partDef.linkedDoors.add(partDef.linkedDoor);
            partDef.linkedDoor = null;
        }
        if (partDef.linkedDoors != null) {
            partDef.linkedVariables = partDef.linkedDoors;
            partDef.linkedDoors = null;
        }
        if (partDef.linkedVariables != null) {
            partDef.interactableVariables = new ArrayList<>();
            partDef.interactableVariables.add(partDef.linkedVariables);
            partDef.linkedVariables = null;
        }
        if (partDef.exhaustPos != null) {
            partDef.particleObjects = new ArrayList<>();
            for (int i = 0; i < partDef.exhaustPos.length; i += 3) {
                JSONParticle particle = new JSONParticle();
                particle.type = ParticleType.SMOKE;
                particle.pos = new Point3D(partDef.exhaustPos[i], partDef.exhaustPos[i + 1], partDef.exhaustPos[i + 2]);
                particle.velocityVector = new Point3D(partDef.exhaustVelocity[i], partDef.exhaustVelocity[i + 1], partDef.exhaustVelocity[i + 2]);
                particle.scale = 1.0F;
                particle.color = new ColorRGB("#D9D9D9");
                particle.transparency = 0.25F;
                particle.toTransparency = 0.25F;
                partDef.particleObjects.add(particle);
            }
            partDef.exhaustPos = null;
            partDef.exhaustVelocity = null;
        }
        if (partDef.exhaustObjects != null) {
            partDef.particleObjects = new ArrayList<>();
            for (minecrafttransportsimulator.jsondefs.JSONPartDefinition.ExhaustObject exhaust : partDef.exhaustObjects) {
                JSONParticle particle = new JSONParticle();
                particle.type = ParticleType.SMOKE;
                particle.pos = exhaust.pos;
                particle.velocityVector = exhaust.velocity;
                particle.scale = exhaust.scale;
                particle.color = new ColorRGB("#D9D9D9");
                particle.transparency = 0.25F;
                particle.toTransparency = 0.25F;
                partDef.particleObjects.add(particle);
            }
            partDef.exhaustObjects = null;
        }
        if (partDef.rotationVariable != null) {
            partDef.animations = new ArrayList<>();
            JSONAnimationDefinition animation = new JSONAnimationDefinition();
            animation.animationType = AnimationComponentType.ROTATION;
            animation.variable = partDef.rotationVariable;
            animation.centerPoint = partDef.rotationPosition;
            animation.axis = partDef.rotationAngles;
            animation.clampMin = partDef.rotationClampMin;
            animation.clampMax = partDef.rotationClampMax;
            animation.absolute = partDef.rotationAbsolute;
            partDef.animations.add(animation);
            partDef.rotationVariable = null;
            partDef.rotationPosition = null;
            partDef.rotationAngles = null;
            partDef.rotationClampMin = 0;
            partDef.rotationClampMax = 0;
            partDef.rotationAbsolute = false;
        }
        if (partDef.translationVariable != null) {
            if (partDef.animations == null) {
                partDef.animations = new ArrayList<>();
            }
            JSONAnimationDefinition animation = new JSONAnimationDefinition();
            animation.animationType = AnimationComponentType.TRANSLATION;
            animation.variable = partDef.translationVariable;
            animation.axis = partDef.translationPosition;
            animation.clampMin = partDef.translationClampMin;
            animation.clampMax = partDef.translationClampMax;
            animation.absolute = partDef.translationAbsolute;
            partDef.animations.add(animation);
            partDef.translationVariable = null;
            partDef.translationPosition = null;
            partDef.translationClampMin = 0;
            partDef.translationClampMax = 0;
            partDef.translationAbsolute = false;
        }
        for (byte i = 0; i < partDef.types.size(); ++i) {
            String partName = partDef.types.get(i);
            switch (partName) {
                case "wheel":
                case "skid":
                case "pontoon":
                case "tread":
                    if (partName.equals("tread")) {
                        partDef.turnsWithSteer = true;
                    }
                    partDef.types.set(i, "ground_" + partName);
                    break;
                case "crate":
                case "barrel":
                case "crafting_table":
                case "furnace":
                case "brewing_stand":
                    partDef.types.set(i, "interactable_" + partName);
                    partDef.minValue = 0;
                    partDef.maxValue = 1;
                    break;
                case "fertilizer":
                case "harvester":
                case "planter":
                case "plow":
                    partDef.types.set(i, "effector_" + partName);
                    break;
                case "custom":
                    partDef.types.set(i, "generic");
                    break;
            }
            //Re-get the part name in case it changed.
            partName = partDef.types.get(i);

            //If we have ground devices that are wheels, but no animations, add those automatically.
            //Also check if we do have animations, but it's a rotation at the part's position.
            //Old format for rotations was to be local to the part, not the vehicle.
            if (partName.startsWith("ground_wheel") && partDef.turnsWithSteer) {
                if (partDef.animations == null) {
                    if (partDef.applyAfter == null) {
                        partDef.animations = new ArrayList<>();
                        JSONAnimationDefinition animation = new JSONAnimationDefinition();
                        animation.centerPoint = partDef.pos.copy();
                        animation.axis = new Point3D(0, partDef.pos.z > 0 ? -1 : 1, 0);
                        animation.animationType = AnimationComponentType.ROTATION;
                        animation.variable = "rudder";
                        partDef.animations.add(animation);
                    }
                } else {
                    for (JSONAnimationDefinition animation : partDef.animations) {
                        if (animation.animationType.equals(AnimationComponentType.ROTATION) && animation.variable.equals("rudder") && animation.centerPoint.isZero()) {
                            animation.centerPoint.set(partDef.pos);
                        }
                    }
                }
            }

            //If we have additional parts, check those too.
            if (partDef.additionalParts != null) {
                for (JSONPartDefinition additionalPartDef : partDef.additionalParts) {
                    performVehiclePartDefLegacyCompats(additionalPartDef);
                }
            }
        }
        //If we defined a player scale, change it to new form.
        if (partDef.widthScale != 0 || partDef.heightScale != 0) {
            if (partDef.widthScale == 0) {
                partDef.widthScale = 1;
            }
            if (partDef.heightScale == 0) {
                partDef.heightScale = 1;
            }
            partDef.playerScale = new Point3D(partDef.widthScale, partDef.heightScale, partDef.widthScale);
            partDef.widthScale = 0;
            partDef.heightScale = 0;
        }
        //Set mirroring to new format.  Only do this for wheels, or things with inverse mirroring set.
        for (byte i = 0; i < partDef.types.size(); ++i) {
            String partName = partDef.types.get(i);
            if (partDef.rot == null && (partName.startsWith("ground_wheel") || partDef.inverseMirroring)) {
                partDef.isMirrored = (partDef.pos.x < 0 && !partDef.inverseMirroring) || (partDef.pos.x >= 0 && partDef.inverseMirroring);
                if (partDef.isMirrored && partDef.rot == null) {
                    partDef.rot = new RotationMatrix().rotateY(180);
                }
                partDef.inverseMirroring = false;
                break;
            }
        }
    }

    private static void performVehicleConnectionLegacyCompats(AJSONInteractableEntity interactableDef) {
        if (interactableDef.connections != null) {
            interactableDef.connectionGroups = new ArrayList<>();
            JSONConnectionGroup hitchGroup = null;
            JSONConnectionGroup hookupGroup = null;
            for (JSONConnection connection : interactableDef.connections) {
                if (connection.hookup) {
                    if (hookupGroup == null) {
                        hookupGroup = new JSONConnectionGroup();
                        hookupGroup.connections = new ArrayList<>();
                        hookupGroup.groupName = "HOOKUP";
                        hookupGroup.hookup = true;
                    }
                    connection.hookup = false;
                    hookupGroup.connections.add(connection);
                } else {
                    if (hitchGroup == null) {
                        hitchGroup = new JSONConnectionGroup();
                        hitchGroup.connections = new ArrayList<>();
                        hitchGroup.groupName = "TRAILER";
                        hitchGroup.canInitiateConnections = true;
                    }
                    hitchGroup.connections.add(connection);
                }
            }
            if (hookupGroup != null) {
                interactableDef.connectionGroups.add(hookupGroup);
            }
            if (hitchGroup != null) {
                interactableDef.connectionGroups.add(hitchGroup);
            }
            interactableDef.connections = null;
        }

        //Check for old hookup variables and distances.
        if (interactableDef.connectionGroups != null) {
            for (JSONConnectionGroup connectionGroup : interactableDef.connectionGroups) {
                if (connectionGroup.hookup) {
                    connectionGroup.isHookup = true;
                    connectionGroup.hookup = false;
                }
                if (!connectionGroup.isHookup && !connectionGroup.isHitch) {
                    connectionGroup.isHitch = true;
                }
                for (JSONConnection connection : connectionGroup.connections) {
                    if (connection.distance == 0) {
                        connection.distance = 2;
                    }
                }
                if (connectionGroup.canIntiateConnections) {
                    connectionGroup.canInitiateConnections = true;
                    connectionGroup.canIntiateConnections = false;
                }
                if (connectionGroup.canIntiateSubConnections) {
                    connectionGroup.canInitiateSubConnections = true;
                    connectionGroup.canIntiateSubConnections = false;
                }
            }
        }
    }

    private static void performVehicleCollisionLegacyCompats(AJSONInteractableEntity interactableDef) {
        if (interactableDef.doors != null) {
            if (interactableDef.collisionGroups == null) {
                interactableDef.collisionGroups = new ArrayList<>();
            }
            for (minecrafttransportsimulator.jsondefs.JSONDoor door : interactableDef.doors) {
                //Check if door should auto-open when placed.
                if (!door.closedByDefault) {
                    //We can assume rendering exists.  Cause who the heck makes doors that don't animate?
                    if (interactableDef.rendering.initialVariables == null) {
                        interactableDef.rendering.initialVariables = new ArrayList<>();
                    }
                    interactableDef.rendering.initialVariables.add(door.name);
                }
                //Add door to collision box listing for updates.
                JSONCollisionGroup collisionGroup = new JSONCollisionGroup();
                collisionGroup.collisions = new ArrayList<>();
                collisionGroup.isInterior = true;
                JSONCollisionBox collision = new JSONCollisionBox();
                collision.variableName = door.ignoresClicks ? null : door.name;
                collision.pos = door.closedPos;
                collision.width = door.width;
                collision.height = door.height;
                collisionGroup.armorThickness = door.armorThickness;
                collisionGroup.collisions.add(collision);

                //Create animations for this door.
                collisionGroup.animations = new ArrayList<>();
                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.axis = door.openPos.subtract(door.closedPos);
                animation.animationType = AnimationComponentType.TRANSLATION;
                animation.variable = door.name;
                collisionGroup.animations.add(animation);

                //Add group to list.
                interactableDef.collisionGroups.add(collisionGroup);
            }
            interactableDef.doors = null;
        }

        if (interactableDef.collision != null) {
            if (interactableDef.collisionGroups == null) {
                interactableDef.collisionGroups = new ArrayList<>();
            }
            List<JSONCollisionBox> interiorBoxes = new ArrayList<>();
            List<JSONCollisionBox> exteriorBoxes = new ArrayList<>();
            for (JSONCollisionBox boxDef : interactableDef.collision) {
                if (boxDef.isInterior) {
                    interiorBoxes.add(boxDef);
                    boxDef.isInterior = false;
                } else {
                    exteriorBoxes.add(boxDef);
                }
            }

            if (!interiorBoxes.isEmpty()) {
                JSONCollisionGroup collisionGroup = new JSONCollisionGroup();
                collisionGroup.collisions = interiorBoxes;
                collisionGroup.isInterior = true;
                interactableDef.collisionGroups.add(collisionGroup);
            }
            if (!exteriorBoxes.isEmpty()) {
                JSONCollisionGroup collisionGroup = new JSONCollisionGroup();
                collisionGroup.collisions = exteriorBoxes;
                interactableDef.collisionGroups.add(collisionGroup);
            }

            interactableDef.collision = null;
        }

        if (interactableDef.collisionGroups != null) {
            for (JSONCollisionGroup group : interactableDef.collisionGroups) {
                for (JSONCollisionBox box : group.collisions) {
                    if (box.variableName != null && box.variableType == null) {
                        box.variableType = JSONAction.ActionType.TOGGLE;
                    }
                    if (box.variableName != null) {
                        box.action = new JSONAction();
                        box.action.action = box.variableType;
                        box.action.variable = box.variableName;
                        box.action.value = box.variableValue;
                        box.action.clampMin = box.clampMin;
                        box.action.clampMax = box.clampMax;
                        box.variableType = null;
                        box.variableName = null;
                        box.variableValue = 0;
                        box.clampMin = 0;
                        box.clampMax = 0;
                    }
                }
            }
        }
    }

    private static void performAnimationLegacyCompats(JSONRendering rendering) {
        if (rendering.textMarkings != null) {
            rendering.textObjects = new ArrayList<>();
            for (JSONRendering.VehicleDisplayText marking : rendering.textMarkings) {
                JSONText object = new JSONText();
                object.lightsUp = rendering.textLighted;
                object.color = marking.color;
                object.scale = marking.scale;
                object.maxLength = rendering.displayTextMaxLength;
                object.pos = marking.pos;
                object.rot = marking.rot;
                object.fieldName = "Text";
                object.defaultText = rendering.defaultDisplayText;
                rendering.textObjects.add(object);
            }
            rendering.textMarkings = null;
            rendering.defaultDisplayText = null;
            rendering.displayTextMaxLength = 0;
            rendering.textLighted = false;
        }
        if (rendering.rotatableModelObjects != null) {
            if (rendering.animatedObjects == null) {
                rendering.animatedObjects = new ArrayList<>();
            }
            for (JSONRendering.VehicleRotatableModelObject rotatable : rendering.rotatableModelObjects) {
                JSONAnimatedObject object = null;
                for (JSONAnimatedObject testObject : rendering.animatedObjects) {
                    if (testObject.objectName.equals(rotatable.partName)) {
                        object = testObject;
                        break;
                    }
                }
                if (object == null) {
                    object = new JSONAnimatedObject();
                    object.objectName = rotatable.partName;
                    object.animations = new ArrayList<>();
                    rendering.animatedObjects.add(object);
                }

                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.animationType = AnimationComponentType.ROTATION;
                animation.variable = rotatable.rotationVariable;
                animation.centerPoint = rotatable.rotationPoint;
                animation.axis = rotatable.rotationAxis != null ? rotatable.rotationAxis : new Point3D();
                animation.clampMin = rotatable.rotationClampMin;
                animation.clampMax = rotatable.rotationClampMax;
                animation.absolute = rotatable.absoluteValue;
                if (rotatable.rotationVariable.equals("steering_wheel")) {
                    animation.variable = "rudder";
                    animation.axis.invert();
                }
                if (rotatable.rotationVariable.equals("door")) {
                    animation.duration = 30;
                }
                object.animations.add(animation);
            }
            rendering.rotatableModelObjects = null;
        }
        if (rendering.translatableModelObjects != null) {
            if (rendering.animatedObjects == null) {
                rendering.animatedObjects = new ArrayList<>();
            }
            for (JSONRendering.VehicleTranslatableModelObject translatable : rendering.translatableModelObjects) {
                JSONAnimatedObject object = null;
                for (JSONAnimatedObject testObject : rendering.animatedObjects) {
                    if (testObject.objectName.equals(translatable.partName)) {
                        object = testObject;
                        break;
                    }
                }
                if (object == null) {
                    object = new JSONAnimatedObject();
                    object.objectName = translatable.partName;
                    object.animations = new ArrayList<>();
                    rendering.animatedObjects.add(object);
                }

                JSONAnimationDefinition animation = new JSONAnimationDefinition();
                animation.animationType = AnimationComponentType.TRANSLATION;
                animation.variable = translatable.translationVariable;
                animation.axis = translatable.translationAxis;
                animation.clampMin = translatable.translationClampMin;
                animation.clampMax = translatable.translationClampMax;
                animation.absolute = translatable.absoluteValue;
                if (translatable.translationVariable.equals("steering_wheel")) {
                    animation.variable = "rudder";
                    animation.axis.invert();
                }
                if (translatable.translationVariable.equals("door")) {
                    animation.duration = 30;
                }
                object.animations.add(animation);
            }
            rendering.translatableModelObjects = null;
        }
        if (rendering.particles != null) {
            for (JSONParticle particle : rendering.particles) {
                if (particle.quantity == 0) {
                    particle.quantity = 1;
                }
            }
        }
    }

    private static void performModelLegacyCompats(AJSONMultiModelProvider definition) {
        if (definition.rendering == null) {
            definition.rendering = new JSONRendering();
        } else if (definition.rendering.particles != null) {
            //Remove any drip particles, those don't exist anymore.
            definition.rendering.particles.removeIf(particle -> particle.type == JSONParticle.ParticleType.DRIP);
        }

        try {
            List<RenderableVertices> parsedModel = AModelParser.parseModel(definition.getModelLocation(definition.definitions.get(0)), true);

            //If we don't have lights, check for them.
            if (definition.rendering.lightObjects == null) {
                definition.rendering.lightObjects = new ArrayList<>();
                for (RenderableVertices object : parsedModel) {
                    if (object.name.contains("&")) {
                        JSONLight lightDef = new JSONLight();
                        lightDef.objectName = object.name;
                        lightDef.brightnessAnimations = new ArrayList<>();
                        lightDef.color = new ColorRGB(object.name.substring(object.name.indexOf('_') + 1, object.name.indexOf('_') + 7));
                        lightDef.brightnessAnimations = new ArrayList<>();

                        //Add standard animation variable for light name.
                        String lowerCaseName = object.name.toLowerCase(Locale.ROOT);
                        JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
                        if (lowerCaseName.contains("brakelight")) {
                            activeAnimation.variable = "brake";
                        } else if (lowerCaseName.contains("backuplight")) {
                            activeAnimation.variable = "engine_reversed_1";
                        } else if (lowerCaseName.contains("daytimelight")) {
                            activeAnimation.variable = "engines_on";
                        } else if (lowerCaseName.contains("navigationlight")) {
                            activeAnimation.variable = "navigation_light";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasNavLights = true;
                        } else if (lowerCaseName.contains("strobelight")) {
                            activeAnimation.variable = "strobe_light";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasStrobeLights = true;
                        } else if (lowerCaseName.contains("taxilight")) {
                            activeAnimation.variable = "taxi_light";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasTaxiLights = true;
                        } else if (lowerCaseName.contains("landinglight")) {
                            activeAnimation.variable = "landing_light";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasLandingLights = true;
                        } else if (lowerCaseName.contains("runninglight")) {
                            activeAnimation.variable = "running_light";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasRunningLights = true;
                        } else if (lowerCaseName.contains("headlight")) {
                            activeAnimation.variable = "headlight";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasHeadlights = true;
                        } else if (lowerCaseName.contains("leftturnlight")) {
                            activeAnimation.variable = "left_turn_signal";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasTurnSignals = true;
                        } else if (lowerCaseName.contains("rightturnlight")) {
                            activeAnimation.variable = "right_turn_signal";
                            if (definition instanceof JSONVehicle)
                                ((JSONVehicle) definition).motorized.hasTurnSignals = true;
                        } else if (lowerCaseName.contains("emergencylight")) {
                            activeAnimation.variable = "EMERLTS";
                            if (definition.rendering.customVariables == null) {
                                definition.rendering.customVariables = new ArrayList<>();
                            }
                            if (definition instanceof JSONVehicle)
                                definition.rendering.customVariables.add("EMERLTS");
                        } else if (lowerCaseName.contains("stoplight") || lowerCaseName.contains("cautionlight") || lowerCaseName.contains("golight")) {
                            //Traffic signal detected.  Get light name for variable.
                            String[] lightNames = lowerCaseName.split("light");
                            lightNames[0] = lightNames[0].replace("&", "");
                            activeAnimation.variable = lightNames[0] + "_" + "light";
                            if (lightNames.length > 2) {
                                activeAnimation.variable += "_" + lightNames[2];
                            }

                            //If the light is a stop light, create a cycle for it for un-linked states.
                            if (lightNames[0].equals("stop")) {
                                JSONAnimationDefinition cycleInhibitor = new JSONAnimationDefinition();
                                cycleInhibitor.animationType = AnimationComponentType.INHIBITOR;
                                cycleInhibitor.variable = "linked";
                                cycleInhibitor.clampMin = 1.0F;
                                cycleInhibitor.clampMax = 1.0F;
                                lightDef.brightnessAnimations.add(cycleInhibitor);

                                JSONAnimationDefinition cycleAnimation = new JSONAnimationDefinition();
                                cycleAnimation.animationType = AnimationComponentType.VISIBILITY;
                                cycleAnimation.variable = "0_10_10_cycle";
                                cycleAnimation.clampMin = 1.0F;
                                cycleAnimation.clampMax = 1.0F;
                                lightDef.brightnessAnimations.add(cycleAnimation);

                                JSONAnimationDefinition lightActivator = new JSONAnimationDefinition();
                                lightActivator.animationType = AnimationComponentType.ACTIVATOR;
                                lightActivator.variable = "linked";
                                lightActivator.clampMin = 1.0F;
                                lightActivator.clampMax = 1.0F;
                                lightDef.brightnessAnimations.add(lightActivator);

                                JSONAnimationDefinition lightInhibitor = new JSONAnimationDefinition();
                                lightInhibitor.animationType = AnimationComponentType.INHIBITOR;
                                lightInhibitor.variable = "linked";
                                lightInhibitor.clampMin = 0.0F;
                                lightInhibitor.clampMax = 0.0F;
                                lightDef.brightnessAnimations.add(lightInhibitor);
                            }
                        }

                        if (activeAnimation.variable != null) {
                            activeAnimation.animationType = AnimationComponentType.VISIBILITY;
                            activeAnimation.clampMin = activeAnimation.variable.equals("brake") ? 0.01F : 1.0F;
                            activeAnimation.clampMax = 1.0F;
                            lightDef.brightnessAnimations.add(activeAnimation);
                        }

                        //Set light to be electric.
                        lightDef.isElectric = true;

                        //Get flashing cycle rate and convert to cycle variable if required.
                        //Look at flash bits from right to left until we hit one that's not on.  Count how many ticks are on and use that for cycle.
                        int flashBits = Integer.decode("0x" + object.name.substring(object.name.indexOf('_', object.name.indexOf('_') + 7) + 1, object.name.lastIndexOf('_')));
                        int ticksTillOn = 0;
                        int ticksOn = 0;
                        boolean foundOn = false;
                        for (byte i = 0; i < 20; ++i) {
                            if (((flashBits >> i) & 1) == 1) {
                                //We are on, increment on counter and set on.
                                if (!foundOn) {
                                    foundOn = true;
                                    ticksTillOn = i;
                                }
                                ++ticksOn;
                            } else {
                                //If we were previously on, we are at the end of the cycle.
                                if (foundOn) {
                                    break;
                                }
                            }
                        }
                        if ((ticksOn - ticksTillOn) != 20) {
                            JSONAnimationDefinition cycleAnimation = new JSONAnimationDefinition();
                            cycleAnimation.animationType = AnimationComponentType.VISIBILITY;
                            cycleAnimation.variable = ticksTillOn + "_" + ticksOn + "_" + (20 - ticksOn - ticksTillOn) + "_cycle";
                            cycleAnimation.clampMin = 1.0F;
                            cycleAnimation.clampMax = 1.0F;
                            lightDef.brightnessAnimations.add(cycleAnimation);
                        }

                        String lightProperties = object.name.substring(object.name.lastIndexOf('_') + 1);
                        boolean renderFlare = Integer.parseInt(lightProperties.substring(0, 1)) > 0;
                        lightDef.emissive = Integer.parseInt(lightProperties.substring(1, 2)) > 0;
                        lightDef.covered = Integer.parseInt(lightProperties.substring(2, 3)) > 0;
                        boolean renderBeam = lightProperties.length() == 4 ? Integer.parseInt(lightProperties.substring(3)) > 0 : (lowerCaseName.contains("headlight") || lowerCaseName.contains("landinglight") || lowerCaseName.contains("taxilight") || lowerCaseName.contains("streetlight"));

                        if (renderFlare || renderBeam) {
                            if (lightDef.blendableComponents == null) {
                                lightDef.blendableComponents = new ArrayList<>();
                            }

                            float[] masterVertex = new float[8];
                            for (int i = 0; i < object.vertices.capacity(); i += 8 * 3) {
                                float minX = 999;
                                float maxX = -999;
                                float minY = 999;
                                float maxY = -999;
                                float minZ = 999;
                                float maxZ = -999;
                                for (byte j = 0; j < 8 * 3; j += 8) {
                                    object.vertices.get(masterVertex);
                                    minX = Math.min(masterVertex[5], minX);
                                    maxX = Math.max(masterVertex[5], maxX);
                                    minY = Math.min(masterVertex[6], minY);
                                    maxY = Math.max(masterVertex[6], maxY);
                                    minZ = Math.min(masterVertex[7], minZ);
                                    maxZ = Math.max(masterVertex[7], maxZ);
                                }
                                JSONLightBlendableComponent blendable = new JSONLightBlendableComponent();
                                if (renderFlare) {
                                    blendable.flareHeight = 3 * Math.max(Math.max((maxX - minX), (maxY - minY)), (maxZ - minZ));
                                    blendable.flareWidth = blendable.flareHeight;
                                }
                                if (renderBeam) {
                                    blendable.beamDiameter = Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY) * 64F;
                                    blendable.beamLength = blendable.beamDiameter * 3;
                                }
                                blendable.pos = new Point3D(minX + (maxX - minX) / 2D, minY + (maxY - minY) / 2D, minZ + (maxZ - minZ) / 2D);
                                blendable.axis = new Point3D(masterVertex[0], masterVertex[1], masterVertex[2]);

                                lightDef.blendableComponents.add(blendable);
                            }
                        }
                        object.vertices.rewind();
                        definition.rendering.lightObjects.add(lightDef);
                    }
                }
            }

            //Now check for tread rollers.
            //We need to convert them into the new path system.
            List<String> leftRollers = new ArrayList<>();
            List<String> rightRollers = new ArrayList<>();
            for (RenderableVertices object : parsedModel) {
                if (object.name.toLowerCase(Locale.ROOT).contains("roller")) {
                    //Add roller to roller lists.
                    if (object.name.toLowerCase(Locale.ROOT).startsWith("l") || object.name.toLowerCase(Locale.ROOT).startsWith("$l")) {
                        leftRollers.add(object.name);
                    } else {
                        rightRollers.add(object.name);
                    }
                }
            }

            if (!leftRollers.isEmpty() || !rightRollers.isEmpty()) {
                //Sort rollers by name.
                Comparator<String> sorter = (roller1, roller2) -> {
                    int roller1Index = Integer.parseInt(roller1.substring(roller1.lastIndexOf('_') + 1));
                    int roller2Index = Integer.parseInt(roller2.substring(roller2.lastIndexOf('_') + 1));
                    return Integer.compare(roller1Index, roller2Index);
                };
                leftRollers.sort(sorter);
                rightRollers.sort(sorter);

                //If we don't have any left rollers, then we are an older system that didn't split them.
                //Copy the rollers to the left side for the part slot pathing.
                if (leftRollers.isEmpty()) {
                    leftRollers.addAll(rightRollers);
                }

                //Find the part slot and def these rollers go to.
                int leftSlotIndex = -1;
                int rightSlotIndex = -1;
                for (JSONPartDefinition partDef : ((AJSONPartProvider) definition).parts) {
                    if (partDef.types.contains("ground_tread")) {
                        if (partDef.pos.x <= 0) {
                            rightSlotIndex = ((AJSONPartProvider) definition).parts.indexOf(partDef) + 1;
                            if (partDef.treadPath == null) {
                                partDef.treadPath = rightRollers;
                            }
                        } else {
                            leftSlotIndex = ((AJSONPartProvider) definition).parts.indexOf(partDef) + 1;
                            if (partDef.treadPath == null) {
                                partDef.treadPath = leftRollers;
                            }
                        }
                    }
                }

                //Check if an animation for the rollers exists already.
                //We use a set here to prevent duplicates in the loop (less calls).
                Set<String> allRollers = new HashSet<>();
                allRollers.addAll(leftRollers);
                allRollers.addAll(rightRollers);

                for (String rollerName : allRollers) {
                    boolean animationPresent = false;
                    if (definition.rendering != null && definition.rendering.animatedObjects != null) {
                        for (JSONAnimatedObject animatedObject : definition.rendering.animatedObjects) {
                            if (rollerName.equals(animatedObject.objectName)) {
                                animationPresent = true;
                                break;
                            }
                        }
                    }

                    if (!animationPresent) {
                        //Get the model object for the roller.
                        RenderableVertices rollerObject = null;
                        TreadRoller roller = null;
                        for (RenderableVertices object : parsedModel) {
                            if (object.name.equals(rollerName)) {
                                rollerObject = object;
                                roller = new TreadRoller(object);
                                break;
                            }
                        }

                        //Create new animation.
                        JSONAnimationDefinition animation = new JSONAnimationDefinition();
                        animation.animationType = AnimationComponentType.ROTATION;
                        animation.variable = "ground_rotation_" + (leftRollers.contains(rollerName) ? leftSlotIndex : rightSlotIndex);

                        //Set roller center and axis.
                        //360 degrees is 1 block, so if we have a roller of circumference of 1,
                        //then we want a axis of 1 so it will have a linear movement of 1 every 360 degrees.
                        //Knowing this, we can calculate the linear velocity for this roller, as a roller with
                        //half the circumference needs double the factor, and vice-versa.  Basically, we get
                        //the ratio of the two circumferences of the "standard" roller and our roller.
                        animation.centerPoint = roller.centerPoint;
                        animation.axis = new Point3D((1.0D / Math.PI) / (roller.radius * 2D), 0, 0);

                        //Create animated object and save.
                        if (definition.rendering == null) {
                            definition.rendering = new JSONRendering();
                        }
                        if (definition.rendering.animatedObjects == null) {
                            definition.rendering.animatedObjects = new ArrayList<>();
                        }
                        JSONAnimatedObject animatedObject = new JSONAnimatedObject();
                        animatedObject.objectName = rollerObject.name;
                        animatedObject.animations = new ArrayList<>();
                        animatedObject.animations.add(animation);
                        definition.rendering.animatedObjects.add(animatedObject);
                    }
                }
            }
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("Could not do model-based legacy compats on " + definition.packID + ":" + definition.systemName + ".  Lights and treads will likely not be present on this model.");
            InterfaceManager.coreInterface.logError(e.getMessage());
        }
    }
}
