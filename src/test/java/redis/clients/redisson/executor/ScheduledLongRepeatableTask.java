package redis.clients.redisson.executor;

import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.annotation.RInject;

public class ScheduledLongRepeatableTask implements Runnable {

    @RInject
    private RedissonClient redisson;
    
    private String counterName;
    private String objectName;
    
    public ScheduledLongRepeatableTask() {
    }
    
    public ScheduledLongRepeatableTask(String counterName, String objectName) {
        super();
        this.counterName = counterName;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        if (redisson.getAtomicLong(counterName).incrementAndGet() == 3) {
            for (int i = 0; i < Long.MAX_VALUE; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("interrupted " + i);
                    redisson.getBucket(objectName).set(i);
                    return;
                }
            }
        }
    }

}
