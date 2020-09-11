package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import mcinterface.InterfaceGame;
import mcinterface.InterfaceNetwork;
import mcinterface.InterfaceRender;
import mcinterface.WrapperBlock;
import mcinterface.WrapperEntity;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.packets.instances.PacketBulletHit;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**This part class is special, in that it does not extend APart.
 * This is because bullets do not render as vehicle parts, and instead
 * are particles.  This allows them to be independent of the
 * vehicle that fired them.
 * 
 * As particles, bullets are client-side only.  This prevents them from getting stuck
 * in un-loaded chunks on the server, and prevents the massive network usage that
 * would be required to spawn 100s of bullets from a machine gun into the world.
 * 
 * @author don_bruce
 */

public final class ParticleBullet extends AParticle{
	private final JSONPart definition;
	private final int playerID;
	private final EntityVehicleF_Physics vehicle;
	private final BoundingBox box;
	
	private final Map<JSONPart, Integer> bulletDisplayLists = new HashMap<JSONPart, Integer>();
	
    public ParticleBullet(WrapperWorld world, Point3d position, Point3d motion, JSONPart definition, WrapperPlayer player, EntityVehicleF_Physics vehicle){
    	super(world, position, motion);
        this.definition = definition;
        this.playerID = player.getID();
        this.vehicle = vehicle;
        this.box = new BoundingBox(position, getSize()/2D, getSize()/2D, getSize()/2D);
    }
	
	@Override
	public void update(boolean onGround){
		double velocity = motion.length();
		Damage damage = new Damage("bullet", velocity*definition.bullet.diameter/5*ConfigSystem.configObject.damage.bulletDamageFactor.value, box, null);
		
		//Check for collided entities and attack them.
		Map<WrapperEntity, BoundingBox> attackedEntities = world.attackEntities(damage, vehicle, motion);
		if(!attackedEntities.isEmpty()){
			if(playerID == InterfaceGame.getClientPlayer().getID()){
				for(WrapperEntity entity : attackedEntities.keySet()){
					InterfaceNetwork.sendToServer(new PacketBulletHit(attackedEntities.get(entity) != null ? attackedEntities.get(entity) : box, velocity, definition, entity));
				}
			}
			age = maxAge;
			return;
		}
		
		//Didn't hit an entity.  Check for blocks.
		//We may hit more than one block here if we're a big bullet.  That's okay.
		if(box.updateCollidingBlocks(world, motion)){
			for(WrapperBlock block : box.collidingBlocks){
				Point3d position = new Point3d(block.getPosition());
				InterfaceNetwork.sendToServer(new PacketBulletHit(new BoundingBox(position, box.widthRadius, box.heightRadius, box.depthRadius), velocity, definition, null));
			}
			age = maxAge;
			return;
		}
					
		//We didn't collide with anything, slow down and fall down towards the ground.
		motion.multiply(0.98D);
		motion.y -= 0.0245D;
		
		//Send our updated position to the super.
		//Doing this last lets us damage on the first update tick.
		super.update(onGround);
	}
	
	@Override
	public float getScale(float partialTicks){
		return 1.0F;
	}
	
	@Override
	public float getSize(){
		return definition.bullet.diameter/1000F;
	}
	
	@Override
	protected int generateMaxAge(){
		return 10*20;
	}
	
	@Override
	public int getTextureIndex(){
		return -1;
	}
	
	@Override
	public boolean isBright(){
		return definition.bullet.type.equals("tracer");
	}
	
	@Override
	public void render(float partialTicks){
        //Parse the model if we haven't already.
        if(!bulletDisplayLists.containsKey(definition)){
        	final String modelLocation;
        	if(definition.general.modelName != null){
				modelLocation = "objmodels/parts/" + definition.general.modelName + ".obj";
			}else{
				modelLocation = "objmodels/parts/" + definition.systemName + ".obj";
			}
        	Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(definition.packID, modelLocation);
        	int displayListIndex = GL11.glGenLists(1);
    		GL11.glNewList(displayListIndex, GL11.GL_COMPILE);
    		GL11.glBegin(GL11.GL_TRIANGLES);
    		for(Entry<String, Float[][]> entry : parsedModel.entrySet()){
				for(Float[] vertex : entry.getValue()){
					GL11.glTexCoord2f(vertex[3], vertex[4]);
					GL11.glNormal3f(vertex[5], vertex[6], vertex[7]);
					GL11.glVertex3f(-vertex[0], vertex[1], vertex[2]);
				}
    		}
    		GL11.glEnd();
    		GL11.glEndList();
        	bulletDisplayLists.put(definition, displayListIndex);
        }
        
        //Bind the texture for this bullet.
        InterfaceRender.bindTexture(definition.packID, "textures/items/" + definition.systemName + ".png");
        
        //Render the parsed model.  Translation will already have been applied, 
        //so we just need to rotate ourselves based on our velocity.
        double yaw = Math.toDegrees(Math.atan2(motion.x, motion.z));
        double pitch = -Math.toDegrees(Math.asin(motion.y/Math.sqrt(motion.x*motion.x+motion.y*motion.y+motion.z*motion.z)));
        GL11.glRotated(yaw, 0, 1, 0);
        GL11.glRotated(pitch, 1, 0, 0);
        GL11.glCallList(bulletDisplayLists.get(definition));
	}
}
