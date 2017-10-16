package redis.clients.redisson.spring.support;

import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import redis.clients.redisson.BaseTest;
import redis.clients.redisson.RedisRunner;
import redis.clients.redisson.RedissonFairLock;
import redis.clients.redisson.RedissonLiveObjectServiceTest.TestREntity;
import redis.clients.redisson.RedissonMultiLock;
import redis.clients.redisson.RedissonReadLock;
import redis.clients.redisson.RedissonRedLock;
import redis.clients.redisson.RedissonRuntimeEnvironment;
import redis.clients.redisson.RedissonWriteLock;
import redis.clients.redisson.api.LocalCachedMapOptions;
import redis.clients.redisson.api.RAtomicDouble;
import redis.clients.redisson.api.RAtomicLong;
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
import redis.clients.redisson.api.RExecutorService;
import redis.clients.redisson.api.RGeo;
import redis.clients.redisson.api.RHyperLogLog;
import redis.clients.redisson.api.RKeys;
import redis.clients.redisson.api.RLexSortedSet;
import redis.clients.redisson.api.RList;
import redis.clients.redisson.api.RListMultimap;
import redis.clients.redisson.api.RLiveObject;
import redis.clients.redisson.api.RLiveObjectService;
import redis.clients.redisson.api.RLocalCachedMap;
import redis.clients.redisson.api.RLock;
import redis.clients.redisson.api.RMap;
import redis.clients.redisson.api.RMapCache;
import redis.clients.redisson.api.RObject;
import redis.clients.redisson.api.RPatternTopic;
import redis.clients.redisson.api.RPermitExpirableSemaphore;
import redis.clients.redisson.api.RPriorityDeque;
import redis.clients.redisson.api.RPriorityQueue;
import redis.clients.redisson.api.RQueue;
import redis.clients.redisson.api.RReadWriteLock;
import redis.clients.redisson.api.RRemoteService;
import redis.clients.redisson.api.RScoredSortedSet;
import redis.clients.redisson.api.RScript;
import redis.clients.redisson.api.RSemaphore;
import redis.clients.redisson.api.RSet;
import redis.clients.redisson.api.RSetCache;
import redis.clients.redisson.api.RSetMultimap;
import redis.clients.redisson.api.RSetMultimapCache;
import redis.clients.redisson.api.RSortedSet;
import redis.clients.redisson.api.RTopic;
import redis.clients.redisson.api.RemoteInvocationOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author Rui Gu (https://github.com/jackygurui)
 */
@RunWith(Parameterized.class)
public class SpringNamespaceObjectTest extends BaseTest {
    
    private static ApplicationContext context;
    
    @BeforeClass
    public static void setupClass() throws Exception {
        if (!RedissonRuntimeEnvironment.isTravis) {
            startContext();
        }
    }

    @AfterClass
    public static void shutDownClass() {
        if (!RedissonRuntimeEnvironment.isTravis) {
            stopContext();
        }
    }
    
    @Before
    public void setup() throws Exception {
        if (RedissonRuntimeEnvironment.isTravis) {
            startContext();
        }
    }

    @After
    public void shutDown() {
        if (RedissonRuntimeEnvironment.isTravis) {
            stopContext();
        }
    }

    @Override
    protected boolean flushBetweenTests() {
        return false;
    }
    
    public static void startContext() {
        TestREntity entity = new TestREntity("live-object");
        entity.setValue("1");
        defaultRedisson.getLiveObjectService().merge(entity);
        entity = new TestREntity("live-object-ext");
        entity.setValue("1");
        defaultRedisson.getLiveObjectService().merge(entity);
        
        System.setProperty("redisAddress", RedisRunner.getDefaultRedisServerBindAddressAndPort());
        context = new ClassPathXmlApplicationContext("classpath:org/redisson/spring/support/redisson_objects.xml");
    }
    
    public static void stopContext() {
        ((ConfigurableApplicationContext) context).close();
    }
    
    @Parameters(name = "{index}: key=[{0}], class=[{1}], parent=[{2}]")
    public static Collection<Object[]> tests() {
        return Arrays.asList(new Object[][]{
            {"binary-stream", RBinaryStream.class, null},
            {"geo", RGeo.class, null},
            {"set-cache", RSetCache.class, null},
            {"map-cache", RMapCache.class, null},
            {"bucket", RBucket.class, null},
            {"buckets", RBuckets.class, null},
            {"hyper-log-log", RHyperLogLog.class, null},
            {"list", RList.class, null},
            {"list-multimap", RListMultimap.class, null},
            {"local-cached-map", RLocalCachedMap.class, null},
            {"local-options", LocalCachedMapOptions.class, null},
            {"map", RMap.class, null},
            {"set-multimap", RSetMultimap.class, null},
            {"set-multimap-cache", RSetMultimapCache.class, null},
            {"semaphore", RSemaphore.class, null},
            {"permit-expirable-semaphore", RPermitExpirableSemaphore.class, null},
            {"lock", RLock.class, null},
            {"fair-lock", RedissonFairLock.class, null},
            {"read-write-lock", RReadWriteLock.class, null},
            {"read-lock", RedissonReadLock.class, "read-write-lock"},
            {"write-lock", RedissonWriteLock.class, "read-write-lock"},
            {"multi-lock", RedissonMultiLock.class, null},
            {"lock-1", RLock.class, null},
            {"fair-lock-1", RedissonFairLock.class, null},
            {"read-lock-1", RedissonReadLock.class, "read-write-lock"},
            {"write-lock-1", RedissonWriteLock.class, "read-write-lock"},
            {"red-lock", RedissonRedLock.class, null},
            {"lock-2", RLock.class, null},
            {"fair-lock-2", RedissonFairLock.class, null},
            {"read-lock-2", RedissonReadLock.class, "read-write-lock"},
            {"write-lock-2", RedissonWriteLock.class, "read-write-lock"},
            {"set", RSet.class, null},
            {"sorted-set", RSortedSet.class, null},
            {"scored-sorted-set", RScoredSortedSet.class, null},
            {"lex-sorted-set", RLexSortedSet.class, null},
            {"topic", RTopic.class, null},
            {"pattern-topic", RPatternTopic.class, null},
            {"queue", RQueue.class, null},
            {"delayed-queue", RDelayedQueue.class, "queue"},
            {"priority-queue", RPriorityQueue.class, null},
            {"priority-deque", RPriorityDeque.class, null},
            {"blocking-queue", RBlockingQueue.class, null},
            {"bounded-blocking-queue", RBoundedBlockingQueue.class, null},
            {"deque", RDeque.class, null},
            {"blocking-deque", RBlockingDeque.class, null},
            {"atomic-long", RAtomicLong.class, null},
            {"atomic-double", RAtomicDouble.class, null},
            {"count-down-latch", RCountDownLatch.class, null},
            {"bit-set", RBitSet.class, null},
            {"bloom-filter", RBloomFilter.class, null},
            {"script", RScript.class, null},
            {"executor-service", RExecutorService.class, null},
            {"remote-service", RRemoteService.class, null},
            {"rpc-client", redis.clients.redisson.RedissonRemoteServiceTest.RemoteInterface.class, null},
            {"options", RemoteInvocationOptions.class, null},
            {"keys", RKeys.class, null},
            {"live-object-service", RLiveObjectService.class, null},
            {"live-object", RLiveObject.class, null},
            {"binary-stream-ext", RBinaryStream.class, null},
            {"geo-ext", RGeo.class, null},
            {"set-cache-ext", RSetCache.class, null},
            {"map-cache-ext", RMapCache.class, null},
            {"bucket-ext", RBucket.class, null},
            {"buckets-ext", RBuckets.class, null},
            {"hyper-log-log-ext", RHyperLogLog.class, null},
            {"list-ext", RList.class, null},
            {"list-multimap-ext", RListMultimap.class, null},
            {"local-cached-map-ext", RLocalCachedMap.class, null},
            {"local-options-ext", LocalCachedMapOptions.class, null},
            {"map-ext", RMap.class, null},
            {"set-multimap-ext", RSetMultimap.class, null},
            {"set-multimap-cache-ext", RSetMultimapCache.class, null},
            {"semaphore-ext", RSemaphore.class, null},
            {"permit-expirable-semaphore-ext", RPermitExpirableSemaphore.class, null},
            {"lock-ext", RLock.class, null},
            {"fair-lock-ext", RedissonFairLock.class, null},
            {"read-write-lock-ext", RReadWriteLock.class, null},
            {"read-lock-ext", RedissonReadLock.class, "read-write-lock-ext"},
            {"write-lock-ext", RedissonWriteLock.class, "read-write-lock-ext"},
            {"multi-lock-ext", RedissonMultiLock.class, null},
            {"lock-1-ext", RLock.class, null},
            {"fair-lock-1-ext", RedissonFairLock.class, null},
            {"read-lock-1-ext", RedissonReadLock.class, "read-write-lock-ext"},
            {"write-lock-1-ext", RedissonWriteLock.class, "read-write-lock-ext"},
            {"red-lock-ext", RedissonRedLock.class, null},
            {"lock-2-ext", RLock.class, null},
            {"fair-lock-2-ext", RedissonFairLock.class, null},
            {"read-lock-2-ext", RedissonReadLock.class, "read-write-lock-ext"},
            {"write-lock-2-ext", RedissonWriteLock.class, "read-write-lock-ext"},
            {"set-ext", RSet.class, null},
            {"sorted-set-ext", RSortedSet.class, null},
            {"scored-sorted-set-ext", RScoredSortedSet.class, null},
            {"lex-sorted-set-ext", RLexSortedSet.class, null},
            {"topic-ext", RTopic.class, null},
            {"pattern-topic-ext", RPatternTopic.class, null},
            {"queue-ext", RQueue.class, null},
            {"delayed-queue-ext", RDelayedQueue.class, "queue-ext"},
            {"priority-queue-ext", RPriorityQueue.class, null},
            {"priority-deque-ext", RPriorityDeque.class, null},
            {"blocking-queue-ext", RBlockingQueue.class, null},
            {"bounded-blocking-queue-ext", RBoundedBlockingQueue.class, null},
            {"deque-ext", RDeque.class, null},
            {"blocking-deque-ext", RBlockingDeque.class, null},
            {"atomic-long-ext", RAtomicLong.class, null},
            {"atomic-double-ext", RAtomicDouble.class, null},
            {"count-down-latch-ext", RCountDownLatch.class, null},
            {"bit-set-ext", RBitSet.class, null},
            {"bloom-filter-ext", RBloomFilter.class, null},
            {"script-ext", RScript.class, null},
            {"executor-service-ext", RExecutorService.class, null},
            {"remote-service-ext", RRemoteService.class, null},
            {"rpc-client-ext", redis.clients.redisson.RedissonRemoteServiceTest.RemoteInterface.class, null},
            {"options-ext", RemoteInvocationOptions.class, null},
            {"keys-ext", RKeys.class, null},
            {"live-object-service-ext", RLiveObjectService.class, null},
            {"live-object-ext", RLiveObject.class, null},
        });
    }
    
    @Parameter
    public String key;
    
    @Parameter(1)
    public Class cls;
    
    @Parameter(2)
    public String parentKey;
    
    @Test
    public void testRObjects() {
        Object bean = context.getBean(key);
        assertTrue(cls.isInstance(bean));
        if (RObject.class.isAssignableFrom(cls)) {
            assertEquals(parentKey == null ? key : parentKey, RObject.class.cast(bean).getName());
        }
        if (RTopic.class.isAssignableFrom(cls)) {
            assertEquals(key, RTopic.class.cast(bean).getChannelNames().get(0));
        }
        if (RPatternTopic.class.isAssignableFrom(cls)) {
            assertEquals(key, RPatternTopic.class.cast(bean).getPatternNames().get(0));
        }
        if (RLiveObject.class.isAssignableFrom(cls)) {
            assertEquals(key, RLiveObject.class.cast(bean).getLiveObjectId());
        }
    }
}
