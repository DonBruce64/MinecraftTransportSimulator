import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The FMCMRT (Flans Mod Compiled Model Recovery Tool) is used to recover and
 * re-build models that have been compiled into .java files that Toolbox can
 * import.  These models must be de-compiled by an external program, but
 * once this is done they can be corrected with this program.
 * 
 * @author don_bruce
 */
public class FMCMRT{
	private static final Map<String, String> mappings = genMappings();
	
	private static Map<String, String> genMappings(){
		Map<String, String> mappings = new HashMap<>();
		mappings.put("func_78790_a", "addBox");
		mappings.put("func_78793_a", "setRotationPoint");
		mappings.put("field_78795_f", "rotateAngleX");
		mappings.put("field_78796_g", "rotateAngleY");
		mappings.put("field_78808_h", "rotateAngleZ");
		mappings.put("this.", "");
		return mappings;
	}
	public static void main(String[] args){
		//Coord2D needs to have only ints in it.
		//Same goes for the indexes before the float[] array.
		try{
			//Check to make sure we passed-in a file for conversion.
			if(args.length == 0){
				System.out.println("ERROR: No file specified for conversion.");
				return;
			}
			//Set-up the input and output file object.  If we can't find the input, error out and exit.
			File decompFile = new File(args[0]);
			if(!decompFile.exists()){
				System.out.println("ERROR: File not found.  Check to make sure the file and FMCMR are in the same folder and you are running this program from that folder.");
				return;
			}
			BufferedReader decompFileReader = new BufferedReader(new FileReader(decompFile));
			BufferedWriter convertedFileWriter = new BufferedWriter(new FileWriter(new File("converted_" + decompFile.getName())));
			
			//Iterate over all lines in the decomp file and put them in the converted file.
			while(decompFileReader.ready()){
				String line = decompFileReader.readLine();
				
				//Trim spaces from the line to make parsing easier.
				line = line.trim();
				
				//Perform mappings replacement.
				for(Entry<String, String> mapping : mappings.entrySet()){
					line = line.replace(mapping.getKey(), mapping.getValue());
				}
				
				//If we are at the start of the class, make sure we are aligned.
				//The decompiler sometimes doesn't align the class def on a single line.
				//This causes parts to not be split right.
				if(line.startsWith("public class")){
					line += " extends ModelVehicle";
				}else if(line.startsWith("extends")){
					//Don't write this line.
					continue;
				}
				
				//If the line is a rotate line, trim the parethesis around it.
				//This is needed so Toolbox can see a line with rotateAngle.
				if(line.contains("rotateAngle")){
					line = line.substring(1).replace(").rotateAngle", ".rotateAngle");
				}
				
				//Check to see if we have an array index in the line.  If so, we know it's part of a part.
				int arrayBracketStartIndex = line.indexOf('[');
				int arrayBracketEndIndex = line.indexOf(']');
				if(arrayBracketStartIndex != -1 && arrayBracketEndIndex != -1 && arrayBracketStartIndex < arrayBracketEndIndex){
					//Check to make sure we are actually referencing a part.  This will be true if we have one of the following:
					//1) "new ModelRendererTurbo(this,".
					//2) .addBox.
					//3) .addShapebox.
					//4) .addShape3D
					//If we are, add on a comment to the end of the line.
					if(line.contains("new ModelRendererTurbo(this,") || line.contains(".addBox") || line.contains(".addShapeBox") || line.contains(".addShape3D")){
						int indexNumber = Integer.valueOf(line.substring(arrayBracketStartIndex + 1, arrayBracketEndIndex));
						String partName = line.substring(0, arrayBracketStartIndex).trim();
						line += " // " + partName + "_" + String.valueOf(indexNumber);
						
						//If we have a shape3D, do special logic to remove floating-point numbers.
						if(line.contains("addShape3D")){
							//Go through the line, detecting all Coord2Ds.
							//If we see one, grab the first two numbers and strip their decimal places.
							//Append those numbers to the old string in the indexes found.
							int c2dIndexStart = line.indexOf("new Coord2D(") + "new Coord2D(".length();
							int c2dIndexDoubleEnd = 0;
							String newCombinedString = "";
							while(c2dIndexStart != -1 + "new Coord2D(".length()){
								//Add anything before this C2D that we passed-over.  This needs to be done before we start parsing.
								newCombinedString += line.substring(c2dIndexDoubleEnd, c2dIndexStart);
								
								//Get the first number knowing that it will have a comma or space after it.
								int indexComma = line.indexOf(",", c2dIndexStart);
								int indexSpace = line.indexOf(" ", c2dIndexStart);
								int indexfirstEnd = indexComma < indexSpace ? indexComma : indexSpace;
								String firstNumber = line.substring(c2dIndexStart, indexfirstEnd);
								
								//Get the second number, knowing that we will have both a comma and a space after the first.
								indexComma = line.indexOf(",", indexfirstEnd + 2);
								indexSpace = line.indexOf(" ", indexfirstEnd + 2);
								c2dIndexDoubleEnd = indexComma < indexSpace ? indexComma : indexSpace;
								String secondNumber = line.substring(indexfirstEnd + 2, c2dIndexDoubleEnd);
								
								//Add numbers to the combined string, stripping off the decimal portions.
								newCombinedString += firstNumber.substring(0, firstNumber.length() - 3) + ", " + secondNumber.substring(0, secondNumber.length() - 3);
								
								//Get the next index.
								c2dIndexStart = line.indexOf("new Coord2D(", c2dIndexStart) + "new Coord2D(".length();
							}
							
							//Next we need to check for the float that's after the Coord2D array.
							//Should be the first number after, so we can look for an F and catch and strip it.
							int floatDecimalIndex = line.indexOf(".0F", c2dIndexDoubleEnd);
							newCombinedString += line.substring(c2dIndexDoubleEnd, floatDecimalIndex);
							
							//Add the last bit to the combined string, and set the line to that value.
							line = newCombinedString + line.substring(floatDecimalIndex + 3);
						}
					}
				}
				
				//Write the modified line to the converted file, add a newline to keep things aligned.
				convertedFileWriter.write(line + "\n");
			}
			
			//Close reader and writer.
			decompFileReader.close();
			convertedFileWriter.close();
		
		}catch(IOException e){
			System.out.println("ERROR: I/O Exception caught when performing operations.");
			e.printStackTrace();
		}
	}
}