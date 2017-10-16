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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import redis.clients.redisson.api.BatchResult;
import redis.clients.redisson.api.RAtomicDoubleAsync;
import redis.clients.redisson.api.RAtomicLongAsync;
import redis.clients.redisson.api.RBatch;
import redis.clients.redisson.api.RBitSetAsync;
import redis.clients.redisson.api.RBlockingDequeAsync;
import redis.clients.redisson.api.RBlockingQueueAsync;
import redis.clients.redisson.api.RBucketAsync;
import redis.clients.redisson.api.RDequeAsync;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.api.RGeoAsync;
import redis.clients.redisson.api.RHyperLogLogAsync;
import redis.clients.redisson.api.RKeysAsync;
import redis.clients.redisson.api.RLexSortedSetAsync;
import redis.clients.redisson.api.RListAsync;
import redis.clients.redisson.api.RMapAsync;
import redis.clients.redisson.api.RMapCacheAsync;
import redis.clients.redisson.api.RMultimapAsync;
import redis.clients.redisson.api.RMultimapCacheAsync;
import redis.clients.redisson.api.RQueueAsync;
import redis.clients.redisson.api.RScoredSortedSetAsync;
import redis.clients.redisson.api.RScriptAsync;
import redis.clients.redisson.api.RSetAsync;
import redis.clients.redisson.api.RSetCacheAsync;
import redis.clients.redisson.api.RTopicAsync;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.command.CommandBatchService;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.eviction.EvictionScheduler;

/**
 *
 *
 * @author Nikita Koksharov
 *
 */
public class RedissonBatch implements RBatch {

    private final EvictionScheduler evictionScheduler;
    private final CommandBatchService executorService;
    private final UUID id;

    private long timeout;
    private int retryAttempts;
    private long retryInterval;

    private int syncSlaves;
    private long syncTimeout;
    private boolean skipResult;

    protected RedissonBatch(UUID id, EvictionScheduler evictionScheduler, ConnectionManager connectionManager) {
        this.executorService = new CommandBatchService(connectionManager);
        this.evictionScheduler = evictionScheduler;
        this.id = id;
    }

    @Override
    public <V> RBucketAsync<V> getBucket(String name) {
        return new RedissonBucket<V>(executorService, name);
    }

    @Override
    public <V> RBucketAsync<V> getBucket(String name, Codec codec) {
        return new RedissonBucket<V>(codec, executorService, name);
    }

    @Override
    public <V> RHyperLogLogAsync<V> getHyperLogLog(String name) {
        return new RedissonHyperLogLog<V>(executorService, name);
    }

    @Override
    public <V> RHyperLogLogAsync<V> getHyperLogLog(String name, Codec codec) {
        return new RedissonHyperLogLog<V>(codec, executorService, name);
    }

    @Override
    public <V> RListAsync<V> getList(String name) {
        return new RedissonList<V>(executorService, name, null);
    }

    @Override
    public <V> RListAsync<V> getList(String name, Codec codec) {
        return new RedissonList<V>(codec, executorService, name, null);
    }

    @Override
    public <K, V> RMapAsync<K, V> getMap(String name) {
        return new RedissonMap<K, V>(executorService, name, null, null);
    }

    @Override
    public <K, V> RMapAsync<K, V> getMap(String name, Codec codec) {
        return new RedissonMap<K, V>(codec, executorService, name, null, null);
    }

    @Override
    public <V> RSetAsync<V> getSet(String name) {
        return new RedissonSet<V>(executorService, name, null);
    }

    @Override
    public <V> RSetAsync<V> getSet(String name, Codec codec) {
        return new RedissonSet<V>(codec, executorService, name, null);
    }

    @Override
    public <M> RTopicAsync<M> getTopic(String name) {
        return new RedissonTopic<M>(executorService, name);
    }

    @Override
    public <M> RTopicAsync<M> getTopic(String name, Codec codec) {
        return new RedissonTopic<M>(codec, executorService, name);
    }

    @Override
    public <V> RQueueAsync<V> getQueue(String name) {
        return new RedissonQueue<V>(executorService, name, null);
    }

    @Override
    public <V> RQueueAsync<V> getQueue(String name, Codec codec) {
        return new RedissonQueue<V>(codec, executorService, name, null);
    }

    @Override
    public <V> RBlockingQueueAsync<V> getBlockingQueue(String name) {
        return new RedissonBlockingQueue<V>(executorService, name, null);
    }

    @Override
    public <V> RBlockingQueueAsync<V> getBlockingQueue(String name, Codec codec) {
        return new RedissonBlockingQueue<V>(codec, executorService, name, null);
    }

    @Override
    public <V> RBlockingDequeAsync<V> getBlockingDeque(String name) {
        return new RedissonBlockingDeque<V>(executorService, name, null);
    }

    @Override
    public <V> RBlockingDequeAsync<V> getBlockingDeque(String name, Codec codec) {
        return new RedissonBlockingDeque<V>(codec, executorService, name, null);
    }

    @Override
    public <V> RDequeAsync<V> getDeque(String name) {
        return new RedissonDeque<V>(executorService, name, null);
    }

    @Override
    public <V> RDequeAsync<V> getDeque(String name, Codec codec) {
        return new RedissonDeque<V>(codec, executorService, name, null);
    }

    @Override
    public RAtomicLongAsync getAtomicLong(String name) {
        return new RedissonAtomicLong(executorService, name);
    }

    @Override
    public RAtomicDoubleAsync getAtomicDouble(String name) {
        return new RedissonAtomicDouble(executorService, name);
    }

    @Override
    public <V> RScoredSortedSetAsync<V> getScoredSortedSet(String name) {
        return new RedissonScoredSortedSet<V>(executorService, name, null);
    }

    @Override
    public <V> RScoredSortedSetAsync<V> getScoredSortedSet(String name, Codec codec) {
        return new RedissonScoredSortedSet<V>(codec, executorService, name, null);
    }

    @Override
    public RLexSortedSetAsync getLexSortedSet(String name) {
        return new RedissonLexSortedSet(executorService, name, null);
    }

    @Override
    public RBitSetAsync getBitSet(String name) {
        return new RedissonBitSet(executorService, name);
    }

    @Override
    public <K, V> RMapCacheAsync<K, V> getMapCache(String name, Codec codec) {
        return new RedissonMapCache<K, V>(codec, evictionScheduler, executorService, name, null, null);
    }

    @Override
    public <K, V> RMapCacheAsync<K, V> getMapCache(String name) {
        return new RedissonMapCache<K, V>(evictionScheduler, executorService, name, null, null);
    }

    @Override
    public RScriptAsync getScript() {
        return new RedissonScript(executorService);
    }

    @Override
    public RKeysAsync getKeys() {
        return new RedissonKeys(executorService);
    }

    @Override
    public <V> RSetCacheAsync<V> getSetCache(String name) {
        return new RedissonSetCache<V>(evictionScheduler, executorService, name, null);
    }

    @Override
    public <V> RSetCacheAsync<V> getSetCache(String name, Codec codec) {
        return new RedissonSetCache<V>(codec, evictionScheduler, executorService, name, null);
    }

    @Override
    public RBatch syncSlaves(int slaves, long timeout, TimeUnit unit) {
        this.syncSlaves = slaves;
        this.syncTimeout = unit.toMillis(timeout);
        return this;
    }
    
    @Override
    public RBatch skipResult() {
        this.skipResult = true;
        return this;
    }
    
    @Override
    public RBatch retryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
        return this;
    }
    
    @Override
    public RBatch retryInterval(long retryInterval, TimeUnit unit) {
        this.retryInterval = unit.toMillis(retryInterval);
        return this;
    }
    
    @Override
    public RBatch timeout(long timeout, TimeUnit unit) {
        this.timeout = unit.toMillis(timeout);
        return this;
    }
    
    @Override
    public BatchResult<?> execute() {
        return executorService.execute(syncSlaves, syncTimeout, skipResult, timeout, retryAttempts, retryInterval);
    }

    @Override
    public void executeSkipResult() {
        executorService.execute(syncSlaves, syncTimeout, true, timeout, retryAttempts, retryInterval);
    }
    
    @Override
    public RFuture<Void> executeSkipResultAsync() {
        return executorService.executeAsync(syncSlaves, syncTimeout, true, timeout, retryAttempts, retryInterval);
    }
    
    @Override
    public RFuture<BatchResult<?>> executeAsync() {
        return executorService.executeAsync(syncSlaves, syncTimeout, skipResult, timeout, retryAttempts, retryInterval);
    }
    
    @Override
    public <K, V> RMultimapAsync<K, V> getSetMultimap(String name) {
        return new RedissonSetMultimap<K, V>(id, executorService, name);
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getSetMultimap(String name, Codec codec) {
        return new RedissonSetMultimap<K, V>(id, codec, executorService, name);
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimap(String name) {
        return new RedissonListMultimap<K, V>(id, executorService, name);
    }

    @Override
    public <K, V> RMultimapAsync<K, V> getListMultimap(String name, Codec codec) {
        return new RedissonListMultimap<K, V>(id, codec, executorService, name);
    }

    @Override
    public <V> RGeoAsync<V> getGeo(String name) {
        return new RedissonGeo<V>(executorService, name, null);
    }
    
    @Override
    public <V> RGeoAsync<V> getGeo(String name, Codec codec) {
        return new RedissonGeo<V>(codec, executorService, name, null);
    }
    
    @Override
    public <K, V> RMultimapCacheAsync<K, V> getSetMultimapCache(String name) {
        return new RedissonSetMultimapCache<K, V>(id, evictionScheduler, executorService, name);
    }
    
    @Override
    public <K, V> RMultimapCacheAsync<K, V> getSetMultimapCache(String name, Codec codec) {
        return new RedissonSetMultimapCache<K, V>(id, evictionScheduler, codec, executorService, name);
    }

    @Override
    public <K, V> RMultimapCacheAsync<K, V> getListMultimapCache(String name) {
        return new RedissonListMultimapCache<K, V>(id, evictionScheduler, executorService, name);
    }
    
    @Override
    public <K, V> RMultimapCacheAsync<K, V> getListMultimapCache(String name, Codec codec) {
        return new RedissonListMultimapCache<K, V>(id, evictionScheduler, codec, executorService, name);
    }

    protected void enableRedissonReferenceSupport(Redisson redisson) {
        this.executorService.enableRedissonReferenceSupport(redisson);
    }

}
