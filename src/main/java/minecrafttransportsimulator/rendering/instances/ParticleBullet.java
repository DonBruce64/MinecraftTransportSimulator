package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.mcinterface.IWrapperBlock;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketBulletHit;
import minecrafttransportsimulator.rendering.components.AParticle;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.parts.PartGun;

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
	private final ItemPart bullet;
	private final PartGun gun;
	private final int bulletNumber;
	private final double initalVelocity;
	private final IWrapperEntity gunController;
	private final Point3d blockMotionStep = new Point3d(0D, 0D, 0D);
	
	private final Map<ItemPart, Integer> bulletDisplayLists = new HashMap<ItemPart, Integer>();
	
	private double armorPenetrated;
	
    public ParticleBullet(Point3d position, Point3d motion, ItemPart bullet, PartGun gun, IWrapperEntity gunController){
    	super(gun.vehicle.world, position, motion);
    	this.bullet = bullet;
    	this.gun = gun;
        this.bulletNumber = gun.bulletsFired;
        this.initalVelocity = motion.length();
        this.gunController = gunController;
    }
	
	@Override
	public void update(){
		//Get current velocity and possible damage.
		double velocity = motion.length();
		Damage damage = new Damage("bullet", velocity*bullet.definition.bullet.diameter/5*ConfigSystem.configObject.damage.bulletDamageFactor.value, box, null);
		
		//Check for collided entities and attack them.
		//If we collide with an armored vehicle, try to penetrate it.
		Map<IWrapperEntity, BoundingBox> attackedEntities = world.attackEntities(damage, gun.vehicle, motion);
		if(!attackedEntities.isEmpty()){
			for(IWrapperEntity entity : attackedEntities.keySet()){
				if(attackedEntities.get(entity) != null){
					BoundingBox hitBox = attackedEntities.get(entity);
					if(hitBox.armorThickness != 0){
						if(hitBox.armorThickness < bullet.definition.bullet.armorPenetration*velocity/initalVelocity - armorPenetrated){
							armorPenetrated += hitBox.armorThickness;
							continue;
						}
					}
					MasterLoader.networkInterface.sendToServer(new PacketBulletHit(hitBox, velocity, bullet, gun, bulletNumber, entity, gunController));
					age = maxAge;
				}else{
					box.globalCenter.setTo(entity.getPosition());
					MasterLoader.networkInterface.sendToServer(new PacketBulletHit(box, velocity, bullet, gun, bulletNumber, entity, gunController));
					age = maxAge;
				}
			}
			
			//If we hit something, don't process anything further.
			if(age == maxAge){
				return;
			}
		}
		
		//Didn't hit an entity.  Check for blocks.
		//We may hit more than one block here if we're a big bullet.  That's okay.
		//Because blocks are checked by their size, we need to manually check in 1-block steps.
		double maxSteps = motion.length();
		for(int i=-1; i<maxSteps + 2; ++i){
			blockMotionStep.setTo(motion).multiply(i/maxSteps);
			if(box.updateCollidingBlocks(world, blockMotionStep)){
				for(IWrapperBlock block : box.collidingBlocks){
					Point3d position = new Point3d(block.getPosition());
					MasterLoader.networkInterface.sendToServer(new PacketBulletHit(new BoundingBox(position, box.widthRadius, box.heightRadius, box.depthRadius), velocity, bullet, gun, bulletNumber, null, gunController));
				}
				age = maxAge;
				return;
			}
		}
		
		//Now that we have checked for collision, adjust motion to compensate for bullet movement.
		motion.multiply(0.98D);
		motion.y -= 0.0245D;
		
		//Send our updated motion to the super to update the position.
		//Doing this last lets us damage on the first update tick.
		super.update();
	}
	
	@Override
	public boolean collidesWithBlocks(){
		return false;
	}
	
	@Override
	public float getScale(float partialTicks){
		return 1.0F;
	}
	
	@Override
	public float getSize(){
		return bullet != null ? bullet.definition.bullet.diameter/1000F : super.getSize();
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
		return bullet.definition.bullet.type.equals("tracer");
	}
	
	@Override
	public void render(float partialTicks){
        //Parse the model if we haven't already.
        if(!bulletDisplayLists.containsKey(bullet)){
        	Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(bullet.definition.getModelLocation());
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
        	bulletDisplayLists.put(bullet, displayListIndex);
        }
        
        //Bind the texture for this bullet.
        MasterLoader.renderInterface.bindTexture(bullet.definition.getTextureLocation(bullet.subName));
        
        //Render the parsed model.  Translation will already have been applied, 
        //so we just need to rotate ourselves based on our velocity.
        double yaw = Math.toDegrees(Math.atan2(motion.x, motion.z));
        double pitch = -Math.toDegrees(Math.asin(motion.y/Math.sqrt(motion.x*motion.x+motion.y*motion.y+motion.z*motion.z)));
        GL11.glRotated(yaw, 0, 1, 0);
        GL11.glRotated(pitch, 1, 0, 0);
        GL11.glCallList(bulletDisplayLists.get(bullet));
	}
}
