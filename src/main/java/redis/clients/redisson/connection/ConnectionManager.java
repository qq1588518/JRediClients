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
package redis.clients.redisson.connection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import redis.clients.redisson.api.NodeType;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.client.RedisClient;
import redis.clients.redisson.client.RedisConnection;
import redis.clients.redisson.client.RedisPubSubListener;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.client.protocol.RedisCommand;
import redis.clients.redisson.command.CommandSyncService;
import redis.clients.redisson.config.Config;
import redis.clients.redisson.config.MasterSlaveServersConfig;
import redis.clients.redisson.misc.InfinitySemaphoreLatch;
import redis.clients.redisson.misc.RPromise;
import redis.clients.redisson.pubsub.AsyncSemaphore;

import io.netty.channel.EventLoopGroup;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

/**
 *
 * @author Nikita Koksharov
 *
 */
public interface ConnectionManager {
    
    CommandSyncService getCommandExecutor();
    
    ExecutorService getExecutor();
    
    URI getLastClusterNode();
    
    Config getCfg();

    boolean isClusterMode();

    AsyncSemaphore getSemaphore(String channelName);
    
    <R> RFuture<R> newSucceededFuture(R value);

    ConnectionEventsHub getConnectionEventsHub();

    boolean isShutdown();

    boolean isShuttingDown();

    RFuture<PubSubConnectionEntry> subscribe(Codec codec, String channelName, RedisPubSubListener<?>... listeners);

    RFuture<PubSubConnectionEntry> subscribe(Codec codec, String channelName, AsyncSemaphore semaphore, RedisPubSubListener<?>... listeners);
    
    IdleConnectionWatcher getConnectionWatcher();

    <R> RFuture<R> newFailedFuture(Throwable cause);

    Collection<RedisClientEntry> getClients();

    void shutdownAsync(RedisClient client);

    int calcSlot(String key);

    MasterSlaveServersConfig getConfig();

    Codec getCodec();

    Collection<MasterSlaveEntry> getEntrySet();
    
    MasterSlaveEntry getEntry(int slot);
    
    <R> RPromise<R> newPromise();

    void releaseRead(NodeSource source, RedisConnection connection);

    void releaseWrite(NodeSource source, RedisConnection connection);

    RFuture<RedisConnection> connectionReadOp(NodeSource source, RedisCommand<?> command);

    RFuture<RedisConnection> connectionWriteOp(NodeSource source, RedisCommand<?> command);

    RedisClient createClient(NodeType type, URI address, int timeout, int commandTimeout);

    RedisClient createClient(NodeType type, URI address);

    MasterSlaveEntry getEntry(InetSocketAddress addr);

    PubSubConnectionEntry getPubSubEntry(String channelName);

    RFuture<PubSubConnectionEntry> psubscribe(String pattern, Codec codec, RedisPubSubListener<?>... listeners);
    
    RFuture<PubSubConnectionEntry> psubscribe(String pattern, Codec codec, AsyncSemaphore semaphore, RedisPubSubListener<?>... listeners);

    Codec unsubscribe(String channelName, AsyncSemaphore lock);
    
    RFuture<Codec> unsubscribe(String channelName, boolean temporaryDown);

    RFuture<Codec> punsubscribe(String channelName, boolean temporaryDown);

    Codec punsubscribe(String channelName, AsyncSemaphore lock);
    
    void shutdown();

    void shutdown(long quietPeriod, long timeout, TimeUnit unit);
    
    EventLoopGroup getGroup();

    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);

    InfinitySemaphoreLatch getShutdownLatch();
    
    RFuture<Boolean> getShutdownPromise();

}
