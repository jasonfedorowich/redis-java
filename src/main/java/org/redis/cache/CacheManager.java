package org.redis.cache;

import lombok.RequiredArgsConstructor;
import org.redis.protocol.*;

import java.time.Instant;

@RequiredArgsConstructor
public class CacheManager {

    private final Cache cache;

    public Response handle(Request request){
        if(request instanceof GetRequest g){
            return handleGet(g);
        }else if(request instanceof SetRequest s){
            return handleSet(s);
        }else if(request instanceof DeleteRequest d){
            return handleDel(d);
        }else{
            throw new UnsupportedOperationException("Unsupported request type");
        }
    }

    private Response handleDel(DeleteRequest deleteRequest) {
        Node data = cache.delete(deleteRequest.key());
        Response.ResponseBuilder builder = Response.builder();
        if(data == null){
            builder.responseCode(ResponseCode.NOT_FOUND);
        }else{
            builder.responseCode(ResponseCode.OK)
                    .data(data.getValue());
        }

        return builder.build();
    }

    private Response handleSet(SetRequest setRequest) {
        Node newNode = new Node(setRequest.value().getBytes(), Instant.now().toEpochMilli(), setRequest.ttl());
        cache.set(setRequest.key(), newNode);
        return Response.builder()
                .responseCode(ResponseCode.OK)
                .build();
    }

    private Response handleGet(GetRequest getRequest) {
        Node data = cache.get(getRequest.key());
        Response.ResponseBuilder builder = Response.builder();
        if(data == null){
            builder.responseCode(ResponseCode.NOT_FOUND);
        }else{
            if(data.getTtl() == -1){
                builder.responseCode(ResponseCode.OK)
                        .data(data.getValue());
            }else{
                Instant lastUsed = Instant.ofEpochMilli(data.getLastUsed());
                Instant now = Instant.now();
                if(lastUsed.plusMillis(data.getTtl()).isBefore(now)){
                    cache.delete(getRequest.key());
                    builder.responseCode(ResponseCode.NOT_FOUND);
                }else{
                    data.setLastUsed(now.toEpochMilli());
                    builder.responseCode(ResponseCode.OK)
                            .data(data.getValue());
                }
            }

        }

        return Response.builder()
                .build();
    }
}
