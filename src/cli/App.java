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
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String mode = args[0].toLowerCase();
        
        if ("send".equals(mode)) {
            if (args.length < 2) {
                System.out.println("Error: File path required for send mode");
                printUsage();
                return;
            }
            runSenderMode(args[1]);
        } else if ("receive".equals(mode)) {
            String saveDir = args.length > 1 ? args[1] : DEFAULT_SAVE_DIR;
            runReceiverMode(saveDir);
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar SpeedLAN.jar Send <file-path>");
        System.out.println("  java -jar SpeedLAN.jar Receive [save-directory]");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  java -jar SpeedLAN.jar Send C:\\path\\to\\file.txt");
        System.out.println("  java -jar SpeedLAN.jar Receive");
        System.out.println("  java -jar SpeedLAN.jar Receive C:\\Users\\Shadow\\Downloads");
    }

    private static void runSenderMode(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Error: Invalid file - " + filePath);
            return;
        }

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
            
            System.out.print("\nSelect receiver (number): ");
            Scanner scanner = new Scanner(System.in);
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (choice < 0 || choice >= peers.size()) {
                System.out.println("Invalid selection");
                return;
            }
            
            Peer selectedPeer = peers.get(choice);
            
            System.out.println("Starting transfer...");
            int chunkSize = TransferManager.calculateChunkSize(100 * 1024 * 1024, 0.01);
            System.out.println("Chunk size: " + (chunkSize / 1024) + " KB");
            
            TransferManager.sendFile(file, selectedPeer.address(), Config.TRANSFER_PORT, chunkSize, (transferred, total) -> {
                double percent = (transferred * 100.0) / total;
                System.out.printf("\rProgress: %.1f%%", percent);
            });
            
            System.out.println("\nFile sent successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runReceiverMode(String saveDir) {
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
            
            System.out.println("Receiver running. Press Ctrl+C to stop.");
            
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            System.out.println("\nReceiver stopped.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
