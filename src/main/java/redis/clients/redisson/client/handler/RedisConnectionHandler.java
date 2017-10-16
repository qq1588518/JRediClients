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
package redis.clients.redisson.client.handler;

import redis.clients.redisson.client.RedisClient;
import redis.clients.redisson.client.RedisConnection;

import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class RedisConnectionHandler extends BaseConnectionHandler<RedisConnection> {

    public RedisConnectionHandler(RedisClient redisClient) {
        super(redisClient);
    }
    
    @Override
    RedisConnection createConnection(ChannelHandlerContext ctx) {
        return new RedisConnection(redisClient, ctx.channel(), connectionPromise);
    }

}
