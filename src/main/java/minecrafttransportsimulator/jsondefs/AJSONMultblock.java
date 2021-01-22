package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class AJSONMultblock<GeneralConfig extends AJSONMultblock<GeneralConfig>.General> extends AJSONMultiModelProvider<GeneralConfig>{

    public class General extends AJSONMultiModelProvider<GeneralConfig>.General{
    	public List<JSONCollisionArea> collisionAreas;
    }
}