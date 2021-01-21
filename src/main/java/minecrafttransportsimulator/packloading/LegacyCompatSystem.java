package minecrafttransportsimulator.packloading;

import java.util.ArrayList;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition.AnimationComponentType;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONItem.JSONBooklet.BookletPage;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONParticleObject;
import minecrafttransportsimulator.jsondefs.JSONParticleObject.ParticleComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleConnection;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart.ExhaustObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;

/**
 * Class responsible for applying legacy compat code to JSONs.  All legacy compat code should
 * go here.  Once a definition calls methods in this class, it can be assumed to be in the most
 * modern form possible and ready for all the current systems.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public final class LegacyCompatSystem{
	
	public static void performLegacyCompats(AJSONItem<?> definition){
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
	}
	
	private static void performVehicleLegacyCompats(JSONVehicle definition){
		//Move vehicle parameters to the motorized section.
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
				definition.general.isAircraft = true;
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
			definition.connections = new ArrayList<VehicleConnection>();
			for(String hitchName : definition.motorized.hitchTypes){
				VehicleConnection connection = definition.new VehicleConnection();
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
				definition.connections = new ArrayList<VehicleConnection>();
			}
			VehicleConnection connection = definition.new VehicleConnection();
			connection.hookup = true;
			connection.type = definition.motorized.hookupType;
			connection.pos = definition.motorized.hookupPos;
			definition.connections.add(connection);
			definition.motorized.hookupType = null;
			definition.motorized.hookupPos = null;
		}
		
		for(VehiclePart partDef : definition.parts){
			try{
				performVehiclePartDefLegacyCompats(partDef);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on part entry #" + (definition.parts.indexOf(partDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
		}
		
		if(definition.rendering != null){
			try{
				performAnimationLegacyCompats(definition.rendering);
			}catch(Exception e){
				throw new NullPointerException("Could not perform Legacy Compats on rendering section due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
			}
		}
    }
	
	private static void performPartLegacyCompats(JSONPart definition){
		//If we are a part without a definition, add one so we don't crash on other systems.
		if(definition.definitions == null){
			definition.definitions = new ArrayList<JSONSubDefinition>();
			JSONSubDefinition subDefinition = new JSONSubDefinition();
			subDefinition.extraMaterials = new ArrayList<String>();
			subDefinition.name = definition.general.name;
			subDefinition.subName = "";
			definition.definitions.add(subDefinition);
		}
		
		if(definition.engine != null){
			//If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
			if(definition.general.type.equals("engine_jet") && definition.engine.jetPowerFactor == 0){
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
			switch(definition.general.type){
				case("wheel"):{
					definition.general.type = "ground_" + definition.general.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.isWheel = true;
					definition.ground.width = definition.wheel.diameter/2F;
					definition.ground.height = definition.wheel.diameter;
					definition.ground.lateralFriction = definition.wheel.lateralFriction;
					definition.ground.motiveFriction = definition.wheel.motiveFriction;
					definition.wheel = null;
					break;
				}case("skid"):{
					definition.general.type = "ground_" + definition.general.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.width = definition.skid.width;
					definition.ground.height = definition.skid.width;
					definition.ground.lateralFriction = definition.skid.lateralFriction;
					definition.skid = null;
					break;
				}case("pontoon"):{
					definition.general.type = "ground_" + definition.general.type;
					definition.ground = definition.new JSONPartGroundDevice();
					definition.ground.canFloat = true;
					definition.ground.width = definition.pontoon.width;
					definition.ground.height = definition.pontoon.width;
					definition.ground.lateralFriction = definition.pontoon.lateralFriction;
					definition.ground.extraCollisionBoxOffset = definition.pontoon.extraCollisionBoxOffset;
					definition.pontoon = null;
					break;
				}case("tread"):{
					definition.general.type = "ground_" + definition.general.type;
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
					definition.general.type = "interactable_crate";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = "crate";
					definition.interactable.inventoryUnits = 1;
					definition.interactable.feedsVehicles = true;
					break;
				}case("barrel"):{
					definition.general.type = "interactable_barrel";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = "barrel";
					definition.interactable.inventoryUnits = 1;
					break;
				}case("crafting_table"):{
					definition.general.type = "interactable_crafting_table";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = "crafting_table";
					break;
				}case("furnace"):{
					definition.general.type = "interactable_furnace";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = "furnace";
					break;
				}case("brewing_stand"):{
					definition.general.type = "interactable_brewing_stand";
					definition.interactable = definition.new JSONPartInteractable();
					definition.interactable.interactionType = "brewing_stand";
					break;
				}case("fertilizer"):{
					definition.general.type = "effector_fertilizer";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = "fertilizer";
					definition.effector.blocksWide = 1;
					break;
				}case("harvester"):{
					definition.general.type = "effector_harvester";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = "harvester";
					definition.effector.blocksWide = 1;
					break;
				}case("planter"):{
					definition.general.type = "effector_planter";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = "planter";
					definition.effector.blocksWide = 1;
					break;
				}case("plow"):{
					definition.general.type = "effector_plow";
					definition.effector = definition.new JSONPartEffector();
					definition.effector.type = "plow";
					definition.effector.blocksWide = 1;
					break;
				}case("custom"):{
					definition.general.type = "generic";
					definition.generic = definition.new JSONPartGeneric();
					definition.generic.height = definition.custom.height;
					definition.generic.width = definition.custom.width;
					definition.custom = null;
					break;
				}
			}
			
			//If the part is a ground_ type, and canGoFlat, auto-set flat height.
			if(definition.general.type.startsWith("ground_") && definition.ground.canGoFlat && definition.ground.flatHeight == 0){
				definition.ground.flatHeight = definition.ground.height/2F;
			}
			
			//If the part is a seat, and doesn't have a seat sub-section, add one.
			if(definition.general.type.startsWith("seat") && definition.seat == null){
				definition.seat = definition.new JSONPartSeat();
			}
		}
		
		if(definition.subParts != null){
    		for(VehiclePart subPartDef : definition.subParts){
    			try{
    				performVehiclePartDefLegacyCompats(subPartDef);
    			}catch(Exception e){
    				throw new NullPointerException("Could not perform Legacy Compats on sub-part entry #" + (definition.subParts.indexOf(subPartDef) + 1) + " due to an unknown error.  This is likely due to a missing or incorrectly-named field.");
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
				animation.centerPoint = new Point3d(0, 0, 0);
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
			for(minecrafttransportsimulator.jsondefs.JSONPoleComponent.TextLine line : definition.general.textLines){
				JSONText object = new JSONText();
				object.color = line.color;
				object.scale = line.scale;
				object.maxLength = line.characters;
				object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.01D);
				object.rot = new Point3d(0, 0, 0);
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
	}
	
	private static void performDecorLegacyCompats(JSONDecor definition){
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
			for(minecrafttransportsimulator.jsondefs.JSONDecor.TextLine line : definition.general.textLines){
				JSONText object = new JSONText();
				object.lightsUp = true;
				object.color = line.color;
				object.scale = line.scale;
				if(lineNumber++ < 3){
					object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.0001D);
					object.rot = new Point3d(0, 0, 0);
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
	}
	
	private static void performItemLegacyCompats(JSONItem definition){
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
		//Make the materials empty, as the parser doesn't like them null.
		definition.general.materials = new ArrayList<String>();
	}
	
	private static void performVehiclePartDefLegacyCompats(VehiclePart partDef){
		if(partDef.additionalPart != null){
			partDef.additionalParts = new ArrayList<VehiclePart>();
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
				animation.centerPoint = new Point3d(0, 0, 0);
				animation.axis = new Point3d(0, -1, 0);
				animation.animationType = AnimationComponentType.ROTATION;
				animation.variable = "rudder";
				partDef.animations.add(animation);
			}
			
			//If we have additional parts, check those too.
			if(partDef.additionalParts != null){
				for(VehiclePart additionalPartDef : partDef.additionalParts){
					performVehiclePartDefLegacyCompats(additionalPartDef);
				}
			}
		}
	}
    
    private static void performAnimationLegacyCompats(VehicleRendering rendering){
    	if(rendering.textMarkings != null){
    		rendering.textObjects = new ArrayList<JSONText>();
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleDisplayText marking : rendering.textMarkings){
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
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject rotatable : rendering.rotatableModelObjects){
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
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject translatable : rendering.translatableModelObjects){
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
