package network;

import java.net.InetAddress;

public class Peer {
    private final String name;
    private final InetAddress address;
    private final int port;
    
    public Peer(String name, InetAddress address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }
    
    public String name() { return name; }
    public InetAddress address() { return address; }
    public int port() { return port; }
    
    @Override
    public String toString() {
        return name + " (" + address.getHostAddress() + ")";
    }
}
