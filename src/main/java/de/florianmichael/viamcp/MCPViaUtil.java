/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * Helper to attach the ViaMCP protocol pipeline to a Minecraft client channel.
 */
package de.florianmichael.viamcp;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

public final class MCPViaUtil {

    private MCPViaUtil() {
    }

    /**
     * Appends the ViaMCP pipeline to [channel] when a non-native target protocol is selected.
     * Must be called after the vanilla pipeline handlers (decoder/encoder/...) have been added.
     */
    public static void hookPipeline(final Channel channel) {
        try {
            if (channel instanceof SocketChannel
                    && ViaLoadingBase.getInstance() != null
                    && ViaLoadingBase.getInstance().getTargetVersion().getVersion() != ViaMCP.NATIVE_VERSION) {
                final UserConnection user = new UserConnectionImpl(channel, true);
                new ProtocolPipelineImpl(user);
                channel.pipeline().addLast(new MCPVLBPipeline(user));
            }
        } catch (final Throwable t) {
            // Never break the vanilla connect because of Via.
        }
    }
}
