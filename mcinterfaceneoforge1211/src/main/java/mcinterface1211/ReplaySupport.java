package mcinterface1211;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Detects replay mods without a compile dependency.  Flashback plays a recording back on a server of its own
 * (an {@code IntegratedServer} subclass) that holds only the recorded entities, and sends them to the viewer.
 * Replay Mod needs no entry here: it has no server, so none of our server logic runs for it.
 */
final class ReplaySupport {
    private static final String FLASHBACK_REPLAY_SERVER = "com.moulberry.flashback.playback.ReplayServer";
    private static final ClassValue<Boolean> REPLAY_SERVER_CLASSES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> serverClass) {
            for (Class<?> clazz = serverClass; clazz != null && clazz != MinecraftServer.class; clazz = clazz.getSuperclass()) {
                if (clazz.getName().equals(FLASHBACK_REPLAY_SERVER)) {
                    return true;
                }
            }
            return false;
        }
    };

    private ReplaySupport() {
    }

    /**
     * True if the server only plays a recording back.  Such a server must not get the things we spawn for real players
     * (a gun and a render forwarder): its viewer is not a player in the world, and the recorded entities it holds are
     * inert copies that only carry their state to the viewer.
     */
    static boolean isReplayServer(MinecraftServer server) {
        return server != null && REPLAY_SERVER_CLASSES.get(server.getClass());
    }

    /**
     * True on a client that is watching a Flashback recording, which is a client of a replay server.  The server a
     * client runs in-process (singleplayer, or a replay) is the current server; a remote client has none.  No client
     * classes are used, so this is safe in common code.
     */
    static boolean isReplayClient() {
        return isReplayServer(ServerLifecycleHooks.getCurrentServer());
    }
}
