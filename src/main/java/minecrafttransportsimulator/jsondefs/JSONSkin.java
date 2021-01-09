package minecrafttransportsimulator.jsondefs;

public class JSONSkin extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>{
    
	@SuppressWarnings("hiding")
    public class SkinGeneral extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>.General{
		public String packID;
		public String systemName;
    }
}
