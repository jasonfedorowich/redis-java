package org.redis.io;

import lombok.RequiredArgsConstructor;
import org.redis.cache.KVCache;
import org.redis.protocol.*;

@RequiredArgsConstructor
public class CacheManager {

    private final KVCache kvCache;

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
        ResponseCode responseCode;
        byte[] data = kvCache.delete(deleteRequest.key());
        if(data == null){
            responseCode = ResponseCode.NOT_FOUND;
        }else{
            responseCode = ResponseCode.OK;
        }

        return Response.builder()
                .responseCode(responseCode)
                .data(data)
                .build();
    }

    private Response handleSet(SetRequest setRequest) {
        kvCache.set(setRequest.key(), setRequest.value().getBytes());
        return Response.builder()
                .responseCode(ResponseCode.OK)
                .build();
    }

    private Response handleGet(GetRequest getRequest) {
        ResponseCode responseCode;
        byte[] data = kvCache.get(getRequest.key());
        if(data == null){
            responseCode = ResponseCode.NOT_FOUND;
        }else{
            responseCode = ResponseCode.OK;
        }

        return Response.builder()
                .responseCode(responseCode)
                .data(data)
                .build();
    }
}
