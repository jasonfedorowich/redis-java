package org.redis.protocol;

import lombok.Getter;
import lombok.Setter;

public interface Request {

    boolean isReadOnly();

    String key();

    @Setter
    @Getter
    class RequestBuilder{
        private int args;
        private
        public Request build(){

        }
    }

}
