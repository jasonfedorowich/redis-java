package org.redis.io;

import lombok.extern.slf4j.Slf4j;
import org.redis.cache.KVCache;
import org.redis.protocol.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class BlockingServer implements AutoCloseable{
    //todo make configurable

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private final CacheManager cacheManager;
    private final int port;

    private BlockingServer(int port, ExecutorService executorService, CacheManager cacheManager){
        try{
            this.serverSocket = new ServerSocket(port);
            this.executorService = executorService;
            this.cacheManager = cacheManager;
            this.port = port;
        }catch (IOException e){
          log.error("Unable to open socket");
          //todo create exception
          throw new RuntimeException("");
        }

    }
    //todo does this need thread pool or can it manage its own?
    public static BlockingServer open(int port, ExecutorService executorService, CacheManager cacheManager){
        return new BlockingServer(port, executorService, cacheManager);
    }

    public void accept(){
        try {
            log.info("Server started listening on port: {}", port);
            Socket socket = serverSocket.accept();
            executorService.execute(new ThreadHandler(socket));
        } catch (IOException e) {
            log.error("Unable to open client socket");
            throw new RuntimeException(e);
        }
    }


    @Override
    public void close() throws Exception {
        serverSocket.close();
    }

    class ThreadHandler implements Runnable{

        private final Socket socket;

        public ThreadHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));

                int r;
                while(socket.isConnected()){
                    while((r = dataInputStream.readInt()) != -1){
                        try{
                            //blocking can largely ignore the size r of the byte stream
                            int n = dataInputStream.read();
                            for(int i = 0; i < n; i++){
                                int args = dataInputStream.read();
                                List<String> req = readRequest(args, dataInputStream);
                                Request request = read(req.get(0), req.subList(1, req.size()));
                                Response response = cacheManager.handle(request);
                                writeResponse(response, dataOutputStream);
                                dataOutputStream.flush();
                            }

                        }catch (Exception e){
                            log.error("Error received: {}", e.toString());
                            dataOutputStream.write(ResponseCode.ERROR.getCode());
                            throw new RuntimeException("Error from IO thread", e);
                        }

                        dataOutputStream.flush();


                    }
                }
                dataOutputStream.flush();
                dataInputStream.close();
                dataOutputStream.close();




            } catch (IOException e) {
                //todo change error
                log.error("Unable to read input stream");
                throw new RuntimeException(e);
            }
        }

        private void writeResponse(Response response, DataOutputStream dataOutputStream) {
            try {
                dataOutputStream.write(response.getResponseCode().getCode());
                if(response.getData() == null){
                    dataOutputStream.writeInt(0);
                }else{
                    dataOutputStream.writeInt(response.getData().length);
                    dataOutputStream.write(response.getData());
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to write to output stream", e);
            }
        }

        private List<String> readRequest(int args, DataInputStream dataInputStream) {
            List<String> input = new LinkedList<>();
            for(int i = 0; i < args; i++){
                input.add(readString(dataInputStream));
            }
            return input;

        }

        private String readString(DataInputStream dataInputStream) {
            int len;
            try {
                len = dataInputStream.readInt();
            } catch (IOException e) {
                log.error("Invalid protocol format; invalid length of string");
                throw new RuntimeException(e);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < len; i++) {
                char c;
                try {
                    c = (char)dataInputStream.read();
                } catch (IOException e) {
                    log.error("Invalid protocol format not enough data for string");
                    throw new RuntimeException(e);
                }
                stringBuilder.append(c);
            }
            return stringBuilder.toString();
        }

        private Request read(String type, List<String> req) {
            return switch (type) {
                case "get" -> new GetRequest(req);
                case "set" -> new SetRequest(req);
                default -> throw new UnsupportedOperationException("Unsupported operation: " + type);
            };

        }
    }

}
