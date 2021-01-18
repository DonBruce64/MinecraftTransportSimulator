package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSkin extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>{
    
	@SuppressWarnings("hiding")
    public class SkinGeneral extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>.General{
		@JSONRequired
		@JSONDescription("The packID of the vehicle/part/decor/etc. that this skin goes to.")
		public String packID;
		@JSONRequired
		@JSONDescription("his is the registration name for the pack component that this skin goes to.  Generally, this is the name of the JSON file for the component.  If you don’t want to search for this value in a massive pack, you can always check the exported JSON file from devMode, as it contains this parameter.")
		public String systemName;
    }
}
