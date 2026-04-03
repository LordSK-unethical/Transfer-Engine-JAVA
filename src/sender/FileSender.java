package sender;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import network.DiscoveryService;
import network.Peer;
import transfer.TransferManager;

public class FileSender {
    private final DiscoveryService discovery;
    private String deviceName;

    public FileSender(String deviceName) throws IOException {
        this.deviceName = deviceName;
        this.discovery = new DiscoveryService(deviceName);
    }

    public void startDiscovery() {
        discovery.start();
    }

    public void stopDiscovery() {
        discovery.close();
    }

    public void sendFile(File file, Peer peer) throws IOException {
        System.out.println("Sending " + file.getName() + " to " + peer.name());
        
        TransferManager.sendFile(file, peer.address(), peer.port(), (transferred, total) -> {
            double percent = (transferred * 100.0) / total;
            System.out.printf("\rProgress: %.1f%%", percent);
        });
        
        System.out.println("\nFile sent successfully!");
    }
}
