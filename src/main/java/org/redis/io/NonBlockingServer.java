package org.redis.io;

import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.ExecutorService;

@Slf4j
public class NonBlockingServer {

    private final int MAX_RETRIES = 10;

    private final ServerSocketChannel serverSocketChannel;
    private final int port;
    private final ExecutorService executorService;
    private final CacheManager cacheManager;

    public NonBlockingServer(int port, ExecutorService executorService, CacheManager cacheManager) {
        ServerSocketChannel serverSocketChannel = null;
        try {
            this.port = port;
            this.executorService = executorService;
            this.cacheManager = cacheManager;
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            this.serverSocketChannel = serverSocketChannel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static NonBlockingServer open(int port, ExecutorService executorService, CacheManager cacheManager){
        return new NonBlockingServer(port, executorService, cacheManager);
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
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
                    while(socketChannel.isConnected() || socketChannel.isOpen()){
                        try{
                            readBuffer = read(socketChannel, readBuffer);
                            List<Request> requests = tryRequest(readBuffer);
                            for(Request req: requests){
                                Response response = cacheManager.handle(req);
                                bufferForWrite(response, writeBuffer);
                            }

                            writeBuffer = write(socketChannel, writeBuffer);
                            readBuffer = prepareNext(readBuffer);
                        }catch (InvalidRequestFormat e){
                            log.error("Error received: {}", e.toString());
                            ByteBuffer error = ByteBuffer.allocate(1);
                            error.put((byte)ResponseCode.ERROR.getCode());
                            socketChannel.write(error);
                        }


                    }
//                    //todo does it only read up to fill the readBuffer or do we need to allocate more readBuffer space?
//                    ByteBuffer nArgs = ByteBuffer.allocate(1024);
//                    if(tryToRead(socketChannel, nArgs, MAX_RETRIES)) throw new IOException("Invalid argument; insufficient bytes");
//                    //nArgs.position(0);
//                    nArgs.rewind();
//                    byte i = 0;
//                    byte n = nArgs.get();
//                    BufferedReadObject bufferedReadObject = new BufferedReadObject(1024);
//                    BufferedWriteObject bufferedWriteObject = new BufferedWriteObject(1024);
//                    while(socketChannel.isConnected() && i < n){
//
////                        ByteBuffer argsBuff = ByteBuffer.allocate(1);
////                        if(tryToRead(socketChannel, argsBuff, MAX_RETRIES)) throw new IOException("Invalid argument; insufficient bytes");
////                        argsBuff.position(0);
////                        byte args = argsBuff.get();
////                        BufferedObjectReader bufferedObjectReader = new BufferedObjectReader(args);
////                        while(!readBuffer(socketChannel, bufferedObjectReader)){}
////                        while(!writeBuffer(socketChannel, bufferedObjectReader)){}
//                        bufferedReadObject.read(socketChannel);
//                        if(bufferedReadObject.canWrite()){
//                            Request request = bufferedReadObject.getRequest();
//                            Response response = cacheManager.handle(request);
//                            bufferedWriteObject.readBuffer(response);
//                            i++;
//                        }
//                       // if(bufferedWriteObject.canWrite()){
//                         //   socketChannel.write()
//                        //}
//
//
//                    }

                } catch (IOException e) {
                    log.error("Error received: {}", e.toString());
                    ByteBuffer error = ByteBuffer.allocate(1);
                    error.put((byte)ResponseCode.ERROR.getCode());
                    socketChannel.write(error);
                    throw new RuntimeException(e);
                }
                socketChannel.close();

            }catch (IOException e){
                //todo change error
                log.error("Unable to read input stream");
                throw new RuntimeException(e);
            }



        }

        private ByteBuffer write(SocketChannel socketChannel, ByteBuffer writeBuffer) {
            return null;
        }

        private void bufferForWrite(Response response, ByteBuffer writeBuffer) {
            writeBuffer.put((byte)response.getResponseCode().getCode());
            if(response.getData() == null){
                writeBuffer.putInt(0);
            }else{
                writeBuffer.putInt(response.getData().length);
                writeBuffer.put(response.getData());
            }
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
            buffer.position(0);
            int length;
            try{
                length = buffer.getInt();
                if(4 + length > writePosition){
                    buffer.position(writePosition);
                    return List.of();
                }
            }catch (BufferUnderflowException e){
                //not enough bytes to read for request
                buffer.position(writePosition);
                return List.of();
            }


            try{
                return parseRequest(buffer);
            }catch (BufferUnderflowException e){
                log.error("Invalid request");
                buffer.rewind();
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

        private boolean writeBuffer(SocketChannel socketChannel, BufferedReadObject bufferedReadObject) {
            return false;
        }

        private boolean readBuffer(SocketChannel socketChannel, BufferedReadObject bufferedReadObject) {

           // return bufferedReadObject.hasAllArgs();
            return true;
        }

        boolean tryToRead(SocketChannel socketChannel, ByteBuffer buffer, int retries) throws IOException {
            int n;
            while((n = socketChannel.read(buffer)) <= 0 && retries >= 0){
                retries--;
            }
            return retries <= 0 && n > 0;
        }
    }

    private class BufferedReadObject {
        private ByteBuffer buffer;
        private int bufferSize;
        private int bytesRead;
        private int bytesConsumed;
        private Queue<Request> requests = new LinkedList<>();

        enum State{
            NEED_ARGS,
            NEED_LENGTH,
            READ_STRING,

        }

        private State currentState;

        public BufferedReadObject(int bufferSize) {
            buffer = ByteBuffer.allocate(bufferSize);
            currentState = State.NEED_ARGS;
        }

        public void read(SocketChannel socketChannel) throws IOException {
            bytesRead += socketChannel.read(buffer);
            if(!buffer.hasRemaining()){
                ByteBuffer newBuffer = ByteBuffer.allocate(bufferSize * 2);
                newBuffer.put(buffer);
                buffer = newBuffer;
                bufferSize *= 2;

            }

            tryFlushBuffer();

        }

        public Request getRequest() {
            return requests.poll();
        }


        private void tryFlushBuffer() {
            switch (currentState){
                case NEED_ARGS:{
                    buffer.position(bytesConsumed);
                    byte args = buffer.get();
                    if(args != 0){

                        currentState = State.NEED_LENGTH;
                    }

                }
            }
        }

        public boolean canWrite() {
            return !requests.isEmpty();
        }
    }

    private class BufferedWriteObject {
        private ByteBuffer buffer;

        public BufferedWriteObject(int bufferSize) {
            buffer = ByteBuffer.wrap(new byte[bufferSize]);
        }

        public void buffer(Response response) {

        }
    }
}
