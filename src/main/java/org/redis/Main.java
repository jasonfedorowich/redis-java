package org.redis;

import org.redis.cache.Cache;
import org.redis.cache.CacheManager;
import org.redis.io.NonBlockingServer;

import java.util.concurrent.Executors;

public class Main {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    //todo pass from properties
    private static final int NUMBER_OF_THREADS = 10;
    private static final int PORT = 7878;
    public static void main(String[] args) {
        //todo add better logging
        System.out.println("Server started....");
        //todo fix 'new Cache()'
        NonBlockingServer blockingServer = NonBlockingServer.open(PORT, new CacheManager(new Cache(3)), NUMBER_OF_THREADS);
        while(true){
            blockingServer.accept();
            //executorService.execute(new ThreadHandler(socket));
            //System.out.println("Client connected on addr " + socket.getInetAddress());
        }


    }
}