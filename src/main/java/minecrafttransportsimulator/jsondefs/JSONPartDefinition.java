package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPartDefinition{
	public boolean isSubPart;
	@JSONRequired
    public Point3d pos;
    public Point3d rot;
    public boolean turnsWithSteer;
    public boolean isController;
    public boolean isPermanent;
    public boolean isSpare;
    public boolean inverseMirroring;
    public boolean canDisableGun;
    public boolean forceCameras;
    @JSONRequired
    public List<String> types;
    public List<String> customTypes;
    public float minValue;
    public float maxValue;
    public float minYaw;
    public float maxYaw;
    public float minPitch;
    public float maxPitch;
    @Deprecated
    public JSONPartDefinition additionalPart;
    public List<JSONPartDefinition> additionalParts;
    public List<String> linkedDoors;
    public String defaultPart;
    @Deprecated
    public String linkedDoor;
    
    //Animation variables.
    public List<JSONAnimationDefinition> animations;
    @Deprecated
    public String translationVariable;
    @Deprecated
    public Point3d translationPosition;
    @Deprecated
    public float translationClampMin;
    @Deprecated
    public float translationClampMax;
    @Deprecated
    public boolean translationAbsolute;
    @Deprecated
    public String rotationVariable;
    @Deprecated
    public Point3d rotationPosition;
    @Deprecated
    public Point3d rotationAngles;
    @Deprecated
    public float rotationClampMin;
    @Deprecated
    public float rotationClampMax;
    @Deprecated
    public boolean rotationAbsolute;
    
    //Ground-specific variables.
    public float extraCollisionBoxOffset;
    public float treadDroopConstant;
    
    //Tread-specific part variables.
    @Deprecated
    public float[] treadYPoints;
    @Deprecated
    public float[] treadZPoints;
    @Deprecated
    public float[] treadAngles;
    
    //Seat-specific part variables.
    public Point3d dismountPos;
    public List<JSONPotionEffect> seatEffects; 
    
    //Engine-specific part variables.
    @Deprecated
    public float[] exhaustPos;
    @Deprecated
    public float[] exhaustVelocity;
    @Deprecated
    public List<ExhaustObject> exhaustObjects;
    public List<JSONParticleObject> particleObjects;
    public float intakeOffset;
    
    @Deprecated
	public class ExhaustObject{
    	@SuppressWarnings("hiding")
		public Point3d pos;
    	public Point3d velocity;
    	public float scale;
    }
}
