package org.redis;

import org.redis.cache.KVCache;
import org.redis.io.BlockingServer;
import org.redis.io.CacheManager;
import org.redis.io.NonBlockingServer;

import java.util.concurrent.Executors;

public class Main {

    //todo pass from properties
    private static final int NUMBER_OF_THREADS = 10;
    private static final int PORT = 7878;
    public static void main(String[] args) {
        System.out.println("Hello world!");
        var executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        NonBlockingServer blockingServer = NonBlockingServer.open(PORT, executorService, new CacheManager(KVCache.newSimpleInMemoryCache()));

        while(true){
            blockingServer.accept();
            //executorService.execute(new ThreadHandler(socket));
            //System.out.println("Client connected on addr " + socket.getInetAddress());
        }


    }
}