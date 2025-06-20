package org.redis.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
//todo make cache persist data so on restart it can load data

@Slf4j
public class Cache {

    private static final int SNAPSHOT_LIMIT = 6;
    private static final int WRITE_THRESHOLD = 7;

    //todo use a "group" where each member is responsible for partitions and the leader knows which key
    //partitions is a constant and each
    private int partitions;

    private List<Partition> cache;

    public Cache(int partitions) {
        this.partitions = partitions;
        cache = new ArrayList<>(partitions);

        loadData();

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::saveData, 1000, 1000, TimeUnit.MILLISECONDS);

    }

    private void loadData() {
        for(int i = 0; i < partitions; i++){
            //todo add configurable path

            String partitionDirectory = String.format("partition-%d", i);
            Path dir = Path.of(partitionDirectory);
            if(Files.exists(dir)){
                loadLatestSnapshot(dir);
            }else{
                try {
                    Files.createDirectory(dir);
                } catch (IOException e) {
                    log.error("Unable to create directory: {} with: {}", dir, e.toString());
                    throw new RuntimeException(e);
                }
                cache.add(new Partition(new ConcurrentHashMap<>()));
            }
        }
    }

    private void loadLatestSnapshot(Path dir) {
        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            List<Path> files = new ArrayList<>();
            for (Path file : directoryStream) {
                files.add(file);
            }
            files.sort((f1, f2)-> f2.toString().compareTo(f1.toString()));
            for (Path file : files) {
                try {
                    log.trace("Loading file: {}", file);
                    ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file.toFile()));
                    ConcurrentHashMap<String, Node> map = (ConcurrentHashMap<String, Node>) objectInputStream.readObject();
                    cache.add(new Partition(map));
                    break;
                } catch (IOException e) {
                    throw new RuntimeException("Partition file not found: " + file, e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to read object", e);
                }

            }
            cache.add(new Partition(new ConcurrentHashMap<>()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void saveData(){
        for(int i = 0; i < partitions; i++){
            saveData(i, 1);
        }
    }

    private void saveData(int partition, int threshold){
        String fileName = String.format("partition-%d/%d.dat", partition, Instant.now().toEpochMilli());
        try {
            if(cache.get(partition).getModifications().get() < threshold) return;
            log.trace("Saving file: {}", fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
            objectOutputStream.writeObject(cache.get(partition).getMap());
            cache.get(partition).reset();
        } catch (IOException e) {
            log.error("Unable to save object with: {}", e.toString());
            throw new RuntimeException("Unable to save object", e);
        }
        Path partitionPath = Path.of(String.format("partition-%d", partition));
        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(partitionPath)) {
            LinkedList<Path> files = new LinkedList<>();
            for(Path path: directoryStream){
                files.add(path);
            }

            files.sort(Comparator.comparing(Path::toString));
            while(files.size() > SNAPSHOT_LIMIT){
                Files.deleteIfExists(files.removeFirst());
            }
        } catch (IOException e) {
            log.error("Unable to read directory: {}", partitionPath);
            throw new RuntimeException(e);
        }

    }


    public Node get(String key) {
        int partition = getPartition(key, this.partitions);
        return cache.get(partition).get(key);
    }


    public void set(String key, Node value) {
        int partition = getPartition(key, this.partitions);
        cache.get(partition).put(key, value);
        CompletableFuture.runAsync(()-> saveData(partition, WRITE_THRESHOLD));
    }

    public Node delete(String key) {
        int partition = getPartition(key, this.partitions);
        Node node = cache.get(partition).remove(key);
        CompletableFuture.runAsync(()-> saveData(partition, WRITE_THRESHOLD));
        return node;
    }

    private int getPartition(String key, int partitions) {
        int hc = key.hashCode();
        return hc % partitions;
    }

    @Setter
    @Getter
    static class Partition{
        private AtomicInteger modifications = new AtomicInteger(0);
        private ConcurrentHashMap<String, Node> map;


        public Partition(ConcurrentHashMap<String, Node> map) {
            this.map = map;
        }

        private Node get(String key){
            return map.get(key);
        }

        private void put(String key, Node node){
            map.put(key, node);
            modifications.incrementAndGet();
        }

        private Node remove(String key){
            Node node = map.remove(key);
            modifications.incrementAndGet();
            return node;
        }

        public void reset() {
            modifications.set(0);
        }
    }
}
