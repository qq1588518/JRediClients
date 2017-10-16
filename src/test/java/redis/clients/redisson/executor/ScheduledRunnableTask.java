package redis.clients.redisson.executor;

import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.annotation.RInject;

public class ScheduledRunnableTask implements Runnable {

    @RInject
    private RedissonClient redisson;
    private String objectName;
    
    public ScheduledRunnableTask() {
    }
    
    public ScheduledRunnableTask(String objectName) {
        super();
        this.objectName = objectName;
    }

    @Override
    public void run() {
        redisson.getAtomicLong(objectName).incrementAndGet();
    }

}
