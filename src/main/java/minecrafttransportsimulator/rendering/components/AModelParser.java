package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.rendering.instances.ModelParserLT;
import minecrafttransportsimulator.rendering.instances.ModelParserOBJ;

/**Abstract class for parsing models.  This contains methods for determining what models
 * the parser can parse, and the operations for parsing them into the form MTS needs.
 * It also stores a list of created parsers for use when requesting a model be parsed.
 * By default, an OBJ parser is created when this class is first accessed, but one may
 * add other parsers as they see fit.
 * 
 * @author don_bruce
 */
public abstract class AModelParser{
	private static final Map<String, AModelParser> parsers = new HashMap<String, AModelParser>();
	public static final String ROLLER_OBJECT_NAME = "roller";
	public static final String WINDOW_OBJECT_NAME = "window";
	public static final String ONLINE_TEXTURE_OBJECT_NAME = "url";
	public static final String TRANSLUCENT_OBJECT_NAME = "translucent";
	
	public AModelParser(){
		parsers.put(getModelSuffix(), this);
	}
	
	static{
		new ModelParserOBJ();
		new ModelParserLT();
	}

	/**
	 *  Returns the model file suffix for the model that can be parsed by this parser.
	 *  If a parser is added with the same suffix as an existing parser, it is replaced.
	 *  The suffix returned should be the file-extension portion after the dot.
	 */
	protected abstract String getModelSuffix();
	
	/**
	 *  Parses the model at the passed-in location. The return value is a list of objects parsed.
	 */
	protected abstract List<RenderableObject> parseModelInternal(String modelLocation);
	/**
	 *  Attempts to obtain the parser for the passed-in modelLocation.  After this, the model
	 *  is parsed and returned.  If no parser is found, an exception is thrown.
	 */
	public static List<RenderableObject> parseModel(String modelLocation){
		AModelParser parser = parsers.get(modelLocation.substring(modelLocation.lastIndexOf(".") + 1));
		if(parser != null){
			return parser.parseModelInternal(modelLocation);
		}else{
			throw new IllegalArgumentException("No parser found for model format of " + modelLocation.substring(modelLocation.lastIndexOf(".") + 1));
		}
	}
	
	/**
	 *  Parses the model at the passed-in location, and generates all {@link RenderableModelObject}s for it.
	 *  These are returned as a list.  Objects in the parsed model are cross-checked with the passed-in 
	 *  definition to ensure the proper constructors are created.  All objects in the model
	 *  are assured to be turned into one of the objects in the returned list.
	 */
	public static <AnimationEntity extends AEntityD_Definable<?>> List<RenderableModelObject<AnimationEntity>> generateRenderables(AEntityD_Definable<?> entity){
		String modelLocation = entity.definition.getModelLocation(entity.subName);
		List<RenderableObject> parsedModelObjects = parseModel(modelLocation);
		List<RenderableModelObject<AnimationEntity>> modelObjects = new ArrayList<RenderableModelObject<AnimationEntity>>();
		for(RenderableObject parsedObject : parsedModelObjects){
			//If we are a tread roller, make a roller rather than a standard object.
			if(parsedObject.name.toLowerCase().contains(ROLLER_OBJECT_NAME)){
				modelObjects.add(new RenderableTreadRoller<AnimationEntity>(modelLocation, parsedObject, modelObjects));
			}else{
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObject, modelObjects));
			}
		}
		return modelObjects;
	}
}
