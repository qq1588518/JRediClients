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

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import redis.clients.redisson.api.ClusterNodesGroup;
import redis.clients.redisson.api.LocalCachedMapOptions;
import redis.clients.redisson.api.MapOptions;
import redis.clients.redisson.api.Node;
import redis.clients.redisson.api.NodesGroup;
import redis.clients.redisson.api.RAtomicDouble;
import redis.clients.redisson.api.RAtomicLong;
import redis.clients.redisson.api.RBatch;
import redis.clients.redisson.api.RBinaryStream;
import redis.clients.redisson.api.RBitSet;
import redis.clients.redisson.api.RBlockingDeque;
import redis.clients.redisson.api.RBlockingQueue;
import redis.clients.redisson.api.RBloomFilter;
import redis.clients.redisson.api.RBoundedBlockingQueue;
import redis.clients.redisson.api.RBucket;
import redis.clients.redisson.api.RBuckets;
import redis.clients.redisson.api.RCountDownLatch;
import redis.clients.redisson.api.RDelayedQueue;
import redis.clients.redisson.api.RDeque;
import redis.clients.redisson.api.RGeo;
import redis.clients.redisson.api.RHyperLogLog;
import redis.clients.redisson.api.RKeys;
import redis.clients.redisson.api.RLexSortedSet;
import redis.clients.redisson.api.RList;
import redis.clients.redisson.api.RListMultimap;
import redis.clients.redisson.api.RListMultimapCache;
import redis.clients.redisson.api.RLiveObjectService;
import redis.clients.redisson.api.RLocalCachedMap;
import redis.clients.redisson.api.RLock;
import redis.clients.redisson.api.RMap;
import redis.clients.redisson.api.RMapCache;
import redis.clients.redisson.api.RPatternTopic;
import redis.clients.redisson.api.RPermitExpirableSemaphore;
import redis.clients.redisson.api.RPriorityDeque;
import redis.clients.redisson.api.RPriorityQueue;
import redis.clients.redisson.api.RQueue;
import redis.clients.redisson.api.RReadWriteLock;
import redis.clients.redisson.api.RRemoteService;
import redis.clients.redisson.api.RScheduledExecutorService;
import redis.clients.redisson.api.RScoredSortedSet;
import redis.clients.redisson.api.RScript;
import redis.clients.redisson.api.RSemaphore;
import redis.clients.redisson.api.RSet;
import redis.clients.redisson.api.RSetCache;
import redis.clients.redisson.api.RSetMultimap;
import redis.clients.redisson.api.RSetMultimapCache;
import redis.clients.redisson.api.RSortedSet;
import redis.clients.redisson.api.RTopic;
import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.RedissonReactiveClient;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.codec.CodecProvider;
import redis.clients.redisson.command.CommandExecutor;
import redis.clients.redisson.config.Config;
import redis.clients.redisson.config.ConfigSupport;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.eviction.EvictionScheduler;
import redis.clients.redisson.liveobject.provider.ResolverProvider;
import redis.clients.redisson.misc.RedissonObjectFactory;
import redis.clients.redisson.pubsub.SemaphorePubSub;

import io.netty.util.internal.PlatformDependent;

/**
 * Main infrastructure class allows to get access
 * to all Redisson objects on top of Redis server.
 *
 * @author Nikita Koksharov
 *
 */
public class Redisson implements RedissonClient {

    static {
        RedissonObjectFactory.warmUp();
        RedissonReference.warmUp();
    }
    
    protected final QueueTransferService queueTransferService = new QueueTransferService();
    protected final EvictionScheduler evictionScheduler;
    protected final ConnectionManager connectionManager;
    
    protected final ConcurrentMap<Class<?>, Class<?>> liveObjectClassCache = PlatformDependent.newConcurrentHashMap();
    protected final CodecProvider codecProvider;
    protected final ResolverProvider resolverProvider;
    protected final Config config;
    protected final SemaphorePubSub semaphorePubSub = new SemaphorePubSub();

    protected final UUID id = UUID.randomUUID();

    protected Redisson(Config config) {
        this.config = config;
        Config configCopy = new Config(config);
        
        connectionManager = ConfigSupport.createConnectionManager(configCopy);
        evictionScheduler = new EvictionScheduler(connectionManager.getCommandExecutor());
        codecProvider = configCopy.getCodecProvider();
        resolverProvider = configCopy.getResolverProvider();
    }
    
    public EvictionScheduler getEvictionScheduler() {
        return evictionScheduler;
    }
    
    public CommandExecutor getCommandExecutor() {
        return connectionManager.getCommandExecutor();
    }
    
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Create sync/async Redisson instance with default config
     *
     * @return Redisson instance
     */
    public static RedissonClient create() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//        config.useMasterSlaveConnection().setMasterAddress("127.0.0.1:6379").addSlaveAddress("127.0.0.1:6389").addSlaveAddress("127.0.0.1:6399");
//        config.useSentinelConnection().setMasterName("mymaster").addSentinelAddress("127.0.0.1:26389", "127.0.0.1:26379");
//        config.useClusterServers().addNodeAddress("127.0.0.1:7000");
        return create(config);
    }

    /**
     * Create sync/async Redisson instance with provided config
     *
     * @param config for Redisson
     * @return Redisson instance
     */
    public static RedissonClient create(Config config) {
        Redisson redisson = new Redisson(config);
        if (config.isRedissonReferenceEnabled()) {
            redisson.enableRedissonReferenceSupport();
        }
        return redisson;
    }

    /**
     * Create reactive Redisson instance with default config
     *
     * @return Redisson instance
     */
    public static RedissonReactiveClient createReactive() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//        config.useMasterSlaveConnection().setMasterAddress("127.0.0.1:6379").addSlaveAddress("127.0.0.1:6389").addSlaveAddress("127.0.0.1:6399");
//        config.useSentinelConnection().setMasterName("mymaster").addSentinelAddress("127.0.0.1:26389", "127.0.0.1:26379");
//        config.useClusterServers().addNodeAddress("127.0.0.1:7000");
        return createReactive(config);
    }

    /**
     * Create reactive Redisson instance with provided config
     *
     * @param config for Redisson
     * @return Redisson instance
     */
    public static RedissonReactiveClient createReactive(Config config) {
        RedissonReactive react = new RedissonReactive(config);
        if (config.isRedissonReferenceEnabled()) {
            react.enableRedissonReferenceSupport();
        }
        return react;
    }
    
    @Override
    public RBinaryStream getBinaryStream(String name) {
        return new RedissonBinaryStream(connectionManager.getCommandExecutor(), name);
    }
    
    @Override
    public <V> RGeo<V> getGeo(String name) {
        return new RedissonGeo<V>(connectionManager.getCommandExecutor(), name, this);
    }
    
    @Override
    public <V> RGeo<V> getGeo(String name, Codec codec) {
        return new RedissonGeo<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBucket<V> getBucket(String name) {
        return new RedissonBucket<V>(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <V> RBucket<V> getBucket(String name, Codec codec) {
        return new RedissonBucket<V>(codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public RBuckets getBuckets() {
        return new RedissonBuckets(this, connectionManager.getCommandExecutor());
    }
    
    @Override
    public RBuckets getBuckets(Codec codec) {
        return new RedissonBuckets(this, codec, connectionManager.getCommandExecutor());
    }
    
    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name) {
        return new RedissonHyperLogLog<V>(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name, Codec codec) {
        return new RedissonHyperLogLog<V>(codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <V> RList<V> getList(String name) {
        return new RedissonList<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RList<V> getList(String name, Codec codec) {
        return new RedissonList<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name) {
        return new RedissonListMultimap<K, V>(id, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name, Codec codec) {
        return new RedissonListMultimap<K, V>(id, codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, LocalCachedMapOptions<K, V> options) {
        return new RedissonLocalCachedMap<K, V>(connectionManager.getCommandExecutor(), name, options, evictionScheduler, this);
    }

    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, Codec codec, LocalCachedMapOptions<K, V> options) {
        return new RedissonLocalCachedMap<K, V>(codec, connectionManager.getCommandExecutor(), name, options, evictionScheduler, this);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name) {
        return new RedissonMap<K, V>(connectionManager.getCommandExecutor(), name, this, null);
    }
    
    @Override
    public <K, V> RMap<K, V> getMap(String name, MapOptions<K, V> options) {
        return new RedissonMap<K, V>(connectionManager.getCommandExecutor(), name, this, options);
    }

    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name) {
        return new RedissonSetMultimap<K, V>(id, connectionManager.getCommandExecutor(), name);
    }
    
    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name) {
        return new RedissonSetMultimapCache<K, V>(id, evictionScheduler, connectionManager.getCommandExecutor(), name);
    }
    
    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name, Codec codec) {
        return new RedissonSetMultimapCache<K, V>(id, evictionScheduler, codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name) {
        return new RedissonListMultimapCache<K, V>(id, evictionScheduler, connectionManager.getCommandExecutor(), name);
    }
    
    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name, Codec codec) {
        return new RedissonListMultimapCache<K, V>(id, evictionScheduler, codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name, Codec codec) {
        return new RedissonSetMultimap<K, V>(id, codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <V> RSetCache<V> getSetCache(String name) {
        return new RedissonSetCache<V>(evictionScheduler, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RSetCache<V> getSetCache(String name, Codec codec) {
        return new RedissonSetCache<V>(codec, evictionScheduler, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name) {
        return new RedissonMapCache<K, V>(evictionScheduler, connectionManager.getCommandExecutor(), name, this, null);
    }

    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, MapOptions<K, V> options) {
        return new RedissonMapCache<K, V>(evictionScheduler, connectionManager.getCommandExecutor(), name, this, options);
    }
    
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec) {
        return new RedissonMapCache<K, V>(codec, evictionScheduler, connectionManager.getCommandExecutor(), name, this, null);
    }
    
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec, MapOptions<K, V> options) {
        return new RedissonMapCache<K, V>(codec, evictionScheduler, connectionManager.getCommandExecutor(), name, this, options);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec) {
        return new RedissonMap<K, V>(codec, connectionManager.getCommandExecutor(), name, this, null);
    }
    
    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec, MapOptions<K, V> options) {
        return new RedissonMap<K, V>(codec, connectionManager.getCommandExecutor(), name, this, options);
    }

    @Override
    public RLock getLock(String name) {
        return new RedissonLock(connectionManager.getCommandExecutor(), name, id);
    }

    @Override
    public RLock getFairLock(String name) {
        return new RedissonFairLock(connectionManager.getCommandExecutor(), name, id);
    }
    
    @Override
    public RReadWriteLock getReadWriteLock(String name) {
        return new RedissonReadWriteLock(connectionManager.getCommandExecutor(), name, id);
    }

    @Override
    public <V> RSet<V> getSet(String name) {
        return new RedissonSet<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RSet<V> getSet(String name, Codec codec) {
        return new RedissonSet<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public RScript getScript() {
        return new RedissonScript(connectionManager.getCommandExecutor());
    }

    @Override
    public RScheduledExecutorService getExecutorService(String name) {
        return new RedissonExecutorService(connectionManager.getCodec(), connectionManager.getCommandExecutor(), this, name, queueTransferService);
    }
    
    @Override
    @Deprecated
    public RScheduledExecutorService getExecutorService(Codec codec, String name) {
        return getExecutorService(name, codec);
    }

    @Override
    public RScheduledExecutorService getExecutorService(String name, Codec codec) {
        return new RedissonExecutorService(codec, connectionManager.getCommandExecutor(), this, name, queueTransferService);
    }
    
    @Override
    public RRemoteService getRemoteService() {
        return new RedissonRemoteService(this, connectionManager.getCommandExecutor());
    }

    @Override
    public RRemoteService getRemoteService(String name) {
        return new RedissonRemoteService(this, name, connectionManager.getCommandExecutor());
    }
    
    @Override
    public RRemoteService getRemoteService(Codec codec) {
        return new RedissonRemoteService(codec, this, connectionManager.getCommandExecutor());
    }
    
    @Override
    public RRemoteService getRemoteService(String name, Codec codec) {
        return new RedissonRemoteService(codec, this, name, connectionManager.getCommandExecutor());
    }

    @Override
    public <V> RSortedSet<V> getSortedSet(String name) {
        return new RedissonSortedSet<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RSortedSet<V> getSortedSet(String name, Codec codec) {
        return new RedissonSortedSet<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
        return new RedissonScoredSortedSet<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name, Codec codec) {
        return new RedissonScoredSortedSet<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public RLexSortedSet getLexSortedSet(String name) {
        return new RedissonLexSortedSet(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <M> RTopic<M> getTopic(String name) {
        return new RedissonTopic<M>(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <M> RTopic<M> getTopic(String name, Codec codec) {
        return new RedissonTopic<M>(codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <M> RPatternTopic<M> getPatternTopic(String pattern) {
        return new RedissonPatternTopic<M>(connectionManager.getCommandExecutor(), pattern);
    }

    @Override
    public <M> RPatternTopic<M> getPatternTopic(String pattern, Codec codec) {
        return new RedissonPatternTopic<M>(codec, connectionManager.getCommandExecutor(), pattern);
    }

    @Override
    public <V> RDelayedQueue<V> getDelayedQueue(RQueue<V> destinationQueue) {
        if (destinationQueue == null) {
            throw new NullPointerException();
        }
        return new RedissonDelayedQueue<V>(queueTransferService, destinationQueue.getCodec(), connectionManager.getCommandExecutor(), destinationQueue.getName());
    }
    
    @Override
    public <V> RQueue<V> getQueue(String name) {
        return new RedissonQueue<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RQueue<V> getQueue(String name, Codec codec) {
        return new RedissonQueue<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name) {
        return new RedissonBlockingQueue<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name, Codec codec) {
        return new RedissonBlockingQueue<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }
    
    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name) {
        return new RedissonBoundedBlockingQueue<V>(semaphorePubSub, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name, Codec codec) {
        return new RedissonBoundedBlockingQueue<V>(semaphorePubSub, codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RDeque<V> getDeque(String name) {
        return new RedissonDeque<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RDeque<V> getDeque(String name, Codec codec) {
        return new RedissonDeque<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name) {
        return new RedissonBlockingDeque<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name, Codec codec) {
        return new RedissonBlockingDeque<V>(codec, connectionManager.getCommandExecutor(), name, this);
    };

    @Override
    public RAtomicLong getAtomicLong(String name) {
        return new RedissonAtomicLong(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public RAtomicDouble getAtomicDouble(String name) {
        return new RedissonAtomicDouble(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public RCountDownLatch getCountDownLatch(String name) {
        return new RedissonCountDownLatch(connectionManager.getCommandExecutor(), name, id);
    }

    @Override
    public RBitSet getBitSet(String name) {
        return new RedissonBitSet(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public RSemaphore getSemaphore(String name) {
        return new RedissonSemaphore(connectionManager.getCommandExecutor(), name, semaphorePubSub);
    }
    
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String name) {
        return new RedissonPermitExpirableSemaphore(connectionManager.getCommandExecutor(), name, semaphorePubSub);
    }


    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name) {
        return new RedissonBloomFilter<V>(connectionManager.getCommandExecutor(), name);
    }

    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name, Codec codec) {
        return new RedissonBloomFilter<V>(codec, connectionManager.getCommandExecutor(), name);
    }

    @Override
    public RKeys getKeys() {
        return new RedissonKeys(connectionManager.getCommandExecutor());
    }

    @Override
    public RBatch createBatch() {
        RedissonBatch batch = new RedissonBatch(id, evictionScheduler, connectionManager);
        if (config.isRedissonReferenceEnabled()) {
            batch.enableRedissonReferenceSupport(this);
        }
        return batch;
    }

    @Override
    public RLiveObjectService getLiveObjectService() {
        return new RedissonLiveObjectService(this, liveObjectClassCache, codecProvider, resolverProvider);
    }
    
    @Override
    public void shutdown() {
        connectionManager.shutdown();
    }
    
    
    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit unit) {
        connectionManager.shutdown(quietPeriod, timeout, unit);
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
    public ResolverProvider getResolverProvider() {
        return resolverProvider;
    }

    @Override
    public NodesGroup<Node> getNodesGroup() {
        return new RedisNodes<Node>(connectionManager);
    }

    @Override
    public ClusterNodesGroup getClusterNodesGroup() {
        if (!connectionManager.isClusterMode()) {
            throw new IllegalStateException("Redisson is not in cluster mode!");
        }
        return new RedisClusterNodes(connectionManager);
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
        this.connectionManager.getCommandExecutor().enableRedissonReferenceSupport(this);
    }

    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name) {
        return new RedissonPriorityQueue<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name, Codec codec) {
        return new RedissonPriorityQueue<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }
    
    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name) {
        return new RedissonPriorityDeque<V>(connectionManager.getCommandExecutor(), name, this);
    }

    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name, Codec codec) {
        return new RedissonPriorityDeque<V>(codec, connectionManager.getCommandExecutor(), name, this);
    }
    

}

