package redis.clients.redis;

/**
 * Created by qq24139297 on 17/3/16. 
 * 默认保存为map, 保证更新不会被覆盖
 */
public interface RedisInterface {
	public String getUnionKey();

	public String getRedisKeyEnumString();
}
