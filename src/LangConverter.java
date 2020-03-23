import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class LangConverter {
	
	public static void main(String args[]){
		try{
			File currentDir = new File(LangConverter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			
			//Find .lang file.
			File langFile = null;
			for(File file : currentDir.listFiles()){
				if(file.getName().endsWith(".lang")){
					langFile = file;
					break;
				}
			}
			if(langFile == null){
				System.out.println("ERROR: No lang file found in current directory!");
				return;
			}
			
			//Index entries.
			Map<String, String> names = new HashMap<String, String>();
			Map<String, String> descriptions = new HashMap<String, String>();
			BufferedReader reader = new BufferedReader(new FileReader(langFile));
			while(reader.ready()){
				String line = reader.readLine();
				int equalsIndex = line.indexOf('=');
				if(equalsIndex != -1){
					String text = line.substring(equalsIndex + 1);
					int lastDot = line.substring(0, equalsIndex).lastIndexOf('.');
					int secondLastDot = line.substring(0, lastDot).lastIndexOf('.');
					if(lastDot != -1){
						if(line.contains("name")){
							String key = line.substring(secondLastDot + 1, equalsIndex - ".name".length());
							names.put(key, text);
						}else if(line.contains("description")){
							String key;
							if(line.startsWith("description")){
								key = line.substring(lastDot + 1, equalsIndex);
							}else{
								key = line.substring(secondLastDot + 1, equalsIndex - ".description".length());
							}
							descriptions.put(key, text);
						}
					}
				}
			}
			reader.close();
			
			//Make the conversions dir if it doesn't exist.
			File conversionDir = new File(currentDir, "conversions");
			if(!conversionDir.exists()){
				conversionDir.mkdir();
			}
			
			//Now iterate over all .json files and add-in entries as appropriate.
			for(File file : currentDir.listFiles()){
				String fileName = file.getName();
				if(fileName.endsWith(".json") && !fileName.contains("converted")){
					fileName = fileName.substring(0, fileName.length() - ".json".length());
					reader = new BufferedReader(new FileReader(file));
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(conversionDir, fileName + ".json")));
					boolean isVehicle = false;
					while(reader.ready()){
						String line = reader.readLine();
						if(line.contains("definitions")){
							isVehicle = true;
							writer.write(line + "\n");
						}else if(line.contains("\"general\"")){
							writer.write(line + "\n");
							if(names.containsKey(fileName) && !isVehicle){
								writer.write("\t\"name\" : \"" + names.get(fileName) + "\",\n");
							}
							if(descriptions.containsKey(fileName)){
								writer.write("\t\"description\" : \"" + descriptions.get(fileName) + "\",\n");
							}
						}else if(line.contains("subName")){
							String subName = line.substring(line.indexOf('"', line.indexOf(':')) + 1, line.lastIndexOf('"'));
							if(names.containsKey(fileName + subName)){
								writer.write("\t\t\"name\" : \"" + names.get(fileName + subName) + "\",\n");
							}
							writer.write(line + "\n");
						}else{
							writer.write(line + "\n");	
						}
					}
					reader.close();
					writer.close();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
