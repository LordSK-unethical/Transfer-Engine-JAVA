package cli;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import config.Config;
import network.*;
import receiver.FileReceiver;
import sender.FileSender;
import transfer.TransferManager;

public class App {
    private static final String DEVICE_NAME = System.getProperty("user.name", "Device");
    private static final String DEFAULT_SAVE_DIR = System.getProperty("user.home") + "/Downloads";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== SpeedLAN File Transfer ===");
        System.out.println("1. Send file");
        System.out.println("2. Receive file");
        System.out.print("Choose mode: ");
        
        String choice = scanner.nextLine().trim();
        
        if ("1".equals(choice)) {
            runSenderMode(scanner);
        } else if ("2".equals(choice)) {
            runReceiverMode(scanner);
        } else {
            System.out.println("Invalid choice");
        }
    }

    private static void runSenderMode(Scanner scanner) {
        try (DiscoveryService discovery = new DiscoveryService(DEVICE_NAME)) {
            discovery.start();
            System.out.println("Searching for receivers...");
            Thread.sleep(2000);
            
            List<Peer> peers = discovery.getAvailablePeers();
            if (peers.isEmpty()) {
                System.out.println("No receivers found. Make sure a receiver is running.");
                return;
            }
            
            System.out.println("\nAvailable receivers:");
            for (int i = 0; i < peers.size(); i++) {
                System.out.println((i + 1) + ". " + peers.get(i));
            }
            
            System.out.print("\nSelect receiver: ");
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= peers.size()) {
                System.out.println("Invalid selection");
                return;
            }
            
            Peer selectedPeer = peers.get(choice);
            
            System.out.print("Enter file path: ");
            String filePath = scanner.nextLine().trim();
            File file = new File(filePath);
            
            if (!file.exists() || !file.isFile()) {
                System.out.println("Invalid file");
                return;
            }
            
            System.out.println("Measuring network...");
            double rtt = TransferManager.measureRTT(selectedPeer.address(), Config.TRANSFER_PORT);
            long bandwidth = TransferManager.measureBandwidth(selectedPeer.address(), Config.TRANSFER_PORT, 1024 * 1024);
            
            int chunkSize = TransferManager.calculateChunkSize(bandwidth, rtt);
            System.out.println("RTT: " + (rtt * 1000) + "ms, Bandwidth: " + (bandwidth / (1024 * 1024)) + " MB/s");
            System.out.println("Chunk size: " + (chunkSize / 1024) + " KB");
            
            TransferManager.sendFile(file, selectedPeer.address(), Config.TRANSFER_PORT, (transferred, total) -> {
                double percent = (transferred * 100.0) / total;
                System.out.printf("\rProgress: %.1f%%", percent);
            });
            
            System.out.println("\nFile sent successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void runReceiverMode(Scanner scanner) {
        System.out.print("Save directory [" + DEFAULT_SAVE_DIR + "]: ");
        String saveDir = scanner.nextLine().trim();
        if (saveDir.isEmpty()) {
            saveDir = DEFAULT_SAVE_DIR;
        }
        
        try {
            Files.createDirectories(Paths.get(saveDir));
        } catch (IOException e) {
            System.err.println("Error creating directory: " + e.getMessage());
            return;
        }
        
        try (DiscoveryService discovery = new DiscoveryService(DEVICE_NAME)) {
            discovery.start();
            
            FileReceiver receiver = new FileReceiver(DEVICE_NAME);
            receiver.start(saveDir);
            
            System.out.println("\nPress Enter to stop...");
            scanner.nextLine();
            
            receiver.stop();
            discovery.close();
            System.out.println("Receiver stopped.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
