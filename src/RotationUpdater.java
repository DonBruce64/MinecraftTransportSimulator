import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class RotationUpdater {
	
	public static void main(String args[]){
		try{
			File currentDir = new File(RotationUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			
			//Iterate through all directories and sub-directories for JSON files.
			for(File file : currentDir.listFiles()){
				if(file.isDirectory()){
					parseDirectory(file);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void parseDirectory(File directory){
		//Iterate through all files in this directory.
		for(File file : directory.listFiles()){
			if(file.isDirectory()){
				//Need to parse the directory in this directory.
				parseDirectory(file);
			}else{
				//If file is a json start updating it.
				if(file.getName().endsWith(".json")){
					try{
						System.out.println("Found file: " + file.getName());
						BufferedReader reader = new BufferedReader(new FileReader(file));
						List<String> outputLines = new ArrayList<String>();
						List<String> sectionLines = new ArrayList<String>();
						int variableLineIndex = -1;
						int axisLineIndex = -1;
						
						//Keep reading lines until we get to rotation or translation definitions.
						//If we are in a definition, check if we need to update it.
						boolean inDefinition = false;
						boolean inSection = false;
						while(reader.ready()){
							String line = reader.readLine();
							if(inDefinition){
								if(inSection){
									//If we are in a section, don't add lines to the output.
									//Instead, save them for parsing later.
									sectionLines.add(line);
									if(line.contains("Variable")){
										variableLineIndex = sectionLines.size() - 1;
									}else if(line.contains("Axis")){
										axisLineIndex = sectionLines.size() - 1;
									}else if(line.contains("}")){
										//End of the section, apply math operations and save new definition.
										//First get the variable itself.
										String variableLine = sectionLines.get(variableLineIndex);
										String variable = variableLine.substring(variableLine.lastIndexOf("\"", variableLine.lastIndexOf("\"") - 1) + 1, variableLine.lastIndexOf("\""));
										System.out.println("Found variable: " + variable);
										
										//Check if the variable needs to be re-named.
										boolean renameVariable = true;
										if(variable.equals("gearshift") || variable.equals("gearshift_hvertical") || variable.equals("gearshift_hhorizontal")){
											variable = "engine_" + variable + "_1";
										}else if(variable.equals("driveshaft")){
											variable = "engine_driveshaft_rotation_1";
										}else if(variable.equals("engine")){
											variable = "engine_rotation_1";
										}else if(variable.equals("steeringwheel")){
											variable = "steering_wheel";
										}else{
											renameVariable = false;
										}
										if(renameVariable){
											//Add back the new variable name and update it back to the list.
											variableLine = variableLine.substring(0, variableLine.lastIndexOf("\"", variableLine.lastIndexOf("\"") - 1) + 1) + variable + variableLine.substring(variableLine.lastIndexOf("\""), variableLine.length());
											sectionLines.set(variableLineIndex, variableLine);
											System.out.println("Applied re-name to: " + variable);
										}
										
										//Get the variable factor.
										float variableFactor;
										if(variable.equals("door") || variable.equals("hood")){
											variableFactor = 60;
										}else if(variable.equals("throttle") || variable.equals("brake")){
											variableFactor = 25;
										}else if(variable.equals("p_brake") || variable.equals("hookup") || variable.equals("trailer")){
											variableFactor = 30;
										}else{
											variableFactor = 1;
										}
										
										//Apply the variable factor if required.
										if(variableFactor != 1){
											String axisLine = sectionLines.get(axisLineIndex);
											int firstComma = axisLine.indexOf(",");
											int secondComma = axisLine.indexOf(",", firstComma + 1);
											float firstNumber = Float.valueOf(axisLine.substring(axisLine.indexOf("[") + 1, firstComma))*variableFactor;
											float secondNumber = Float.valueOf(axisLine.substring(firstComma + 1, secondComma))*variableFactor;
											float thirdNumber = Float.valueOf(axisLine.substring(secondComma + 1, axisLine.indexOf("]")))*variableFactor;
											
											//Update the axis line.
											axisLine = axisLine.substring(0, axisLine.indexOf("[") + 1) + firstNumber + ", " + secondNumber + ", " + thirdNumber + axisLine.substring(axisLine.indexOf("]"));
											sectionLines.set(axisLineIndex, axisLine);
											System.out.println("Applied factor of: " + variableFactor);
										}
										
										//Save all the lines to the master list and reset the flag.
										outputLines.addAll(sectionLines);
										sectionLines.clear();
										inSection = false;
									}
								}else{
									outputLines.add(line);
									if(line.contains("{")){
										inSection = true;
									}
								}
							}else{
								//If we have an instrument, apply corrections to it as well.
								if(line.contains("hudpos")){
									System.out.println("Auto-fixing instrument position.");
									String spacing = line.substring(0, line.indexOf("\""));
									int hudX = Integer.valueOf(line.substring(line.indexOf("[") + 1, line.indexOf(",")).trim());
									int hudY = Integer.valueOf(line.substring(line.indexOf(",") + 1, line.indexOf("]")).trim());
									
									hudX = (int) (400D*(hudX/100D));
									hudY = (int) (2D*(hudY/100D -0.5D)*140);
									
									outputLines.add(spacing + "\"hudX\" : " + hudX + ",");
									outputLines.add(spacing + "\"hudY\" : " + hudY + ",");
								}else if(line.contains("hudScale")){
									System.out.println("Auto-fixing instrument scale.");
									String spacing = line.substring(0, line.indexOf("\""));
									float hudScale = Float.valueOf(line.substring(line.indexOf(":") + 1, line.length()));
									outputLines.add(spacing + "\"hudScale\" : " + hudScale/2);
								}else{
									outputLines.add(line);
								}
								if(line.contains("rotatableModelObjects") || line.contains("translatableModelObjects")){
									inDefinition = true;
								}
							}
						}
						reader.close();
						
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						for(String line : outputLines){
							writer.write(line + "\n");
						}
						writer.flush();
						writer.close();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
	}
}
