package transfer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

import config.Config;

public class TransferManager {
    private static final int MIN_CHUNK = Config.DEFAULT_MIN_CHUNK;
    private static final int MAX_CHUNK = Config.DEFAULT_MAX_CHUNK;

    public static int calculateChunkSize(long bandwidthBytesPerSec, double rttSeconds) {
        long calculated = bandwidthBytesPerSec * (long)(rttSeconds * 1000);
        return (int) Math.min(MAX_CHUNK, Math.max(MIN_CHUNK, calculated));
    }

    public static double measureRTT(InetAddress target, int port) throws IOException {
        long start = System.nanoTime();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(1000);
            String ping = "SPEEDLAN:PING";
            DatagramPacket packet = new DatagramPacket(ping.getBytes(), ping.length(), target, port);
            socket.send(packet);
            
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000_000.0;
    }

    public static long measureBandwidth(InetAddress target, int port, int testSize) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int serverPort = serverSocket.getLocalPort();
            
            Thread acceptor = new Thread(() -> {
                try {
                    Socket client = serverSocket.accept();
                    client.close();
                } catch (IOException e) {}
            });
            acceptor.start();
            
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(target, port), 1000);
                OutputStream out = socket.getOutputStream();
                
                long start = System.nanoTime();
                byte[] data = new byte[testSize];
                out.write(data);
                out.flush();
                socket.close();
                long end = System.nanoTime();
                
                return (long) (testSize * 1_000_000_000.0 / (end - start));
            }
        }
    }

    public static void sendFile(File file, InetAddress target, int port, TransferProgress progress) 
            throws IOException {
        long fileSize = file.length();
        int chunkSize = calculateChunkSize(100 * 1024 * 1024, 0.01);
        
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int serverPort = serverSocket.getLocalPort();
            
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(target, port), 5000);
                
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(file.getName());
                dos.writeLong(fileSize);
                dos.writeInt(chunkSize);
                dos.flush();
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[chunkSize];
                    long transferred = 0;
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, read);
                        transferred += read;
                        if (progress != null) {
                            progress.onProgress(transferred, fileSize);
                        }
                    }
                }
            }
        }
    }

    public static void receiveFile(Socket socket, String saveDir, TransferProgress progress) 
            throws IOException {
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        String fileName = dis.readUTF();
        long fileSize = dis.readInt();
        int chunkSize = dis.readInt();
        
        File outputFile = Paths.get(saveDir, fileName).toFile();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[chunkSize];
            long transferred = 0;
            int read;
            while (transferred < fileSize && (read = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                transferred += read;
                if (progress != null) {
                    progress.onProgress(transferred, fileSize);
                }
            }
        }
    }

    @FunctionalInterface
    public interface TransferProgress {
        void onProgress(long transferred, long total);
    }
}
