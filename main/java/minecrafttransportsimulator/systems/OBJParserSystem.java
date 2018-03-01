package minecrafttransportsimulator.systems;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**Class responsible for parsing OBJ models into arrays that can be fed to the GPU.
 * Much more versatile than the Forge system.
 * 
 * @author don_bruce
 */
public final class OBJParserSystem{
	public static Map<String, Float[][]> parseOBJModel(ResourceLocation location){
		try{
			Map<String, Float[][]> partMap = new HashMap<String, Float[][]>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()));
			
			String partName = null;
			final List<Float[]> vertexList = new ArrayList<Float[]>();
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
						partMap.put(partName, compileVertexArray(vertexList, textureList, faceList, partName.toLowerCase().contains("window")));
						vertexList.clear();
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
						coords[1] = Float.valueOf(line.substring(space + 1, vertexEnd));
						textureList.add(coords);
					}else if(line.startsWith("f ")){
						faceList.add(line.trim().substring(2, line.trim().length()));
					}
				}
			}
			//End of file.  Save the last part in process and close the file.
			partMap.put(partName, compileVertexArray(vertexList, textureList, faceList, partName.toLowerCase().contains("window")));
			reader.close();
			return partMap;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static Float[][] compileVertexArray(List<Float[]> vertexList, List<Float[]> textureList, List<String> faceList, boolean isWindow){
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
				int slash = faceDef.indexOf('/');
				int faceEnd = faceDef.lastIndexOf('/') == slash ? faceDef.length() : faceDef.lastIndexOf('/');
				int vertexNumber = Integer.valueOf(faceDef.substring(0, slash)) - 1;
				//Make sure texture is defined for this shape before trying to load it in.
				if(faceDef.substring(slash + 1, faceEnd).equals("")){
					faceString = "";
					break;
				}else{
					int textureNumber = Integer.valueOf(faceDef.substring(slash + 1, faceEnd)) - 1;
					faceValues.add(new Integer[]{vertexNumber, textureNumber});
				}
			}
			
			if(!faceString.isEmpty()){
				//This only happens when there's quads in an obj.
				//If we are not a window make a second face by
				//duplicating point 3, adding point 4, and duplicating point 1.
				//Otherwise put the 4th face in the list.
				int defEnd = faceString.indexOf(' ');
				if(defEnd != -1){
					faceString = faceString.substring(0, defEnd);
				}
				
				int slash = faceString.indexOf('/');
				int faceEnd = faceString.lastIndexOf('/') == slash ? faceString.length() : faceString.lastIndexOf('/');
				int vertexNumber = Integer.valueOf(faceString.substring(0, slash)) - 1;
				int textureNumber = Integer.valueOf(faceString.substring(slash + 1, faceEnd)) - 1;
				if(!isWindow){
					faceValues.add(faceValues.get(faceValues.size() - 1));
					faceValues.add(new Integer[]{vertexNumber, textureNumber});
					faceValues.add(faceValues.get(faceValues.size() - 5));
				}else{
					faceValues.add(new Integer[]{vertexNumber, textureNumber});
				}
			}
		}
		
		//Get the correct offset for face values in the lists.
		//Find the smallest face number and use that as the offset.
		int vertexOffset = Integer.MAX_VALUE;
		int textureOffset = Integer.MAX_VALUE;
		for(Integer[] face : faceValues){
			vertexOffset = Math.min(vertexOffset, face[0]);
			textureOffset = Math.min(textureOffset, face[1]);
		}
		
		//Populate the vertex array in order of the vertcies used in the faces.
		List<Float[]> vertexArray = new ArrayList<Float[]>();
		for(Integer[] face : faceValues){
			vertexArray.add(vertexList.get(face[0] - vertexOffset));
		}
		
		//Now populate the texture array.
		//If we are parsing windows override the texture coords.
		List<Float[]> textureArray = new ArrayList<Float[]>();
		if(isWindow){
			for(int i=0; i<=vertexArray.size() - 4; i += 4){
				textureArray.add(new Float[]{0.0F, 1.0F});
				textureArray.add(new Float[]{1.0F, 1.0F});
				textureArray.add(new Float[]{1.0F, 0.0F});
				textureArray.add(new Float[]{0.0F, 0.0F});
			}
		}else{
			for(Integer[] face : faceValues){
				textureArray.add(textureList.get(face[1] - textureOffset));
			}
		}

		//Finally, create a normal array from the vertex array.
		List<Float[]> normalArray = new ArrayList<Float[]>();
		for(int i=0; i<=faceValues.size() - (isWindow ? 4 : 3); i += (isWindow ? 4 : 3)){
			Float[] faceVertex1 = vertexArray.get(i);
			Float[] faceVertex2 = vertexArray.get(i + 1);
			Float[] faceVertex3 = vertexArray.get(i + 2);
			Vector3f v1 = new Vector3f(faceVertex1[0], faceVertex1[1], faceVertex1[2]);
			Vector3f v2 = new Vector3f(faceVertex2[0], faceVertex2[1], faceVertex2[2]);
			Vector3f v3 = new Vector3f(faceVertex3[0], faceVertex3[1], faceVertex3[2]);
			Vector3f norm = Vector3f.cross(Vector3f.sub(v2, v1, null), Vector3f.sub(v3, v1, null), null).normalise(null);
			
			//Add once for each vertex that was parsed.
			normalArray.add(new Float[]{norm.x, norm.y, norm.z});
			normalArray.add(new Float[]{norm.x, norm.y, norm.z});
			normalArray.add(new Float[]{norm.x, norm.y, norm.z});
			//Although we calculated normals with 3 vertices, the same normals apply for the 4th if this is a window.
			if(isWindow){
				normalArray.add(new Float[]{norm.x, norm.y, norm.z});	
			}
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
