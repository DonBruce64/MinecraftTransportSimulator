package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.Map;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.items.instances.ItemDecor.DecorComponentType;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.AJSONInteractableEntity;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONItem.General.TextLine;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONConnectionGroup;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONInstrumentDefinition;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartEngine.EngineSound;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition.ExhaustObject;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONParticle.ParticleType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Class responsible for applying legacy compat code to JSONs.  All legacy compat code should
 * go here.  Once a definition calls methods in this class, it can be assumed to be in the most
 * modern form possible and ready for all the current systems.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public final class LegacyCompatSystem{
	
	public static void performLegacyCompats(AJSONItem definition){
		if(definition instanceof JSONVehicle){
			performVehicleLegacyCompats((JSONVehicle) definition);
		}else if(definition instanceof JSONPart){
			performPartLegacyCompats((JSONPart) definition);
		}else if(definition instanceof JSONInstrument){
			performInstrumentLegacyCompats((JSONInstrument) definition);
		}else if(definition instanceof JSONPoleComponent){
			performPoleLegacyCompats((JSONPoleComponent) definition);
		}else if(definition instanceof JSONDecor){
			performDecorLegacyCompats((JSONDecor) definition);
		}else if(definition instanceof JSONItem){
			performItemLegacyCompats((JSONItem) definition);
		}else if(definition instanceof JSONSkin){
			performSkinLegacyCompats((JSONSkin) definition);
		}
		//This goes after we've made default definitions.
		if(definition.general.modelName != null){
			AJSONMultiModelProvider provider = (AJSONMultiModelProvider) definition;
			for(JSONSubDefinition subDef : provider.definitions){
				subDef.modelName = definition.general.modelName;
			}
			definition.general.modelName = null;
		}
		
		//Parse the model and do LCs on it if we need to do so for lights.
		if(ConfigSystem.configObject != null && ConfigSystem.configObject.general.doLegacyLightCompats.value && definition instanceof AJSONMultiModelProvider && !(definition instanceof JSONSkin)){
			performLightLegacyCompats((AJSONMultiModelProvider) definition);
		}
	}
	
	private static void performVehicleLegacyCompats(JSONVehicle definition){
		//Move vehicle parameters to the motorized section.
		if(definition.general.emptyMass > 0){
			definition.motorized.isAircraft = definition.general.isAircraft;
			definition.general.isAircraft = false;
	    	definition.motorized.isBlimp = definition.general.isBlimp;
	    	definition.general.isBlimp = false;
	    	definition.motorized.hasOpenTop = definition.general.openTop;
	    	definition.general.openTop = false;
	    	definition.motorized.emptyMass = definition.general.emptyMass;
	    	definition.general.emptyMass = 0;
		}
		
		if(definition.car != null){
			definition.motorized.isBigTruck = definition.car.isBigTruck;
			definition.motorized.isFrontWheelDrive = definition.car.isFrontWheelDrive;
			definition.motorized.isRearWheelDrive = definition.car.isRearWheelDrive;
			definition.motorized.hasCruiseControl = definition.car.hasCruiseControl;
			definition.motorized.axleRatio = definition.car.axleRatio;
			definition.motorized.dragCoefficient = definition.car.dragCoefficient;
			definition.car = null;
		}
		
		//If we still have the old type parameter and are an aircraft, set the flag to true.
		if(definition.general.type != null){
			if(definition.general.type.equals("plane") || definition.general.type.equals("blimp") || definition.general.type.equals("helicopter")){
				definition.motorized.isAircraft = true;
			}
			if(definition.general.type.equals("blimp")){
				definition.motorized.isBlimp = true;
			}
			definition.general.type = null;
		}
		
		if(definition.plane != null){
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
			if(definition.motorized.aileronArea == 0){
				definition.motorized.aileronArea = definition.motorized.wingArea/5F;
			}
		}
		
		if(definition.blimp != null){
			definition.general.isAircraft = true;
			definition.general.isBlimp = true;
			definition.motorized.crossSectionalArea = definition.blimp.crossSectionalArea;
			definition.motorized.tailDistance = definition.blimp.tailDistance;
			definition.motorized.rudderArea = definition.blimp.rudderArea;
			definition.motorized.ballastVolume = definition.blimp.ballastVolume;
			definition.blimp = null;
		}
		
		//Check for old hitches and hookups.
		if(definition.motorized.hitchPos != null){
			definition.connections = new ArrayList<JSONConnection>();
			for(String hitchName : definition.motorized.hitchTypes){
				JSONConnection connection = new JSONConnection();
				connection.hookup = false;
				connection.type = hitchName;
				connection.pos = definition.motorized.hitchPos;
				if(connection.mounted){
					connection.rot = new Point3d();
				}
				definition.connections.add(connection);
			}
			definition.motorized.hitchPos = null;
			definition.motorized.hitchTypes = null;
		}
		if(definition.motorized.hookupPos != null){
			if(definition.connections == null){
				definition.connections = new ArrayList<JSONConnection>();
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
		if(definition.motorized.instruments != null){
			definition.instruments = definition.motorized.instruments;
			definition.motorized.instruments = null;
			for(JSONInstrumentDefinition def : definition.instruments){
				if(def.optionalPartNumber != 0){
					def.placeOnPanel = true;
				}
			}
		}
		
		//Check for old flaps.
		if(definition.motorized.hasFlaps){
			definition.motorized.flapSpeed = 0.1F;
			definition.motorized.flapNotches = new ArrayList<Float>();
			for(int i=0; i<=EntityVehicleF_Physics.MAX_FLAP_ANGLE_REFERENCE/10/5; ++i){
				definition.motorized.flapNotches.add((float) (i*5));
			}
			definition.motorized.hasFlaps = false;
		}
		
		//Check if we didn't specify a braking force.
		if(definition.motorized.brakingFactor == 0){
			definition.motorized.brakingFactor = 1.0F;
		}
		
		//Move cruiseControl to autopilot.
		if(definition.motorized.hasCruiseControl){
			definition.motorized.hasAutopilot = true;
			definition.motorized.hasCruiseControl = false;
		}
		
		//Add hookup variables if we are a trailer and don't have them.
		if(definition.motorized.isTrailer && definition.motorized.hookupVariables == null){
			definition.motorized.hookupVariables = new ArrayList<String>();
			definition.motorized.hookupVariables.add("electric_power");
			definition.motorized.hookupVariables.add("engine_gear_1");
			definition.motorized.hookupVariables.add("engines_on");
			definition.motorized.hookupVariables.add("right_turn_signal");
			definition.motorized.hookupVariables.add("left_turn_signal");
			definition.motorized.hookupVariables.add("runninglight");
			definition.motorized.hookupVariables.add("headlight");
			definition.motorized.hookupVariables.add("emergencylight");
		}
		
		for(JSONPartDefinition partDef : definition.parts){
			try{
				performVehiclePartDefLegacyCompats(partDef);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on part entry #" + (definition.parts.indexOf(partDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
		}
		
		performVehicleConnectionLegacyCompats(definition);
		
		//Do rendering compats.
		if(definition.rendering != null){
			//Check for old HUD stuff.
			if(definition.rendering.hudTexture != null){
				definition.motorized.hudTexture = definition.rendering.hudTexture;
				definition.rendering.hudTexture = null;
			}
			if(definition.rendering.panelTexture != null){
				definition.motorized.panelTexture = definition.rendering.panelTexture;
				definition.rendering.panelTexture = null;
			}
			if(definition.rendering.panelTextColor != null){
				definition.motorized.panelTextColor = definition.rendering.panelTextColor;
				definition.rendering.panelTextColor = null;
			}
			if(definition.rendering.panelLitTextColor != null){
				definition.motorized.panelLitTextColor = definition.rendering.panelLitTextColor;
				definition.rendering.panelLitTextColor = null;
			}
		
			//Do compats for sounds.
			if(definition.rendering.sounds == null){
				definition.rendering.sounds = new ArrayList<JSONSound>();
				if(definition.motorized.hornSound != null){
					JSONSound hornSound = new JSONSound();
					hornSound.name = definition.motorized.hornSound;
					hornSound.looping = true;
					hornSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition hornDef = new JSONAnimationDefinition();
					hornDef.animationType = AnimationComponentType.VISIBILITY;
					hornDef.variable = "horn";
					hornDef.clampMin = 1.0F;
					hornDef.clampMax = 1.0F;
					hornSound.activeAnimations.add(hornDef);
					definition.rendering.sounds.add(hornSound);
					definition.motorized.hornSound = null;
				}
				if(definition.motorized.sirenSound != null){
					JSONSound sirenSound = new JSONSound();
					sirenSound.name = definition.motorized.sirenSound;
					sirenSound.looping = true;
					sirenSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition sirenDef = new JSONAnimationDefinition();
					sirenDef.animationType = AnimationComponentType.VISIBILITY;
					sirenDef.variable = "siren";
					sirenDef.clampMin = 1.0F;
					sirenDef.clampMax = 1.0F;
					sirenSound.activeAnimations.add(sirenDef);
					definition.rendering.sounds.add(sirenSound);
					if(definition.rendering.customVariables == null){
						definition.rendering.customVariables = new ArrayList<String>();
					}
					definition.rendering.customVariables.add("siren");
					definition.motorized.sirenSound = null;
				}
				if(definition.motorized.isBigTruck){
					JSONSound airbrakeSound = new JSONSound();
					airbrakeSound.name = MasterLoader.resourceDomain + ":air_brake_activating";
					airbrakeSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition airbrakeDef = new JSONAnimationDefinition();
					airbrakeDef.animationType = AnimationComponentType.VISIBILITY;
					airbrakeDef.variable = "p_brake";
					airbrakeDef.clampMin = 1.0F;
					airbrakeDef.clampMax = 1.0F;
					airbrakeSound.activeAnimations.add(airbrakeDef);
					definition.rendering.sounds.add(airbrakeSound);
					
					JSONSound backupBeeperSound = new JSONSound();
					backupBeeperSound.name = MasterLoader.resourceDomain + ":backup_beeper";
					backupBeeperSound.looping = true;
					backupBeeperSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
			if(definition.rendering.particles == null){
				definition.rendering.particles = new ArrayList<JSONParticle>();
				int engineNumber = 0;
				for(JSONPartDefinition partDef : definition.parts){
					if(partDef.particleObjects != null){
						++engineNumber;
						int pistonNumber = 0;
						for(JSONParticle exhaustDef : partDef.particleObjects){
							++pistonNumber;
							exhaustDef.type = ParticleType.SMOKE;
							exhaustDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
							backfireDef.color = "#000000";
							backfireDef.scale = 2.5F;
							backfireDef.quantity = 5;
							backfireDef.pos = exhaustDef.pos;
							backfireDef.initialVelocity = exhaustDef.initialVelocity;
							backfireDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
			}
			
			try{
				performAnimationLegacyCompats(definition.rendering);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
		}
    }
	
	private static void performPartLegacyCompats(JSONPart definition){
		//Move general things to generic section.
		if(definition.general.type != null){
			if(definition.generic == null){
				definition.generic = definition.new JSONPartGeneric();
			}
			definition.generic.type = definition.general.type;
			definition.general.type = null;
			definition.generic.customType = definition.general.customType;
			definition.general.customType = null;
			definition.generic.disableMirroring = definition.general.disableMirroring;
			definition.general.disableMirroring = false;
			definition.generic.useVehicleTexture = definition.general.useVehicleTexture;
			definition.general.useVehicleTexture = false;
		}
		
		//If we are a part without a definition, add one so we don't crash on other systems.
		if(definition.definitions == null){
			definition.definitions = new ArrayList<JSONSubDefinition>();
			JSONSubDefinition subDefinition = new JSONSubDefinition();
			subDefinition.extraMaterials = new ArrayList<String>();
			subDefinition.name = definition.general.name;
			subDefinition.subName = "";
			definition.definitions.add(subDefinition);
		}
		
		//Move subParts to parts if we have them there.
		if(definition.subParts != null){
			definition.parts = definition.subParts;
			definition.subParts = null;
		}
		
		if(definition.engine != null){
			//If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
			if(definition.generic.type.equals("engine_jet") && definition.engine.jetPowerFactor == 0){
				definition.engine.jetPowerFactor = 1.0F;
				definition.engine.bypassRatio = definition.engine.gearRatios.get(0);
				definition.engine.gearRatios.set(0, 1.0F);
			}
			
			//If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
			if(definition.engine.gearRatios.size() == 1){
				definition.engine.propellerRatio = 1/definition.engine.gearRatios.get(0);
				definition.engine.gearRatios.clear();
				definition.engine.gearRatios.add(-1F);
				definition.engine.gearRatios.add(0F);
				definition.engine.gearRatios.add(1F);
			}
			
			//Check various engine parameters that shouldn't be 0 as they might not be set.
			if(definition.engine.shiftSpeed == 0){
				definition.engine.shiftSpeed = 20;
			}
			if(definition.engine.revResistance == 0){
				definition.engine.revResistance = 10;
			}
			if(definition.engine.idleRPM == 0){
				definition.engine.idleRPM = definition.engine.maxRPM < 15000 ? 500 : 2000;
			}
			if(definition.engine.maxSafeRPM == 0){
				definition.engine.maxSafeRPM = definition.engine.maxRPM < 15000 ? definition.engine.maxRPM - (definition.engine.maxRPM - 2500)/2 : (int) (definition.engine.maxRPM/1.1);
			}
			
			//If we don't have matching up-shift and down-shift numbers, we are an engine that came before multiple reverse gears.
			if(definition.engine.upShiftRPM != null){
				while(definition.engine.upShiftRPM.size() < definition.engine.gearRatios.size()){
					definition.engine.upShiftRPM.add(0, 0);
				}
			}
			if(definition.engine.downShiftRPM != null){
				while(definition.engine.downShiftRPM.size() < definition.engine.gearRatios.size()){
					definition.engine.downShiftRPM.add(0, 0);
				}
			}
		}else{
			//Check for old ground devices, crates, barrels, effectors, and customs.
			switch(definition.generic.type){
				case("wheel"):{
					definition.generic.type = "ground_" + definition.generic.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.isWheel = true;
					definition.ground.width = definition.wheel.diameter/2F;
					definition.ground.height = definition.wheel.diameter;
					definition.ground.lateralFriction = definition.wheel.lateralFriction;
					definition.ground.motiveFriction = definition.wheel.motiveFriction;
					definition.wheel = null;
					break;
				}case("skid"):{
					definition.generic.type = "ground_" + definition.generic.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.width = definition.skid.width;
					definition.ground.height = definition.skid.width;
					definition.ground.lateralFriction = definition.skid.lateralFriction;
					definition.skid = null;
					break;
				}case("pontoon"):{
					definition.generic.type = "ground_" + definition.generic.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.canFloat = true;
					definition.ground.width = definition.pontoon.width;
					definition.ground.height = definition.pontoon.width;
					definition.ground.lateralFriction = definition.pontoon.lateralFriction;
					definition.ground.extraCollisionBoxOffset = definition.pontoon.extraCollisionBoxOffset;
					definition.pontoon = null;
					break;
				}case("tread"):{
					definition.generic.type = "ground_" + definition.generic.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.isTread = true;
					definition.ground.width = definition.tread.width;
					definition.ground.height = definition.tread.width;
					definition.ground.lateralFriction = definition.tread.lateralFriction;
					definition.ground.motiveFriction = definition.tread.motiveFriction;
					definition.ground.extraCollisionBoxOffset = definition.tread.extraCollisionBoxOffset;
					definition.ground.spacing = definition.tread.spacing;
					definition.tread = null;
					break;
				}case("crate"):{
					definition.generic.type = "interactable_crate";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = InteractableComponentType.CRATE;
					definition.interactable.inventoryUnits = 1;
					definition.interactable.feedsVehicles = true;
					break;
				}case("barrel"):{
					definition.generic.type = "interactable_barrel";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = InteractableComponentType.BARREL;
					definition.interactable.inventoryUnits = 1;
					break;
				}case("crafting_table"):{
					definition.generic.type = "interactable_crafting_table";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = InteractableComponentType.CRAFTING_TABLE;
					break;
				}case("furnace"):{
					definition.generic.type = "interactable_furnace";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = InteractableComponentType.FURNACE;
					break;
				}case("brewing_stand"):{
					definition.generic.type = "interactable_brewing_stand";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = InteractableComponentType.BREWING_STAND;
					break;
				}case("fertilizer"):{
					definition.generic.type = "effector_fertilizer";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.FERTILIZER;
					break;
				}case("harvester"):{
					definition.generic.type = "effector_harvester";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.HARVESTER;
					break;
				}case("planter"):{
					definition.generic.type = "effector_planter";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.PLANTER;
					break;
				}case("plow"):{
					definition.generic.type = "effector_plow";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.PLOW;
					break;
				}case("custom"):{
					definition.generic.type = "generic";
					definition.generic.height = definition.custom.height;
					definition.generic.width = definition.custom.width;
					definition.custom = null;
					break;
				}
			}
			
			//If the part is a ground_ type, and canGoFlat, auto-set flat height.
			if(definition.generic.type.startsWith("ground_") && definition.ground.canGoFlat && definition.ground.flatHeight == 0){
				definition.ground.flatHeight = definition.ground.height/2F;
			}
			
			//If the part is a seat, and doesn't have a seat sub-section, add one.
			if(definition.generic.type.startsWith("seat") && definition.seat == null){
				definition.seat = definition.new JSONPartSeat();
			}
			
			//If the part is a gun, set yaw and pitch speed if not set.
			if(definition.generic.type.startsWith("gun")){
				if(definition.gun.yawSpeed == 0){
					definition.gun.yawSpeed = 50/definition.gun.diameter + 1/definition.gun.length;
				}
				if(definition.gun.pitchSpeed == 0){
					definition.gun.pitchSpeed = 50/definition.gun.diameter + 1/definition.gun.length;
				}
			}
		}
		
		if(definition.parts != null){
    		for(JSONPartDefinition subPartDef : definition.parts){
    			try{
    				performVehiclePartDefLegacyCompats(subPartDef);
    			}catch(Exception e){
    				throw new NullPointerException("Could not perform Legacy Compats on sub-part entry #" + (definition.parts.indexOf(subPartDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
    			}
    		}
		}
		
		if(definition.rendering != null){
			try{
				performAnimationLegacyCompats(definition.rendering);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
		}
		
		performVehicleConnectionLegacyCompats(definition);
		
		//Do compats for engine and gun sounds.
		if(definition.rendering == null || definition.rendering.sounds == null){
			if(definition.engine != null){
				if(definition.rendering == null){
					definition.rendering = new JSONRendering();
				}
				if(definition.rendering.sounds == null){
					definition.rendering.sounds = new ArrayList<JSONSound>();
				}
				
				//Starting sound plays when engine goes from stopped to running.
				JSONSound startingSound = new JSONSound();
				startingSound.name = definition.packID + ":" + definition.systemName + "_starting";
				startingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				stoppingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				sputteringSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition sputteringActiveDef = new JSONAnimationDefinition();
				sputteringActiveDef.animationType = AnimationComponentType.VISIBILITY;
				sputteringActiveDef.variable = "engine_backfired";
				sputteringActiveDef.clampMin = 1.0F;
				sputteringActiveDef.clampMax = 1.0F;
				sputteringSound.activeAnimations.add(sputteringActiveDef);
				definition.rendering.sounds.add(sputteringSound);
				
				//Griding sound plays when engine has a bad shift.
				JSONSound grindingSound = new JSONSound();
				grindingSound.name = MasterLoader.resourceDomain + ":engine_shifting_grinding";
				grindingSound.forceSound = true;
				grindingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				crankingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition crankingActiveDef = new JSONAnimationDefinition();
				crankingActiveDef.animationType = AnimationComponentType.VISIBILITY;
				crankingActiveDef.variable = "engine_starter";
				crankingActiveDef.clampMin = 1.0F;
				crankingActiveDef.clampMax = 1.0F;
				crankingSound.activeAnimations.add(crankingActiveDef);
				
				crankingSound.pitchAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition crankingPitchDef = new JSONAnimationDefinition();
				if(!definition.engine.isCrankingNotPitched){
					crankingPitchDef.animationType = AnimationComponentType.TRANSLATION;
					crankingPitchDef.variable = "electric_power";
					crankingPitchDef.axis = new Point3d(0, 1D/10D, 0);
					crankingPitchDef.offset = 0.3F;
					crankingPitchDef.clampMax = 1.0F;
					definition.engine.isCrankingNotPitched = false;
				}else{
					crankingPitchDef = new JSONAnimationDefinition();
					crankingPitchDef.animationType = AnimationComponentType.TRANSLATION;
					crankingPitchDef.variable = "engine_rpm";
					crankingPitchDef.axis = new Point3d(0, 1D/(definition.engine.maxRPM < 15000 ? 500D : 2000D), 0);
				}
				crankingSound.pitchAnimations.add(crankingPitchDef);
				definition.rendering.sounds.add(crankingSound);
				
				//Running sound plays when engine is running, and pitch-shifts to match engine speed.
				if(definition.engine.customSoundset != null){
					for(EngineSound customSound : definition.engine.customSoundset){
						JSONSound runningSound = new JSONSound();
						runningSound.name = customSound.soundName;
						runningSound.looping = true;
						runningSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
						runningSound.volumeAnimations = new ArrayList<JSONAnimationDefinition>();
						runningSound.pitchAnimations = new ArrayList<JSONAnimationDefinition>();
						
						//Add default sound on/off variable.
						JSONAnimationDefinition runningStartDef = new JSONAnimationDefinition();
						runningStartDef.animationType = AnimationComponentType.VISIBILITY;
						runningStartDef.variable = "engine_powered";
						runningStartDef.clampMin = 1.0F;
						runningStartDef.clampMax = 1.0F;
						runningSound.activeAnimations.add(runningStartDef);
						
						//Add volume variable.
						JSONAnimationDefinition runningVolumeDef = new JSONAnimationDefinition();
						if(customSound.volumeAdvanced){
							runningVolumeDef.animationType = AnimationComponentType.ROTATION;
							runningVolumeDef.variable = "engine_rpm";
							runningVolumeDef.axis = new Point3d(-0.000001/(customSound.volumeLength/1000), 0, customSound.volumeCenter);
							runningVolumeDef.offset = (customSound.volumeLength/20000) + 1;
						}else{
							runningVolumeDef.animationType = AnimationComponentType.TRANSLATION;
							runningVolumeDef.variable = "engine_rpm_percent";
							runningVolumeDef.axis = new Point3d(0, (customSound.volumeMax - customSound.volumeIdle), 0);
							runningVolumeDef.offset = customSound.volumeIdle + (definition.engine.maxRPM < 15000 ? 500 : 2000)/definition.engine.maxRPM;
						}
						runningSound.volumeAnimations.add(runningVolumeDef);
						
						//Add pitch variable.
						JSONAnimationDefinition runningPitchDef = new JSONAnimationDefinition();
						if(customSound.pitchAdvanced){
							runningPitchDef.animationType = AnimationComponentType.ROTATION;
							runningPitchDef.variable = "engine_rpm";
							runningPitchDef.axis = new Point3d(-0.000001/(customSound.pitchLength/1000), 0, customSound.pitchCenter);
							runningPitchDef.offset = (customSound.pitchLength/20000) + 1;
						}else{
							runningPitchDef.animationType = AnimationComponentType.TRANSLATION;
							runningPitchDef.variable = "engine_rpm_percent";
							runningPitchDef.axis = new Point3d(0, (customSound.pitchMax - customSound.pitchIdle), 0);
							runningPitchDef.offset = customSound.pitchIdle + (definition.engine.maxRPM < 15000 ? 500 : 2000)/definition.engine.maxRPM;
						}
						runningSound.pitchAnimations.add(runningPitchDef);
						
						//Add the sound.
						definition.rendering.sounds.add(runningSound);
					}
					definition.engine.customSoundset = null;
				}else{
					JSONSound runningSound = new JSONSound();
					runningSound.name = definition.packID + ":" + definition.systemName + "_running";
					runningSound.looping = true;
					runningSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition runningVolumeDef = new JSONAnimationDefinition();
					runningVolumeDef.animationType = AnimationComponentType.VISIBILITY;
					runningVolumeDef.variable = "engine_powered";
					runningVolumeDef.clampMin = 1.0F;
					runningVolumeDef.clampMax = 1.0F;
					runningSound.activeAnimations.add(runningVolumeDef);
					
					runningSound.pitchAnimations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition runningPitchDef = new JSONAnimationDefinition();
					runningPitchDef.animationType = AnimationComponentType.TRANSLATION;
					runningPitchDef.variable = "engine_rpm";
					//Pitch should be 0.35 at idle, with a 0.35 increase for every 2500 RPM, or every 25000 RPM for jet (high-revving) engines by default.
					runningPitchDef.axis = new Point3d(0, 0.35/(definition.engine.maxRPM < 15000 ? 500 : 5000), 0);
					runningPitchDef.offset = 0.35F;
					runningSound.pitchAnimations.add(runningPitchDef);
					definition.rendering.sounds.add(runningSound);
				}
				
				
			}else if(definition.gun != null){
				if(definition.rendering == null){
					definition.rendering = new JSONRendering();
				}
				if(definition.rendering.sounds == null){
					definition.rendering.sounds = new ArrayList<JSONSound>();
				}
				
				JSONSound firingSound = new JSONSound();
				firingSound.name = definition.packID + ":" + definition.systemName + "_firing";
				firingSound.forceSound = true;
				firingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition firingDef = new JSONAnimationDefinition();
				firingDef.animationType = AnimationComponentType.VISIBILITY;
				firingDef.variable = "gun_fired";
				firingDef.clampMin = 1.0F;
				firingDef.clampMax = 1.0F;
				firingSound.activeAnimations.add(firingDef);
				definition.rendering.sounds.add(firingSound);
				
				JSONSound reloadingSound = new JSONSound();
				reloadingSound.name = definition.packID + ":" + definition.systemName + "_reloading";
				reloadingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition reloadingDef = new JSONAnimationDefinition();
				reloadingDef.animationType = AnimationComponentType.VISIBILITY;
				reloadingDef.variable = "gun_reload";
				reloadingDef.clampMin = 1.0F;
				reloadingDef.clampMax = 1.0F;
				reloadingSound.activeAnimations.add(reloadingDef);
				definition.rendering.sounds.add(reloadingSound);
			}else if(definition.ground != null){
				if(definition.rendering == null){
					definition.rendering = new JSONRendering();
				}
				if(definition.rendering.sounds == null){
					definition.rendering.sounds = new ArrayList<JSONSound>();
				}
				
				JSONSound blowoutSound = new JSONSound();
				blowoutSound.name = MasterLoader.resourceDomain + ":wheel_blowout";
				blowoutSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition blowoutDef = new JSONAnimationDefinition();
				blowoutDef.animationType = AnimationComponentType.VISIBILITY;
				blowoutDef.variable = "ground_isflat";
				blowoutDef.clampMin = 1.0F;
				blowoutDef.clampMax = 1.0F;
				blowoutSound.activeAnimations.add(blowoutDef);
				definition.rendering.sounds.add(blowoutSound);
				
				JSONSound strikingSound = new JSONSound();
				strikingSound.name = MasterLoader.resourceDomain + ":" + "wheel_striking";
				strikingSound.forceSound = true;
				strikingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition strikingDef = new JSONAnimationDefinition();
				strikingDef.animationType = AnimationComponentType.VISIBILITY;
				strikingDef.variable = "ground_contacted";
				strikingDef.clampMin = 1.0F;
				strikingDef.clampMax = 1.0F;
				strikingSound.activeAnimations.add(strikingDef);
				definition.rendering.sounds.add(strikingSound);
				
				JSONSound skiddingSound = new JSONSound();
				skiddingSound.name = MasterLoader.resourceDomain + ":" + "wheel_skidding";
				skiddingSound.looping = true;
				skiddingSound.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
		if(definition.rendering != null && definition.rendering.particles == null){
			definition.rendering.particles = new ArrayList<JSONParticle>();
			if(definition.engine != null){
				//Small overheat.
				JSONParticle particleDef = new JSONParticle();
				particleDef.type = ParticleType.SMOKE;
				particleDef.color = "#000000";
				particleDef.spawnEveryTick = true;
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				particleDef.color = "#000000";
				particleDef.spawnEveryTick = true;
				particleDef.quantity = 1;
				particleDef.scale = 2.5F;
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				particleDef.color = "#000000";
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
				particleDef.color = "#FF0000";
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
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
			}else if(definition.ground != null){
				//Contact smoke.
				JSONParticle particleDef = new JSONParticle();
				particleDef.type = ParticleType.SMOKE;
				particleDef.color = "#FFFFFF";
				particleDef.quantity = 4;
				particleDef.initialVelocity = new Point3d(0, 0.15, 0);
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
				activeAnimation.animationType = AnimationComponentType.VISIBILITY;
				activeAnimation.variable = "ground_contacted";
				activeAnimation.clampMin = 1.0F;
				activeAnimation.clampMax = 1.0F;
				particleDef.activeAnimations.add(activeAnimation);
				definition.rendering.particles.add(particleDef);
				
				//Burnout smoke.
				particleDef = new JSONParticle();
				particleDef.type = ParticleType.SMOKE;
				particleDef.color = "#FFFFFF";
				particleDef.quantity = 4;
				particleDef.initialVelocity = new Point3d(0, 0.15, 0);
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				activeAnimation = new JSONAnimationDefinition();
				activeAnimation.animationType = AnimationComponentType.VISIBILITY;
				activeAnimation.variable = "ground_slipping";
				activeAnimation.clampMin = 1.0F;
				activeAnimation.clampMax = 1.0F;
				particleDef.activeAnimations.add(activeAnimation);
				definition.rendering.particles.add(particleDef);
				
				//Wheel dirt.
				particleDef = new JSONParticle();
				particleDef.type = ParticleType.BREAK;
				particleDef.color = "#FFFFFF";
				particleDef.quantity = 1;
				particleDef.initialVelocity = new Point3d(0, 0.2, -0.2);
				particleDef.activeAnimations = new ArrayList<JSONAnimationDefinition>();
				activeAnimation = new JSONAnimationDefinition();
				activeAnimation.animationType = AnimationComponentType.VISIBILITY;
				activeAnimation.variable = "ground_slipping";
				activeAnimation.clampMin = 1.0F;
				activeAnimation.clampMax = 1.0F;
				particleDef.activeAnimations.add(activeAnimation);
				definition.rendering.particles.add(particleDef);
			}
		}
	}
	
	private static void performInstrumentLegacyCompats(JSONInstrument definition){
		//Check if we have any old component definitions.  If so, we need
		//to make all textures light-up.
		boolean oldDefinition = false;
		for(JSONInstrument.Component component : definition.components){
			if(component.rotationVariable != null || component.translationVariable != null){
				oldDefinition = true;
			}
		}
		//Convert any old component definitions to the new style.
		for(JSONInstrument.Component component : definition.components){
			if(oldDefinition){
				component.lightUpTexture = true;
				component.overlayTexture = component.lightOverlay;
				component.lightOverlay = false;
			}
			if(component.rotationVariable != null){
				component.animations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition animation = new JSONAnimationDefinition();
				animation.animationType = AnimationComponentType.ROTATION;
				animation.variable = component.rotationVariable;
				animation.centerPoint = new Point3d();
				animation.axis = new Point3d(0, 0, component.rotationFactor);
				animation.offset = component.rotationOffset;
				animation.clampMin = component.rotationClampMin;
				animation.clampMax = component.rotationClampMax;
				if(component.rotationFactor < 0){
					animation.offset = -animation.offset;
					float temp = animation.clampMin;
					animation.clampMin = -animation.clampMax;
					animation.clampMax = -temp;
				}
				animation.absolute = component.rotationAbsoluteValue;
				component.animations.add(animation);
				component.rotationVariable = null;
				component.rotationFactor = 0;
				component.rotationOffset = 0;
				component.rotationClampMin = 0;
				component.rotationClampMax = 0;
				component.rotationAbsoluteValue = false;
			}
			if(component.translationVariable != null){
				if(component.animations == null){
					component.animations = new ArrayList<JSONAnimationDefinition>();
				}
				JSONAnimationDefinition animation = new JSONAnimationDefinition();
				animation.animationType = AnimationComponentType.TRANSLATION;
				animation.variable = component.translationVariable;
				if(component.translateHorizontal){
					animation.axis = new Point3d(component.translationFactor, 0, 0);
				}else{
					animation.axis = new Point3d(0, component.translationFactor, 0);
				}
				animation.clampMin = component.translationClampMin;
				animation.clampMax = component.translationClampMax;
				animation.absolute = component.translationAbsoluteValue;
				//If we were rotating the texture, and not the window, we need to do the translation first.
				//This is due to how the old animation system did rendering.
				if(component.rotateWindow){
					component.animations.add(animation);
				}else{
					component.animations.add(0, animation);
				}
				component.translateHorizontal = false;
				component.translationVariable = null;
				component.translationFactor = 0;
				component.translationClampMin = 0;
				component.translationClampMax = 0;
				component.translationAbsoluteValue = false;
			}
		}
	}
	
	private static void performPoleLegacyCompats(JSONPoleComponent definition){
		//If we are a pole without a definition, add one so we don't crash on other systems.
		if(definition.definitions == null){
			definition.definitions = new ArrayList<JSONSubDefinition>();
			JSONSubDefinition subDefinition = new JSONSubDefinition();
			subDefinition.extraMaterials = new ArrayList<String>();
			subDefinition.name = definition.general.name;
			subDefinition.subName = "";
			definition.definitions.add(subDefinition);
		}
				
		//If we are a sign using the old textlines, update them.
		if(definition.general.textLines != null){
			definition.general.textObjects = new ArrayList<JSONText>();
			for(TextLine line : definition.general.textLines){
				JSONText object = new JSONText();
				object.color = line.color;
				object.scale = line.scale;
				object.maxLength = line.characters;
				object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.01D);
				object.rot = new Point3d();
				object.fieldName = "TextLine #" + (definition.general.textObjects.size() + 1);
				definition.general.textObjects.add(object);
			}
			definition.general.textLines = null;
		}
		
		//If we are a sign using the old textObjects location, move it.
		if(definition.general.textObjects != null){
			if(definition.rendering == null){
				definition.rendering = new JSONRendering();
			}
			definition.rendering.textObjects = definition.general.textObjects;
			definition.general.textObjects = null;
		}
		
		//Set default text to blank for sign text objects.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(JSONText text : definition.rendering.textObjects){
				if(text.defaultText == null){
					text.defaultText = "";
				}
			}
		}
		
		//Move pole general properties to new location.
		if(definition.general.type != null){
			definition.pole = definition.new JSONPoleGeneric();
			definition.pole.type = PoleComponentType.valueOf(definition.general.type.toUpperCase());
			definition.general.type = null;
			definition.pole.radius = definition.general.radius;
			definition.general.radius = 0;
		}
		
		//Create a animation set for core poles if they don't have one and use the old auto-render systems.
		if(definition.pole.type.equals(PoleComponentType.CORE) && definition.rendering == null){
			definition.rendering = new JSONRendering();
			definition.rendering.animatedObjects = new ArrayList<JSONAnimatedObject>();
			for(Axis axis : Axis.values()){
				if(!axis.equals(Axis.NONE)){
					JSONAnimatedObject connectorModelObject = new JSONAnimatedObject();
					connectorModelObject.objectName = axis.name().toLowerCase();
					connectorModelObject.animations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition connectorVisibilityInhibitor = new JSONAnimationDefinition();
					
					connectorVisibilityInhibitor.animationType = AnimationComponentType.INHIBITOR;
					connectorVisibilityInhibitor.variable = "solid_present_" + axis.name().toLowerCase();
					connectorVisibilityInhibitor.clampMin = 1.0F;
					connectorVisibilityInhibitor.clampMax = 1.0F;
					connectorModelObject.animations.add(connectorVisibilityInhibitor);
					
					JSONAnimationDefinition connectorVisibility = new JSONAnimationDefinition();
					connectorVisibility.animationType = AnimationComponentType.VISIBILITY;
					connectorVisibility.variable = "neighbor_present_" + axis.name().toLowerCase();
					connectorVisibility.clampMin = 1.0F;
					connectorVisibility.clampMax = 1.0F;
					connectorModelObject.animations.add(connectorVisibility);
					definition.rendering.animatedObjects.add(connectorModelObject);
					
					
					JSONAnimatedObject solidModelObject = new JSONAnimatedObject();
					solidModelObject.objectName = axis.name().toLowerCase() + "_solid";
					solidModelObject.animations = new ArrayList<JSONAnimationDefinition>();
					JSONAnimationDefinition solidVisibility = new JSONAnimationDefinition();
					solidVisibility.animationType = AnimationComponentType.VISIBILITY;
					solidVisibility.variable = "solid_present_" + axis.name().toLowerCase();
					solidVisibility.clampMin = 1.0F;
					solidVisibility.clampMax = 1.0F;
					solidModelObject.animations.add(solidVisibility);
					definition.rendering.animatedObjects.add(solidModelObject);
					
					if(axis.equals(Axis.UP) || axis.equals(Axis.DOWN)){
						JSONAnimatedObject slabModelObject = new JSONAnimatedObject();
						slabModelObject.objectName = axis.name().toLowerCase() + "_slab";
						slabModelObject.animations = new ArrayList<JSONAnimationDefinition>();
						JSONAnimationDefinition slabVisibility = new JSONAnimationDefinition();
						slabVisibility.animationType = AnimationComponentType.VISIBILITY;
						slabVisibility.variable = "slab_present_" + axis.name().toLowerCase();
						slabVisibility.clampMin = 1.0F;
						slabVisibility.clampMax = 1.0F;
						slabModelObject.animations.add(slabVisibility);
						definition.rendering.animatedObjects.add(slabModelObject);
					}
				}
			}
		}
	}
	
	private static void performDecorLegacyCompats(JSONDecor definition){
		//Move decor general properties to new location.
		if(definition.decor == null){
			definition.decor = definition.new JSONDecorGeneric();
			if(definition.general.type != null){
				definition.decor.type = DecorComponentType.valueOf(definition.general.type.toUpperCase());
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
		if(definition.decor.type == null){
			definition.decor.type = DecorComponentType.GENERIC;
		}
		
		//If we are a decor without a definition, add one so we don't crash on other systems.
		if(definition.definitions == null){
			definition.definitions = new ArrayList<JSONSubDefinition>();
			JSONSubDefinition subDefinition = new JSONSubDefinition();
			subDefinition.extraMaterials = new ArrayList<String>();
			subDefinition.name = definition.general.name;
			subDefinition.subName = "";
			definition.definitions.add(subDefinition);
		}
				
		//If we are a decor using the old textlines, update them.
		if(definition.general.textLines != null){
			definition.general.textObjects = new ArrayList<JSONText>();
			int lineNumber = 0;
			for(TextLine line : definition.general.textLines){
				JSONText object = new JSONText();
				object.lightsUp = true;
				object.color = line.color;
				object.scale = line.scale;
				if(lineNumber++ < 3){
					object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.0001D);
					object.rot = new Point3d();
				}else{
					object.pos = new Point3d(line.xPos, line.yPos, line.zPos - 0.0001D);
					object.rot = new Point3d(0, 180, 0);
				}
				object.fieldName = "TextLine #" + (definition.general.textObjects.size() + 1);
				definition.general.textObjects.add(object);
			}
			 definition.general.textLines = null;
		}
		
		//If we are a sign using the old textObjects location, move it.
		if(definition.general.textObjects != null){
			if(definition.rendering == null){
				definition.rendering = new JSONRendering();
			}
			definition.rendering.textObjects = definition.general.textObjects;
			definition.general.textObjects = null;
		}
		
		//Set default text to blank for decor text objects.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(JSONText text : definition.rendering.textObjects){
				if(text.defaultText == null){
					text.defaultText = "";
				}
			}
		}
		
		//If we have crafting things in the decor, move them.
		if(definition.decor.items != null || definition.decor.itemTypes != null){
			definition.decor.crafting = new JSONCraftingBench();
			definition.decor.crafting.itemTypes = definition.decor.itemTypes;
			definition.decor.itemTypes = null;
			definition.decor.crafting.partTypes = definition.decor.partTypes;
			definition.decor.partTypes = null;
			definition.decor.crafting.items = definition.decor.items;
			definition.decor.items = null;
		}
	}
	
	private static void performItemLegacyCompats(JSONItem definition){
		//Move item type if required.
		if(definition.item == null){
			definition.item = definition.new JSONItemGeneric();
			if(definition.general.type != null){
				definition.item.type = ItemComponentType.valueOf(definition.general.type.toUpperCase());
				definition.general.type = null;
			}
		}
		
		//Set item type to NONE if null.
		if(definition.item.type == null){
			definition.item.type = ItemComponentType.NONE;
		}
		
		//Add blank fieldNames for booklets, as they shouldn't exist.
		if(definition.booklet != null){
			for(JSONText text : definition.booklet.titleText){
				text.fieldName = "";
			}
			for(BookletPage page : definition.booklet.pages){
				for(JSONText text : page.pageText){
					text.fieldName = "";
				}
			}
		}
	}
	
	private static void performSkinLegacyCompats(JSONSkin definition){
		//Move skin properties to new location, if we have them.
		if(definition.general.packID != null){
			definition.skin = definition.new Skin();
			definition.skin.packID = definition.general.packID;
			definition.general.packID = null;
			definition.skin.systemName = definition.general.systemName;
			definition.general.systemName = null;
		}
		//Make the materials empty, as the parser doesn't like them null.
		definition.general.materials = new ArrayList<String>();
	}
	
	private static void performVehiclePartDefLegacyCompats(JSONPartDefinition partDef){
		if(partDef.additionalPart != null){
			partDef.additionalParts = new ArrayList<JSONPartDefinition>();
			partDef.additionalParts.add(partDef.additionalPart);
			partDef.additionalPart = null;
		}
		if(partDef.linkedDoor != null){
			partDef.linkedDoors = new ArrayList<String>();
			partDef.linkedDoors.add(partDef.linkedDoor);
			partDef.linkedDoor = null;
		}
		if(partDef.exhaustPos != null){
			partDef.particleObjects = new ArrayList<JSONParticle>();
			for(int i=0; i<partDef.exhaustPos.length; i+=3){
				JSONParticle particle = new JSONParticle();
				particle.type = ParticleType.SMOKE;
				particle.pos = new Point3d(partDef.exhaustPos[i], partDef.exhaustPos[i+1], partDef.exhaustPos[i+2]);
				particle.velocityVector = new Point3d(partDef.exhaustVelocity[i], partDef.exhaustVelocity[i+1], partDef.exhaustVelocity[i+2]);
				particle.scale = 1.0F;
				particle.color = "#D9D9D9";
				particle.transparency = 0.25F;
				particle.toTransparency = 0.25F;
				partDef.particleObjects.add(particle);
			}
			partDef.exhaustPos = null;
			partDef.exhaustVelocity = null;
		}
		if(partDef.exhaustObjects != null) {
			partDef.particleObjects = new ArrayList<JSONParticle>();
			for(ExhaustObject exhaust : partDef.exhaustObjects) {
				JSONParticle particle = new JSONParticle();
				particle.type = ParticleType.SMOKE;
				particle.pos = exhaust.pos;
				particle.velocityVector = exhaust.velocity;
				particle.scale = exhaust.scale;
				particle.color = "#D9D9D9";
				particle.transparency = 0.25F;
				particle.toTransparency = 0.25F;
				partDef.particleObjects.add(particle);
			}
			partDef.exhaustObjects = null;
		}
		if(partDef.rotationVariable != null){
			partDef.animations = new ArrayList<JSONAnimationDefinition>();
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
		if(partDef.translationVariable != null){
			if(partDef.animations == null){
				partDef.animations = new ArrayList<JSONAnimationDefinition>();
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
		for(byte i=0; i<partDef.types.size(); ++i){
			String partName = partDef.types.get(i);
			if(partName.equals("wheel") || partName.equals("skid") || partName.equals("pontoon") || partName.equals("tread")){
				if(partName.equals("tread")){
					partDef.turnsWithSteer = true;
				}
				partDef.types.set(i, "ground_" + partName);
			}else if(partName.equals("crate") || partName.equals("barrel") || partName.equals("crafting_table") || partName.equals("furnace") || partName.equals("brewing_stand")){
				partDef.types.set(i, "interactable_" + partName);
				partDef.minValue = 0;
				partDef.maxValue = 1;
			}else if(partName.equals("fertilizer") || partName.equals("harvester") || partName.equals("planter") || partName.equals("plow")){
				partDef.types.set(i, "effector_" + partName);
			}else if(partName.equals("custom")){
				partDef.types.set(i, "generic");
			}
			
			//If we have ground devices that are wheels, but no animations, add those automatically.
			if(partName.equals("ground_wheel") && partDef.turnsWithSteer && partDef.animations == null){
				partDef.animations = new ArrayList<JSONAnimationDefinition>();
				JSONAnimationDefinition animation = new JSONAnimationDefinition();
				animation.centerPoint = new Point3d();
				animation.axis = new Point3d(0, partDef.pos.z > 0 ? -1 : 1, 0);
				animation.animationType = AnimationComponentType.ROTATION;
				animation.variable = "rudder";
				partDef.animations.add(animation);
			}
			
			//If we have additional parts, check those too.
			if(partDef.additionalParts != null){
				for(JSONPartDefinition additionalPartDef : partDef.additionalParts){
					performVehiclePartDefLegacyCompats(additionalPartDef);
				}
			}
		}
	}
	
	private static void performVehicleConnectionLegacyCompats(AJSONInteractableEntity interactableDef){
		if(interactableDef.connections != null){
			interactableDef.connectionGroups = new ArrayList<JSONConnectionGroup>();
			JSONConnectionGroup hitchGroup = null;
			JSONConnectionGroup hookupGroup = null;
			for(JSONConnection connection : interactableDef.connections){
				if(connection.hookup){
					if(hookupGroup == null){
						hookupGroup = new JSONConnectionGroup();
						hookupGroup.connections = new ArrayList<JSONConnection>();
						hookupGroup.groupName = "HOOKUP";
						hookupGroup.hookup = true;
					}
					connection.hookup = false;
					hookupGroup.connections.add(connection);
				}else{
					if(hitchGroup == null){
						hitchGroup = new JSONConnectionGroup();
						hitchGroup.connections = new ArrayList<JSONConnection>();
						hitchGroup.groupName = "TRAILER";
						hitchGroup.canIntiateConnections = true;
					}
					hitchGroup.connections.add(connection);
				}
			}
			if(hookupGroup != null){
				interactableDef.connectionGroups.add(hookupGroup);
			}
			if(hitchGroup != null){
				interactableDef.connectionGroups.add(hitchGroup);
			}
			interactableDef.connections = null;
		}
	}
    
    private static void performAnimationLegacyCompats(JSONRendering rendering){
    	if(rendering.textMarkings != null){
    		rendering.textObjects = new ArrayList<JSONText>();
    		for(JSONRendering.VehicleDisplayText marking : rendering.textMarkings){
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
    	if(rendering.rotatableModelObjects != null){
    		if(rendering.animatedObjects == null){
    			rendering.animatedObjects = new ArrayList<JSONAnimatedObject>();
    		}
    		for(JSONRendering.VehicleRotatableModelObject rotatable : rendering.rotatableModelObjects){
    			JSONAnimatedObject object = null;
    			for(JSONAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(rotatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = new JSONAnimatedObject();
    				object.objectName = rotatable.partName;
    				object.animations = new ArrayList<JSONAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			JSONAnimationDefinition animation = new JSONAnimationDefinition();
    			animation.animationType = AnimationComponentType.ROTATION;
    	    	animation.variable = rotatable.rotationVariable;
    	    	animation.centerPoint = rotatable.rotationPoint;
    	    	animation.axis = rotatable.rotationAxis;
    	    	animation.clampMin = rotatable.rotationClampMin;
    	    	animation.clampMax = rotatable.rotationClampMax;
    	    	animation.absolute = rotatable.absoluteValue;
    	    	if(rotatable.rotationVariable.equals("steering_wheel")){
    	    		animation.variable = "rudder";
    	    		animation.axis.multiply(-1D);
    	    	}
    	    	if(rotatable.rotationVariable.equals("door")){
    	    		animation.duration = 30;
    	    	}
    	    	object.animations.add(animation);
			}
    		rendering.rotatableModelObjects = null;
    	}
    	if(rendering.translatableModelObjects != null){
    		if(rendering.animatedObjects == null){
    			rendering.animatedObjects = new ArrayList<JSONAnimatedObject>();
    		}
    		for(JSONRendering.VehicleTranslatableModelObject translatable : rendering.translatableModelObjects){
    			JSONAnimatedObject object = null;
    			for(JSONAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(translatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = new JSONAnimatedObject();
    				object.objectName = translatable.partName;
    				object.animations = new ArrayList<JSONAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			JSONAnimationDefinition animation = new JSONAnimationDefinition();
    			animation.animationType = AnimationComponentType.TRANSLATION;
    	    	animation.variable = translatable.translationVariable;
    	    	animation.axis = translatable.translationAxis;
    	    	animation.clampMin = translatable.translationClampMin;
    	    	animation.clampMax = translatable.translationClampMax;
    	    	animation.absolute = translatable.absoluteValue;
    	    	if(translatable.translationVariable.equals("steering_wheel")){
    	    		animation.variable = "rudder";
    	    		animation.axis.multiply(-1D);
    	    	}
    	    	if(translatable.translationVariable.equals("door")){
    	    		animation.duration = 30;
    	    	}
    	    	object.animations.add(animation);
			}
    		rendering.translatableModelObjects = null;
    	}
    }
    
    private static void performLightLegacyCompats(AJSONMultiModelProvider definition){
		if(definition.rendering == null){
			definition.rendering = new JSONRendering();
		}
		if(definition.rendering.lightObjects == null){
			try{
				definition.rendering.lightObjects = new ArrayList<JSONLight>();
				Map<String, Float[][]> parsedModel = AModelParser.parseModel(definition.getModelLocation(definition.definitions.get(0).subName));
				for(String objectName : parsedModel.keySet()){
					if(objectName.contains("&")){
						JSONLight lightDef = new JSONLight();
						lightDef.objectName = objectName;
						lightDef.brightnessAnimations = new ArrayList<JSONAnimationDefinition>();
						lightDef.color = "#" + objectName.substring(objectName.indexOf('_') + 1, objectName.indexOf('_') + 7);
						lightDef.brightnessAnimations = new ArrayList<JSONAnimationDefinition>();
						
						//Add standard animation variable for light name.
						String lowerCaseName = objectName.toLowerCase();
						JSONAnimationDefinition activeAnimation = new JSONAnimationDefinition();
						activeAnimation.axis = new Point3d(0, 1, 0);
						if(lowerCaseName.contains("brakelight")){
							activeAnimation.variable = "brake";
						}else if(lowerCaseName.contains("backuplight")){
							activeAnimation.variable = "engine_reversed_1";
						}else if(lowerCaseName.contains("daytimelight")){
							activeAnimation.variable = "engines_on";
						}else if(lowerCaseName.contains("navigationlight")){
							activeAnimation.variable = "navigation_light";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasNavLights = true;
						}else if(lowerCaseName.contains("strobelight")){
							activeAnimation.variable = "strobe_light";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasStrobeLights = true;
						}else if(lowerCaseName.contains("taxilight")){
							activeAnimation.variable = "taxi_light";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasTaxiLights = true;
						}else if(lowerCaseName.contains("landinglight")){
							activeAnimation.variable = "landing_light";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasLandingLights = true;
						}else if(lowerCaseName.contains("runninglight")){
							activeAnimation.variable = "running_light";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasRunningLights = true;
						}else if(lowerCaseName.contains("headlight")){
							activeAnimation.variable = "headlight";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasHeadlights = true;
						}else if(lowerCaseName.contains("leftturnlight")){
							activeAnimation.variable = "left_turn_signal";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasTurnSignals = true;
						}else if(lowerCaseName.contains("rightturnlight")){
							activeAnimation.variable = "right_turn_signal";
							if(definition instanceof JSONVehicle)((JSONVehicle) definition).motorized.hasTurnSignals = true;
						}else if(lowerCaseName.contains("emergencylight")){
							activeAnimation.variable = "EMERLTS";
							if(definition.rendering.customVariables == null){
								definition.rendering.customVariables = new ArrayList<String>();
							}
							if(definition instanceof JSONVehicle)definition.rendering.customVariables.add("EMERLTS");
						}else if(lowerCaseName.contains("stoplight") || lowerCaseName.contains("cautionlight") || lowerCaseName.contains("golight")){
							//Traffic signal detected.  Get light name for variable.
							String[] lightNames = lowerCaseName.split("light");
							lightNames[0] = lightNames[0].replace("&", "");
							activeAnimation.variable = lightNames[0] + "_" + "light";
							if(lightNames.length > 2){
								activeAnimation.variable += "_" + lightNames[2];
							}
							
							//If the light is a stop light, create a cycle for it for un-linked states.
							if(lightNames[0].equals("stop")){
								JSONAnimationDefinition cycleInhibitor = new JSONAnimationDefinition();
								cycleInhibitor.animationType = AnimationComponentType.INHIBITOR;
								cycleInhibitor.variable = "linked";
								cycleInhibitor.clampMin = 1.0F;
								cycleInhibitor.clampMax = 1.0F;
								lightDef.brightnessAnimations.add(cycleInhibitor);
								
								JSONAnimationDefinition cycleAnimation = new JSONAnimationDefinition();
								cycleAnimation.animationType = AnimationComponentType.TRANSLATION;
								cycleAnimation.variable = "0_10_10_cycle";
								cycleAnimation.axis = new Point3d(0, 1, 0);
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
						
						if(activeAnimation.variable != null){
							activeAnimation.animationType = AnimationComponentType.TRANSLATION;
							lightDef.brightnessAnimations.add(activeAnimation);
						}
						
						//If we are a part or vehicle, add electric power.
						if(definition instanceof JSONVehicle || definition instanceof JSONPart){
							JSONAnimationDefinition electricAnimation = new JSONAnimationDefinition();
							electricAnimation.animationType = AnimationComponentType.TRANSLATION;
							electricAnimation.variable = "electric_power";
							electricAnimation.axis = new Point3d(0, 1/0.75D/12D, 0);
							electricAnimation.offset = -0.15F;
							electricAnimation.clampMin = 0.0001F;
							electricAnimation.clampMax = 1.0F;
							lightDef.brightnessAnimations.add(electricAnimation);
						}
						
						//If we are a decor, add redstone power.
						if(definition instanceof JSONDecor){
							JSONAnimationDefinition redstoneAnimation = new JSONAnimationDefinition();
							redstoneAnimation.animationType = AnimationComponentType.TRANSLATION;
							redstoneAnimation.variable = "redstone_level";
							redstoneAnimation.axis = new Point3d(0, -1/15D, 0);
							lightDef.brightnessAnimations.add(redstoneAnimation);
						}
						
						//Get flashing cycle rate and convert to cycle variable if required.
						//Look at flash bits from right to left until we hit one that's not on.  Count how many ticks are on and use that for cycle.
						int flashBits = Integer.decode("0x" + objectName.substring(objectName.indexOf('_', objectName.indexOf('_') + 7) + 1, objectName.lastIndexOf('_')));
						int ticksTillOn = 0;
						int ticksOn = 0;
						boolean foundOn = false;
						for(byte i=0; i<20; ++i){
							if(((flashBits >> i) & 1) != 1){
								if(foundOn){
									break;
								}
							}else if(!foundOn){
								foundOn = true;
								ticksTillOn = i;
							}
							++ticksOn;
						}
						if((ticksOn - ticksTillOn) != 20){
							JSONAnimationDefinition cycleAnimation = new JSONAnimationDefinition();
							cycleAnimation.animationType = AnimationComponentType.TRANSLATION;
							cycleAnimation.variable = ticksTillOn + "_" + ticksOn + "_" + (20-ticksOn-ticksTillOn) + "_cycle";
							cycleAnimation.axis = new Point3d(0, 1, 0);
							lightDef.brightnessAnimations.add(cycleAnimation);
						}
						
						
						String lightProperties = objectName.substring(objectName.lastIndexOf('_') + 1);
						boolean renderFlare = Integer.valueOf(lightProperties.substring(0, 1)) > 0;
						lightDef.emissive = Integer.valueOf(lightProperties.substring(1, 2)) > 0;
						lightDef.covered = Integer.valueOf(lightProperties.substring(2, 3)) > 0;
						boolean renderBeam = lightProperties.length() == 4 ? Integer.valueOf(lightProperties.substring(3)) > 0 : (lowerCaseName.contains("headlight") || lowerCaseName.contains("landinglight") || lowerCaseName.contains("taxilight") || lowerCaseName.contains("streetlight"));
						
						if(renderFlare || renderBeam){
							if(lightDef.blendableComponents == null){
								lightDef.blendableComponents = new ArrayList<JSONLightBlendableComponent>();
							}
							
							Float[][] masterVertices = parsedModel.get(objectName);
							for(int i=0; i<masterVertices.length/6; ++i){
								double minX = 999;
								double maxX = -999;
								double minY = 999;
								double maxY = -999;
								double minZ = 999;
								double maxZ = -999;
								for(byte j=0; j<6; ++j){
									Float[] masterVertex = masterVertices[i*6 + j];
									minX = Math.min(masterVertex[0], minX);
									maxX = Math.max(masterVertex[0], maxX);
									minY = Math.min(masterVertex[1], minY);
									maxY = Math.max(masterVertex[1], maxY);
									minZ = Math.min(masterVertex[2], minZ);
									maxZ = Math.max(masterVertex[2], maxZ);
								}
								JSONLightBlendableComponent blendable = lightDef.new JSONLightBlendableComponent();
								if(renderFlare){
									blendable.flareHeight = (float) (3*Math.max(Math.max((maxX - minX), (maxY - minY)), (maxZ - minZ)));
									blendable.flareWidth = blendable.flareHeight;
								}
								if(renderBeam){
									blendable.beamDiameter = (float) Math.max(Math.max(maxX - minX, maxZ - minZ), maxY - minY)*64F;
									blendable.beamLength = blendable.beamDiameter*3;
								}
								blendable.pos = new Point3d(minX + (maxX - minX)/2D, minY + (maxY - minY)/2D, minZ + (maxZ - minZ)/2D);;
								blendable.axis = new Point3d(masterVertices[i*6][5], masterVertices[i*6][6], masterVertices[i*6][7]);
								
								lightDef.blendableComponents.add(blendable);
							}
						}
						
						definition.rendering.lightObjects.add(lightDef);
					}
				}
			}catch(Exception e){
				InterfaceCore.logError("Could not do light-based legacy compats on " + definition.packID + ":" + definition.systemName + ".  Lights will likely not be present on this model.");
				InterfaceCore.logError(e.getMessage());
			}
    	}
    }
}
