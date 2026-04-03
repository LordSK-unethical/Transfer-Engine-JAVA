package receiver;

import java.io.*;
import java.net.*;

import config.Config;
import transfer.TransferManager;

public class FileReceiver {
    private final String deviceName;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread receiverThread;

    public FileReceiver(String deviceName) throws IOException {
        this.deviceName = deviceName;
    }

    public void start(String saveDirectory) throws IOException {
        this.serverSocket = new ServerSocket(Config.TRANSFER_PORT);
        this.running = true;
        
        System.out.println("Receiver started on port " + Config.TRANSFER_PORT);
        System.out.println("Saving files to: " + saveDirectory);
        
        receiverThread = new Thread(() -> acceptConnections(saveDirectory));
        receiverThread.start();
    }

    private void acceptConnections(String saveDirectory) {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                new Thread(() -> handleConnection(client, saveDirectory)).start();
            } catch (IOException e) {
                if (running) {
                    // handle error
                }
            }
        }
    }

    private void handleConnection(Socket socket, String saveDirectory) {
        try {
            System.out.println("Receiving file...");
            TransferManager.receiveFile(socket, saveDirectory, (transferred, total) -> {
                double percent = (transferred * 100.0) / total;
                System.out.printf("\rProgress: %.1f%%", percent);
            });
            System.out.println("\nFile received successfully!");
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (receiverThread != null) receiverThread.join(1000);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
