package minecrafttransportsimulator.systems.pack;

import java.util.ArrayList;
import java.util.List;

public class PackObject {


    public PackGeneralConfig general;
    public PackMotorizedConfig motorized;
    public PackPlane plane;
    public List<PackCollisionBox> collision = new ArrayList<PackCollisionBox>();
    public List<PackPart> parts = new ArrayList<PackPart>();
    public PackRenderingConfig rendering;
    public PackExtraConfig extras;

}
