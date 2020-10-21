package minecrafttransportsimulator.packloading;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.tree.analysis.Value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle;

/**This class contains the various custom type adapters used in JSON parsing operations.
 * Put here to customize how we import/export JSONs.  For importing, this is pretty much
 * un-changed.  For exporting, this prevents writing default values to the JSON for clarity,
 * and changes how lists are written to make the JSONs more compact.
*
* @author don_bruce
*/
public class JSONTypeAdapters{
	
	public static Gson getParserWithAdapters(){
		return new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.registerTypeAdapter(Boolean.class, booleanAdapter)
				.registerTypeAdapter(Integer.class, integerAdapter)
				.registerTypeAdapter(Float.class, floatAdapter)
				.registerTypeAdapter(Point3d.class, point3dAdapter)
				.registerTypeAdapter(new TypeToken<List<String>>(){}.getType(), stringListAdapter)
				.create();
	}
	
	private static final TypeAdapter<Boolean> booleanAdapter = new TypeAdapter<Boolean>(){	
		@Override
		public Boolean read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				return reader.nextBoolean();
			}
		}
		
		@Override
		public void write(JsonWriter writer, Boolean value) throws IOException{
			//Only write booleans that are true to save space.
			if(!value){
				writer.nullValue();
			}else{
				writer.value(value);
			}
		}
	};
	
	private static final TypeAdapter<Integer> integerAdapter = new TypeAdapter<Integer>(){	
		@Override
		public Integer read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				return reader.nextInt();
			}
		}
		
		@Override
		public void write(JsonWriter writer, Integer value) throws IOException{
			//Only write non-zero integers to save space.
			if(value.intValue() == 0){
				writer.nullValue();
			}else{
				writer.value(value);
			}
		}
	};
	
	private static final TypeAdapter<Float> floatAdapter = new TypeAdapter<Float>(){	
		@Override
		public Float read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				return (float) reader.nextDouble();
			}
		}
		
		@Override
		public void write(JsonWriter writer, Float value) throws IOException{
			//Only write non-zero floats to save space.
			if(value.floatValue() == 0){
				writer.nullValue();
			}else{
				writer.value(value);
			}
		}
	};
	
	private static final TypeAdapter<Point3d> point3dAdapter = new TypeAdapter<Point3d>(){
		@Override
		public Point3d read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				reader.beginArray();
				Point3d value = new Point3d(reader.nextDouble(), reader.nextDouble(), reader.nextDouble());
				reader.endArray();
				return value;
			}
		}
		
		@Override
		public void write(JsonWriter writer, Point3d value) throws IOException{
			if(value == null){
				writer.nullValue();
				return;
			}else{
				//Setting the indent to nothing prevents GSON from applying newlines to Point3ds.
				//We need to set the indent to the value afterwards though to keep pretty printing.
				writer.beginArray();
				writer.setIndent("");
				writer.value(value.x);
				writer.value(value.y);
				writer.value(value.z);
				writer.endArray();
				writer.setIndent("  ");
			}
		}
	};
	
	private static final TypeAdapter<List<String>> stringListAdapter = new TypeAdapter<List<String>>(){	
		@Override
		public List<String> read(JsonReader reader) throws IOException{
			if(reader.peek() == JsonToken.NULL){
				reader.nextNull();
				return null;
			}else{
				List<String> value = new ArrayList<String>();
				reader.beginArray();
				while(reader.hasNext()){
					value.add(reader.nextString());
				}
				reader.endArray();
				return value;
			}
		}
		
		@Override
		public void write(JsonWriter writer, List<String> value) throws IOException{
			if(value == null){
				writer.nullValue();
			}else{
				//Setting the indent to nothing prevents GSON from applying newlines to String lists.
				//We need to set the indent to the value afterwards though to keep pretty printing.
				writer.beginArray();
				writer.setIndent("");
				for(String item : value){
					writer.value(item);
				}
				writer.endArray();
				writer.setIndent("  ");
			}
		}
	};
}
