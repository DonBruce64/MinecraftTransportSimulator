package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;

import mcinterface1122.MasterInterface;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemBooklet;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.items.instances.ItemPole;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONBooklet;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart.ExhaustObject;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRendering;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packloading.JSONTypeAdapters;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.packloading.PackResourceLoader.PackStructure;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public final class PackParserSystem{
	/**Links packs to the jar files that they are a part of.  Used for pack loading only: asset loading uses Java classpath systems.**/
	private static Map<String, File> packJarMap = new HashMap<String, File>();
	
	/**All registered pack definition are stored in this list as they are added.  Used to handle loading operations.**/
	public static Map<String, JSONPack> packMap = new HashMap<String, JSONPack>();
	//TODO make this private when we get rid of the old loader system.
	
	/**List of pack faults.  This is for packs that didn't get loaded due to missing dependencies.**/
	public static Map<String, List<String>> faultMap = new HashMap<String, List<String>>();
	
	/**All registered pack items are stored in this map as they are added.  Used to sort items in the creative tab,
	 * and will be sent to packs for item registration when so asked via {@link #getItemsForPack(String)}.  May also
	 * be used if we need to lookup a registered part item.  Map is keyed by packID to allow sorting for items from 
	 * different packs, while the sub-map is keyed by the part's {@link AJSONItem#systemName}.**/
	private static TreeMap<String, LinkedHashMap<String, AItemPack<?>>> packItemMap = new TreeMap<String, LinkedHashMap<String, AItemPack<?>>>();

	/**Custom Gson instance for parsing packs.*/
	public static final Gson packParser = JSONTypeAdapters.getParserWithAdapters();
	

    //-----START OF NEW INIT LOGIC-----
	/**
     * Called to check if the passed-in jar file is a pack.  If so, it, and all the pack
     * definitions, are loaded into the system.  A single jar file may contain more than
     * one pack if there are multiple definition files in it.  Alternately, a single jar
     * may contain a single pack, just with different directories of assets to load depending
     * on what mods and packs have been loaded alongside it.  No packs should be loaded between
     * the jar-checking code and the pack-loading code, as all possible packs and mods must
     * be loaded prior to trying to load the pack in case there are dependencies.
     */
    public static void checkJarForPacks(File packJar){
    	try{
	    	ZipFile jarFile = new ZipFile(packJar);
			Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.getName().endsWith("packdefinition.json")){
					JSONPack packDef = packParser.fromJson(new InputStreamReader(jarFile.getInputStream(entry), "UTF-8"), JSONPack.class);
					packJarMap.put(packDef.packID, packJar);
					packMap.put(packDef.packID, packDef);
				}
			}
			jarFile.close();
    	}catch(Exception e){
			MasterLoader.coreInterface.logError("ERROR: A fault was encountered when trying to check file " + packJar.getName() + " for pack data.  This pack will not be loaded.");
			e.printStackTrace();
		}
    } 
	
	/**
     * Called to load and parse all packs.  this must be done after the initial checking for
     * packs to ensure we see all possible dependencies.  This method checks to make
     * sure the pack's dependent parameters are valid and the pack can be loaded prior to
     * performing any actual loading operations.
     */
    public static void parseAllPacks(){
    	List<String> packIDs = new ArrayList<String>(packMap.keySet());
    	Iterator<String> iterator = packMap.keySet().iterator();
    	while(iterator.hasNext()){
    		JSONPack packDef = packMap.get(iterator.next());
    		//Don't parse the core pack.  THat's all internal.
    		if(packDef.packID.equals(MasterLoader.resourceDomain)){
    			continue;
    		}
    		
    		//Create a listing of sub-directories we need to look in for pack definitions.
    		//These will be modified by activators or blockers.
    		List<String> validSubDirectories = new ArrayList<String>();
    		
    		//If we don't have any of the activating sets, don't load the pack. 
    		if(packDef.activators != null){
    			for(String subDirectory : packDef.activators.keySet()){
    				if(!packDef.activators.get(subDirectory).isEmpty()){
    					for(String activator : packDef.activators.get(subDirectory)){
    	    				if(packIDs.contains(activator) || MasterLoader.coreInterface.isModPresent(activator)){
    	    					validSubDirectories.add(subDirectory);
        						break;
        					}
    	    			}
    				}else{
    					validSubDirectories.add(subDirectory);
    				}
    			}
    		}else{
    			validSubDirectories.add("");
    		}
    		
    		//If we have a blocking set, and we were going to load the pack, don't load it.
    		if(packDef.blockers != null){
    			for(String subDirectory : packDef.blockers.keySet()){
	    			for(String blocker : packDef.blockers.get(subDirectory)){
	    				if(packIDs.contains(blocker) || MasterLoader.coreInterface.isModPresent(blocker)){
	    					validSubDirectories.remove(subDirectory);
    						break;
    					}
	    			}
    			}
    		}
    		
    		//If we have dependent sets, make sure we log a pack fault.
    		if(packDef.dependents != null){
    			for(String dependent : packDef.dependents){
					if(packIDs.contains(dependent) || MasterLoader.coreInterface.isModPresent(dependent)){
						faultMap.put(packDef.packID, packDef.dependents);
						break;
					}
    			}
    		}
    		
    		//Load the pack components into the game.
    		//We iterate over all the sub-folders we found from the packDef checks.
    		PackStructure structure = PackStructure.values()[packDef.fileStructure];
    		for(String subDirectory : validSubDirectories){
	    		String assetPathPrefix = "assets/" + packDef.packID + "/";
				if(!subDirectory.isEmpty()){
					assetPathPrefix += subDirectory + "/";
				}
				
				try{
		    		ZipFile jarFile = new ZipFile(packJarMap.get(packDef.packID));
					Enumeration<? extends ZipEntry> entries = jarFile.entries();
					List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
					while(entries.hasMoreElements()){
						//Get next entry and path.
						ZipEntry entry = entries.nextElement();
						String entryFullPath = entry.getName();
						if(entryFullPath.startsWith(assetPathPrefix) && entryFullPath.endsWith(".json")){
							//JSON is in correct folder.  Get path properties and ensure they match our specs.
							//Need the asset folder structure between the main prefix and the asset itself.
							//This lets us know what asset we need to create as all assets are in their own folders.
							String fileName = entryFullPath.substring(entryFullPath.lastIndexOf('/') + 1);
							String assetPath = entryFullPath.substring(assetPathPrefix.length(), entryFullPath.substring(0, entryFullPath.length() - fileName.length()).lastIndexOf("/") + 1);
							if(!structure.equals(PackStructure.MODULAR)){
								//Need to trim the jsondefs folder to get correct sub-folder of jsondefs data.
								//Modular structure does not have a jsondefs folder, so we don't need to trim it off for that.
								//If we aren't modular, and aren't in a jsondefs folder, skip this entry.
								if(assetPath.startsWith("jsondefs/")){
									assetPath = assetPath.substring("jsondefs/".length());
								}else{
									continue;
								}
							}
							
							//Check to make sure json isn't an item JSON or our pack definition.
							if(!fileName.equals("packdefinition.json") && (structure.equals(PackStructure.MODULAR) ? !fileName.endsWith("_item.json") : entryFullPath.contains("jsondefs"))){
								//Get classification and JSON class type to use with GSON system.
								ItemClassification classification = ItemClassification.fromDirectory(assetPath.substring(0, assetPath.indexOf("/") + 1));
								Class<? extends AJSONItem<?>> jsonClass = null;
								if(classification != null){
									switch(classification){
										case VEHICLE : jsonClass = JSONVehicle.class; break;
										case PART : jsonClass = JSONPart.class; break;
										case INSTRUMENT : jsonClass = JSONInstrument.class; break;
										case POLE : jsonClass = JSONPoleComponent.class; break;
										case ROAD : jsonClass = JSONRoadComponent.class; break;
										case DECOR : jsonClass = JSONDecor.class; break;
										case ITEM : jsonClass = JSONItem.class; break;
										case BOOKLET : jsonClass = JSONBooklet.class; break;
									}
								}else{
									MasterLoader.coreInterface.logError("ERROR: Could not determine what type of JSON to create for asset: " + fileName + ".  Check your folder paths.");
									continue;
								}
								
								//Create the JSON instance.
								AJSONItem<?> definition;
								try{
									definition = packParser.fromJson(new InputStreamReader(jarFile.getInputStream(entry), "UTF-8"), jsonClass);
									performLegacyCompats(definition);
								}catch(Exception e){
									MasterLoader.coreInterface.logError("ERROR: Could not parse: " + packDef.packID + ":" + fileName);
						    		MasterLoader.coreInterface.logError(e.getMessage());
						    		continue;
								}
								
								//Remove the classification folder from the assetPath.  We don't use this for the resource-loading code.
								//Instead, this will be loaded by referencing the definition.  This also allows us to omit the path
								//if we are loading a non-default pack format.
								assetPath = assetPath.substring(classification.toDirectory().length());
								
								//Create all required items.
								if(definition instanceof AJSONMultiModelProvider){
									for(AJSONMultiModelProvider<?>.SubDefinition subDefinition : ((AJSONMultiModelProvider<?>) definition).definitions){
							    		try{
							    			if(subDefinition.extraMaterials != null){
							    				AItemPack<?> item;
							    				switch(classification){
													case VEHICLE : item = new ItemVehicle((JSONVehicle) definition, subDefinition.subName); break;
													case PART : item = new ItemPart((JSONPart) definition, subDefinition.subName); break;
													default : {
														throw new IllegalArgumentException("ERROR: A classification for a normal item is trying to register as a multi-model provider.  This is an error in the core mod.  Contact the mod author.  Asset being loaded is: " + fileName);
													}
												}
							    				packItems.add(setupItem(item, packDef.packID, fileName.substring(0, fileName.length() - ".json".length()), subDefinition.subName, assetPath, classification));
							    			}else{
							    				throw new NullPointerException();
							    			}
							    		}catch(Exception e){
							    			throw new NullPointerException("Unable to parse definition #" + (((AJSONMultiModelProvider<?>) definition).definitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
							    		}
						    		}
								}else{
									AItemPack<?> item;
				    				switch(classification){
										case INSTRUMENT : item = new ItemInstrument((JSONInstrument) definition); break;
										case POLE : item = ((JSONPoleComponent) definition).general.type.equals("core") ? new ItemPole((JSONPoleComponent) definition) : new ItemPoleComponent((JSONPoleComponent) definition); break;
										case ROAD : item = new ItemRoadComponent((JSONRoadComponent) definition); break;
										case DECOR : item = new ItemDecor((JSONDecor) definition); break;
										case ITEM : item = new ItemItem((JSONItem) definition); break;
										case BOOKLET : item = new ItemBooklet((JSONBooklet) definition); break;
										default : {
											throw new IllegalArgumentException("ERROR: No corresponding classification found for asset: " + fileName + " Contact the mod author!");
										}
									}
				    				packItems.add(setupItem(item, packDef.packID, fileName.substring(0, fileName.length() - ".json".length()), "", assetPath, classification));
								}
							}
						}
					}
					
					//Done parsing.  Close the jarfile, sort the items we parsed, and send the to the loader.
					jarFile.close();
					packItems.sort(new Comparator<AItemPack<?>>(){
						@Override
						public int compare(AItemPack<?> itemA, AItemPack<?> itemB){
							String totalAName = itemA.definition.classification.toDirectory() + itemA.definition.prefixFolders + itemA.definition.systemName;
							if(itemA instanceof AItemSubTyped){
								totalAName += ((AItemSubTyped) itemA).subName;
							}
							String totalBName = itemB.definition.classification.toDirectory() + itemB.definition.prefixFolders + itemB.definition.systemName;
							if(itemB instanceof AItemSubTyped){
								totalBName += ((AItemSubTyped) itemB).subName;
							}
							return totalAName.compareTo(totalBName);
						}
						
					});
					for(AItemPack<?> item : packItems){
						MasterInterface.createItem(item);
					}
				}catch(Exception e){
					MasterLoader.coreInterface.logError("ERROR: Could not start parsing of pack: " + packDef.packID);
					e.printStackTrace();
				}
    		}
    	}
    }
	
	
    //-----START OF OLD INIT LOGIC-----
    /**Packs should call this upon load to add their content to the mod.
     * This will return an array of strings that correspond to content types.
     * These content types will be content that has items in the jsondefs folder
     * that the pack should send to MTS.  The pack should only send the location
     * of such an item as it will allow MTS to load the information in modpacks.**/
    public static String[] getValidPackContentNames(){
    	return ItemClassification.getAllTypesAsStrings().toArray(new String[ItemClassification.values().length]);
    }
    
    /**Packs should call this upon load to add their vehicles to the mod.**/
    public static void addVehicleDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONVehicle definition = packParser.fromJson(jsonReader, JSONVehicle.class);
    		performLegacyCompats(definition);
    		for(JSONVehicle.SubDefinition subDefinition : definition.definitions){
	    		try{
	    			if(subDefinition.extraMaterials != null){
	    				MasterInterface.createItem(setupItem(new ItemVehicle(definition, subDefinition.subName), packID, jsonFileName, subDefinition.subName, "", ItemClassification.VEHICLE));
	    			}else{
	    				throw new NullPointerException();
	    			}
	    		}catch(Exception e){
	    			e.printStackTrace();
	    			throw new NullPointerException("Unable to parse definition #" + (definition.definitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
	    		}
    		}
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    /**Packs should call this upon load to add their parts to the mod.**/
    public static void addPartDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONPart definition = packParser.fromJson(jsonReader, JSONPart.class);
    		performLegacyCompats(definition);
    		for(JSONPart.SubDefinition subDefinition : definition.definitions){
	    		try{
	    			if(subDefinition.extraMaterials != null){
	    				MasterInterface.createItem(setupItem(new ItemPart(definition, subDefinition.subName), packID, jsonFileName, subDefinition.subName, "", ItemClassification.PART));
		    		}else{
	    				throw new NullPointerException();
	    			}
	    		}catch(Exception e){
	    			throw new NullPointerException("Unable to parse definition #" + (definition.definitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
	    		}
    		}
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their instrument set to the mod.**/
    public static void addInstrumentDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		MasterInterface.createItem(setupItem(new ItemInstrument(packParser.fromJson(jsonReader, JSONInstrument.class)), packID, jsonFileName, "", "", ItemClassification.INSTRUMENT));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their pole components to the mod.**/
    public static void addPoleDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONPoleComponent definition = packParser.fromJson(jsonReader, JSONPoleComponent.class);
    		performLegacyCompats(definition);
    		MasterInterface.createItem(setupItem(definition.general.type.equals("core") ? new ItemPole(definition) : new ItemPoleComponent(definition), packID, jsonFileName, "", "", ItemClassification.POLE));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their road components to the mod.**/
    public static void addRoadDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONRoadComponent definition = packParser.fromJson(jsonReader, JSONRoadComponent.class);
    		performLegacyCompats(definition);
    		MasterInterface.createItem(setupItem(new ItemRoadComponent(definition), packID, jsonFileName, "", "", ItemClassification.ROAD));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their decor blocks to the mod.**/
    public static void addDecorDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONDecor definition = packParser.fromJson(jsonReader, JSONDecor.class);
    		performLegacyCompats(definition);
    		MasterInterface.createItem(setupItem(new ItemDecor(definition), packID, jsonFileName, "", "", ItemClassification.DECOR));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their crafting items to the mod.**/
    public static void addItemDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		MasterInterface.createItem(setupItem(new ItemItem(packParser.fromJson(jsonReader, JSONItem.class)), packID, jsonFileName, "", "", ItemClassification.ITEM));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their booklets to the mod.**/
    public static void addBookletDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		MasterInterface.createItem(setupItem(new ItemBooklet(packParser.fromJson(jsonReader, JSONBooklet.class)), packID, jsonFileName, "", "", ItemClassification.BOOKLET));
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**
     * Sets up the item in the system. Item must be created prior to this as we can't use generics for instantiation.
     */
    public static AItemPack<?> setupItem(AItemPack<?> item, String packID, String systemName, String subName, String prefixFolders, ItemClassification classification){
    	//Set code-based definition values.
    	item.definition.packID = packID;
    	item.definition.systemName = systemName;
    	item.definition.prefixFolders = prefixFolders;
    	item.definition.classification = classification;
    	
    	//Put the item in the map in the registry.
    	if(!packItemMap.containsKey(packID)){
    		packItemMap.put(packID, new LinkedHashMap<String, AItemPack<?>>());
    	}
    	packItemMap.get(packID).put(item.definition.systemName + subName, item);
    	
    	//Return the item for construction convenience.
    	return item;
    }
    
    /**
     * Perform legacy compats.  This is used to allow older packs to remain compatible.
     * Legacy compats may be removed ONLY when older packs have updated!
     */
    private static <JSONDefinition extends AJSONItem<?>> void performLegacyCompats(JSONDefinition definition){
    	if(definition instanceof JSONPart){
    		JSONPart partDef = (JSONPart) definition;
    		//If we are a part without a definition, add one so we don't crash on other systems.
    		if(partDef.definitions == null){
    			partDef.definitions = new ArrayList<JSONPart.SubDefinition>();
    			JSONPart.SubDefinition subDefinition = partDef.new SubDefinition();
    			subDefinition.extraMaterials = new ArrayList<String>();
    			subDefinition.name = definition.general.name;
    			subDefinition.subName = "";
    			partDef.definitions.add(subDefinition);
    		}
    		
    		if(partDef.engine != null){
    			//If we are an engine_jet part, and our jetPowerFactor is 0, we are a legacy jet engine.
    			if(partDef.general.type.equals("engine_jet") && partDef.engine.jetPowerFactor == 0){
    				partDef.engine.jetPowerFactor = 1.0F;
    				partDef.engine.bypassRatio = partDef.engine.gearRatios[0];
    				partDef.engine.gearRatios[0] = 1.0F;
    			}
    			
    			//If we only have one gearRatio, add two more gears as we're a legacy propeller-based engine.
    			if(partDef.engine.gearRatios.length == 1){
    				partDef.engine.propellerRatio = 1/partDef.engine.gearRatios[0];
    				partDef.engine.gearRatios = new float[]{-1, 0, 1};
    			}
    			
    			//If our shiftSpeed is 0, we are a legacy engine that didn't set a shift speed.
    			if(partDef.engine.shiftSpeed == 0){
    				partDef.engine.shiftSpeed = 20;
    			}
    			//If our revResistance is 0, we are a legacy engine that didn't set a rev Resistance.
    			if (partDef.engine.revResistance == 0){
    				partDef.engine.revResistance = 10;
    			}
    		}else if(partDef.gun != null){
    			//Make sure turrets are set as turrets.
    			if(partDef.general.type.equals("gun_turret")){
    				partDef.gun.isTurret = true;
    			}
    		}else if(partDef.bullet != null) {
    			if (partDef.bullet.type != null) {
    				partDef.bullet.types = new ArrayList<String>();
    				partDef.bullet.types.add(partDef.bullet.type);
    			}
    		}else{
    			//Check for old ground devices, crates, barrels, and effectors.
    			switch(partDef.general.type){
    				case("wheel"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.isWheel = true;
    					partDef.ground.width = partDef.wheel.diameter/2F;
    					partDef.ground.height = partDef.wheel.diameter;
    					partDef.ground.lateralFriction = partDef.wheel.lateralFriction;
    					partDef.ground.motiveFriction = partDef.wheel.motiveFriction;
    					break;
    				}case("skid"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.width = partDef.skid.width;
    					partDef.ground.height = partDef.skid.width;
    					partDef.ground.lateralFriction = partDef.skid.lateralFriction;
    					break;
    				}case("pontoon"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.canFloat = true;
    					partDef.ground.width = partDef.pontoon.width;
    					partDef.ground.height = partDef.pontoon.width;
    					partDef.ground.lateralFriction = partDef.pontoon.lateralFriction;
    					partDef.ground.extraCollisionBoxOffset = partDef.pontoon.extraCollisionBoxOffset;
    					break;
    				}case("tread"):{
    					partDef.general.type = "ground_" + partDef.general.type;
    					partDef.ground = partDef.new JSONPartGroundDevice();
    					partDef.ground.isTread = true;
    					partDef.ground.width = partDef.tread.width;
    					partDef.ground.height = partDef.tread.width;
    					partDef.ground.lateralFriction = partDef.tread.lateralFriction;
    					partDef.ground.motiveFriction = partDef.tread.motiveFriction;
    					partDef.ground.extraCollisionBoxOffset = partDef.tread.extraCollisionBoxOffset;
    					partDef.ground.spacing = partDef.tread.spacing;
    					break;
    				}case("crate"):{
    					partDef.general.type = "interactable_crate";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "crate";
    					partDef.interactable.inventoryUnits = 1;
    					partDef.interactable.feedsVehicles = true;
    					break;
    				}case("barrel"):{
    					partDef.general.type = "interactable_barrel";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "barrel";
    					partDef.interactable.inventoryUnits = 1;
    					break;
    				}case("crafting_table"):{
    					partDef.general.type = "interactable_crafting_table";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "crafting_table";
    					break;
    				}case("furnace"):{
    					partDef.general.type = "interactable_furnace";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "furnace";
    					break;
    				}case("brewing_stand"):{
    					partDef.general.type = "interactable_brewing_stand";
    					partDef.interactable = partDef.new JSONPartInteractable();
    					partDef.interactable.interactionType = "brewing_stand";
    					break;
    				}case("fertilizer"):{
    					partDef.general.type = "effector_fertilizer";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "fertilizer";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("harvester"):{
    					partDef.general.type = "effector_harvester";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "harvester";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("planter"):{
    					partDef.general.type = "effector_planter";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "planter";
    					partDef.effector.blocksWide = 1;
    					break;
    				}case("plow"):{
    					partDef.general.type = "effector_plow";
    					partDef.effector = partDef.new JSONPartEffector();
    					partDef.effector.type = "plow";
    					partDef.effector.blocksWide = 1;
    					break;
    				}
    			}
    		}
    		
    		if(partDef.subParts != null){
	    		//Check all part slots for ground device names and update them.
	    		//Also check if we define an additional part, and make it a list instead.
	    		//Finally, switch all crates and barrels to effectors with the appropriate type.
	    		for(VehiclePart subPartDef : partDef.subParts){
	    			if(subPartDef.additionalPart != null){
	    				subPartDef.additionalParts = new ArrayList<VehiclePart>();
	    				subPartDef.additionalParts.add(subPartDef.additionalPart);
	    			}
	    			for(byte i=0; i<subPartDef.types.size(); ++i){
	    				String subPartName = subPartDef.types.get(i);
	    				if(subPartName.equals("wheel") || subPartName.equals("skid") || subPartName.equals("pontoon") || subPartName.equals("tread")){
	    					if(subPartName.equals("tread")){
	    						subPartDef.turnsWithSteer = true;
	    					}
	    					subPartDef.types.set(i, "ground_" + subPartName);
	    				}else if(subPartName.equals("crate") || subPartName.equals("barrel") || subPartName.equals("crafting_table") || subPartName.equals("furnace") || subPartName.equals("brewing_stand")){
	    					subPartDef.types.set(i, "interactable_" + subPartName);
	    					subPartDef.minValue = 0;
	    					subPartDef.maxValue = 1;
	    				}else if(subPartName.equals("fertilizer") || subPartName.equals("harvester") || subPartName.equals("planter") || subPartName.equals("plow")){
	    					subPartDef.types.set(i, "effector_" + subPartName);
	    				}
	    				//If we have additional parts, check those too.
	    				if(subPartDef.additionalParts != null){
	    					for(VehiclePart additionalPartDef : subPartDef.additionalParts){
		    					for(byte j=0; j<additionalPartDef.types.size(); ++j){
		    	    				String additionalPartName = additionalPartDef.types.get(j);
		    	    				if(additionalPartName.equals("wheel") || additionalPartName.equals("skid") || additionalPartName.equals("pontoon") || additionalPartName.equals("tread")){
		    	    					if(additionalPartName.equals("tread")){
		    	    						additionalPartDef.turnsWithSteer = true;
		    	    					}
		    	    					additionalPartDef.types.set(j, "ground_" + additionalPartName);
		    	    				}else if(additionalPartName.equals("crate") || additionalPartName.equals("barrel") || additionalPartName.equals("crafting_table") || additionalPartName.equals("furnace") || additionalPartName.equals("brewing_stand")){
		    	    					additionalPartDef.types.set(i, "interactable_" + additionalPartName);
		    	    					additionalPartDef.minValue = 0;
		    	    					additionalPartDef.maxValue = 1;
		    	    				}else if(additionalPartName.equals("fertilizer") || additionalPartName.equals("harvester") || additionalPartName.equals("planter") || additionalPartName.equals("plow")){
		    	    					additionalPartDef.types.set(i, "effector_" + additionalPartName);
		    	    				}
		    	    			}
	    					}
	    				}
	    			}
	    		}
    		}
    		
    		if(partDef.rendering != null){
    			doAnimationLegacyCompats(partDef.rendering, new JSONVehicle());
    		}
    	}else if(definition instanceof JSONVehicle){
    		JSONVehicle vehicleDef = (JSONVehicle) definition;
    		//Move vehicle parameters to the motorized section.
    		if(vehicleDef.car != null){
    			vehicleDef.motorized.isBigTruck = vehicleDef.car.isBigTruck;
    			vehicleDef.motorized.isFrontWheelDrive = vehicleDef.car.isFrontWheelDrive;
    			vehicleDef.motorized.isRearWheelDrive = vehicleDef.car.isRearWheelDrive;
    			vehicleDef.motorized.hasCruiseControl = vehicleDef.car.hasCruiseControl;
    			vehicleDef.motorized.axleRatio = vehicleDef.car.axleRatio;
    			vehicleDef.motorized.dragCoefficient = vehicleDef.car.dragCoefficient;
    			vehicleDef.car = null;
    		}
    		
    		//If we still have the old type parameter and are an aircraft, set the flag to true.
    		if(vehicleDef.general.type != null){
    			if(vehicleDef.general.type.equals("plane") || vehicleDef.general.type.equals("blimp") || vehicleDef.general.type.equals("helicopter")){
    				vehicleDef.general.isAircraft = true;
    			}
    			vehicleDef.general.type = null;
    		}
    		
    		if(vehicleDef.plane != null){
    			vehicleDef.general.isAircraft = true;
    			vehicleDef.motorized.hasFlaps = vehicleDef.plane.hasFlaps;
    			vehicleDef.motorized.hasAutopilot = vehicleDef.plane.hasAutopilot;
    			vehicleDef.motorized.wingSpan = vehicleDef.plane.wingSpan;
    			vehicleDef.motorized.wingArea = vehicleDef.plane.wingArea;
    			vehicleDef.motorized.tailDistance = vehicleDef.plane.tailDistance;
    			vehicleDef.motorized.aileronArea = vehicleDef.plane.aileronArea;
    			vehicleDef.motorized.elevatorArea = vehicleDef.plane.elevatorArea;
    			vehicleDef.motorized.rudderArea = vehicleDef.plane.rudderArea;
    			vehicleDef.plane = null;
    			
    			//If aileronArea is 0, we're a legacy plane and need to adjust.
    			if(vehicleDef.motorized.aileronArea == 0){
    				vehicleDef.motorized.aileronArea = vehicleDef.motorized.wingArea/5F;
    			}
    		}
    		
    		if(vehicleDef.blimp != null){
    			vehicleDef.general.isAircraft = true;
    			vehicleDef.general.isBlimp = true;
    			vehicleDef.motorized.crossSectionalArea = vehicleDef.blimp.crossSectionalArea;
    			vehicleDef.motorized.tailDistance = vehicleDef.blimp.tailDistance;
    			vehicleDef.motorized.rudderArea = vehicleDef.blimp.rudderArea;
    			vehicleDef.motorized.ballastVolume = vehicleDef.blimp.ballastVolume;
    			vehicleDef.blimp = null;
    		}
    		
    		//Check all part slots for ground device names and update them.
    		//Also check if we define an additional part, and make it a list instead.
    		//Finally, switch all crates and barrels to effectors with the appropriate type.
    		for(VehiclePart partDef : vehicleDef.parts){
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
    				partDef.exhaustObjects = new ArrayList<ExhaustObject>();
    				for(int i=0; i<partDef.exhaustPos.length; i+=3){
    					ExhaustObject exhaust = partDef.new ExhaustObject();
    					exhaust.pos = new Point3d(partDef.exhaustPos[i], partDef.exhaustPos[i+1], partDef.exhaustPos[i+2]);
    					exhaust.velocity = new Point3d(partDef.exhaustVelocity[i], partDef.exhaustVelocity[i+1], partDef.exhaustVelocity[i+2]);
    					exhaust.scale = 1.0F;
    					partDef.exhaustObjects.add(exhaust);
    				}
    				partDef.exhaustPos = null;
    			}
    			if(partDef.rotationVariable != null){
    				partDef.animations = new ArrayList<VehicleAnimationDefinition>();
    				VehicleAnimationDefinition animation = vehicleDef.new VehicleAnimationDefinition();
    				animation.animationType = "rotation";
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
    					partDef.animations = new ArrayList<VehicleAnimationDefinition>();
    				}
    				VehicleAnimationDefinition animation = vehicleDef.new VehicleAnimationDefinition();
    				animation.animationType = "translation";
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
    				}
    				//If we have additional parts, check those too.
    				if(partDef.additionalParts != null){
    					for(VehiclePart additionalPartDef : partDef.additionalParts){
	    					for(byte j=0; j<additionalPartDef.types.size(); ++j){
	    	    				String additionalPartName = additionalPartDef.types.get(j);
	    	    				if(additionalPartName.equals("wheel") || additionalPartName.equals("skid") || additionalPartName.equals("pontoon") || additionalPartName.equals("tread")){
	    	    					if(additionalPartName.equals("tread")){
	    	    						additionalPartDef.turnsWithSteer = true;
	    	    					}
	    	    					additionalPartDef.types.set(j, "ground_" + additionalPartName);
	    	    				}else if(additionalPartName.equals("crate") || additionalPartName.equals("barrel") || additionalPartName.equals("crafting_table") || additionalPartName.equals("furnace") || additionalPartName.equals("brewing_stand")){
	    	    					additionalPartDef.types.set(i, "interactable_" + additionalPartName);
	    	    					additionalPartDef.minValue = 0;
	    	    					additionalPartDef.maxValue = 1;
	    	    				}else if(additionalPartName.equals("fertilizer") || additionalPartName.equals("harvester") || additionalPartName.equals("planter") || additionalPartName.equals("plow")){
	    	    					additionalPartDef.types.set(i, "effector_" + additionalPartName);
	    	    				}
	    	    			}
    					}
    				}
    			}
    		}
    		
    		if(vehicleDef.rendering != null){
    			doAnimationLegacyCompats(vehicleDef.rendering, new JSONVehicle());
    		}
    	}else if(definition instanceof JSONPoleComponent){
    		JSONPoleComponent pole = (JSONPoleComponent) definition;
    		//If we are a sign using the old textlines, update them.
    		if(pole.general.textLines != null){
    			pole.general.textObjects = new ArrayList<JSONText>();
    			for(minecrafttransportsimulator.jsondefs.JSONPoleComponent.TextLine line : pole.general.textLines){
    				JSONText object = new JSONText();
    				object.color = line.color;
    				object.scale = line.scale;
    				object.maxLength = line.characters;
    				object.pos = new Point3d(line.xPos, line.yPos, line.zPos + 0.01D);
    				object.rot = new Point3d(0, 0, 0);
    				object.fieldName = "TextLine #" + (pole.general.textObjects.size() + 1);
    				pole.general.textObjects.add(object);
    			}
    			pole.general.textLines = null;
    		}
    	}else if(definition instanceof JSONDecor){
    		JSONDecor decor = (JSONDecor) definition;
    		//If we are a decor using the old textlines, update them.
    		if(decor.general.textLines != null){
    			decor.general.textObjects = new ArrayList<JSONText>();
    			int lineNumber = 0;
    			for(minecrafttransportsimulator.jsondefs.JSONDecor.TextLine line : decor.general.textLines){
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
    				object.fieldName = "TextLine #" + (decor.general.textObjects.size() + 1);
    				decor.general.textObjects.add(object);
    			}
    			 decor.general.textLines = null;
    		}
    	}
    }
    
    private static void doAnimationLegacyCompats(VehicleRendering rendering, JSONVehicle jsonInstance){
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
    	}
    	if(rendering.rotatableModelObjects != null){
    		if(rendering.animatedObjects == null){
    			rendering.animatedObjects = new ArrayList<VehicleAnimatedObject>();
    		}
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleRotatableModelObject rotatable : rendering.rotatableModelObjects){
    			VehicleAnimatedObject object = null;
    			for(VehicleAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(rotatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = jsonInstance.new VehicleAnimatedObject();
    				object.objectName = rotatable.partName;
    				object.animations = new ArrayList<VehicleAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			VehicleAnimationDefinition animation = jsonInstance.new VehicleAnimationDefinition();
    			animation.animationType = "rotation";
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
    			rendering.animatedObjects = new ArrayList<VehicleAnimatedObject>();
    		}
    		for(minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleTranslatableModelObject translatable : rendering.translatableModelObjects){
    			VehicleAnimatedObject object = null;
    			for(VehicleAnimatedObject testObject : rendering.animatedObjects){
    				if(testObject.objectName.equals(translatable.partName)){
    					object = testObject;
    					break;
    				}
    			}
    			if(object == null){
    				object = jsonInstance.new VehicleAnimatedObject();
    				object.objectName = translatable.partName;
    				object.animations = new ArrayList<VehicleAnimationDefinition>();
    				rendering.animatedObjects.add(object);
    			}
    			
    			VehicleAnimationDefinition animation = jsonInstance.new VehicleAnimationDefinition();
    			animation.animationType = "translation";
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
    
    //--------------------START OF HELPER METHODS--------------------
    public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem<?>> PackItem getItem(String packID, String systemName){
    	return getItem(packID, systemName, "");
    }
    
    @SuppressWarnings("unchecked")
	public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem<?>> PackItem getItem(String packID, String systemName, String subName){
    	if(packItemMap.containsKey(packID)){
    		return (PackItem) packItemMap.get(packID).get(systemName + subName);
    	}
    	return null;
    }
    
    public static boolean arePacksPresent(){
    	//We always have 1 pack: the core pack.
    	return packItemMap.size() > 1;
    }
    
    public static Set<String> getAllPackIDs(){
    	return packItemMap.keySet();
    }
    
    public static JSONPack getPackConfiguration(String packID){
    	return packMap.get(packID);
    }
    
    public static List<AItemPack<?>> getAllItemsForPack(String packID){
    	return new ArrayList<AItemPack<?>>(packItemMap.get(packID).values());
    }
    
    public static List<AItemPack<?>> getAllPackItems(){
    	List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
    	for(String packID : packItemMap.keySet()){
    		packItems.addAll(getAllItemsForPack(packID));
    	}
    	return packItems;
    }
}
