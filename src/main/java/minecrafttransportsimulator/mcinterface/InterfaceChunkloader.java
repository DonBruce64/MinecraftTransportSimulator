package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@EventBusSubscriber
public class InterfaceChunkloader implements LoadingCallback{
	private static Map<BuilderEntity, Ticket> entityTickets = new HashMap<BuilderEntity, Ticket>();
	public static final InterfaceChunkloader INSTANCE = new InterfaceChunkloader();
	
	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world){
		for(Ticket modTicket : tickets){
			if(modTicket.getModId().equals(MasterLoader.MODID)){
				modTicket.setChunkListDepth(1);
				if(modTicket.getType().equals(Type.ENTITY)){
					entityTickets.put((BuilderEntity) modTicket.getEntity(), modTicket);
				}
			}
		}
	}
	
	public static void removeEntityTicket(AEntityBase entity){
		if(!entity.world.world.isRemote){
			if(entityTickets.containsKey(entity.wrapper.entity)){
				ForgeChunkManager.releaseTicket(entityTickets.get(entity.wrapper.entity));
				entityTickets.remove(entity.wrapper.entity);
			}
		}
	}
	
	@SubscribeEvent
	public static void onWorldUnloadEvent(WorldEvent.Unload event){
		if(!event.getWorld().isRemote){
			//Remove all tickets for entities in this world.
			Iterator<BuilderEntity> iterator = entityTickets.keySet().iterator();
			while(iterator.hasNext()){
				BuilderEntity builder = iterator.next();
				//Not sure HOW this can be null during boot, but it can be....
				if(builder != null){
					if(builder.world.provider.getDimension() == event.getWorld().provider.getDimension()){
						iterator.remove();
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event){
		//Need to tick event this, as entities in unloaded chunks don't get update calls, so we need
		//to ensure when they do move into one, they force the update.
		//To prevent CMEs here, we use an indexed checker.
		if(!event.world.isRemote){
			for(int i=0; i<event.world.loadedEntityList.size(); ++i){
				Entity entity = event.world.loadedEntityList.get(i);
				if(entity instanceof BuilderEntity){
					if(((BuilderEntity) entity).entity instanceof EntityVehicleF_Physics){
						if(entityTickets.containsKey(entity)){
							if(!entity.isDead){
								ForgeChunkManager.forceChunk(entityTickets.get(entity), new ChunkPos(entity.chunkCoordX, entity.chunkCoordZ));
							}else{
								removeEntityTicket(((BuilderEntity) entity).entity);
							}
						}else{
							Ticket ticket = ForgeChunkManager.requestTicket(MasterLoader.INSTANCE, entity.world, Type.ENTITY);
							if(ticket != null){
								ticket.setChunkListDepth(1);
								ticket.bindEntity(entity);
								entityTickets.put((BuilderEntity) entity, ticket);
							}
						}
					}
				}
			}
		}
	}
}
