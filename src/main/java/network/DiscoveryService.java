package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import config.Config;

public class DiscoveryService implements AutoCloseable {
    private final DatagramSocket socket;
    private final List<Peer> discoveredPeers = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private Thread listenerThread;
    private Thread announcerThread;
    private final String deviceName;

    public DiscoveryService(String deviceName) throws IOException {
        this.deviceName = deviceName;
        this.socket = new DatagramSocket(Config.DISCOVERY_PORT);
        this.socket.setSoTimeout(Config.SOCKET_TIMEOUT_MS);
    }

    public void start() {
        running = true;
        listenerThread = new Thread(this::listen, "discovery-listener");
        announcerThread = new Thread(this::announcePresence, "discovery-announcer");
        listenerThread.start();
        announcerThread.start();
    }

    private void listen() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("SPEEDLAN:AVAILABLE:")) {
                    String name = message.substring("SPEEDLAN:AVAILABLE:".length());
                    if (!name.equals(deviceName)) {
                        Peer peer = new Peer(name, packet.getAddress(), packet.getPort());
                        discoveredPeers.removeIf(p -> p.address().equals(peer.address()));
                        discoveredPeers.add(peer);
                    }
                }
            } catch (Exception e) {
                // timeout expected
            }
        }
    }

    private void announcePresence() {
        while (running) {
            try {
                String message = "SPEEDLAN:AVAILABLE:" + deviceName;
                byte[] buffer = message.getBytes();
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, Config.DISCOVERY_PORT);
                socket.send(packet);
                Thread.sleep(Config.DISCOVERY_INTERVAL_MS);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public List<Peer> getAvailablePeers() {
        return new ArrayList<>(discoveredPeers);
    }

    public void stop() {
        running = false;
        try {
            if (listenerThread != null) listenerThread.join(1000);
            if (announcerThread != null) announcerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stop();
        socket.close();
    }
}
