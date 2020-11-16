package minecrafttransportsimulator.jsondefs;

public class JSONSkin extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>{
    
    public class SkinGeneral extends AJSONMultiModelProvider<JSONSkin.SkinGeneral>.General{
		public String packID;
		public String systemName;
    }
}
