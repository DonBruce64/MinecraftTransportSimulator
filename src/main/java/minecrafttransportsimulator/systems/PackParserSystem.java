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
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packloading.JSONTypeAdapters;
import minecrafttransportsimulator.packloading.LegacyCompatSystem;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.packloading.PackResourceLoader.PackStructure;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**Links packs to the jar files that they are a part of.  Used for pack loading only: asset loading uses Java classpath systems.**/
	private static Map<String, File> packJarMap = new HashMap<String, File>();
	
	/**All registered pack definitions are stored in this list as they are added.  Used to handle loading operations.**/
	public static Map<String, JSONPack> packMap = new HashMap<String, JSONPack>();
	//TODO make this private when we get rid of the old loader system.

	/**All registered skin definitions are stored in this list as they are added.  These have to be added after all packs are loaded.**/
	private static Map<String, Map<String, JSONSkin>> skinMap = new HashMap<String, Map<String, JSONSkin>>();
	
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
     * Called to parse all packs and set up the main mod.  All directories in the passed-in list will be checked
     * for pack definitions.  After this, they will be created and loaded into the main mod.
     */
    public static void parsePacks(List<File> packDirectories){
    	//First get all pack definitions from the passed-in directories.
    	for(File directory : packDirectories){
    		for(File file : directory.listFiles()){
    			if(file.getName().endsWith(".jar")){
					checkJarForPacks(file);
				}
    		}
    	}
    	
    	//Next, parse all packs in those definitions.
    	parseAllPacks();
    	
    	//Check for custom skins.
    	parseAllSkins();
    	
    	//Now that all the items and skins are parsed, create them.
    	createAllItems();
    } 

	/**
     * Called to check if the passed-in jar file is a pack.  If so, it, and all the pack
     * definitions, are loaded into the system.  A single jar file may contain more than
     * one pack if there are multiple definition files in it.  Alternately, a single jar
     * may contain a single pack, just with different directories of assets to load depending
     * on what mods and packs have been loaded alongside it.  No packs should be loaded between
     * the jar-checking code and the pack-loading code, as all possible packs and mods must
     * be loaded prior to trying to load the pack in case there are dependencies.
     */
    private static void checkJarForPacks(File packJar){
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
    private static void parseAllPacks(){
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
										case SKIN : jsonClass = JSONSkin.class; break;
									}
								}else{
									MasterLoader.coreInterface.logError("ERROR: Could not determine what type of JSON to create for asset: " + fileName + ".  Check your folder paths.");
									continue;
								}
								
								//Create the JSON instance.
								AJSONItem<?> definition;
								try{
									definition = packParser.fromJson(new InputStreamReader(jarFile.getInputStream(entry), "UTF-8"), jsonClass);
									LegacyCompatSystem.performLegacyCompats(definition);
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
									//Check if the definition is a skin.  If so, we need to just add it to the skin map for processing later.
									//We don't create skin items right away as the pack they go to might not yet be loaded.
									if(definition instanceof JSONSkin){
										JSONSkin skinDef = (JSONSkin) definition;
										if(!skinMap.containsKey(skinDef.general.packID)){
											skinMap.put(skinDef.general.packID, new HashMap<String, JSONSkin>());
										}
										skinMap.get(skinDef.general.packID).put(skinDef.general.systemName, skinDef);
									}else{
										//Set code-based definition values prior to parsing all definitions out.
					    		    	definition.packID = packDef.packID;
					    		    	definition.systemName = fileName.substring(0, fileName.length() - ".json".length());
					    		    	definition.prefixFolders = assetPath;
					    		    	definition.classification = classification;
										parseAllDefinitions((AJSONMultiModelProvider<?>) definition, ((AJSONMultiModelProvider<?>) definition).definitions);
									}
								}else{
									AItemPack<?> item;
				    				switch(classification){
										case INSTRUMENT : item = new ItemInstrument((JSONInstrument) definition); break;
										case POLE : item = ((JSONPoleComponent) definition).general.type.equals("core") ? new ItemPole((JSONPoleComponent) definition) : new ItemPoleComponent((JSONPoleComponent) definition); break;
										case ROAD : item = new ItemRoadComponent((JSONRoadComponent) definition); break;
										case ITEM : item = new ItemItem((JSONItem) definition); break;
										case BOOKLET : item = new ItemBooklet((JSONBooklet) definition); break;
										default : {
											throw new IllegalArgumentException("ERROR: No corresponding classification found for asset: " + fileName + " Contact the mod author!");
										}
									}
				    				
				    				//Set code-based definition values.
				    		    	item.definition.packID = packDef.packID;
				    		    	item.definition.systemName = fileName.substring(0, fileName.length() - ".json".length());
				    		    	item.definition.prefixFolders = assetPath;
				    		    	item.definition.classification = classification;
				    		    	
				    		    	//Put the item in the map in the registry.
				    		    	if(!packItemMap.containsKey(item.definition.packID)){
				    		    		packItemMap.put(item.definition.packID, new LinkedHashMap<String, AItemPack<?>>());
				    		    	}
				    		    	packItemMap.get(item.definition.packID).put(item.definition.systemName, item);
								}
							}
						}
					}
					
					//Done parsing.  Close the jarfile.
					jarFile.close();
				}catch(Exception e){
					MasterLoader.coreInterface.logError("ERROR: Could not start parsing of pack: " + packDef.packID);
					e.printStackTrace();
				}
    		}
    	}
    }
    
    /**
     * Called to load and parse all skins.  Skins are applied to existing pack vehicles in other packs if those
     * packs are loaded.  This is run after all packs are parsed to ensure that all pack definitions are loaded
     * prior to attempting to add skin definitions.
     */
    private static void parseAllSkins(){
    	for(String packID : skinMap.keySet()){
    		//Is the pack for this skin loaded?
    		if(packItemMap.containsKey(packID)){
    			//Check all skin items for the pack, and add them if they exist.
    			//The pack item map is keyed by the systemName plus the subName, so we can't
    			//just get the pack item with the systemName from that map.
    			//Since all items share the same definition file, if we change one definition
    			//we change all definitions, so only add the skins to the definition once.
    			for(String systemName : skinMap.get(packID).keySet()){
    				for(AItemPack<?> packItem : packItemMap.get(packID).values()){
    					if(packItem.definition.systemName.equals(systemName)){
    						//Parse and create all of the new definitions.
    						AJSONMultiModelProvider<?> oldDefinition = (AJSONMultiModelProvider<?>) packItem.definition;
    						List<JSONSubDefinition> newDefinitions = skinMap.get(packID).get(systemName).definitions;
    						parseAllDefinitions(oldDefinition, newDefinitions);
    						
        					//Add the new definitions to the existing definitions of the existing item.
        					//This ensures the skins appear in the same tab as the existing item.
    						oldDefinition.definitions.addAll(newDefinitions);
        					break;
    					}
    				}
    			}
    		}
    	}
    }
    
    /**
     * Called to sort and create all pack items.  This must be called after all pack item processing to ensure proper sorting order.
     */
    private static void createAllItems(){
    	for(String packID : packItemMap.keySet()){
    		List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
    		packItems.addAll(packItemMap.get(packID).values());
    		packItems.sort(new Comparator<AItemPack<?>>(){
    			@Override
    			public int compare(AItemPack<?> itemA, AItemPack<?> itemB){
    				String totalAName = itemA.definition.classification.toDirectory() + itemA.definition.prefixFolders + itemA.definition.systemName;
    				if(itemA instanceof AItemSubTyped){
    					totalAName += ((AItemSubTyped<?>) itemA).subName;
    				}
    				String totalBName = itemB.definition.classification.toDirectory() + itemB.definition.prefixFolders + itemB.definition.systemName;
    				if(itemB instanceof AItemSubTyped){
    					totalBName += ((AItemSubTyped<?>) itemB).subName;
    				}
    				return totalAName.compareTo(totalBName);
    			}
    			
    		});
    		for(AItemPack<?> item : packItems){
    			MasterInterface.createItem(item);
    		}
    	}
    }
    
    /**
     * Helper method to parse multi-definition pack items.
     * Generated items are added to the passed-in list.
     */
    private static void parseAllDefinitions(AJSONMultiModelProvider<?> mainDefinition, List<JSONSubDefinition> subDefinitions){
    	Map<String, AItemPack<?>> packItems = new HashMap<String, AItemPack<?>>();
    	for(JSONSubDefinition subDefinition : subDefinitions){
			try{
				if(subDefinition.extraMaterials != null){
					AItemPack<?> item;
					switch(mainDefinition.classification){
						case VEHICLE : item = new ItemVehicle((JSONVehicle) mainDefinition, subDefinition.subName); break;
						case PART : item = new ItemPart((JSONPart) mainDefinition, subDefinition.subName); break;
						case DECOR : item = new ItemDecor((JSONDecor) mainDefinition, subDefinition.subName); break;
						default : {
							throw new IllegalArgumentException("ERROR: A classification for a normal item is trying to register as a multi-model provider.  This is an error in the core mod.  Contact the mod author.  Asset being loaded is: " + mainDefinition.packID + ":" + mainDefinition.systemName);
						}
					}
					
					//Add the pack item to the map.  We need to make sure all subDefinitions
					//are okay before adding the entire definition.
					packItems.put(item.definition.systemName + subDefinition.subName, item);
				}else{
					throw new NullPointerException();
				}
			}catch(Exception e){
				throw new NullPointerException("Unable to parse definition #" + (subDefinitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
			}
		}
    	
    	//All definitions were okay.  Add items to the registry.
    	if(!packItemMap.containsKey(mainDefinition.packID)){
    		packItemMap.put(mainDefinition.packID, new LinkedHashMap<String, AItemPack<?>>());
    	}
    	packItemMap.get(mainDefinition.packID).putAll(packItems);
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
    		LegacyCompatSystem.performLegacyCompats(definition);
    		Iterator <JSONSubDefinition> iterator = definition.definitions.iterator();
    		while(iterator.hasNext()){
    			JSONSubDefinition subDefinition = iterator.next();
    			if(subDefinition == null || subDefinition.extraMaterials == null){
    				iterator.remove();
    				throw new NullPointerException("Unable to parse definition #" + (definition.definitions.indexOf(subDefinition) + 1) + " due to a formatting error.");
    			}else{
	    			setupItem(new ItemVehicle(definition, subDefinition.subName), packID, jsonFileName, subDefinition.subName, "", ItemClassification.VEHICLE);
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
    		LegacyCompatSystem.performLegacyCompats(definition);
    		for(JSONSubDefinition subDefinition : definition.definitions){
	    		try{
	    			if(subDefinition.extraMaterials != null){
	    				setupItem(new ItemPart(definition, subDefinition.subName), packID, jsonFileName, subDefinition.subName, "", ItemClassification.PART);
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
    		JSONInstrument definition = packParser.fromJson(jsonReader, JSONInstrument.class);
    		LegacyCompatSystem.performLegacyCompats(definition);
    		setupItem(new ItemInstrument(definition), packID, jsonFileName, "", "", ItemClassification.INSTRUMENT);
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their pole components to the mod.**/
    public static void addPoleDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONPoleComponent definition = packParser.fromJson(jsonReader, JSONPoleComponent.class);
    		LegacyCompatSystem.performLegacyCompats(definition);
    		setupItem(definition.general.type.equals("core") ? new ItemPole(definition) : new ItemPoleComponent(definition), packID, jsonFileName, "", "", ItemClassification.POLE);
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their road components to the mod.**/
    public static void addRoadDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONRoadComponent definition = packParser.fromJson(jsonReader, JSONRoadComponent.class);
    		LegacyCompatSystem.performLegacyCompats(definition);
    		setupItem(new ItemRoadComponent(definition), packID, jsonFileName, "", "", ItemClassification.ROAD);
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their decor blocks to the mod.**/
    public static void addDecorDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		JSONDecor definition = packParser.fromJson(jsonReader, JSONDecor.class);
    		LegacyCompatSystem.performLegacyCompats(definition);
    		for(JSONSubDefinition subDefinition : definition.definitions){
	    		try{
	    			if(subDefinition.extraMaterials != null){
	    				setupItem(new ItemDecor(definition, subDefinition.subName), packID, jsonFileName, subDefinition.subName, "", ItemClassification.DECOR);
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
    
    /**Packs should call this upon load to add their crafting items to the mod.**/
    public static void addItemDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemItem(packParser.fromJson(jsonReader, JSONItem.class)), packID, jsonFileName, "", "", ItemClassification.ITEM);
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }
    
    /**Packs should call this upon load to add their booklets to the mod.**/
    public static void addBookletDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){
    	try{
    		setupItem(new ItemBooklet(packParser.fromJson(jsonReader, JSONBooklet.class)), packID, jsonFileName, "", "", ItemClassification.BOOKLET);
    	}catch(Exception e){
    		MasterLoader.coreInterface.logError("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + jsonFileName);
    		MasterLoader.coreInterface.logError(e.getMessage());
    	}
    }

	/**Skins aren't supported by the old packloader.  This section is here simply to prevent the old loader from crashing.**/
    public static void addSkinDefinition(InputStreamReader jsonReader, String jsonFileName, String packID){}
    
    /**
     * Sets up the item in the system. Item must be created prior to this as we can't use generics for instantiation.
     */
    private static AItemPack<?> setupItem(AItemPack<?> item, String packID, String systemName, String subName, String prefixFolders, ItemClassification classification){
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
