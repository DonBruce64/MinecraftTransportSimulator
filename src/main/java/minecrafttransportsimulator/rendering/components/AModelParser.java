package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONLight.JSONLightBlendableComponent;

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
	public static final String INTERIOR_WINDOW_SUFFIX = "_autogen_interior";
	public static final String LIGHT_COLOR_SUFFIX = "_autogen_color";
	public static final String LIGHT_FLARE_SUFFIX = "_autogen_flare";
	public static final String LIGHT_COVER_SUFFIX = "_autogen_cover";
	public static final String LIGHT_BEAM_SUFFIX = "_autogen_beam";
	private static final float COLOR_OFFSET = 0.0001F;
	private static final float FLARE_OFFSET = 0.0002F;
	private static final float COVER_OFFSET = 0.0003F;
	private static final float BEAM_OFFSET = -0.15F;
	private static final int BEAM_SEGMENTS = 40;
	
	public AModelParser(){
		parsers.put(getModelSuffix(), this);
	}
	
	static{
		new ModelParserOBJ();
	}

	/**
	 *  Returns the model file suffix for the model that can be parsed by this parser.
	 *  If a parser is added with the same suffix as an existing parser, it is replaced.
	 *  The suffix returned should be the file-extension portion after the dot.
	 */
	protected abstract String getModelSuffix();
	
	/**
	 *  Parses the model at the passed-in location. The return value is a map, with the String
	 *  key being the name of an object on the model, and the value being a two-dimensional array
	 *  of floats.  Each element of the first dimension is a vertex.  Therefore, the second dimension
	 *  are the floats that make up that vertex's properties.  They are in the order as follows:
	 *  <ul>
	 *  <li>The x-coordinate of a vertex on the model.
	 *  <li>The y-coordinate of a vertex on the model.
	 *  <li>The z-coordinate of a vertex on the model.
	 *  <li>The u-coordinate of the UV-mapping for that vertex.
	 *  <li>The v-coordinate of the UV-mapping for that vertex.
	 *  <li>The nX-coordinate of the normal for that vertex, in the x-dimension.
	 *  <li>The nY-coordinate of the normal for that vertex, in the y-dimension.
	 *  <li>The nZ-coordinate of the normal for that vertex, in the z-dimension.
	 *  </ul>
	 */
	protected abstract Map<String, Float[][]> parseModelInternal(String modelLocation);
	
	/**
	 *  Attempts to obtain the parser for the passed-in modelLocation.  After this, the model
	 *  is parsed and returned.  If no parser is found, an exception is thrown.
	 */
	public static Map<String, Float[][]> parseModel(String modelLocation){
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
	 *  The passed-in definition may be null to prevent checking against JSON, though non-JSON model objects
	 *  will still be generated as applicable.
	 */
	public static <AnimationEntity extends AEntityC_Definable<?>> List<RenderableModelObject<AnimationEntity>> generateRenderables(AnimationEntity entity, String modelLocation, List<JSONAnimatedObject> animatedObjects, List<JSONLight> lightObjects){
		Map<String, Float[][]> parsedModelObjects = parseModel(modelLocation);
		List<RenderableModelObject<AnimationEntity>> modelObjects = new ArrayList<RenderableModelObject<AnimationEntity>>();
		for(String parsedObjectName : parsedModelObjects.keySet()){
			JSONAnimatedObject animationDefinition = null;
			if(animatedObjects != null){
				for(JSONAnimatedObject testDef : animatedObjects){
					if(parsedObjectName.equals(testDef.objectName)){
						animationDefinition = testDef;
						break;
					}
				}
			}
			JSONLight lightDefinition = null;
			if(lightObjects != null){
				for(JSONLight testDef : lightObjects){
					if(parsedObjectName.equals(testDef.objectName)){
						lightDefinition = testDef;
						break;
					}
				}
			}
			
			//If we are a window, adjust our UV mapping points to map to a single texture.
			//Add these adjusted points, and then invert them for the interior component.
			if(parsedObjectName.toLowerCase().contains(WINDOW_OBJECT_NAME)){
				Float[][] parsedObject = parsedModelObjects.get(parsedObjectName);
				normalizeUVs(parsedObject);
				
				Float[][] invertedObject = new Float[parsedObject.length][8];
				for(int i=0, j=parsedObject.length-1; i<parsedObject.length; ++i, --j){
					invertedObject[j] = parsedObject[i];
				}
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + INTERIOR_WINDOW_SUFFIX, animationDefinition, lightDefinition, invertedObject, entity));
			}
			
			//If we are a light with emissive color, make a duplicate set of vertices with an offset for the color rendering.
			if(lightDefinition != null && lightDefinition.emissive){
				Float[][] parsedObject = parsedModelObjects.get(parsedObjectName);
				Float[][] offsetObject = new Float[parsedObject.length][8];
				for(int i=0; i<parsedObject.length; ++i){
					offsetObject[i][0] = parsedObject[i][0] + parsedObject[i][5]*COLOR_OFFSET;
					offsetObject[i][1] = parsedObject[i][1] + parsedObject[i][6]*COLOR_OFFSET;
					offsetObject[i][2] = parsedObject[i][2] + parsedObject[i][7]*COLOR_OFFSET;
					offsetObject[i][5] = parsedObject[i][5];
					offsetObject[i][6] = parsedObject[i][6];
					offsetObject[i][7] = parsedObject[i][7];
				}
				normalizeUVs(offsetObject);
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + LIGHT_COLOR_SUFFIX, animationDefinition, lightDefinition, offsetObject, entity));
			}
			
			//If we are a light with a cover, make a duplicate set of vertices with an offset for the cover rendering.
			if(lightDefinition != null && lightDefinition.covered){
				Float[][] parsedObject = parsedModelObjects.get(parsedObjectName);
				Float[][] offsetObject = new Float[parsedObject.length][8];
				for(int i=0; i<parsedObject.length; ++i){
					offsetObject[i][0] = parsedObject[i][0] + parsedObject[i][5]*COVER_OFFSET;
					offsetObject[i][1] = parsedObject[i][1] + parsedObject[i][6]*COVER_OFFSET;
					offsetObject[i][2] = parsedObject[i][2] + parsedObject[i][7]*COVER_OFFSET;
					offsetObject[i][5] = parsedObject[i][5];
					offsetObject[i][6] = parsedObject[i][6];
					offsetObject[i][7] = parsedObject[i][7];
				}
				normalizeUVs(offsetObject);
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + LIGHT_COVER_SUFFIX, animationDefinition, lightDefinition, offsetObject, entity));
			}
			
			//If we are an emissive light, check if we have any flare or beam components to add.
			//If so, we create them and add them as their own model object.
			if(lightDefinition != null && lightDefinition.blendableComponents != null){
				List<JSONLightBlendableComponent> flareDefs = new ArrayList<JSONLightBlendableComponent>();
				List<JSONLightBlendableComponent> beamDefs = new ArrayList<JSONLightBlendableComponent>();
				for(JSONLightBlendableComponent component : lightDefinition.blendableComponents){
					if(component.flareHeight > 0){
						flareDefs.add(component);
					}
					if(component.beamDiameter > 0){
						beamDefs.add(component);
					}
				}
				if(!flareDefs.isEmpty()){
					//6 vertices per flare due to triangle rendering.
					Float[][] flareObject = new Float[flareDefs.size()*6][8];
					for(int i=0; i<flareDefs.size(); ++i){
						JSONLightBlendableComponent flareDef = flareDefs.get(i);
						//Get the angle that is needed to rotate points to the normalized vector.
						Point3d rotation = flareDef.axis.getAngles();
						Point3d vertexOffset = new Point3d();
						Point3d centerOffset = flareDef.axis.copy().multiply(FLARE_OFFSET).add(flareDef.pos);
						for(int j=0; j<6; ++j){
							Float[] newVertex = new Float[8];
							//Get the current UV points.
							switch(j){
								case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
								case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
								case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
								case(3): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
								case(4): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
								case(5): newVertex[3] = 1.0F; newVertex[4] = 0.0F; break;
							}
							
							//Based on the UVs and the axis for the flare, calculate the vertices.
							vertexOffset.x = newVertex[3] == 0.0 ? -flareDef.flareWidth/2D : flareDef.flareWidth/2D;
							vertexOffset.y = newVertex[4] == 0.0 ? flareDef.flareHeight/2D : -flareDef.flareHeight/2D;
							vertexOffset.z = 0;
							vertexOffset.rotateFine(rotation).add(centerOffset);
							newVertex[0] = (float) vertexOffset.x;
							newVertex[1] = (float) vertexOffset.y;
							newVertex[2] = (float) vertexOffset.z;
							
							//Set normals to the normal axis in the JSON.
							newVertex[5] = (float) flareDef.axis.x;
							newVertex[6] = (float) flareDef.axis.y;
							newVertex[7] = (float) flareDef.axis.z;
							
							//Set the actual vertex.
							flareObject[i*6 + j] = newVertex;
						}
					}
					modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + LIGHT_FLARE_SUFFIX, animationDefinition, lightDefinition, flareObject, entity));
				}
				
				if(!beamDefs.isEmpty()){
					//3 vertices per cone-face, each share the same center point.
					//Number of cone faces is equal to the number of segments for beams.
					//We render two beams.  One inner and one outer.
					Float[][] beamObject = new Float[beamDefs.size()*2*BEAM_SEGMENTS*3][8];
					for(int i=0; i<beamDefs.size(); ++i){
						JSONLightBlendableComponent beamDef = beamDefs.get(i);
						//Get the angle that is needed to rotate points to the normalized vector.
						Point3d rotation = beamDef.axis.getAngles();
						Point3d vertexOffset = new Point3d();
						Point3d centerOffset = beamDef.axis.copy().multiply(BEAM_OFFSET).add(beamDef.pos);
						//Go from negative to positive to render both beam-faces in the same loop.
						for(int j=-BEAM_SEGMENTS; j<BEAM_SEGMENTS; ++j){
							for(int k=0; k<3; ++k){
								Float[] newVertex = new Float[8];
								//Get the current UV points.
								//Point 0 is always the center of the beam, 1 and 2 are the outer points.
								switch(k%3){
									case(0): newVertex[3] = 0.0F; newVertex[4] = 0.0F; break;
									case(1): newVertex[3] = 0.0F; newVertex[4] = 1.0F; break;
									case(2): newVertex[3] = 1.0F; newVertex[4] = 1.0F; break;
								}
								
								//Based on the UVs and the axis for the beam, calculate the vertices.
								double currentAngleRad;
								if(j<0){
									currentAngleRad = newVertex[3] == 0.0F ? 2D*Math.PI*((j+1)/(double)BEAM_SEGMENTS) : 2D*Math.PI*(j/(double)BEAM_SEGMENTS);
								}else{
									currentAngleRad = newVertex[3] == 0.0F ? 2D*Math.PI*(j/(double)BEAM_SEGMENTS) : 2D*Math.PI*((j+1)/(double)BEAM_SEGMENTS);
								}
								if(newVertex[4] == 0.0){
									vertexOffset.set(0, 0, 0);
								}else{
									vertexOffset.x = beamDef.beamDiameter/2F*Math.cos(currentAngleRad);
									vertexOffset.y = beamDef.beamDiameter/2F*Math.sin(currentAngleRad);
									vertexOffset.z = beamDef.beamLength;
								}
								vertexOffset.rotateFine(rotation).add(centerOffset);
								newVertex[0] = (float) vertexOffset.x;
								newVertex[1] = (float) vertexOffset.y;
								newVertex[2] = (float) vertexOffset.z;
								
								//Don't care about normals for beam rendering as it's a blending face, so we just set them to 0.
								newVertex[5] = 0F;
								newVertex[6] = 0F;
								newVertex[7] = 0F;
								
								//Set the actual vertex.
								beamObject[i*2*BEAM_SEGMENTS*3 + (j + BEAM_SEGMENTS)*3 + k] = newVertex;
							}
						}
					}
					modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + LIGHT_BEAM_SUFFIX, animationDefinition, lightDefinition, beamObject, entity));
				}
			}
			
			//Add our object to the mapping.
			modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName, animationDefinition, lightDefinition, parsedModelObjects.get(parsedObjectName), entity));
		}
		return modelObjects;
	}
	
	private static void normalizeUVs(Float[][] parsedObject){
		for(int i=0; i<parsedObject.length; ++i){
			if(parsedObject.length > 3 && i%6 >= 3){
				//Second-half of a quad.
				switch(i%6){
					case(3): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(5): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
				}
			}else{
				//Normal tri or first half of quad using tri mapping.
				switch(i%6){
					case(0): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(1): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 1.0F; break;
					case(2): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					
					case(3): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
					case(5): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
				}
			}
		}
	}
}
