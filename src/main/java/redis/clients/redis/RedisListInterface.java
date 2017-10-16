package redis.clients.redis;

/**
 * 列表类型的缓存对象
 * 
 */
public interface RedisListInterface {
	/**
	 * 列表对象的子唯一主键属性
	 */
	public String getSubUniqueKey();
	/**
	 * 列表对象的主键属性
	 */	
	public String getShardingKey();
	public String getRedisKeyEnumString();
}
