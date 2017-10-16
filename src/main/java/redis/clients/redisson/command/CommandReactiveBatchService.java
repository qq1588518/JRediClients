package redis.clients.redisson.command;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import redis.clients.redisson.api.BatchResult;
import redis.clients.redisson.api.RFuture;
import redis.clients.redisson.api.RedissonReactiveClient;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.client.protocol.RedisCommand;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.connection.NodeSource;
import redis.clients.redisson.misc.RPromise;
import redis.clients.redisson.reactive.NettyFuturePublisher;

import reactor.fn.Supplier;
import reactor.rx.action.support.DefaultSubscriber;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class CommandReactiveBatchService extends CommandReactiveService {

    private final CommandBatchService batchService;
    private final Queue<Publisher<?>> publishers = new ConcurrentLinkedQueue<Publisher<?>>();

    public CommandReactiveBatchService(ConnectionManager connectionManager) {
        super(connectionManager);
        batchService = new CommandBatchService(connectionManager);
    }

    @Override
    public <R> Publisher<R> reactive(Supplier<RFuture<R>> supplier) {
        NettyFuturePublisher<R> publisher = new NettyFuturePublisher<R>(supplier);
        publishers.add(publisher);
        return publisher;
    }
    
    @Override
    protected <V, R> void async(boolean readOnlyMode, NodeSource nodeSource,
            Codec codec, RedisCommand<V> command, Object[] params, RPromise<R> mainPromise, int attempt) {
        batchService.async(readOnlyMode, nodeSource, codec, command, params, mainPromise, attempt);
    }

    public RFuture<BatchResult<?>> executeAsync(int syncSlaves, long syncTimeout, boolean skipResult, long responseTimeout, int retryAttempts, long retryInterval) {
        for (Publisher<?> publisher : publishers) {
            publisher.subscribe(new DefaultSubscriber<Object>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(1);
                }
            });
        }

        return batchService.executeAsync(syncSlaves, syncTimeout, skipResult, responseTimeout, retryAttempts, retryInterval);
    }

    @Override
    public CommandAsyncExecutor enableRedissonReferenceSupport(RedissonReactiveClient redissonReactive) {
        batchService.enableRedissonReferenceSupport(redissonReactive);
        return super.enableRedissonReferenceSupport(redissonReactive);
    }
    
}
