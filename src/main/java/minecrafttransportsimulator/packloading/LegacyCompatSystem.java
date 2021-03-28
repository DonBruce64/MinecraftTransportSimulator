package minecrafttransportsimulator.packloading;

import java.util.ArrayList;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.items.instances.ItemDecor.DecorComponentType;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONItem.General.TextLine;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONConnection;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPart.EffectorComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.InteractableComponentType;
import minecrafttransportsimulator.jsondefs.JSONPart.JSONPartEngine.EngineSound;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition.ExhaustObject;
import minecrafttransportsimulator.jsondefs.JSONParticleObject;
import minecrafttransportsimulator.jsondefs.JSONParticleObject.ParticleComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;

/**
 * Class responsible for applying legacy compat code to JSONs.  All legacy compat code should
 * go here.  Once a definition calls methods in this class, it can be assumed to be in the most
 * modern form possible and ready for all the current systems.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public final class LegacyCompatSystem{
	
	public static void performLegacyCompats(AJSONItem definition, String packID, String systemName){
		if(definition instanceof JSONVehicle){
			performVehicleLegacyCompats((JSONVehicle) definition);
		}else if(definition instanceof JSONPart){
			performPartLegacyCompats((JSONPart) definition, packID, systemName);
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
		
		for(JSONPartDefinition partDef : definition.parts){
			try{
				performVehiclePartDefLegacyCompats(partDef);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on part entry #" + (definition.parts.indexOf(partDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
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
		
		//Check if we didn't specify a braking force.
		if(definition.motorized.brakingFactor == 0){
			definition.motorized.brakingFactor = 1.0F;
		}
		
		try{
			performAnimationLegacyCompats(definition.rendering);
		}catch(Exception e){
			throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
		}
    }
	
	private static void performPartLegacyCompats(JSONPart definition, String packID, String systemName){
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
				definition.engine.bypassRatio = definition.engine.gearRatios[0];
				definition.engine.gearRatios[0] = 1.0F;
			}
			
			//If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
			if(definition.engine.gearRatios.length == 1){
				definition.engine.propellerRatio = 1/definition.engine.gearRatios[0];
				definition.engine.gearRatios = new float[]{-1, 0, 1};
			}
			
			//If our shiftSpeed is 0, we are a legacy engine that didn't set a shift speed.
			if(definition.engine.shiftSpeed == 0){
				definition.engine.shiftSpeed = 20;
			}
			//If our revResistance is 0, we are a legacy engine that didn't set a rev Resistance.
			if (definition.engine.revResistance == 0){
				definition.engine.revResistance = 10;
			}
		}else if(definition.bullet != null) {
			if (definition.bullet.type != null) {
				definition.bullet.types = new ArrayList<String>();
				definition.bullet.types.add(definition.bullet.type);
			}
			if (definition.bullet.particleObjects != null) {
				for (JSONParticleObject particle : definition.bullet.particleObjects) {
					if(particle.velocityVector == null) {
						particle.velocityVector = new Point3d(0, 0, -particle.velocity);
					}
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
					definition.effector.blocksWide = 1;
					break;
				}case("harvester"):{
					definition.generic.type = "effector_harvester";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.HARVESTER;
					definition.effector.blocksWide = 1;
					break;
				}case("planter"):{
					definition.generic.type = "effector_planter";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.PLANTER;
					definition.effector.blocksWide = 1;
					break;
				}case("plow"):{
					definition.generic.type = "effector_plow";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = EffectorComponentType.PLOW;
					definition.effector.blocksWide = 1;
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
				startingSound.name = packID + ":" + systemName + "_starting";
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
				stoppingSound.name = packID + ":" + systemName + "_stopping";
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
				sputteringSound.name = packID + ":" + systemName + "_sputter";
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
				crankingSound.name = packID + ":" + systemName + "_cranking";
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
					runningSound.name = packID + ":" + systemName + "_running";
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
				firingSound.name = packID + ":" + systemName + "_firing";
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
				reloadingSound.name = packID + ":" + systemName + "_reloading";
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
			partDef.particleObjects = new ArrayList<JSONParticleObject>();
			for(int i=0; i<partDef.exhaustPos.length; i+=3){
				JSONParticleObject particle = new JSONParticleObject();
				particle.type = ParticleComponentType.SMOKE;
				particle.pos = new Point3d(partDef.exhaustPos[i], partDef.exhaustPos[i+1], partDef.exhaustPos[i+2]);
				particle.velocityVector = new Point3d(partDef.exhaustVelocity[i], partDef.exhaustVelocity[i+1], partDef.exhaustVelocity[i+2]);
				particle.scale = 1.0F;
				particle.quantity = 1;
				particle.color = "#D9D9D9";
				particle.toColor = "#D9D9D9";
				particle.transparency = 0.25F;
				particle.toTransparency = 0.25F;
				partDef.particleObjects.add(particle);
			}
			partDef.exhaustPos = null;
			partDef.exhaustVelocity = null;
		}
		if(partDef.exhaustObjects != null) {
			partDef.particleObjects = new ArrayList<JSONParticleObject>();
			for(ExhaustObject exhaust : partDef.exhaustObjects) {
				JSONParticleObject particle = new JSONParticleObject();
				particle.type = ParticleComponentType.SMOKE;
				particle.pos = exhaust.pos;
				particle.velocityVector = exhaust.velocity;
				particle.scale = exhaust.scale;
				particle.quantity = 1;
				particle.color = "#D9D9D9";
				particle.toColor = "#D9D9D9";
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
}
