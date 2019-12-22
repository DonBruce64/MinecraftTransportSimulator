package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.packets.general.PacketBulletHit;
import minecrafttransportsimulator.systems.OBJParserSystem;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

@SideOnly(Side.CLIENT)
public final class PartBullet extends Particle{
	private final String bulletName;
	private final PackPartObject pack;
	private final int playerID;
	private final EntityVehicleE_Powered vehicle;
	
	private final Map<String, Map<String, Float[][]>> parsedBulletModels = new HashMap<String, Map<String, Float[][]>>();
	
    public PartBullet(World world, double x, double y, double z, double motionX, double motionY, double motionZ, String bulletName, int playerID, EntityVehicleE_Powered vehicle){
    	super(world, x, y, z);
        //Set basic properties.
    	this.particleMaxAge = 60;
        this.bulletName = bulletName;
        this.pack = PackParserSystem.getPartPack(this.bulletName);
        this.setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(MTSRegistry.partItemMap.get(bulletName), 0));
        
        //Set physical state and runtime properties.
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.setSize(pack.bullet.diameter/1000F, pack.bullet.diameter/1000F);
        this.setBoundingBox(new AxisAlignedBB(posX - width/2F, posY - height/2F, posZ - width/2F, posX + width/2F, posY + height/2F, posZ + width/2F));
        this.playerID = playerID;
        this.vehicle = vehicle;
    }
	
	@Override
	public void onUpdate(){
		//Do aging.
		if(this.particleAge++ >= this.particleMaxAge){
            this.setExpired();
        }
		
		//If not expired, do logic.
		if(!this.isExpired){
			//We check for entities and blocks in 0.25 block movements.
			//These are done in the same loop to prevent us checking further along if we hit something.
			//Arrows move so slow that this isn't a problem.  Not so for bullets.
			Entity collidedEntity = null;
			BlockPos collidedBlockPos = null;
			double velocity = Math.sqrt(motionX*motionX + motionY*motionY + motionZ*motionZ);
			Vec3d normalizedVelocity = new Vec3d(motionX, motionY, motionZ).normalize();
			for(double velocityOffset=0; velocityOffset<=velocity; velocityOffset+=0.25D){
				for(Entity entity : this.world.getEntitiesWithinAABBExcludingEntity(vehicle, this.getBoundingBox().offset(velocityOffset*normalizedVelocity.x, velocityOffset*normalizedVelocity.y, velocityOffset*normalizedVelocity.z))){
					//We might have hit more than one entity.  Pick the closest one if so.
					//Only do this if the entities in question are NOT vehicles.
					//If they are just use whatever came first in the queue.
					if(collidedEntity == null){
						collidedEntity = entity;
					}else if(!(entity instanceof EntityVehicleE_Powered) && (collidedEntity.getDistanceSq(this.posX, this.posY, this.posZ) > entity.getDistanceSq(this.posX, this.posY, this.posZ))){
						collidedEntity = entity;
					}
				}
				
				//Don't check for blocks if we hit an entity.
				//Entities get priority as they are always on or in front of blocks.
				if(collidedEntity == null){
					BlockPos pos = new BlockPos(this.posX + velocityOffset*normalizedVelocity.x, this.posY + velocityOffset*normalizedVelocity.y, this.posZ + velocityOffset*normalizedVelocity.z);
					IBlockState state = this.world.getBlockState(pos);
					if(state.getBlock().canCollideCheck(state, true)){
						List<AxisAlignedBB> collidingAABBList = new ArrayList<AxisAlignedBB>();
						AxisAlignedBB box = this.getBoundingBox().offset(velocityOffset*normalizedVelocity.x, velocityOffset*normalizedVelocity.y, velocityOffset*normalizedVelocity.z);
						state.addCollisionBoxToList(world, pos, box, collidingAABBList, null, false);
						if(!collidingAABBList.isEmpty()){
							collidedBlockPos = pos;
						}
					}
				}
				
				//If we hit an entity or block, execute hit logic.
				//Only send a packet to the server if we are the ones firing this gun.
				//Other players will see bullets on their screen, but those bullets shouldn't send packets
				//as they themselves were spawned based on controller packet logic.
				//Doing this prevents all clients from sending collision packets to the server.
				if(collidedEntity != null){
					if(this.playerID == Minecraft.getMinecraft().player.getEntityId()){
						MTS.MTSNet.sendToServer(new PacketBulletHit(this.posX + velocityOffset*normalizedVelocity.x, this.posY + velocityOffset*normalizedVelocity.y, this.posZ + velocityOffset*normalizedVelocity.z, velocity, this.bulletName, this.playerID, collidedEntity.getEntityId()));
					}
					this.setExpired();
					return;
				}else if(collidedBlockPos != null){
					if(this.playerID == Minecraft.getMinecraft().player.getEntityId()){
						MTS.MTSNet.sendToServer(new PacketBulletHit(collidedBlockPos.getX(), collidedBlockPos.getY(), collidedBlockPos.getZ(), velocity, this.bulletName, this.playerID, -1));
					}
					this.setExpired();
					return;
				}
			}
						
			//We didn't collide with anything, slow down and fall down towards the ground.
			this.prevPosX = this.posX;
	        this.prevPosY = this.posY;
	        this.prevPosZ = this.posZ;
			this.motionX *= 0.98F;
			this.motionY *= 0.98F;
			this.motionZ *= 0.98F;
			this.motionY -= 0.0245F;
			this.posX += motionX;
			this.posY += motionY;
			this.posZ += motionZ;
			this.setBoundingBox(this.getBoundingBox().offset(motionX, motionY, motionZ));
		}
	}
	
	@Override
	public void renderParticle(BufferBuilder worldRendererIn, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ){
        //Get the current rendering position based on the particles current position and velocity.
        float renderPosX = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTicks - interpPosX);
        float renderPosY = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTicks - interpPosY);
        float renderPosZ = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTicks - interpPosZ);
        
        //Get brightness information.
        int brightness = pack.bullet.type.equals("tracer") ? (15 << 20 | 15 << 4) : getBrightnessForRender(partialTicks); 
        int skyLight = brightness >> 16 & 65535;
        int blockLight = brightness & 65535;
        
        //Parse the model if we haven't already.
        if(!parsedBulletModels.containsKey(bulletName)){
        	ResourceLocation modelLocation;
        	if(pack.general.modelName != null){
				modelLocation = new ResourceLocation(bulletName.substring(0, bulletName.indexOf(':')), "objmodels/parts/" + pack.general.modelName + ".obj");
			}else{
				modelLocation = new ResourceLocation(bulletName.substring(0, bulletName.indexOf(':')), "objmodels/parts/" + bulletName.substring(bulletName.indexOf(':') + 1) + ".obj");
			}
        	parsedBulletModels.put(bulletName, OBJParserSystem.parseOBJModel(modelLocation.getResourceDomain(), modelLocation.getResourcePath()));
        }
        
        //Render the parsed model.
        //Rotate the model point to align with the velocity.
        //The parse parses tris, but we want quads here instead.
        //Skip every 4th and 6th point to render a quad.
        byte index = 1;
        double yaw = -Math.toDegrees(Math.atan2(motionX, motionZ));
        double pitch = -Math.toDegrees(Math.asin(motionY/Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ)));
        for(Entry<String, Float[][]> modelObjects : parsedBulletModels.get(bulletName).entrySet()){
        	for(Float[] modelPoints : modelObjects.getValue()){
        		if(index != 4 && index != 6){
	        		Vec3d rotatedCoords = RotationSystem.getRotatedPoint(new Vec3d(modelPoints[0], modelPoints[1], modelPoints[2]), (float) pitch, (float) yaw, 0);
	        		worldRendererIn.pos(renderPosX + rotatedCoords.x, renderPosY + rotatedCoords.y, renderPosZ + rotatedCoords.z).tex(particleTexture.getMinU() + (particleTexture.getMaxU() - particleTexture.getMinU())*modelPoints[3], particleTexture.getMinV() + (particleTexture.getMaxV() - particleTexture.getMinV())*modelPoints[4]).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        		}
        		index = (byte) (index == 6 ? 1 : index + 1);
        	}
        }
	}
	
	@Override
	public int getBrightnessForRender(float partialTicks){
        if(!pack.bullet.type.equals("tracer")){
			int i = super.getBrightnessForRender(partialTicks);
		    int j = 240;
		    int k = i >> 16 & 255;
		    return 240 | k << 16;
        }else{
        	return super.getBrightnessForRender(partialTicks);
        }
    }
	
	@Override
    public int getFXLayer(){
        return 1;
    }
}
