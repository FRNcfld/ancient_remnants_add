package com.frnc.ancient_remnants_add.network;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 本 mod 的网络通道；目前只有服务端 → 客户端的小地图标记快照。 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(AncientRemnantsAdd.id("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    /** 在 {@code FMLCommonSetupEvent} 里调用一次。 */
    public static void register() {
        CHANNEL.messageBuilder(ClientboundMarkerSyncPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundMarkerSyncPacket::encode)
                .decoder(ClientboundMarkerSyncPacket::decode)
                .consumerMainThread(ClientboundMarkerSyncPacket::handle)
                .add();
    }
}
