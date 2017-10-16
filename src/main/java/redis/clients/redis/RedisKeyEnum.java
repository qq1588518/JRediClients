package redis.clients.redis;

/**
 * 缓存键值
 */
public enum RedisKeyEnum {
	USER("us#"),
	ACCOUNT("ac#"),
	BUNCHER("bu"),
	COLOUR("co#"),
	FASHION("fa#"),
	ITEM("it#"),
	MAKING("ma#"),
	MISSION("mi#"),
	TEAM("te#"),
	WEAPON("we#"), 
	WEAPONSET("ws#")
	;

	private String key;

	RedisKeyEnum(String key) {
		this.key = key;
	}

	public static RedisKeyEnum geRedisKeyEnum(String key) {
		RedisKeyEnum result = null;
		for (RedisKeyEnum temp : RedisKeyEnum.values()) {
			if (temp.getKey().equals(key)) {
				result = temp;
				break;
			}
		}
		return result;
	}

	public String getKey() {
		return this.key;
	}
}