package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MTS;

public final class OBJParserSystem{
	public static Map<String, Float[][]> parseOBJModel(String modelLocation){
		try{
			Map<String, Float[][]> partMap = new HashMap<String, Float[][]>();
			BufferedReader reader = new BufferedReader(new FileReader(MTS.assetDir + File.separatorChar + modelLocation));
			
			String partName = null;
			final List<Float[]> vertexList = new ArrayList<Float[]>();
			final List<Float[]> textureList = new ArrayList<Float[]>();
			final List<String> faceList = new ArrayList<String>();
			while(reader.ready()){
				String line = reader.readLine();
				if(line.startsWith("o")){
					//Declaration of an object.
					//Save current part we are parsing (if any) and start new part.
					if(partName != null){
						partMap.put(partName, compileVertexArray(vertexList, textureList, faceList));
						vertexList.clear();
						textureList.clear();
						faceList.clear();
					}
					partName = line.trim().substring(2, line.length());
				}
				if(partName != null){
					if(line.startsWith("v ")){
						Float[] coords = new Float[3];
						line = line.trim().substring(2, line.length());
						coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ') - 1));
						coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ') - 1));
						coords[2] = Float.valueOf(line.substring(line.lastIndexOf(' ') + 1, line.length()));
						vertexList.add(coords);
					}else if(line.startsWith("vt ")){
						Float[] coords = new Float[2];
						line = line.trim().substring(3, line.length());
						coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ') - 1));
						coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.length()));
						textureList.add(coords);
					}else if(line.startsWith("f ")){
						faceList.add(line.trim().substring(2, line.length()));
					}
				}
			}
			//End of file.  Save the last part in process and close the file.
			partMap.put(partName, compileVertexArray(vertexList, textureList, faceList));
			reader.close();
			return partMap;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static Float[][] compileVertexArray(List<Float[]> vertexList, List<Float[]> textureList, List<String> faceList){
		List<Integer[]> faceValues = new ArrayList<Integer[]>();
		for(String faceString : faceList){
			for(byte i=0; i<3; ++i){
				int defEnd = faceString.indexOf(' ');
				String faceDef;
				if(defEnd != -1){
					faceDef = faceString.substring(0, defEnd);
					faceString = faceString.substring(defEnd + 1);
				}else{
					faceDef = faceString;
					faceString = "";
				}
				int vertexNumber = Integer.valueOf(faceDef.substring(0, faceDef.indexOf('/'))) - 1;
				int textureNumber = Integer.valueOf(faceDef.substring(faceDef.indexOf('/') + 1, faceDef.length())) - 1;
				faceValues.add(new Integer[]{vertexNumber, textureNumber});
			}
			
			if(!faceString.isEmpty()){
				//This only happens when there's quads in an obj.  Make a second face.
				//Duplicate point 3, add point 4, and duplicate point 1.
				faceValues.add(faceValues.get(faceValues.size() - 1));
				int vertexNumber = Integer.valueOf(faceString.substring(0, faceString.indexOf('/'))) - 1;
				int textureNumber = Integer.valueOf(faceString.substring(faceString.indexOf('/') + 1, faceString.length())) - 1;
				faceValues.add(new Integer[]{vertexNumber, textureNumber});
				faceValues.add(faceValues.get(faceValues.size() - 5));
			}
		}
		
		//Get the correct offset for face values in the lists.
		int vertexOffset = Integer.MAX_VALUE;
		int textureOffset = Integer.MAX_VALUE;
		for(Integer[] face : faceValues){
			vertexOffset = Math.min(vertexOffset, face[0]);
			textureOffset = Math.min(textureOffset, face[1]);
		}
		
		//Now populate the vertex arrays.
		List<Float[]> vertexArrays = new ArrayList<Float[]>();
		for(Integer[] face : faceValues){
			vertexArrays.add(new Float[]{
				vertexList.get(face[0] - vertexOffset)[0],
				vertexList.get(face[0] - vertexOffset)[1],
				vertexList.get(face[0] - vertexOffset)[2],
				textureList.get(face[1] - textureOffset)[0],
				textureList.get(face[1] - textureOffset)[1]
				});
		}
		return vertexArrays.toArray(new Float[vertexArrays.size()][5]);
	}
}
