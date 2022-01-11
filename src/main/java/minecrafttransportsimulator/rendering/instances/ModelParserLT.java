package minecrafttransportsimulator.rendering.instances;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

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
				
				FloatBuffer buffer = FloatBuffer.allocate(tile.boxes.size()*6*8);
				for(LTBox box : tile.boxes){
					//TODO create model and return.
				}
			}
			
			return objectList;
		}catch(IOException e){
			throw new IllegalStateException("Could not finish parsing: " + modelLocation + " due to IOException error.  Did the file change state during parsing?");
		}
	}
	
	public static class LTMainModel{
		public List<LTTileEntry> tiles;
		public int[] min;
		public int[] size;
		public int grid;
		public int count;
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
