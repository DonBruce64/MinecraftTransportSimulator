package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;

/**Class responsible for parsing OBJ models into arrays that can be fed to the GPU.
 * Much more versatile than the Forge system.
 * 
 * @author don_bruce
 */
public final class OBJParserSystem{
	public static Map<String, Float[][]> parseOBJModel(String packID, String modelLocation){
		try{
			Map<String, Float[][]> partMap = new HashMap<String, Float[][]>();
			BufferedReader reader = new BufferedReader(new InputStreamReader (OBJParserSystem.class.getResourceAsStream("/assets/" + packID + "/" + modelLocation)));
			
			String partName = null;
			final List<Float[]> vertexList = new ArrayList<Float[]>();
			final List<Float[]> normalList = new ArrayList<Float[]>();
			final List<Float[]> textureList = new ArrayList<Float[]>();
			final List<String> faceList = new ArrayList<String>();
			while(reader.ready()){
				String line = reader.readLine();
				if(line.isEmpty()){
					continue;
				}
				if(line.startsWith("o")){
					//Declaration of an object.
					//Save current part we are parsing (if any) and start new part.
					if(partName != null){
						partMap.put(partName, compileVertexArray(vertexList, normalList, textureList, faceList, partName.toLowerCase().contains("window")));
						vertexList.clear();
						normalList.clear();
						textureList.clear();
						faceList.clear();
					}
					partName = line.trim().substring(2, line.length());
				}
				if(partName != null){
					if(line.startsWith("v ")){
						Float[] coords = new Float[3];
						line = line.trim().substring(2, line.trim().length()).trim();
						coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ')));
						coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
						coords[2] = Float.valueOf(line.substring(line.lastIndexOf(' ') + 1, line.length()));
						vertexList.add(coords);
					}else if(line.startsWith("vt ")){
						Float[] coords = new Float[2];
						line = line.trim().substring(3, line.trim().length()).trim();
						int space = line.indexOf(' ');
						int vertexEnd = line.lastIndexOf(' ') == space ? line.length() : line.lastIndexOf(' ');
						coords[0] = Float.valueOf(line.substring(0, space));
						coords[1] = 1 - Float.valueOf(line.substring(space + 1, vertexEnd));
						textureList.add(coords);
					}else if(line.startsWith("vn ")){
						Float[] coords = new Float[3];
						line = line.trim().substring(2, line.trim().length()).trim();
						coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ')));
						coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
						coords[2] = Float.valueOf(line.substring(line.lastIndexOf(' ') + 1, line.length()));
						normalList.add(coords);
					}else if(line.startsWith("f ")){
						faceList.add(line.trim().substring(2, line.trim().length()));
					}
				}
			}
			//End of file.  Save the last part in process and close the file.
			partMap.put(partName, compileVertexArray(vertexList, normalList, textureList, faceList, partName.toLowerCase().contains("window")));
			reader.close();
			return partMap;
		}catch (Exception e){
			MTS.MTSLog.error("AN ERROR WAS ENCOUNTERED WHEN TRY TO PARSE: " + packID + ":" + modelLocation);
			MTS.MTSLog.error(e.getMessage());
			return null;
		}
	}
	
	private static Float[][] compileVertexArray(List<Float[]> vertexList, List<Float[]> normalList, List<Float[]> textureList, List<String> faceList, boolean isWindow){
		List<Integer[]> vertexDataSets = new ArrayList<Integer[]>();
		for(String faceString : faceList){
			List<Integer[]> faceVertexData = new ArrayList<Integer[]>();	
			while(!faceString.isEmpty()){
				//Get the face string in format X/Y/Z.  Use the space as a separator between vertices making up the face.
				int defEnd = faceString.indexOf(' ');
				String faceDef;
				if(defEnd != -1){
					//Take the faceDef from the faceString and store it.
					faceDef = faceString.substring(0, defEnd);
					faceString = faceString.substring(defEnd + 1);
				}else{
					//We are at the last face vertex here, so just mark the face as the existing string.
					faceDef = faceString;
					faceString = "";
				}
				
				//Vertex number is the first entry before the slash.
				//Texture number is the second entry between the two slashes.
				//Normal number is the third entry after the second slash.
				//Parse all these out and store them in the array.
				int firstSlash = faceDef.indexOf('/');
				int secondSlash = faceDef.lastIndexOf('/');
				int vertexNumber = Integer.valueOf(faceDef.substring(0, firstSlash)) - 1;
				int textureNumber = Integer.valueOf(faceDef.substring(firstSlash + 1, secondSlash)) - 1;
				int normalNumber = Integer.valueOf(faceDef.substring(secondSlash + 1)) - 1;
				
				//If we have three or more points in faceValues, it means we need to make a triangle out of this shape.
				//Add the first point, the most recent point, and this point to make a triangle.
				//Otherwise, just add the face as-is.
				if(faceVertexData.size() >= 3){
					faceVertexData.add(faceVertexData.get(0));
					faceVertexData.add(faceVertexData.get(faceVertexData.size() - 2));
				}
				faceVertexData.add(new Integer[]{vertexNumber, textureNumber, normalNumber});
			}
			vertexDataSets.addAll(faceVertexData);
		}
		
		//Get the correct offset for face values in the lists.
		//Find the smallest face number and use that as the offset.
		int vertexOffset = Integer.MAX_VALUE;
		int textureOffset = Integer.MAX_VALUE;
		int normalOffset = Integer.MAX_VALUE;
		for(Integer[] vertexData : vertexDataSets){
			vertexOffset = Math.min(vertexOffset, vertexData[0]);
			textureOffset = Math.min(textureOffset, vertexData[1]);
			normalOffset = Math.min(normalOffset, vertexData[2]);
		}
		
		//Populate the vertex array in order of the vertices used in the faces.
		List<Float[]> vertexArray = new ArrayList<Float[]>();
		for(Integer[] vertexData : vertexDataSets){
			vertexArray.add(vertexList.get(vertexData[0] - vertexOffset));
		}
		
		//Now populate the texture array.
		//If we are parsing windows override the texture coords.
		List<Float[]> textureArray = new ArrayList<Float[]>();
		if(isWindow){
			for(int i=0; i<vertexDataSets.size(); i+=3){
				textureArray.add(new Float[]{0.0F, 1.0F});
				textureArray.add(new Float[]{1.0F, 1.0F});
				textureArray.add(new Float[]{1.0F, 0.0F});
				//If we have only 3 points, it means this window is just a single triangle.
				//Don't add the 4th fake point and just end compilation here.
				if(vertexArray.size() > 3){
					textureArray.add(new Float[]{0.0F, 1.0F});
					textureArray.add(new Float[]{1.0F, 0.0F});
					textureArray.add(new Float[]{0.0F, 0.0F});
				}else{
					break;
				}
			}
		}else{
			for(Integer[] face : vertexDataSets){
				textureArray.add(textureList.get(face[1] - textureOffset));
			}
		}
		
		//Finally, populate the normal array.
		List<Float[]> normalArray = new ArrayList<Float[]>();
		for(Integer[] face : vertexDataSets){
			normalArray.add(normalList.get(face[2] - normalOffset));
		}

		//Compile arrays and return.
		List<Float[]> compiledArray = new ArrayList<Float[]>();
		for(int i=0; i<vertexArray.size(); ++i){
			compiledArray.add(new Float[]{
				vertexArray.get(i)[0],
				vertexArray.get(i)[1],
				vertexArray.get(i)[2],
				textureArray.get(i)[0],
				textureArray.get(i)[1],
				normalArray.get(i)[0],
				normalArray.get(i)[1],
				normalArray.get(i)[2]
			});
		}
		
		return compiledArray.toArray(new Float[compiledArray.size()][8]);
	}
}
