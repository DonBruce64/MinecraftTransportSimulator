package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSkin extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>{
    
	@SuppressWarnings("hiding")
    public class SkinGeneral extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>.General{
		@JSONRequired
		public String packID;
		@JSONRequired
		public String systemName;
    }
}
