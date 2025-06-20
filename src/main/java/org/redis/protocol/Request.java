package org.redis.protocol;

import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.List;

@ToString
public abstract class Request {

    protected final List<String> args;
    protected final ByteBuffer buffer;

    public Request(int typeSize, List<String> args) {
        this.args = args;
        int n = args.size();
        int l = typeSize;
        for(int i = 0; i < args.size(); i++){
            l += args.get(i).length();
        }
        int m = (n + 2) * 4 + 1 + 1;
        buffer = ByteBuffer.allocate(m + l);
    }

    abstract boolean isReadOnly();

    public String key(){
        return args.get(0);
    }

    public String value(){
        throw new UnsupportedOperationException();
    }

    public long ttl(){
        throw new UnsupportedOperationException();
    }

}
