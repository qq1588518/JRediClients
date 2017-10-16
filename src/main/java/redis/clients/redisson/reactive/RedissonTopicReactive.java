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
package redis.clients.redisson.reactive;

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import redis.clients.redisson.PubSubMessageListener;
import redis.clients.redisson.PubSubStatusListener;
import redis.clients.redisson.RedissonTopic;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.api.RTopic;
import redis.clients.redisson.api.RTopicReactive;
import redis.clients.redisson.api.listener.MessageListener;
import redis.clients.redisson.api.listener.StatusListener;
import redis.clients.redisson.client.RedisPubSubListener;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.command.CommandReactiveExecutor;

import reactor.fn.Supplier;

/**
 * Distributed topic implementation. Messages are delivered to all message listeners across Redis cluster.
 *
 * @author Nikita Koksharov
 *
 * @param <M> message
 */
public class RedissonTopicReactive<M> implements RTopicReactive<M> {

    private final RTopic<M> topic;
    private final CommandReactiveExecutor commandExecutor;
    private final String name;

    public RedissonTopicReactive(CommandReactiveExecutor commandExecutor, String name) {
        this(commandExecutor.getConnectionManager().getCodec(), commandExecutor, name);
    }

    public RedissonTopicReactive(Codec codec, CommandReactiveExecutor commandExecutor, String name) {
        this.topic = new RedissonTopic<M>(codec, commandExecutor, name);
        this.commandExecutor = commandExecutor;
        this.name = name;
    }

    @Override
    public List<String> getChannelNames() {
        return Collections.singletonList(name);
    }

    @Override
    public Publisher<Long> publish(final M message) {
        return commandExecutor.reactive(new Supplier<RFuture<Long>>() {
            @Override
            public RFuture<Long> get() {
                return topic.publishAsync(message);
            }
        });
    }

    @Override
    public Publisher<Integer> addListener(StatusListener listener) {
        return addListener(new PubSubStatusListener<Object>(listener, name));
    };

    @Override
    public Publisher<Integer> addListener(MessageListener<M> listener) {
        PubSubMessageListener<M> pubSubListener = new PubSubMessageListener<M>(listener, name);
        return addListener(pubSubListener);
    }

    private Publisher<Integer> addListener(final RedisPubSubListener<?> pubSubListener) {
        return commandExecutor.reactive(new Supplier<RFuture<Integer>>() {
            @Override
            public RFuture<Integer> get() {
                return ((RedissonTopic<Integer>) topic).addListenerAsync(pubSubListener);
            }
        });
    }


    @Override
    public void removeListener(int listenerId) {
        topic.removeListener(listenerId);
    }


}
