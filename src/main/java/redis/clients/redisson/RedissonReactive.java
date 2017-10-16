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
package redis.clients.redisson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import redis.clients.redisson.api.ClusterNode;
import redis.clients.redisson.api.MapOptions;
import redis.clients.redisson.api.Node;
import redis.clients.redisson.api.NodesGroup;
import redis.clients.redisson.api.RAtomicLongReactive;
import redis.clients.redisson.api.RBatchReactive;
import redis.clients.redisson.api.RBitSetReactive;
import redis.clients.redisson.api.RBlockingQueueReactive;
import redis.clients.redisson.api.RBucketReactive;
import redis.clients.redisson.api.RDequeReactive;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.api.RHyperLogLogReactive;
import redis.clients.redisson.api.RKeysReactive;
import redis.clients.redisson.api.RLexSortedSetReactive;
import redis.clients.redisson.api.RListReactive;
import redis.clients.redisson.api.RLockReactive;
import redis.clients.redisson.api.RMapCacheReactive;
import redis.clients.redisson.api.RMapReactive;
import redis.clients.redisson.api.RPatternTopicReactive;
import redis.clients.redisson.api.RQueueReactive;
import redis.clients.redisson.api.RReadWriteLockReactive;
import redis.clients.redisson.api.RScoredSortedSetReactive;
import redis.clients.redisson.api.RScriptReactive;
import redis.clients.redisson.api.RSemaphoreReactive;
import redis.clients.redisson.api.RSetCacheReactive;
import redis.clients.redisson.api.RSetReactive;
import redis.clients.redisson.api.RTopicReactive;
import redis.clients.redisson.api.RedissonReactiveClient;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.client.protocol.RedisCommands;
import redis.clients.redisson.codec.CodecProvider;
import redis.clients.redisson.command.CommandReactiveService;
import redis.clients.redisson.config.Config;
import redis.clients.redisson.config.ConfigSupport;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.eviction.EvictionScheduler;
import redis.clients.redisson.pubsub.SemaphorePubSub;
import redis.clients.redisson.reactive.RedissonAtomicLongReactive;
import redis.clients.redisson.reactive.RedissonBatchReactive;
import redis.clients.redisson.reactive.RedissonBitSetReactive;
import redis.clients.redisson.reactive.RedissonBlockingQueueReactive;
import redis.clients.redisson.reactive.RedissonBucketReactive;
import redis.clients.redisson.reactive.RedissonDequeReactive;
import redis.clients.redisson.reactive.RedissonHyperLogLogReactive;
import redis.clients.redisson.reactive.RedissonKeysReactive;
import redis.clients.redisson.reactive.RedissonLexSortedSetReactive;
import redis.clients.redisson.reactive.RedissonListReactive;
import redis.clients.redisson.reactive.RedissonLockReactive;
import redis.clients.redisson.reactive.RedissonMapCacheReactive;
import redis.clients.redisson.reactive.RedissonMapReactive;
import redis.clients.redisson.reactive.RedissonPatternTopicReactive;
import redis.clients.redisson.reactive.RedissonQueueReactive;
import redis.clients.redisson.reactive.RedissonReadWriteLockReactive;
import redis.clients.redisson.reactive.RedissonScoredSortedSetReactive;
import redis.clients.redisson.reactive.RedissonScriptReactive;
import redis.clients.redisson.reactive.RedissonSemaphoreReactive;
import redis.clients.redisson.reactive.RedissonSetCacheReactive;
import redis.clients.redisson.reactive.RedissonSetReactive;
import redis.clients.redisson.reactive.RedissonTopicReactive;

/**
 * Main infrastructure class allows to get access
 * to all Redisson objects on top of Redis server.
 *
 * @author Nikita Koksharov
 *
 */
public class RedissonReactive implements RedissonReactiveClient {

    protected final EvictionScheduler evictionScheduler;
    protected final CommandReactiveService commandExecutor;
    protected final ConnectionManager connectionManager;
    protected final Config config;
    protected final CodecProvider codecProvider;
    
    protected final UUID id = UUID.randomUUID();
    protected final SemaphorePubSub semaphorePubSub = new SemaphorePubSub();
    
    protected RedissonReactive(Config config) {
        this.config = config;
        Config configCopy = new Config(config);
        
        connectionManager = ConfigSupport.createConnectionManager(configCopy);
        commandExecutor = new CommandReactiveService(connectionManager);
        evictionScheduler = new EvictionScheduler(commandExecutor);
        codecProvider = config.getCodecProvider();
    }

    @Override
    public RSemaphoreReactive getSemaphore(String name) {
        return new RedissonSemaphoreReactive(commandExecutor, name, semaphorePubSub);
    }
    
    @Override
    public RReadWriteLockReactive getReadWriteLock(String name) {
        return new RedissonReadWriteLockReactive(commandExecutor, name, id);
    }
    
    @Override
    public RLockReactive getLock(String name) {
        return new RedissonLockReactive(commandExecutor, name, id);
    }

    @Override
    public <K, V> RMapCacheReactive<K, V> getMapCache(String name, Codec codec) {
        return new RedissonMapCacheReactive<K, V>(evictionScheduler, codec, commandExecutor, name, null);
    }

    @Override
    public <K, V> RMapCacheReactive<K, V> getMapCache(String name) {
        return new RedissonMapCacheReactive<K, V>(evictionScheduler, commandExecutor, name, null);
    }

    @Override
    public <V> RBucketReactive<V> getBucket(String name) {
        return new RedissonBucketReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RBucketReactive<V> getBucket(String name, Codec codec) {
        return new RedissonBucketReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> List<RBucketReactive<V>> findBuckets(String pattern) {
        RFuture<Collection<String>> r = commandExecutor.readAllAsync(RedisCommands.KEYS, pattern);
        Collection<String> keys = commandExecutor.get(r);

        List<RBucketReactive<V>> buckets = new ArrayList<RBucketReactive<V>>(keys.size());
        for (Object key : keys) {
            if(key != null) {
                buckets.add(this.<V>getBucket(key.toString()));
            }
        }
        return buckets;
    }

    
    
    @Override
    public <V> RHyperLogLogReactive<V> getHyperLogLog(String name) {
        return new RedissonHyperLogLogReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RHyperLogLogReactive<V> getHyperLogLog(String name, Codec codec) {
        return new RedissonHyperLogLogReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> RListReactive<V> getList(String name) {
        return new RedissonListReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RListReactive<V> getList(String name, Codec codec) {
        return new RedissonListReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <K, V> RMapReactive<K, V> getMap(String name) {
        return new RedissonMapReactive<K, V>(commandExecutor, name, null);
    }

    @Override
    public <K, V> RMapReactive<K, V> getMap(String name, Codec codec) {
        return new RedissonMapReactive<K, V>(codec, commandExecutor, name, null);
    }

    @Override
    public <V> RSetReactive<V> getSet(String name) {
        return new RedissonSetReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RSetReactive<V> getSet(String name, Codec codec) {
        return new RedissonSetReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> RScoredSortedSetReactive<V> getScoredSortedSet(String name) {
        return new RedissonScoredSortedSetReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RScoredSortedSetReactive<V> getScoredSortedSet(String name, Codec codec) {
        return new RedissonScoredSortedSetReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public RLexSortedSetReactive getLexSortedSet(String name) {
        return new RedissonLexSortedSetReactive(commandExecutor, name);
    }

    @Override
    public <M> RTopicReactive<M> getTopic(String name) {
        return new RedissonTopicReactive<M>(commandExecutor, name);
    }

    @Override
    public <M> RTopicReactive<M> getTopic(String name, Codec codec) {
        return new RedissonTopicReactive<M>(codec, commandExecutor, name);
    }

    @Override
    public <M> RPatternTopicReactive<M> getPatternTopic(String pattern) {
        return new RedissonPatternTopicReactive<M>(commandExecutor, pattern);
    }

    @Override
    public <M> RPatternTopicReactive<M> getPatternTopic(String pattern, Codec codec) {
        return new RedissonPatternTopicReactive<M>(codec, commandExecutor, pattern);
    }

    @Override
    public <V> RQueueReactive<V> getQueue(String name) {
        return new RedissonQueueReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RQueueReactive<V> getQueue(String name, Codec codec) {
        return new RedissonQueueReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> RBlockingQueueReactive<V> getBlockingQueue(String name) {
        return new RedissonBlockingQueueReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RBlockingQueueReactive<V> getBlockingQueue(String name, Codec codec) {
        return new RedissonBlockingQueueReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> RDequeReactive<V> getDeque(String name) {
        return new RedissonDequeReactive<V>(commandExecutor, name);
    }

    @Override
    public <V> RDequeReactive<V> getDeque(String name, Codec codec) {
        return new RedissonDequeReactive<V>(codec, commandExecutor, name);
    }

    @Override
    public <V> RSetCacheReactive<V> getSetCache(String name) {
        return new RedissonSetCacheReactive<V>(evictionScheduler, commandExecutor, name);
    }

    @Override
    public <V> RSetCacheReactive<V> getSetCache(String name, Codec codec) {
        return new RedissonSetCacheReactive<V>(codec, evictionScheduler, commandExecutor, name);
    }

    @Override
    public RAtomicLongReactive getAtomicLong(String name) {
        return new RedissonAtomicLongReactive(commandExecutor, name);
    }

    @Override
    public RBitSetReactive getBitSet(String name) {
        return new RedissonBitSetReactive(commandExecutor, name);
    }

    @Override
    public RScriptReactive getScript() {
        return new RedissonScriptReactive(commandExecutor);
    }

    @Override
    public RBatchReactive createBatch() {
        RedissonBatchReactive batch = new RedissonBatchReactive(evictionScheduler, connectionManager);
        if (config.isRedissonReferenceEnabled()) {
            batch.enableRedissonReferenceSupport(this);
        }
        return batch;
    }

    @Override
    public RKeysReactive getKeys() {
        return new RedissonKeysReactive(commandExecutor);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public CodecProvider getCodecProvider() {
        return codecProvider;
    }
    
    @Override
    public NodesGroup<Node> getNodesGroup() {
        return new RedisNodes<Node>(connectionManager);
    }

    @Override
    public NodesGroup<ClusterNode> getClusterNodesGroup() {
        if (!connectionManager.isClusterMode()) {
            throw new IllegalStateException("Redisson not in cluster mode!");
        }
        return new RedisNodes<ClusterNode>(connectionManager);
    }

    @Override
    public void shutdown() {
        connectionManager.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return connectionManager.isShutdown();
    }

    @Override
    public boolean isShuttingDown() {
        return connectionManager.isShuttingDown();
    }

    protected void enableRedissonReferenceSupport() {
        this.commandExecutor.enableRedissonReferenceSupport(this);
    }

    @Override
    public <K, V> RMapCacheReactive<K, V> getMapCache(String name, Codec codec, MapOptions<K, V> options) {
        return new RedissonMapCacheReactive<K, V>(evictionScheduler, codec, commandExecutor, name, options);
    }


    @Override
    public <K, V> RMapCacheReactive<K, V> getMapCache(String name, MapOptions<K, V> options) {
        return new RedissonMapCacheReactive<K, V>(evictionScheduler, commandExecutor, name, options);
    }


    @Override
    public <K, V> RMapReactive<K, V> getMap(String name, MapOptions<K, V> options) {
        return new RedissonMapReactive<K, V>(commandExecutor, name, options);
    }


    @Override
    public <K, V> RMapReactive<K, V> getMap(String name, Codec codec, MapOptions<K, V> options) {
        return new RedissonMapReactive<K, V>(codec, commandExecutor, name, options);
    }
}

