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
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target, port), 2000);
            socket.setSoTimeout(2000);
            
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF("SPEEDLAN:RTT");
            dos.flush();
            
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            dis.readUTF();
            
            socket.close();
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000_000.0;
    }

    public static long measureBandwidth(InetAddress target, int port, int testSize) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target, port), 2000);
            socket.setSoTimeout(5000);
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeUTF("SPEEDLAN:BW");
            dos.flush();
            
            long start = System.nanoTime();
            byte[] data = new byte[testSize];
            out.write(data);
            out.flush();
            
            DataInputStream dis = new DataInputStream(in);
            dis.readUTF();
            
            long end = System.nanoTime();
            socket.close();
            
            return (long) (testSize * 1_000_000_000.0 / (end - start));
        }
    }

    public static void sendFile(File file, InetAddress target, int port, TransferProgress progress) 
            throws IOException {
        sendFile(file, target, port, calculateChunkSize(100 * 1024 * 1024, 0.01), progress);
    }
    
    public static void sendFile(File file, InetAddress target, int port, int chunkSize, TransferProgress progress) 
            throws IOException {
        long fileSize = file.length();
        
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
