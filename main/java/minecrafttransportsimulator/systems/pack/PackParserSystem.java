package minecrafttransportsimulator.systems.pack;

import com.google.gson.Gson;
import minecrafttransportsimulator.MTS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem {
    private static Map<String, PackObject> packMap = new HashMap<String, PackObject>();

    public static void init() {
        File assetDir = new File(MTS.assetDir);
        if (!assetDir.exists()) {
            assetDir.mkdirs();
        } else {
            parseDirectory(assetDir);
        }
    }

    private static void parseDirectory(File assetDir) {
        for (File file : assetDir.listFiles()) {
            if (file.isDirectory()) {
                parseDirectory(file);
            } else {
                if (file.getName().endsWith(".json") && !file.getName().contains("SAMPLE")) {
                    parseFile(file);
                }
            }
        }
    }

    private static void parseFile(File file) {
        try {
            Gson gson = new Gson();
            PackObject pack = gson.fromJson(new FileReader(file), PackObject.class);
            packMap.put(pack.general.name, pack);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static PackObject getPack(String name){
        return packMap.get(name);
    }

}
