package org.redis.cache;

import lombok.RequiredArgsConstructor;
import org.redis.error.MissingFileException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
//todo make cache persist data so on restart it can load data

public class Cache {

    private final int partitions;

    private List<ConcurrentHashMap<String, Node>> cache;

    private Map<String, Integer> partition;

    public Cache(int partitions) {
        this.partitions = partitions;
        cache = new ArrayList<>(partitions);

        if(Files.exists(Path.of("partition.dat"))){
            loadData();
        }else{
            partition = new ConcurrentHashMap<>();
            for(int i = 0; i < partitions; i++){
                cache.add(new ConcurrentHashMap<>());
            }
        }


    }

    private void loadData() {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("partition.dat"));
            partition = (ConcurrentHashMap<String, Integer>) objectInputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException("File partition data not available", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to read object", e);
        }
        for(int i = 0; i < partitions; i++){
            String fileName = String.format("partition-%d.dat", i);
            if(Files.exists(Path.of(fileName))){
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileName));
                    cache.add((ConcurrentHashMap<String, Node>) objectInputStream.readObject());
                } catch (IOException e) {
                    throw new RuntimeException("Partition file not found: " + fileName, e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to read object", e);
                }
            }else{
                throw new MissingFileException("File not found ");
            }
        }
    }


    public Node get(String key) {
        int hc = key.hashCode();
        int partition = hc % partitions;
        return cache.get(key);
    }

    public void set(String key, Node value) {
        cache.put(key, value);
        System.out.println(cache);
    }

    public Node delete(String key) {
        return cache.remove(key);
    }
}
