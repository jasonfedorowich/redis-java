package org.redis.io;

import lombok.extern.slf4j.Slf4j;
import org.redis.cache.CacheManager;
import org.redis.error.InvalidRequestFormat;
import org.redis.protocol.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class NonBlockingServer {

    private final ServerSocketChannel serverSocketChannel;
    private final ExecutorService executorService;
    private final CacheManager cacheManager;

    public NonBlockingServer(int port, CacheManager cacheManager, int maxThreads) {
        ServerSocketChannel serverSocketChannel = null;
        try {
            this.executorService = Executors.newFixedThreadPool(maxThreads);
            this.cacheManager = cacheManager;
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            this.serverSocketChannel = serverSocketChannel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static NonBlockingServer open(int port, CacheManager cacheManager, int maxNumberOfThreads){
        return new NonBlockingServer(port, cacheManager, maxNumberOfThreads);
    }

    public void accept(){
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if(socketChannel != null){
                socketChannel.configureBlocking(false);
                //todo configure non-blocking: https://liakh-aliaksandr.medium.com/java-sockets-i-o-blocking-non-blocking-and-asynchronous-fb7f066e4ede ie. read bytes until non q is this necessary?
                executorService.execute(new ThreadHandler(socketChannel));

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ThreadHandler implements Runnable{
        private final SocketChannel socketChannel;
        public ThreadHandler(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try{
                try {
                    ByteBuffer readBuffer = ByteBuffer.allocate(5 * 1024);
                    final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
                    ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

                    writeExecutor.execute(()->{
                        while(socketChannel.isConnected() || socketChannel.isOpen()){
                            while(!writeQueue.isEmpty()){
                                ByteBuffer buffer = writeQueue.peek();
                                try {
                                    socketChannel.write(buffer);
                                } catch (IOException e) {
                                    log.error("Unable to write to buffer");
                                    throw new RuntimeException(e);
                                }
                                if(!buffer.hasRemaining()) writeQueue.poll();

                            }
                        }
                    });

                    while(socketChannel.isConnected() || socketChannel.isOpen()){
                        try{

                            readBuffer = read(socketChannel, readBuffer);
                            List<Request> requests = tryRequest(readBuffer);
                            for(Request req: requests){
                                //todo maybe some validation on the requests here
                                Response response = cacheManager.handle(req);
                                writeQueue.add(bufferForWrite(response));
                            }
                            if(!requests.isEmpty()){
                                readBuffer.clear();
                            }

                        }catch (InvalidRequestFormat e){
                            log.error("Error received: {}", e.toString());
                            ByteBuffer error = ByteBuffer.allocate(1);
                            error.put((byte)ResponseCode.ERROR.getCode());
                            socketChannel.write(error);
                            readBuffer.clear();
                        }

                    }

                    socketChannel.close();
                    executorService.shutdown();
                } catch (IOException e) {
                    //todo gracefully disconnect
                    log.error("Error received: {}", e.toString());
                    ByteBuffer error = ByteBuffer.allocate(1);
                    error.put((byte)ResponseCode.ERROR.getCode());
                    socketChannel.write(error);
                    throw new RuntimeException(e);
                }
                socketChannel.close();
                executorService.shutdown();

            }catch (IOException e){
                //todo change error
                log.error("Unable to read input stream");
                throw new RuntimeException(e);
            }



        }

        private ByteBuffer bufferForWrite(Response response) {
            ByteBuffer writeBuffer = ByteBuffer.allocate(5 * 1024);
            int requiredSize = 1;
            if(response.getData() == null){
                requiredSize += 4;
            }else{
                requiredSize += 4;
                requiredSize += response.getData().length;
            }
            while(requiredSize > writeBuffer.remaining()){
                ByteBuffer newBuffer = ByteBuffer.allocate(writeBuffer.capacity() * 2);
                newBuffer.put(writeBuffer);
                writeBuffer = newBuffer;
            }
            writeBuffer.put((byte)response.getResponseCode().getCode());
            if(response.getData() == null){
                writeBuffer.putInt(0);
            }else{
                writeBuffer.putInt(response.getData().length);
                writeBuffer.put(response.getData());
            }
            writeBuffer.rewind();
            return writeBuffer;
        }

        private ByteBuffer read(SocketChannel socketChannel, ByteBuffer buffer) throws IOException {
            socketChannel.read(buffer);
            if(!buffer.hasRemaining()){
                ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                newBuffer.put(buffer);
                return newBuffer;
            }
            return buffer;
        }

        private List<Request> tryRequest(ByteBuffer buffer) {
            int writePosition = buffer.position();
            ByteBuffer dup = buffer.duplicate();
            dup.rewind();
            int length;
            try{
                length = dup.getInt();
                if(4 + length > writePosition){
                    return List.of();
                }
            }catch (BufferUnderflowException e){
                return List.of();
            }


            try{
                return parseRequest(dup);
            }catch (BufferUnderflowException e){
                log.error("Invalid request");
                buffer.clear();
                throw new InvalidRequestFormat(e);
            }


        }

        private List<Request> parseRequest(ByteBuffer buffer) {
            byte n = buffer.get();
            List<Request> requests = new LinkedList<>();
            for(int i = 0; i < n; i++){
                byte args = buffer.get();
                List<String> req = readRequest(args, buffer);
                requests.add(make(req.get(0), req.subList(1, req.size())));
            }
            //todo if there are still bytes?
            return requests;
        }

        private Request make(String type, List<String> req) {
            return switch (type) {
                case "get" -> new GetRequest(req);
                case "set" -> new SetRequest(req);
                default -> throw new UnsupportedOperationException("Unsupported operation: " + type);
            };
        }

        private List<String> readRequest(byte args, ByteBuffer buffer) {
           List<String> input = new LinkedList<>();
           for(int i = 0; i < args; i++){
               String arg = readString(buffer);
               input.add(arg);
           }
           return input;
        }

        private String readString(ByteBuffer buffer) {
            int len = buffer.getInt();
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < len; i++){
                char c = (char)buffer.get();
                stringBuilder.append(c);
            }
            return stringBuilder.toString();
        }

    }

}
