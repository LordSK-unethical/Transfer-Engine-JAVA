package config;

public final class Config {
    public static final int DEFAULT_MIN_CHUNK = 4096;
    public static final int DEFAULT_MAX_CHUNK = 1024 * 1024 * 10;
    public static final int DISCOVERY_PORT = 45678;
    public static final int TRANSFER_PORT = 45679;
    public static final int DISCOVERY_INTERVAL_MS = 1000;
    public static final int SOCKET_TIMEOUT_MS = 5000;
    public static final int BUFFER_SIZE = 1024 * 1024;

    private Config() {}
}
