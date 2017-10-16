package redis.clients.redisson.executor;

import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.annotation.RInject;

public class ScheduledLongRunnableTask implements Runnable {

    @RInject
    private RedissonClient redisson;
    private String objectName;
    
    public ScheduledLongRunnableTask() {
    }
    
    public ScheduledLongRunnableTask(String objectName) {
        super();
        this.objectName = objectName;
    }

    @Override
    public void run() {
        for (int i = 0; i < Long.MAX_VALUE; i++) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("interrupted " + i);
                redisson.getBucket(objectName).set(i);
                return;
            }
        }
    }

}
