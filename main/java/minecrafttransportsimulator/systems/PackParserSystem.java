package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;

/**Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 * 
 * @author don_bruce
 */
public final class PackParserSystem{
	private static Map<String, Map<String, String>> propertyMap = new HashMap<String, Map<String, String>>();

	public static void init(){
		File assetDir = new File(MTS.assetDir);
		if(!assetDir.exists()){
			assetDir.mkdirs();
		}else{
			parseDirectory(assetDir);
		}
	}
	
	private static void parseDirectory(File assetDir){
		for(File file : assetDir.listFiles()){
			if(file.isDirectory()){
				parseDirectory(file);
			}else{
				if(file.getName().endsWith(".txt") && !file.getName().contains("SAMPLE")){
					parseFile(file);
				}
			}
		}
	}
	
	public static String[] getAllRegisteredNames(){
		return propertyMap.keySet().toArray(new String[0]);
	}
	
	public static boolean doesPropertyExist(String entityName, String propertyName){
		return propertyMap.get(entityName).containsKey(propertyName);
	}
	
	public static boolean getBooleanProperty(String entityName, String propertyName) throws TypeNotPresentException{
		if(propertyMap.get(entityName).containsKey(propertyName)){
			return Boolean.valueOf(propertyMap.get(entityName).get(propertyName));
		}else{
			throw new TypeNotPresentException(propertyName, null);
		}
	}
	
	public static Integer getIntegerProperty(String entityName, String propertyName) throws TypeNotPresentException{
		if(propertyMap.get(entityName).containsKey(propertyName)){
			return Integer.valueOf(propertyMap.get(entityName).get(propertyName));
		}else{
			throw new TypeNotPresentException(propertyName, null);
		}
	}
	
	public static Float getFloatProperty(String entityName, String propertyName) throws TypeNotPresentException{
		if(propertyMap.get(entityName).containsKey(propertyName)){
			return Float.valueOf(propertyMap.get(entityName).get(propertyName));
		}else{
			throw new TypeNotPresentException(propertyName, null);
		}
	}
	
	public static String getStringProperty(String entityName, String propertyName) throws TypeNotPresentException{
		if(propertyMap.get(entityName).containsKey(propertyName)){
			return String.valueOf(propertyMap.get(entityName).get(propertyName));
		}else{
			throw new TypeNotPresentException(propertyName, null);
		}
	}
	
	public static Float[] getFloatArrayProperty(String entityName, String propertyName) throws TypeNotPresentException{
		if(propertyMap.get(entityName).containsKey(propertyName)){
			String property = propertyMap.get(entityName).get(propertyName);
			List<Float> floatList = new ArrayList<Float>();
			floatList.add(Float.valueOf(property.substring(property.indexOf('=') + 1, property.indexOf(',') - 1)));
			property = property.substring(property.indexOf(',') + 1);
			while(property.indexOf(',') != -1){
				floatList.add(Float.valueOf(property.substring(0, property.indexOf(',') - 1)));
				property = property.substring(property.indexOf(',') + 1);
			}
			floatList.add(Float.valueOf(property.substring(0, property.indexOf(';') - 1)));
			return floatList.toArray(new Float[0]);
		}else{
			throw new TypeNotPresentException(propertyName, null);
		}
	}
	
	private static void parseFile(File file){
		String name = getValueForKey(file, "name");
		if(name != null){
			if(propertyMap.containsKey(name)){
				propertyMap.remove(name);
			}
			propertyMap.put(name, new HashMap<String, String>());
			getAllValues(file, propertyMap.get(name));
		}
	}
	
	private static String getValueForKey(File file, String key){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			do{
				line = reader.readLine();
				if(line != null){
					if(line.toLowerCase().startsWith(key.toLowerCase())){
						reader.close();
						return line.substring(line.indexOf('=') + 1, line.indexOf(';') - 1);
					}
				}
			}while(line != null);
			reader.close();
		}catch (Exception e){
			System.err.println("ERROR: FAILURE READING FILE " + file.getAbsolutePath());
		}
		return null;
	}
	
	private static void getAllValues(File file, Map<String, String> mapToAdd){
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			do{
				line = reader.readLine();
				if(line != null){
					if(!line.toLowerCase().startsWith("#") && line.length() > 5 && line.indexOf('=') != -1 && line.indexOf(';') != -1){
						mapToAdd.put(line.substring(0, line.indexOf('=') - 1).trim(), line.substring(line.indexOf('=') + 1, line.indexOf(';') - 1));
					}
				}
			}while(line != null);
			reader.close();
		}catch (Exception e){
			System.err.println("ERROR: FAILURE READING FILE " + file.getAbsolutePath());
		}
	}
}
