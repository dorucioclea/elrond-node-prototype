package network.elrond.p2p.model;

import java.util.ArrayList;
import java.util.List;

import network.elrond.p2p.P2PChannelListener;

public class P2PBroadcastChannel {

    private final P2PBroadcastChannelName name;
    private final P2PConnection connection;
    private final List<P2PChannelListener> listeners = new ArrayList<>();

    public P2PBroadcastChannel(P2PBroadcastChannelName chanelName, P2PConnection connection) {
        this.name = chanelName;
        this.connection = connection;
    }

    public P2PBroadcastChannelName getName() {
        return name;
    }

    public P2PConnection getConnection() {
        return connection;
    }

    public List<P2PChannelListener> getListeners() {
        return listeners;
    }

    public String getChannelIdentifier(Integer destinationShard) {
        String indent = name.toString();
        Integer shardIndex = connection.getShard().getIndex();

        if (P2PChannelType.SHARD_LEVEL.equals(name.getType())) {
            indent += connection.getShard().getIndex();
        } else if (P2PChannelType.GLOBAL_LEVEL.equals(name.getType())) {
            if (destinationShard < shardIndex) {
                indent += destinationShard + "" + connection.getShard().getIndex();
            } else if (destinationShard > shardIndex) {
                indent += connection.getShard().getIndex() + "" + destinationShard;
            }
        }

        return indent;
    }

    @Override
    public String toString() {
        return String.format("P2PBroadcastChannel{name=%s, listeners.size()=%d}", name, listeners.size());
    }
}

