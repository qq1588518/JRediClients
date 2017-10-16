/**
 * Copyright 2016 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redis.clients.redisson.client.codec;

import java.io.IOException;

import redis.clients.redisson.client.handler.State;
import redis.clients.redisson.client.protocol.Decoder;

import io.netty.buffer.ByteBuf;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class IntegerCodec extends StringCodec {

    public static final IntegerCodec INSTANCE = new IntegerCodec();

    public final Decoder<Object> decoder = new Decoder<Object>() {
        @Override
        public Object decode(ByteBuf buf, State state) throws IOException {
            String str = (String) IntegerCodec.super.getValueDecoder().decode(buf, state);
            return Integer.valueOf(str);
        }
    };

    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }

}
