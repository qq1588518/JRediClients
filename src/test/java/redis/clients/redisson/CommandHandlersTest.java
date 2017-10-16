package redis.clients.redisson;

import org.junit.Test;
import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.config.Config;

public class CommandHandlersTest extends BaseTest {

    @Test(expected = RuntimeException.class)
    public void testEncoder() throws InterruptedException {
        Config config = createConfig();
        config.setCodec(new ErrorsCodec());
        
        RedissonClient redisson = Redisson.create(config);
        
        redisson.getBucket("1234").set("1234");
    }
    
    @Test(expected = RuntimeException.class)
    public void testDecoder() {
        redisson.getBucket("1234").set("1234");
        
        Config config = createConfig();
        config.setCodec(new ErrorsCodec());
        
        RedissonClient redisson = Redisson.create(config);
        
        redisson.getBucket("1234").get();
    }
    
}
