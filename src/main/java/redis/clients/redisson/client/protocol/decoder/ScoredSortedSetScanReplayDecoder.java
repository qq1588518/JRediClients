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
package redis.clients.redisson.client.protocol.decoder;

import java.util.List;

import redis.clients.redisson.client.handler.State;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class ScoredSortedSetScanReplayDecoder implements MultiDecoder<ListScanResult<Object>> {

    @Override
    public Object decode(ByteBuf buf, State state) {
        return Long.valueOf(buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public ListScanResult<Object> decode(List<Object> parts, State state) {
        List<Object> values = (List<Object>)parts.get(1);
        for (int i = 1; i < values.size(); i++) {
            values.remove(i);
        }
        return new ListScanResult<Object>((Long)parts.get(0), values);
    }

    @Override
    public boolean isApplicable(int paramNum, State state) {
        return paramNum == 0;
    }

}
