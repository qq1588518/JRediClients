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
package redis.clients.redisson.pubsub;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import redis.clients.redisson.PubSubEntry;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.client.BaseRedisPubSubListener;
import redis.clients.redisson.client.RedisPubSubListener;
import redis.clients.redisson.client.codec.LongCodec;
import redis.clients.redisson.client.protocol.pubsub.PubSubType;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.misc.RPromise;
import redis.clients.redisson.misc.RedissonPromise;
import redis.clients.redisson.misc.TransferListener;

import io.netty.util.internal.PlatformDependent;

/**
 * 
 * @author Nikita Koksharov
 *
 */
abstract class PublishSubscribe<E extends PubSubEntry<E>> {

    private final ConcurrentMap<String, E> entries = PlatformDependent.newConcurrentHashMap();

    public void unsubscribe(final E entry, final String entryName, final String channelName, final ConnectionManager connectionManager) {
        final AsyncSemaphore semaphore = connectionManager.getSemaphore(channelName);
        semaphore.acquire(new Runnable() {
            @Override
            public void run() {
                if (entry.release() == 0) {
                    // just an assertion
                    boolean removed = entries.remove(entryName) == entry;
                    if (!removed) {
                        throw new IllegalStateException();
                    }
                    connectionManager.unsubscribe(channelName, semaphore);
                } else {
                    semaphore.release();
                }
            }
        });

    }

    public E getEntry(String entryName) {
        return entries.get(entryName);
    }

    public RFuture<E> subscribe(final String entryName, final String channelName, final ConnectionManager connectionManager) {
        final AtomicReference<Runnable> listenerHolder = new AtomicReference<Runnable>();
        final AsyncSemaphore semaphore = connectionManager.getSemaphore(channelName);
        final RPromise<E> newPromise = new RedissonPromise<E>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return semaphore.remove(listenerHolder.get());
            }
        };

        Runnable listener = new Runnable() {

            @Override
            public void run() {
                E entry = entries.get(entryName);
                if (entry != null) {
                    entry.aquire();
                    semaphore.release();
                    entry.getPromise().addListener(new TransferListener<E>(newPromise));
                    return;
                }
                
                E value = createEntry(newPromise);
                value.aquire();
                
                E oldValue = entries.putIfAbsent(entryName, value);
                if (oldValue != null) {
                    oldValue.aquire();
                    semaphore.release();
                    oldValue.getPromise().addListener(new TransferListener<E>(newPromise));
                    return;
                }
                
                RedisPubSubListener<Object> listener = createListener(channelName, value);
                connectionManager.subscribe(LongCodec.INSTANCE, channelName, semaphore, listener);
            }
        };
        semaphore.acquire(listener);
        listenerHolder.set(listener);
        
        return newPromise;
    }

    protected abstract E createEntry(RPromise<E> newPromise);

    protected abstract void onMessage(E value, Long message);

    private RedisPubSubListener<Object> createListener(final String channelName, final E value) {
        RedisPubSubListener<Object> listener = new BaseRedisPubSubListener() {

            @Override
            public void onMessage(String channel, Object message) {
                if (!channelName.equals(channel)) {
                    return;
                }

                PublishSubscribe.this.onMessage(value, (Long)message);
            }

            @Override
            public boolean onStatus(PubSubType type, String channel) {
                if (!channelName.equals(channel)) {
                    return false;
                }

                if (type == PubSubType.SUBSCRIBE) {
                    value.getPromise().trySuccess(value);
                    return true;
                }
                return false;
            }

        };
        return listener;
    }

}
