package redis.clients.redisson.executor;

import java.util.concurrent.Callable;

import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.annotation.RInject;

public class IncrementCallableTask implements Callable<String> {

    private String counterName;
    
    @RInject
    private RedissonClient redisson;

    public IncrementCallableTask() {
    }
    
    public IncrementCallableTask(String counterName) {
        super();
        this.counterName = counterName;
    }

    @Override
    public String call() throws Exception {
        redisson.getAtomicLong(counterName).incrementAndGet();
        return "1234";
    }

}
