package receiver;

import java.io.*;
import java.net.*;
import java.nio.file.*;

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
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            String command = dis.readUTF();
            
            if ("SPEEDLAN:RTT".equals(command)) {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("SPEEDLAN:RTT:ACK");
                dos.flush();
                socket.close();
                return;
            }
            
            if ("SPEEDLAN:BW".equals(command)) {
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("SPEEDLAN:BW:ACK");
                dos.flush();
                socket.close();
                return;
            }
            
            String fileName = command;
            long fileSize = dis.readLong();
            int chunkSize = dis.readInt();
            
            System.out.println("Receiving: " + fileName + " (" + fileSize + " bytes)");
            
            File outputFile = Paths.get(saveDirectory, fileName).toFile();
            
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[chunkSize];
                long transferred = 0;
                int read;
                while (transferred < fileSize && (read = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    transferred += read;
                    double percent = (transferred * 100.0) / fileSize;
                    System.out.printf("\rProgress: %.1f%%", percent);
                }
            }
            System.out.println("\nFile received: " + fileName);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
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
