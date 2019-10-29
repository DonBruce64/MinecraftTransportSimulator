package minecrafttransportsimulator.vehicles.parts;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackPartObject.PartBulletConfig;
import minecrafttransportsimulator.packets.general.PacketBulletHit;
import minecrafttransportsimulator.systems.PackParserSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
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
	private final PartBulletConfig bulletPackData;
	private final int playerID;
	private final EntityVehicleE_Powered vehicle;
	
	private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
	
    public PartBullet(World world, double x, double y, double z, double motionX, double motionY, double motionZ, String bulletName, int playerID, EntityVehicleE_Powered vehicle){
    	super(world, x, y, z);
        //Set basic properties.
    	this.particleMaxAge = 60;
        this.bulletName = bulletName;
        this.bulletPackData = PackParserSystem.getPartPack(this.bulletName).bullet;
        
        //Set rendering properties.
        if(bulletPackData.type.equals("tracer")){
        	this.setRBGColorF(1.0F, 0.0F, 0.0F);
        }else{
        	this.setRBGColorF(1.0F, 1.0F, 1.0F);
        }
        this.setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(MTSRegistry.partItemMap.get(bulletName), 0));
        float vSpan = this.particleTexture.getMaxV() - this.particleTexture.getMinV();
		float vMid = this.particleTexture.getMinV() + vSpan/2F;
		minU = this.particleTexture.getMinU();
        maxU = this.particleTexture.getMaxU();
        minV = vMid - vSpan*bulletPackData.texturePercentage/2F;
        maxV = vMid + vSpan*bulletPackData.texturePercentage/2F;
        
        //Set physical state and runtime properties.
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.setSize(bulletPackData.diameter/1000F, bulletPackData.diameter/1000F);
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
			//Check if we collided with anything.  Go in 1-block steps from our current position to the final position based on our motion.
			//Arrows move so slow that this isn't a problem.  Not so for bullets.
			double velocity = Math.sqrt(motionX*motionX + motionY*motionY + motionZ*motionZ);
			double increments = Math.floor(velocity);

			//We check for entities and blocks in 0.25 block movements.
			//These are done in the same loop to prevent us checking further along if we hit something.
			Entity collidedEntity = null;
			BlockPos collidedBlockPos = null;
			for(double d=0; d<increments; d+=0.25D){
				for(Entity entity : this.world.getEntitiesWithinAABBExcludingEntity(vehicle, this.getBoundingBox().offset(motionX*d/increments, motionY*d/increments, motionZ*d/increments))){
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
					BlockPos pos = new BlockPos(this.posX + motionX*d/increments, this.posY + motionY*d/increments, this.posZ + motionZ*d/increments);
					IBlockState state = this.world.getBlockState(pos);
					if(state.getBlock().canCollideCheck(state, true)){
						List<AxisAlignedBB> collidingAABBList = new ArrayList<AxisAlignedBB>();
						AxisAlignedBB box = this.getBoundingBox().offset(motionX*d/increments, motionY*d/increments, motionZ*d/increments);
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
						MTS.MTSNet.sendToServer(new PacketBulletHit(this.posX + motionX*d/increments, this.posY + motionY*d/increments, this.posZ + motionZ*d/increments, velocity, this.bulletName, this.playerID, collidedEntity.getEntityId()));
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
        int brightness = this.bulletPackData.type.equals("tracer") ? (15 << 20 | 15 << 4) : getBrightnessForRender(partialTicks); 
        int skyLight = brightness >> 16 & 65535;
        int blockLight = brightness & 65535;
        
        //Get the texture points as if we only have velocity in the +Z direction.
        //We need two sets of 4.  One for the side view, and one for the top cross-view.
        //Ensure we have a radius of a minimum of 2 pixels for proper rendering.
        float realRadius = Math.max(bulletPackData.diameter/1000F/2F, 0.0625F);
        Vec3d[] texturePointCoords = new Vec3d[]{
        	new Vec3d(0, -realRadius, -realRadius),
        	new Vec3d(0, realRadius, -realRadius),
        	new Vec3d(0, realRadius, realRadius),
        	new Vec3d(0, -realRadius, realRadius),
        	new Vec3d(-realRadius, 0, -realRadius),
        	new Vec3d(realRadius, 0, -realRadius),
        	new Vec3d(realRadius, 0, realRadius),
        	new Vec3d(-realRadius, 0, realRadius)
        };
        
        //Rotate the texture to align with the velocity.
        Vec3d velocityVec = new Vec3d(motionX, motionY, motionZ).normalize();
        double yaw = -Math.toDegrees(Math.atan2(motionX, motionZ));
        double pitch = -Math.toDegrees(Math.asin(motionY/Math.sqrt(motionX*motionX+motionY*motionY+motionZ*motionZ)));
        for(byte i=0; i<8; ++i){
        	texturePointCoords[i] = RotationSystem.getRotatedPoint(texturePointCoords[i], (float) pitch, (float) yaw, 0);
        }

        //Add the points to the vertexBuffer.
        worldRendererIn.pos(renderPosX + texturePointCoords[0].x, renderPosY + texturePointCoords[0].y, renderPosZ + texturePointCoords[0].z).tex((double)maxU, (double)maxV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[1].x, renderPosY + texturePointCoords[1].y, renderPosZ + texturePointCoords[1].z).tex((double)maxU, (double)minV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[2].x, renderPosY + texturePointCoords[2].y, renderPosZ + texturePointCoords[2].z).tex((double)minU, (double)minV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[3].x, renderPosY + texturePointCoords[3].y, renderPosZ + texturePointCoords[3].z).tex((double)minU, (double)maxV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[4].x, renderPosY + texturePointCoords[4].y, renderPosZ + texturePointCoords[4].z).tex((double)maxU, (double)maxV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[5].x, renderPosY + texturePointCoords[5].y, renderPosZ + texturePointCoords[5].z).tex((double)maxU, (double)minV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[6].x, renderPosY + texturePointCoords[6].y, renderPosZ + texturePointCoords[6].z).tex((double)minU, (double)minV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
        worldRendererIn.pos(renderPosX + texturePointCoords[7].x, renderPosY + texturePointCoords[7].y, renderPosZ + texturePointCoords[7].z).tex((double)minU, (double)maxV).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(skyLight, blockLight).endVertex();
    }
	
	@Override
    public int getFXLayer(){
        return 1;
    }
}
