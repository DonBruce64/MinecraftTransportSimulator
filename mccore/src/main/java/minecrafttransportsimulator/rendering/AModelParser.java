package minecrafttransportsimulator.rendering;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Abstract class for parsing models.  This contains methods for determining what models
 * the parser can parse, and the operations for parsing them into the form MTS needs.
 * It also stores a list of created parsers for use when requesting a model be parsed.
 * By default, an OBJ parser is created when this class is first accessed, but one may
 * add other parsers as they see fit.
 *
 * @author don_bruce
 */
public abstract class AModelParser {
    private static final Map<String, AModelParser> parsers = new HashMap<>();
    private static final Map<String, List<RenderableVertices>> parsedVertices = new HashMap<>();
    private static List<RenderableVertices> missingModelTemplate;
    public static final String WINDOW_OBJECT_NAME = "window";
    public static final String ONLINE_TEXTURE_OBJECT_NAME = "url";
    public static final String TRANSLUCENT_OBJECT_NAME = "translucent";
    public static final String MISSING_MODEL_NAME = "__missing_model__";
    public static final String MISSING_MODEL_LOCATION = "/assets/mts/objmodels/rendering/missing.obj";
    public static final String MISSING_MODEL_TEXTURE = "mts:textures/rendering/missing.png";

    public AModelParser() {
        parsers.put(getModelSuffix(), this);
    }

    static {
        new ModelParserOBJ();
        new ModelParserLT();
    }

    /**
     * Returns the model file suffix for the model that can be parsed by this parser.
     * If a parser is added with the same suffix as an existing parser, it is replaced.
     * The suffix returned should be the file-extension portion after the dot.
     */
    protected abstract String getModelSuffix();

    /**
     * Parses the model at the passed-in location. The return value is a list of objects parsed.
     */
    protected abstract List<RenderableVertices> parseModelInternal(String modelLocation);

    /**
     * Attempts to obtain the parser for the passed-in modelLocation.  After this, the model
     * is parsed and returned.  If no parser is found, an exception is thrown.
     * If the model has already been parsed, a cached copy is returned.
     */
    public static List<RenderableVertices> parseModel(String modelLocation, boolean returnCached) {
        List<RenderableVertices> vertices = null;
        if (returnCached) {
            vertices = parsedVertices.get(modelLocation);
        }
        if (vertices == null) {
            AModelParser parser = parsers.get(modelLocation.substring(modelLocation.lastIndexOf(".") + 1));
            if (parser != null) {
                try {
                    vertices = parser.parseModelInternal(modelLocation);
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Could not parse model: " + modelLocation + " due to " + e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "") + ".  Reverting to fallback model.");
                    vertices = getMissingModel(modelLocation);
                }
                if (vertices == null || vertices.isEmpty()) {
                    InterfaceManager.coreInterface.logError("Model: " + modelLocation + " produced no renderable geometry.  Reverting to fallback model.");
                    vertices = getMissingModel(modelLocation);
                }
                if (returnCached) {
                    parsedVertices.put(modelLocation, vertices);
                }
            } else {
                InterfaceManager.coreInterface.logError("No parser found for model format of " + modelLocation.substring(modelLocation.lastIndexOf(".") + 1) + " at: " + modelLocation + ".  Reverting to fallback model.");
                vertices = getMissingModel(modelLocation);
                if (returnCached) {
                    parsedVertices.put(modelLocation, vertices);
                }
            }
        }
        return vertices;
    }

    public static boolean isMissingModel(List<RenderableVertices> vertices) {
        for (RenderableVertices vertexObject : vertices) {
            if (vertexObject.isErrorPlaceholder) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses the model for the passed-in entity, and generates all {@link RenderableModelObject}s for it.
     * These are returned as a list.  Objects in the parsed model are cross-checked with the passed-in
     * definition to ensure the proper constructors are created.  All objects in the model
     * are assured to be turned into one of the objects in the returned list.
     */
    public static List<RenderableModelObject> generateRenderables(AEntityD_Definable<?> entity) {
        String modelLocation = entity.definition.getModelLocation(entity.subDefinition);
        List<RenderableModelObject> modelObjects = new ArrayList<>();
        for (RenderableVertices parsedObject : parseModel(modelLocation, true)) {
            modelObjects.add(new RenderableModelObject(entity, parsedObject));
        }
        return modelObjects;
    }

    private static List<RenderableVertices> getMissingModel(String failedModelLocation) {
        if (missingModelTemplate == null) {
            missingModelTemplate = loadMissingModelTemplate(failedModelLocation);
        }
        return cloneMissingModel(missingModelTemplate);
    }

    private static List<RenderableVertices> loadMissingModelTemplate(String failedModelLocation) {
        if (!MISSING_MODEL_LOCATION.equals(failedModelLocation)) {
            AModelParser parser = parsers.get("obj");
            if (parser != null) {
                try {
                    List<RenderableVertices> parsedMissingModel = parser.parseModelInternal(MISSING_MODEL_LOCATION);
                    if (parsedMissingModel != null && !parsedMissingModel.isEmpty()) {
                        return parsedMissingModel;
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("Could not load bundled missing model at: " + MISSING_MODEL_LOCATION + " due to " + e.getClass().getSimpleName() + (e.getMessage() != null ? " - " + e.getMessage() : "") + ".  Reverting to generated placeholder geometry.");
                }
            }
        }

        List<RenderableVertices> generatedModel = new ArrayList<>();
        generatedModel.add(createGeneratedMissingModel());
        return generatedModel;
    }

    private static List<RenderableVertices> cloneMissingModel(List<RenderableVertices> sourceModel) {
        List<RenderableVertices> clonedModel = new ArrayList<>();
        for (RenderableVertices sourceObject : sourceModel) {
            FloatBuffer duplicatedVertices = sourceObject.vertices.duplicate();
            duplicatedVertices.rewind();
            clonedModel.add(new RenderableVertices(MISSING_MODEL_NAME, duplicatedVertices, sourceObject.cacheVertices, true));
        }
        return clonedModel;
    }

    private static RenderableVertices createGeneratedMissingModel() {
        RenderableVertices fallbackSprite = RenderableVertices.createSprite(1, null, null);
        FloatBuffer duplicatedVertices = fallbackSprite.vertices.duplicate();
        duplicatedVertices.rewind();
        return new RenderableVertices(MISSING_MODEL_NAME, duplicatedVertices, false, true);
    }
}
