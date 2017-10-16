package redis.clients.redisson.executor;

import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.annotation.RInject;

public class IncrementRunnableTask implements Runnable {

    private String counterName;
    
    @RInject
    private RedissonClient redisson;

    public IncrementRunnableTask() {
    }
    
    public IncrementRunnableTask(String counterName) {
        super();
        this.counterName = counterName;
    }

    @Override
    public void run() {
        redisson.getAtomicLong(counterName).incrementAndGet();
    }

}
