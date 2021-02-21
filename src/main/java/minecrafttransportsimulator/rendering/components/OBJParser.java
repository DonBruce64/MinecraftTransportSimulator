package minecrafttransportsimulator.rendering.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;

/**Class responsible for parsing OBJ models into arrays that can be fed to the GPU.
 * Much more versatile than the Forge system.
 * 
 * @author don_bruce
 */
public final class OBJParser{
	
	public static Map<String, Float[][]> parseOBJModel(String modelLocation){
		Map<String, Float[][]> partMap = new HashMap<String, Float[][]>();
		BufferedReader reader;
		try{
			reader = new BufferedReader(new InputStreamReader(OBJParser.class.getResourceAsStream(modelLocation)));
		}catch(Exception e){
			throw new NullPointerException("Attempted to parse the OBJ model at: " + modelLocation + " but could not find it.  Check the path and try again.");
		}
		
		String partName = null;
		final List<Float[]> vertexList = new ArrayList<Float[]>();
		final List<Float[]> normalList = new ArrayList<Float[]>();
		final List<Float[]> textureList = new ArrayList<Float[]>();
		final List<String> faceList = new ArrayList<String>();
		
		try{
			int lineNumber = 0;
			while(reader.ready()){
				String line = reader.readLine();
				++lineNumber;
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
					try{
						partName = line.trim().substring(2, line.length());
					}catch(Exception e){
						throw new IllegalArgumentException("Object found with no name at line: " + lineNumber + " of: " + modelLocation + ".  Make sure your model exporter isn't making things into groups rather than objects.");
					}
				}
				if(partName != null){
					if(line.startsWith("v ")){
						try{
							Float[] coords = new Float[3];
							line = line.trim().substring(2, line.trim().length()).trim();
							coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ')));
							coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
							coords[2] = Float.valueOf(line.substring(line.lastIndexOf(' ') + 1, line.length()));
							vertexList.add(coords);
						}catch(Exception e){
							throw new NumberFormatException("Could not parse vertex info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Vertex lines must consist of only three numbers (X, Y, Z).");
						}
					}else if(line.startsWith("vt ")){
						try{
							Float[] coords = new Float[2];
							line = line.trim().substring(3, line.trim().length()).trim();
							int space = line.indexOf(' ');
							int vertexEnd = line.lastIndexOf(' ') == space ? line.length() : line.lastIndexOf(' ');
							coords[0] = Float.valueOf(line.substring(0, space));
							coords[1] = 1 - Float.valueOf(line.substring(space + 1, vertexEnd));
							textureList.add(coords);
						}catch(Exception e){
							throw new NumberFormatException("Could not parse vertex texture info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Vertex texture lines must consist of only two numbers (U, V).");
						}
					}else if(line.startsWith("vn ")){
						try{
							Float[] coords = new Float[3];
							line = line.trim().substring(2, line.trim().length()).trim();
							coords[0] = Float.valueOf(line.substring(0, line.indexOf(' ')));
							coords[1] = Float.valueOf(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
							coords[2] = Float.valueOf(line.substring(line.lastIndexOf(' ') + 1, line.length()));
							normalList.add(coords);
						}catch(Exception e){
							throw new NumberFormatException("Could not parse normals info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Normals lines must consist of only three numbers (Xn, Yn, Zn).");
						}
					}else if(line.startsWith("f ")){
						faceList.add(line.trim().substring(2, line.trim().length()));
					}
				}
			}
			
			//End of file.  Save the last part in process and close the file.
			try{
				partMap.put(partName, compileVertexArray(vertexList, normalList, textureList, faceList, partName.toLowerCase().contains("window")));
			}catch(Exception e){
				throw new IllegalArgumentException("Could not compile points of: " + modelLocation + ".  This is likely due to missing UV mapping on some or all faces.");
			}
			reader.close();
			return partMap;
			
		}catch(IOException e){
			throw new IllegalStateException("Could not finish parsing: " + modelLocation + " due to IOException error.  Did the file change state during parsing?");
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
	
	/**
	 *  Checks the passed-in model for any {@link RenderableModelObject}s and returns them
	 *  as a list.  They are also removed from the passed-in model, to ready it
	 *  for post-processing and potential DisplayList creation.  These are cross-checked
	 *  with the passed-in definition to ensure the proper constructors are created.
	 *  The passed-in definition may be null to prevent this check and the removal of components
	 *  due to dynamic JSON definitions.
	 */
	public static <AnimationEntity extends AEntityC_Definable<?>> List<RenderableModelObject<AnimationEntity>> generateRenderables(AnimationEntity entity, String modelLocation, Map<String, Float[][]> parsedModel, List<JSONAnimatedObject> animatedObjects){
		//For anything that has a definition as an animation, add it to an animated list.
		//If we find a definition, we remove the object so it doesn't get packed into the main DisplayList.
		List<RenderableModelObject<AnimationEntity>> modelObjects = new ArrayList<RenderableModelObject<AnimationEntity>>();
		if(animatedObjects != null){
			for(JSONAnimatedObject definition : animatedObjects){
				if(parsedModel.containsKey(definition.objectName)){
					modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, definition.objectName, definition, parsedModel.get(definition.objectName), entity));
					parsedModel.remove(definition.objectName);
				}
			}
		}
		
		//Now check for any non-animated model objects.
		Iterator<Entry<String, Float[][]>> iterator = parsedModel.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, Float[][]> entry = iterator.next();
			RenderableModelObject<AnimationEntity> modelObject = new RenderableModelObject<AnimationEntity>(modelLocation, entry.getKey(), null, entry.getValue(), entity);
			if(!modelObject.transforms.isEmpty()){
				modelObjects.add(modelObject);
				iterator.remove();
			}
		}
		
		return modelObjects;
	}
	
	/**
	 *  Generates an OpenGL DisplayList from the passed-in vertex array, returning the index.
	 */
	public static int generateDisplayList(Float[][] vertices){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Float[] vertex : vertices){
			GL11.glTexCoord2f(vertex[3], vertex[4]);
			GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
			GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		GL11.glEndList();
		return displayListIndex;
	}
	
	/**
	 *  Generates an OpenGL DisplayList from the passed-in OBJ model array, returning the index.
	 */
	public static int generateDisplayList(Map<String, Float[][]> parsedModel){
		int displayListIndex = GL11.glGenLists(1);
		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
		GL11.glBegin(GL11.GL_TRIANGLES);
		for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
			for(Float[] vertex : entry.getValue()){
				GL11.glTexCoord2f(vertex[3], vertex[4]);
				GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
				GL11.glVertex3f(vertex[0], vertex[1], vertex[2]);
			}
		}
		GL11.glEnd();
		GL11.glEndList();
		return displayListIndex;
	}
}
