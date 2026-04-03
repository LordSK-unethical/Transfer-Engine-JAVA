package network;

import java.net.InetAddress;

public record Peer(String name, InetAddress address, int port) {
    @Override
    public String toString() {
        return name + " (" + address.getHostAddress() + ")";
    }
}
