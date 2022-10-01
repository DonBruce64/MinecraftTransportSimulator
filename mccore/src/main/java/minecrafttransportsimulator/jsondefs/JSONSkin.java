package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("No, this isn't a new set of clothes for your player.  This is a new set of clothes for your content!  Skins are used to allow for cross-pack liveries to be added dynamically.  In essence, what skins allow for is one pack to have a model with multiple definitions and liveries, and another pack to add-on their own livery to that model.\nFor example: say a pack contains trailers.  Rather than making their own trailer, packs can simply add skins to the trailers of the pack with them, forgoing the pain of making a model, a JSON, and doing updates.  These skins will only be active if the pack containing trailers is present.  By default, these appear in the creative tab of the pack you are making the skins for.  However, depending on the pack-specific settings, they may appear in your creative tab instead.")
public class JSONSkin extends AJSONMultiModelProvider {

    @JSONDescription("The properties for this skin.")
    public Skin skin;

    public static class Skin {
        @JSONRequired
        @JSONDescription("The packID of the vehicle/part/decor/etc. that this skin goes to.")
        public String packID;

        @JSONRequired
        @JSONDescription("his is the registration name for the pack component that this skin goes to.  Generally, this is the name of the JSON file for the component.  If you don't want to search for this value in a massive pack, you can always check the exported JSON file from devMode, as it contains this parameter.")
        public String systemName;
    }
}
