package minecrafttransportsimulator.rendering.instances;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3f;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceEventsModelLoader;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.rendering.components.AModelParser;
import minecrafttransportsimulator.rendering.components.RenderableObject;

/**Class responsible for parsing Little Tiles models into arrays that can be fed to the GPU.
 * 
 * @author don_bruce
 */
public final class ModelParserLT extends AModelParser{
	
	@Override
	protected String getModelSuffix(){
		return "txt";
	}
	
	@Override
	protected List<RenderableObject> parseModelInternal(String modelLocation){
		List<RenderableObject> objectList = new ArrayList<RenderableObject>();
		InputStreamReader reader;
		try{
			reader = new InputStreamReader(ModelParserLT.class.getResourceAsStream(modelLocation));
		}catch(Exception e){
			throw new NullPointerException("Attempted to parse the Little Tiles model at: " + modelLocation + " but could not find it.  Check the path and try again.");
		}
		
		try{
			LTMainModel model = JSONParser.parseStream(reader, LTMainModel.class, null, null);
			reader.close();
			
			//Parse through model boxes and create objects.
			float scale = 1F/model.grid;
			for(LTTileEntry tile : model.tiles){
				if(tile.bBox != null){
					//Move single box to list for ease of parsing.
					tile.boxes = new ArrayList<LTBox>();
					tile.boxes.add(tile.bBox);
				}
				
				//8 floats per vert, 6 verts per side, 6 sides per box.
				FloatBuffer buffer = FloatBuffer.allocate(tile.boxes.size()*6*6*8);
				Point3f normal = new Point3f();
				Point3f min = new Point3f();
				Point3f max = new Point3f();
				
				float[] uvPoints = InterfaceEventsModelLoader.getDefaultBlockTexture(tile.tile.block);
				
				for(LTBox box : tile.boxes){
					for(int i=0; i<6; ++i){
						switch(i){
							case(0):{//Bottom
								normal.set(0, -1, 0);
								min.set(box.pos2[0]*scale, box.pos1[1]*scale, box.pos1[2]*scale);
								max.set(box.pos1[0]*scale, box.pos1[1]*scale, box.pos2[2]*scale);
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[0]-box.pos1[0])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[2]-box.pos1[2])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, false, buffer);
								break;
							}
							case(1):{//Top
								normal.set(0, 1, 0);
								min.set(box.pos1[0]*scale, box.pos2[1]*scale, box.pos1[2]*scale);
								max.set(box.pos2[0]*scale, box.pos2[1]*scale, box.pos2[2]*scale);
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[0]-box.pos1[0])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[2]-box.pos1[2])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, false, buffer);
								break;
							}
							case(2):{//North (-Z)
								normal.set(0, 0, -1);
								min.set(box.pos2[0]*scale, box.pos1[1]*scale, box.pos1[2]*scale);
								max.set(box.pos1[0]*scale, box.pos2[1]*scale, box.pos1[2]*scale);
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[0]-box.pos1[0])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[1]-box.pos1[1])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, true, buffer);
								break;
							}
							case(3):{//South (+Z)
								normal.set(0, 0, 1);
								min.set(box.pos1[0]*scale, box.pos1[1]*scale, box.pos2[2]*scale);
								max.set(box.pos2[0]*scale, box.pos2[1]*scale, box.pos2[2]*scale);
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[0]-box.pos1[0])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[1]-box.pos1[1])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, true, buffer);
								break;
							}
							case(4):{//East (+X)
								normal.set(1, 0, 0);
								min.set(box.pos2[0]*scale, box.pos1[1]*scale, box.pos2[2]*scale);
								max.set(box.pos2[0]*scale, box.pos2[1]*scale, box.pos1[2]*scale);								
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[2]-box.pos1[2])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[1]-box.pos1[1])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, true, buffer);
								break;
							}
							case(5):{//West (-X)
								normal.set(-1, 0, 0);
								min.set(box.pos1[0]*scale, box.pos1[1]*scale, box.pos1[2]*scale);
								max.set(box.pos1[0]*scale, box.pos2[1]*scale, box.pos2[2]*scale);
								float U = uvPoints[0] + (uvPoints[1] - uvPoints[0])*(box.pos2[2]-box.pos1[2])*scale;
								float V = uvPoints[2] + (uvPoints[3] - uvPoints[2])*(box.pos2[1]-box.pos1[1])*scale;
								addFaceToBuffer(min, max, normal, uvPoints[0], U, uvPoints[2], V, true, buffer);
								break;
							}
						}
					}
				}
				RenderableObject newObject = new RenderableObject("little_tiles_generated", RenderableObject.GLOBAL_TEXTURE_NAME, new ColorRGB(tile.tile.color), buffer, false);
				objectList.add(newObject);
			}
			
			return objectList;
		}catch(IOException e){
			throw new IllegalStateException("Could not finish parsing: " + modelLocation + " due to IOException error.  Did the file change state during parsing?");
		}
	}
	
	private static void addFaceToBuffer(Point3f min, Point3f max, Point3f normal, float u, float U, float v, float V, boolean horzontalFace, FloatBuffer buffer){
		for(int i=0; i<6; ++i){
			//Normals are just what the point has.
			buffer.put(normal.x);
			buffer.put(normal.y);
			buffer.put(normal.z);
			
			//Texture and vertex X/Y/Z are based on vertex index.
			switch(i){
				case(0)://Bottom-right
				case(3):{
					buffer.put(U);
					buffer.put(V);
					if(horzontalFace){
						buffer.put(max.x);
						buffer.put(min.y);
						buffer.put(max.z);
					}else{
						buffer.put(max.x);
						buffer.put(max.y);
						buffer.put(max.z);
					}
					break;
				}
				case(1):{//Top-right
					buffer.put(U);
					buffer.put(v);
					if(horzontalFace){
						buffer.put(max.x);
						buffer.put(max.y);
						buffer.put(max.z);
					}else{
						buffer.put(max.x);
						buffer.put(max.y);
						buffer.put(min.z);
					}
					break;
				}
				case(2)://Top-left
				case(4):{
					buffer.put(u);
					buffer.put(v);
					if(horzontalFace){
						buffer.put(min.x);
						buffer.put(max.y);
						buffer.put(min.z);
					}else{
						buffer.put(min.x);
						buffer.put(max.y);
						buffer.put(min.z);
					}
					break;
				}
				case(5):{//Bottom-left
					buffer.put(u);
					buffer.put(V);
					if(horzontalFace){
						buffer.put(min.x);
						buffer.put(min.y);
						buffer.put(min.z);
					}else{
						buffer.put(min.x);
						buffer.put(min.y);
						buffer.put(max.z);
					}					
					break;
				}
			}
		}
	}
	
	public static class LTMainModel{
		public List<LTTileEntry> tiles;
		public int grid;
	}
	
	public static class LTTileEntry{
		public LTBox bBox;
		public List<LTBox> boxes;
		public LTPropertiesEntry tile; 
	}
	
	public static class LTBox{
		public int[] pos1;
		public int[] pos2;
	}
	
	public static class LTPropertiesEntry{
		public int color;
		public String block;
	}
}
