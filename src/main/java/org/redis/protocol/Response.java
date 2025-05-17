package org.redis.protocol;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Response {

    public ResponseCode responseCode;

    public byte[] data;

}
