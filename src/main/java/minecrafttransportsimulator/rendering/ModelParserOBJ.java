package minecrafttransportsimulator.rendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Class responsible for parsing OBJ models into arrays that can be fed to the GPU.
 * Much more versatile than the Forge system.
 *
 * @author don_bruce
 */
public final class ModelParserOBJ extends AModelParser {

    @Override
    protected String getModelSuffix() {
        return "obj";
    }

    @Override
    protected List<RenderableObject> parseModelInternal(String modelLocation) {
        List<RenderableObject> objectList = new ArrayList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(ModelParserOBJ.class.getResourceAsStream(modelLocation)));
        } catch (Exception e) {
            throw new NullPointerException("Attempted to parse the OBJ model at: " + modelLocation + " but could not find it.  Check the path and try again.");
        }

        String objectName = null;
        final List<float[]> vertexList = new ArrayList<>();
        final List<float[]> normalList = new ArrayList<>();
        final List<float[]> textureList = new ArrayList<>();
        final List<String> faceList = new ArrayList<>();

        try {
            int lineNumber = 0;
            while (reader.ready()) {
                String line = reader.readLine();
                ++lineNumber;

                //Do normal parsing.
                if (line.startsWith("o ")) {
                    //Found new object name.  If we are parsing an object, finish up parsing and compile the points for it.
                    if (objectName != null) {
                        if (faceList.isEmpty()) {
                            InterfaceManager.coreInterface.logError("Object " + objectName + " found with no faces defined at line: " + lineNumber + " in: " + modelLocation);
                        } else {
                            compileVertexArray(objectList, vertexList, normalList, textureList, faceList, modelLocation, objectName);
                            objectName = null;
                        }
                    }
                    try {
                        objectName = line.trim().substring(2, line.length());
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Object found with no name at line: " + lineNumber + " of: " + modelLocation + ".  Make sure your model exporter isn't making things into groups rather than objects.");
                    }
                } else if (line.startsWith("v ")) {
                    try {
                        float[] coords = new float[3];
                        line = line.trim().substring(2).trim();
                        coords[0] = Float.parseFloat(line.substring(0, line.indexOf(' ')));
                        coords[1] = Float.parseFloat(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
                        coords[2] = Float.parseFloat(line.substring(line.lastIndexOf(' ') + 1));
                        vertexList.add(coords);
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Could not parse vertex info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Vertex lines must consist of only three numbers (X, Y, Z).");
                    }
                } else if (line.startsWith("vt ")) {
                    try {
                        float[] coords = new float[2];
                        line = line.trim().substring(3).trim();
                        int space = line.indexOf(' ');
                        int vertexEnd = line.lastIndexOf(' ') == space ? line.length() : line.lastIndexOf(' ');
                        coords[0] = Float.parseFloat(line.substring(0, space));
                        //Need to invert the V of the UV to change from texture origin being top-left to OpenGL origin being bottom-left.
                        coords[1] = 1 - Float.parseFloat(line.substring(space + 1, vertexEnd));
                        textureList.add(coords);
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Could not parse vertex texture info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Vertex texture lines must consist of only two numbers (U, V).");
                    }
                } else if (line.startsWith("vn ")) {
                    try {
                        float[] coords = new float[3];
                        line = line.trim().substring(2).trim();
                        coords[0] = Float.parseFloat(line.substring(0, line.indexOf(' ')));
                        coords[1] = Float.parseFloat(line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' ')));
                        coords[2] = Float.parseFloat(line.substring(line.lastIndexOf(' ') + 1));
                        normalList.add(coords);
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Could not parse normals info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Normals lines must consist of only three numbers (Xn, Yn, Zn).");
                    }
                } else if (line.startsWith("f ")) {
                    try {
                        faceList.add(line.trim().substring(2));
                    } catch (Exception e) {
                        InterfaceManager.coreInterface.logError("Could not parse face info at line: " + lineNumber + " of: " + modelLocation + " due to bad formatting.  Face lines must consist of sets of three numbers in the format (V1/T1/N1, V2/T2/N2, ...).");
                    }
                }
            }

            //End of file.  Save the last part in process and close the file.
            compileVertexArray(objectList, vertexList, normalList, textureList, faceList, modelLocation, objectName);
            reader.close();
            return objectList;

        } catch (IOException e) {
            throw new IllegalStateException("Could not finish parsing: " + modelLocation + " due to IOException error.  Did the file change state during parsing?");
        }
    }

    private static void compileVertexArray(List<RenderableObject> objectList, List<float[]> vertexList, List<float[]> normalList, List<float[]> textureList, List<String> faceList, String modelLocation, String objectName) {
        if (objectName == null) {
            InterfaceManager.coreInterface.logError("No object name found in the entire OBJ model file of " + modelLocation + ".  Resorting to 'model' as default.  Are you using groups instead of objects by mistake?");
            objectName = "model";
        }

        try {
            List<Integer[]> vertexDataSets = new ArrayList<>();
            for (String faceString : faceList) {
                List<Integer[]> faceVertexData = new ArrayList<>();
                while (!faceString.isEmpty()) {
                    //Get the face string in format X/Y/Z.  Use the space as a separator between vertices making up the face.
                    int defEnd = faceString.indexOf(' ');
                    String faceDef;
                    if (defEnd != -1) {
                        //Take the faceDef from the faceString and store it.
                        faceDef = faceString.substring(0, defEnd);
                        faceString = faceString.substring(defEnd + 1);
                    } else {
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
                    int vertexNumber = Integer.parseInt(faceDef.substring(0, firstSlash)) - 1;
                    int textureNumber = Integer.parseInt(faceDef.substring(firstSlash + 1, secondSlash)) - 1;
                    int normalNumber = Integer.parseInt(faceDef.substring(secondSlash + 1)) - 1;

                    //If we have three or more points in faceValues, it means we need to make a triangle out of this shape.
                    //Add the first point, the most recent point, and this point to make a triangle.
                    //Otherwise, just add the face as-is.
                    if (faceVertexData.size() >= 3) {
                        faceVertexData.add(faceVertexData.get(0));
                        faceVertexData.add(faceVertexData.get(faceVertexData.size() - 2));
                    }
                    faceVertexData.add(new Integer[]{vertexNumber, textureNumber, normalNumber});
                }
                vertexDataSets.addAll(faceVertexData);
            }

            //Compile buffer.
            FloatBuffer compiledBuffer = FloatBuffer.allocate(vertexDataSets.size() * 8);
            for (Integer[] vertexData : vertexDataSets) {
                compiledBuffer.put(normalList.get(vertexData[2]));
                compiledBuffer.put(textureList.get(vertexData[1]));
                compiledBuffer.put(vertexList.get(vertexData[0]));
            }
            compiledBuffer.flip();
            objectList.add(new RenderableObject(objectName, null, ColorRGB.WHITE, compiledBuffer, true));
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("Could not compile points of: " + modelLocation + ":" + objectName + ".  This is likely due to missing UV mapping on some or all faces.");
        }

        //Clear face list as we don't want to compile them on the next pass.
        faceList.clear();
    }
}
