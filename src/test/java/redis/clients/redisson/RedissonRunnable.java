package redis.clients.redisson;

import redis.clients.redisson.api.RedissonClient;

public interface RedissonRunnable {

    void run(RedissonClient redisson);

}
