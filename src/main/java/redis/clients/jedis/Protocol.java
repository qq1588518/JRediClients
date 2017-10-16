package redis.clients.jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisBusyException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public final class Protocol {

	private static final String ASK_RESPONSE = "ASK";
	private static final String MOVED_RESPONSE = "MOVED";
	private static final String CLUSTERDOWN_RESPONSE = "CLUSTERDOWN";
	private static final String BUSY_RESPONSE = "BUSY";
	private static final String NOSCRIPT_RESPONSE = "NOSCRIPT";

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 6379;
	public static final int DEFAULT_SENTINEL_PORT = 26379;
	public static final int DEFAULT_TIMEOUT = 2000;
	public static final int DEFAULT_DATABASE = 0;

	public static final String CHARSET = "UTF-8";

	public static final byte DOLLAR_BYTE = '$';
	public static final byte ASTERISK_BYTE = '*';
	public static final byte PLUS_BYTE = '+';
	public static final byte MINUS_BYTE = '-';
	public static final byte COLON_BYTE = ':';

	public static final String SENTINEL_MASTERS = "masters";
	public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
	public static final String SENTINEL_RESET = "reset";
	public static final String SENTINEL_SLAVES = "slaves";
	public static final String SENTINEL_FAILOVER = "failover";
	public static final String SENTINEL_MONITOR = "monitor";
	public static final String SENTINEL_REMOVE = "remove";
	public static final String SENTINEL_SET = "set";

	public static final String CLUSTER_NODES = "nodes";
	public static final String CLUSTER_MEET = "meet";
	public static final String CLUSTER_RESET = "reset";
	public static final String CLUSTER_ADDSLOTS = "addslots";
	public static final String CLUSTER_DELSLOTS = "delslots";
	public static final String CLUSTER_INFO = "info";
	public static final String CLUSTER_GETKEYSINSLOT = "getkeysinslot";
	public static final String CLUSTER_SETSLOT = "setslot";
	public static final String CLUSTER_SETSLOT_NODE = "node";
	public static final String CLUSTER_SETSLOT_MIGRATING = "migrating";
	public static final String CLUSTER_SETSLOT_IMPORTING = "importing";
	public static final String CLUSTER_SETSLOT_STABLE = "stable";
	public static final String CLUSTER_FORGET = "forget";
	public static final String CLUSTER_FLUSHSLOT = "flushslots";
	public static final String CLUSTER_KEYSLOT = "keyslot";
	public static final String CLUSTER_COUNTKEYINSLOT = "countkeysinslot";
	public static final String CLUSTER_SAVECONFIG = "saveconfig";
	public static final String CLUSTER_REPLICATE = "replicate";
	public static final String CLUSTER_SLAVES = "slaves";
	public static final String CLUSTER_FAILOVER = "failover";
	public static final String CLUSTER_SLOTS = "slots";
	public static final String PUBSUB_CHANNELS = "channels";
	public static final String PUBSUB_NUMSUB = "numsub";
	public static final String PUBSUB_NUM_PAT = "numpat";

	public static final byte[] BYTES_TRUE = toByteArray(1);
	public static final byte[] BYTES_FALSE = toByteArray(0);

	private Protocol() {
		// this prevent the class from instantiation
	}

	public static void sendCommand(final RedisOutputStream os, final ProtocolCommand command, final byte[]... args) {
		sendCommand(os, command.getRaw(), args);
	}

	private static void sendCommand(final RedisOutputStream os, final byte[] command, final byte[]... args) {
		try {
			os.write(ASTERISK_BYTE);
			os.writeIntCrLf(args.length + 1);
			os.write(DOLLAR_BYTE);
			os.writeIntCrLf(command.length);
			os.write(command);
			os.writeCrLf();

			for (final byte[] arg : args) {
				os.write(DOLLAR_BYTE);
				os.writeIntCrLf(arg.length);
				os.write(arg);
				os.writeCrLf();
			}
		} catch (IOException e) {
			throw new JedisConnectionException(e);
		}
	}

	private static void processError(final RedisInputStream is) {
		String message = is.readLine();
		// TODO: I'm not sure if this is the best way to do this.
		// Maybe Read only first 5 bytes instead?
		if (message.startsWith(MOVED_RESPONSE)) {
			String[] movedInfo = parseTargetHostAndSlot(message);
			throw new JedisMovedDataException(message, new HostAndPort(movedInfo[1], Integer.valueOf(movedInfo[2])),
					Integer.valueOf(movedInfo[0]));
		} else if (message.startsWith(ASK_RESPONSE)) {
			String[] askInfo = parseTargetHostAndSlot(message);
			throw new JedisAskDataException(message, new HostAndPort(askInfo[1], Integer.valueOf(askInfo[2])),
					Integer.valueOf(askInfo[0]));
		} else if (message.startsWith(CLUSTERDOWN_RESPONSE)) {
			throw new JedisClusterException(message);
		} else if (message.startsWith(BUSY_RESPONSE)) {
			throw new JedisBusyException(message);
		} else if (message.startsWith(NOSCRIPT_RESPONSE)) {
			throw new JedisNoScriptException(message);
		}
		throw new JedisDataException(message);
	}

	public static String readErrorLineIfPossible(RedisInputStream is) {
		final byte b = is.readByte();
		// if buffer contains other type of response, just ignore.
		if (b != MINUS_BYTE) {
			return null;
		}
		return is.readLine();
	}

	private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
		String[] response = new String[3];
		String[] messageInfo = clusterRedirectResponse.split(" ");
		String[] targetHostAndPort = HostAndPort.extractParts(messageInfo[2]);
		response[0] = messageInfo[1];
		response[1] = targetHostAndPort[0];
		response[2] = targetHostAndPort[1];
		return response;
	}

	private static Object process(final RedisInputStream is) {

		final byte b = is.readByte();
		if (b == PLUS_BYTE) {
			return processStatusCodeReply(is);
		} else if (b == DOLLAR_BYTE) {
			return processBulkReply(is);
		} else if (b == ASTERISK_BYTE) {
			return processMultiBulkReply(is);
		} else if (b == COLON_BYTE) {
			return processInteger(is);
		} else if (b == MINUS_BYTE) {
			processError(is);
			return null;
		} else {
			throw new JedisConnectionException("Unknown reply: " + (char) b);
		}
	}

	private static byte[] processStatusCodeReply(final RedisInputStream is) {
		return is.readLineBytes();
	}

	private static byte[] processBulkReply(final RedisInputStream is) {
		final int len = is.readIntCrLf();
		if (len == -1) {
			return null;
		}

		final byte[] read = new byte[len];
		int offset = 0;
		while (offset < len) {
			final int size = is.read(read, offset, (len - offset));
			if (size == -1)
				throw new JedisConnectionException("It seems like server has closed the connection.");
			offset += size;
		}

		// read 2 more bytes for the command delimiter
		is.readByte();
		is.readByte();

		return read;
	}

	private static Long processInteger(final RedisInputStream is) {
		return is.readLongCrLf();
	}

	private static List<Object> processMultiBulkReply(final RedisInputStream is) {
		final int num = is.readIntCrLf();
		if (num == -1) {
			return null;
		}
		final List<Object> ret = new ArrayList<Object>(num);
		for (int i = 0; i < num; i++) {
			try {
				ret.add(process(is));
			} catch (JedisDataException e) {
				ret.add(e);
			}
		}
		return ret;
	}

	public static Object read(final RedisInputStream is) {
		return process(is);
	}

	public static final byte[] toByteArray(final boolean value) {
		return value ? BYTES_TRUE : BYTES_FALSE;
	}

	public static final byte[] toByteArray(final int value) {
		return SafeEncoder.encode(String.valueOf(value));
	}

	public static final byte[] toByteArray(final long value) {
		return SafeEncoder.encode(String.valueOf(value));
	}

	public static final byte[] toByteArray(final double value) {
		if (Double.isInfinite(value)) {
			return value == Double.POSITIVE_INFINITY ? "+inf".getBytes() : "-inf".getBytes();
		}
		return SafeEncoder.encode(String.valueOf(value));
	}

	public static enum Command implements ProtocolCommand{
		PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, PUBSUB, ZCOUNT, ZRANGEBYSCORE, ZREVRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, ZLEXCOUNT, ZRANGEBYLEX, ZREVRANGEBYLEX, ZREMRANGEBYLEX, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT, BITPOS, SETRANGE, GETRANGE, EVAL, EVALSHA, SCRIPT, SLOWLOG, OBJECT, BITCOUNT, BITOP, SENTINEL, DUMP, RESTORE, PEXPIRE, PEXPIREAT, PTTL, INCRBYFLOAT, PSETEX, CLIENT, TIME, MIGRATE, HINCRBYFLOAT, SCAN, HSCAN, SSCAN, ZSCAN, WAIT, CLUSTER, ASKING, PFADD, PFCOUNT, PFMERGE, READONLY, GEOADD, GEODIST, GEOHASH, GEOPOS, GEORADIUS, GEORADIUSBYMEMBER, MODULE, BITFIELD, HSTRLEN;

		private final byte[] raw;

		Command() {
			raw = SafeEncoder.encode(this.name());
		}

		@Override
		public byte[] getRaw() {
			return raw;
		}

	}

	public static enum Keyword {
		AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT, RESET, FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME, AND, OR, XOR, NOT, GETNAME, SETNAME, LIST, MATCH, COUNT, PING, PONG;
		public final byte[] raw;

		Keyword() {
			raw = SafeEncoder.encode(this.name().toLowerCase(Locale.ENGLISH));
		}
	}
}

/*
 * Redis 命令 1.Redis命令集中 key相关的基本命令： DEL key 该命令用于在 key 存在时删除 key。
 * 
 * DUMP key 序列化给定 key ，并返回被序列化的值。
 * 
 * EXISTS key 检查给定 key 是否存在。
 * 
 * EXPIRE key seconds 为给定 key 设置过期时间。
 * 
 * EXPIREAT key timestamp EXPIREAT 的作用和 EXPIRE 类似，都用于为 key 设置过期时间。 不同在于 EXPIREAT
 * 命令接受的时间参数是 UNIX 时间戳(unix timestamp)。
 * 
 * PEXPIRE key milliseconds 设置 key 的过期时间以毫秒计。
 * 
 * PEXPIREAT key milliseconds-timestamp 设置 key 过期时间的时间戳(unix timestamp) 以毫秒计
 * 
 * KEYS pattern 查找所有符合给定模式( pattern)的 key 。
 * 
 * MOVE key db 将当前数据库的 key 移动到给定的数据库 db 当中。
 * 
 * PERSIST key 移除 key 的过期时间，key 将持久保持。
 * 
 * PTTL key 以毫秒为单位返回 key 的剩余的过期时间。
 * 
 * TTL key 以秒为单位，返回给定 key 的剩余生存时间(TTL, time to live)。
 * 
 * RANDOMKEY 从当前数据库中随机返回一个 key 。
 * 
 * RENAME key newkey 修改 key 的名称
 * 
 * RENAMENX key newkey 仅当 newkey 不存在时，将 key 改名为 newkey 。
 * 
 * TYPE key 返回 key 所储存的值的类型。
 * 
 * 2.Redis命令集中字符串相关的基本命令：
 * 
 * SET key value 设置指定 key 的值
 * 
 * GET key 获取指定 key 的值。
 * 
 * GETRANGE key start end 返回 key 中字符串值的子字符
 * 
 * GETSET key value 将给定 key 的值设为 value ，并返回 key 的旧值(old value)。
 * 
 * GETBIT key offset 对 key 所储存的字符串值，获取指定偏移量上的位(bit)。
 * 
 * MGET key1 [key2..] 获取所有(一个或多个)给定 key 的值。
 * 
 * SETBIT key offset value 对 key 所储存的字符串值，设置或清除指定偏移量上的位(bit)。
 * 
 * SETEX key seconds value 将值 value 关联到 key ，并将 key 的过期时间设为 seconds (以秒为单位)。
 * 
 * SETNX key value 只有在 key 不存在时设置 key 的值。
 * 
 * SETRANGE key offset value 用 value 参数覆写给定 key 所储存的字符串值，从偏移量 offset 开始。
 * 
 * STRLEN key 返回 key 所储存的字符串值的长度。
 * 
 * MSET key value [key value ...] 同时设置一个或多个 key-value 对。
 * 
 * MSETNX key value [key value ...] 同时设置一个或多个 key-value 对，当且仅当所有给定 key 都不存在。
 * 
 * PSETEX key milliseconds value 这个命令和 SETEX 命令相似，但它以毫秒为单位设置 key 的生存时间，而不是像
 * SETEX 命令那样，以秒为单位。
 * 
 * INCR key 将 key 中储存的数字值增一。
 * 
 * INCRBY key increment 将 key 所储存的值加上给定的增量值（increment） 。
 * 
 * INCRBYFLOAT key increment 将 key 所储存的值加上给定的浮点增量值（increment） 。
 * 
 * DECR key 将 key 中储存的数字值减一。
 * 
 * DECRBY key decrement key 所储存的值减去给定的减量值（decrement） 。
 * 
 * APPEND key value 如果 key 已经存在并且是一个字符串， APPEND 命令将 value 追加到 key 原来的值的末尾。
 * 
 * 3.Redis命令集中hash相关的基本命令： Redis hash
 * 是一个string类型的field和value的映射表，hash特别适合用于存储对象。Redis 中每个 hash 可以存储 2^32 - 1
 * 键值对（40多亿）。
 * 
 * HDEL key field2 [field2] 删除一个或多个哈希表字段
 * 
 * HEXISTS key field 查看哈希表 key 中，指定的字段是否存在。
 * 
 * HGET key field 获取存储在哈希表中指定字段的值。
 * 
 * HGETALL key 获取在哈希表中指定 key 的所有字段和值
 * 
 * HINCRBY key field increment 为哈希表 key 中的指定字段的整数值加上增量 increment 。
 * 
 * HINCRBYFLOAT key field increment 为哈希表 key 中的指定字段的浮点数值加上增量 increment 。
 * 
 * HKEYS key 获取所有哈希表中的字段
 * 
 * HLEN key 获取哈希表中字段的数量
 * 
 * HMGET key field1 [field2] 获取所有给定字段的值
 * 
 * HMSET key field1 value1 [field2 value2 ] 同时将多个 field-value (域-值)对设置到哈希表 key
 * 中。
 * 
 * HSET key field value 将哈希表 key 中的字段 field 的值设为 value 。
 * 
 * HSETNX key field value 只有在字段 field 不存在时，设置哈希表字段的值。
 * 
 * HVALS key 获取哈希表中所有值
 * 
 * HSCAN key cursor [MATCH pattern] [COUNT count] 迭代哈希表中的键值对。
 * 
 * 4.Redis命令集中list相关的基本命令： Redis列表是简单的字符串列表，按照插入顺序排序。你可以添加一个元素导列表的头部（左边）或者尾部（右边）
 * ,一个列表最多可以包含 2^32 - 1 个元素 (4294967295, 每个列表超过40亿个元素)。
 * 
 * BLPOP key1 [key2 ] timeout 移出并获取列表的第一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
 * 
 * BRPOP key1 [key2 ] timeout 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
 * 
 * BRPOPLPUSH source destination timeout 从列表中弹出一个值，将弹出的元素插入到另外一个列表中并返回它；
 * 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
 * 
 * LINDEX key index 通过索引获取列表中的元素
 * 
 * LINSERT key BEFORE|AFTER pivot value 在列表的元素前或者后插入元素
 * 
 * LLEN key 获取列表长度
 * 
 * LPOP key 移出并获取列表的第一个元素
 * 
 * LPUSH key value1 [value2] 将一个或多个值插入到列表头部
 * 
 * LPUSHX key value 将一个值插入到已存在的列表头部
 * 
 * LRANGE key start stop 获取列表指定范围内的元素
 * 
 * LREM key count value 移除列表元素
 * 
 * LSET key index value 通过索引设置列表元素的值
 * 
 * LTRIM key start stop 对一个列表进行修剪(trim)，就是说，让列表只保留指定区间内的元素，不在指定区间之内的元素都将被删除。
 * 
 * RPOP key 移除并获取列表最后一个元素
 * 
 * RPOPLPUSH source destination 移除列表的最后一个元素，并将该元素添加到另一个列表并返回
 * 
 * RPUSH key value1 [value2] 在列表中添加一个或多个值
 * 
 * RPUSHX key value 为已存在的列表添加值
 * 
 * 5.Redis命令集中set相关的基本命令： Redis的Set是string类型的无序集合。集合成员是唯一的，这就意味着集合中不能出现重复的数据。
 * Redis 中 集合是通过哈希表实现的，所以添加，删除，查找的复杂度都是O(1)。 集合中最大的成员数为 2^32 - 1 (4294967295,
 * 每个集合可存储40多亿个成员)。
 * 
 * SADD key member1 [member2] 向集合添加一个或多个成员
 * 
 * SCARD key 获取集合的成员数
 * 
 * SDIFF key1 [key2] 返回给定所有集合的差集
 * 
 * SDIFFSTORE destination key1 [key2] 返回给定所有集合的差集并存储在 destination 中
 * 
 * SINTER key1 [key2] 返回给定所有集合的交集
 * 
 * SINTERSTORE destination key1 [key2] 返回给定所有集合的交集并存储在 destination 中
 * 
 * SISMEMBER key member 判断 member 元素是否是集合 key 的成员
 * 
 * SMEMBERS key 返回集合中的所有成员
 * 
 * SMOVE source destination member 将 member 元素从 source 集合移动到 destination 集合
 * 
 * SPOP key 移除并返回集合中的一个随机元素
 * 
 * SRANDMEMBER key [count] 返回集合中一个或多个随机数
 * 
 * SREM key member1 [member2] 移除集合中一个或多个成员
 * 
 * SUNION key1 [key2] 返回所有给定集合的并集
 * 
 * SUNIONSTORE destination key1 [key2] 所有给定集合的并集存储在 destination 集合中
 * 
 * SSCAN key cursor [MATCH pattern] [COUNT count] 迭代集合中的元素
 * 
 * 6.Redis 有序集合(sorted set)相关的基本命令： Redis 有序集合和集合一样也是string类型元素的集合,且不允许重复的成员。
 * 不同的是每个元素都会关联一个double类型的分数。redis正是通过分数来为集合中的成员进行从小到大的排序。
 * 有序集合的成员是唯一的,但分数(score)却可以重复。 集合是通过哈希表实现的，所以添加，删除，查找的复杂度都是O(1)。 集合中最大的成员数为
 * 2^32 - 1 (4294967295, 每个集合可存储40多亿个成员)。
 * 
 * ZADD key score1 member1 [score2 member2] 向有序集合添加一个或多个成员，或者更新已存在成员的分数
 * 
 * ZCARD key 获取有序集合的成员数
 * 
 * ZCOUNT key min max 计算在有序集合中指定区间分数的成员数
 * 
 * ZINCRBY key increment member 有序集合中对指定成员的分数加上增量 increment
 * 
 * ZINTERSTORE destination numkeys key [key ...] 计算给定的一个或多个有序集的交集并将结果集存储在新的有序集合
 * key 中
 * 
 * ZLEXCOUNT key min max 在有序集合中计算指定字典区间内成员数量
 * 
 * ZRANGE key start stop [WITHSCORES] 通过索引区间返回有序集合成指定区间内的成员
 * 
 * ZRANGEBYLEX key min max [LIMIT offset count] 通过字典区间返回有序集合的成员
 * 
 * ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT] 通过分数返回有序集合指定区间内的成员
 * 
 * ZRANK key member 返回有序集合中指定成员的索引
 * 
 * ZREM key member [member ...] 移除有序集合中的一个或多个成员
 * 
 * ZREMRANGEBYLEX key min max 移除有序集合中给定的字典区间的所有成员
 * 
 * ZREMRANGEBYRANK key start stop 移除有序集合中给定的排名区间的所有成员
 * 
 * ZREMRANGEBYSCORE key min max 移除有序集合中给定的分数区间的所有成员
 * 
 * ZREVRANGE key start stop [WITHSCORES] 返回有序集中指定区间内的成员，通过索引，分数从高到底
 * 
 * ZREVRANGEBYSCORE key max min [WITHSCORES] 返回有序集中指定分数区间内的成员，分数从高到低排序
 * 
 * ZREVRANK key member 返回有序集合中指定成员的排名，有序集成员按分数值递减(从大到小)排序
 * 
 * ZSCORE key member 返回有序集中，成员的分数值
 * 
 * ZUNIONSTORE destination numkeys key [key ...] 计算给定的一个或多个有序集的并集，并存储在新的 key 中
 * 
 * ZSCAN key cursor [MATCH pattern] [COUNT count] 迭代有序集合中的元素（包括元素成员和元素分值）
 * 
 * 7.Redis HyperLogLog Redis 在 2.8.9 版本添加了 HyperLogLog 结构。 Redis HyperLogLog
 * 是用来做基数统计的算法，HyperLogLog 的优点是，在输入元素的数量或者体积非常非常大时，计算基数所需的空间总是固定 的、并且是很小的。 在
 * Redis 里面，每个 HyperLogLog 键只需要花费 12 KB 内存，就可以计算接近 2^64 个不同元素的基
 * 数。这和计算基数时，元素越多耗费内存就越多的集合形成鲜明对比。 但是，因为 HyperLogLog
 * 只会根据输入元素来计算基数，而不会储存输入元素本身，所以 HyperLogLog 不能像集合那样，返回输入的各个元素。
 * 
 * 下列HyperLogLog 的基本命令：
 * 
 * PFADD key element [element ...] 添加指定元素到 HyperLogLog 中。
 * 
 * PFCOUNT key [key ...] 返回给定 HyperLogLog 的基数估算值。
 * 
 * PFMERGE destkey sourcekey [sourcekey ...] 将多个 HyperLogLog 合并为一个 HyperLogLog
 * 
 * 8.Redis 发布订阅 Redis 发布订阅(pub/sub)是一种消息通信模式：发送者(pub)发送消息，订阅者(sub)接收消息。 Redis
 * 客户端可以订阅任意数量的频道。 下图展示了频道 channel1 ， 以及订阅这个频道的三个客户端 —— client2 、 client5 和
 * client1 之间的关系： 当有新消息通过 PUBLISH 命令发送给频道 channel1 时， 这个消息就会被发送给订阅它的三个客户端：
 * 
 * 下列 redis 发布订阅常用命令： PSUBSCRIBE pattern [pattern ...] 订阅一个或多个符合给定模式的频道。
 * 
 * PUBSUB subcommand [argument [argument ...]] 查看订阅与发布系统状态。
 * 
 * PUBLISH channel message 将信息发送到指定的频道。
 * 
 * PUNSUBSCRIBE [pattern [pattern ...]] 退订所有给定模式的频道。
 * 
 * SUBSCRIBE channel [channel ...] 订阅给定的一个或多个频道的信息。
 * 
 * UNSUBSCRIBE [channel [channel ...]] 指退订给定的频道。
 * 
 * 9.Redis 事务 Redis 事务可以一次执行多个命令， 并且带有以下两个重要的保证：
 * 事务是一个单独的隔离操作：事务中的所有命令都会序列化、按顺序地执行。事务在执行的过程中，不会被其他客户端发送来的命令请求所打断。
 * 事务是一个原子操作：事务中的命令要么全部被执行，要么全部都不执行。 一个事务从开始到执行会经历以下三个阶段： 开始事务。命令入队。执行事务。
 * 
 * DISCARD 取消事务，放弃执行事务块内的所有命令。
 * 
 * EXEC 执行所有事务块内的命令。
 * 
 * MULTI 标记一个事务块的开始。
 * 
 * UNWATCH 取消 WATCH 命令对所有 key 的监视。
 * 
 * WATCH key [key ...] 监视一个(或多个) key ，如果在事务执行之前这个(或这些) key 被其他命令所改动，那么事务将被打断。
 * 
 * 10.Redis 脚本 Redis 脚本使用 Lua 解释器来执行脚本。 Reids 2.6 版本通过内嵌支持 Lua 环境。执行脚本的常用命令为
 * EVAL。 下列redis 脚本常用命令：
 * 
 * EVAL script numkeys key [key ...] arg [arg ...] 执行 Lua 脚本。
 * 
 * EVALSHA sha1 numkeys key [key ...] arg [arg ...] 执行 Lua 脚本。
 * 
 * SCRIPT EXISTS script [script ...] 查看指定的脚本是否已经被保存在缓存当中。
 * 
 * SCRIPT FLUSH 从脚本缓存中移除所有脚本。
 * 
 * SCRIPT KILL 杀死当前正在运行的 Lua 脚本。
 * 
 * SCRIPT LOAD script 将脚本 script 添加到脚本缓存中，但并不立即执行这个脚本。
 * 
 * 11.Redis 连接 Redis 连接命令主要是用于连接 redis 服务。 下列 redis 连接的基本命令：
 * 
 * AUTH password 验证密码是否正确
 * 
 * ECHO message 打印字符串
 * 
 * PING 查看服务是否运行
 * 
 * QUIT 关闭当前连接
 * 
 * SELECT index 切换到指定的数据库
 * 
 * 12.Redis 服务器 Redis 服务器命令主要是用于管理 redis 服务。 下表列出了 redis 服务器的相关命令:
 * 
 * BGREWRITEAOF 异步执行一个 AOF（AppendOnly File） 文件重写操作
 * 
 * BGSAVE 在后台异步保存当前数据库的数据到磁盘
 * 
 * CLIENT KILL [ip:port] [ID client-id] 关闭客户端连接
 * 
 * CLIENT LIST 获取连接到服务器的客户端连接列表
 * 
 * CLIENT GETNAME 获取连接的名称
 * 
 * CLIENT PAUSE timeout 在指定时间内终止运行来自客户端的命令
 * 
 * CLIENT SETNAME connection-name 设置当前连接的名称
 * 
 * CLUSTER SLOTS 获取集群节点的映射数组
 * 
 * COMMAND 获取 Redis 命令详情数组
 * 
 * COMMAND COUNT 获取 Redis 命令总数
 * 
 * COMMAND GETKEYS 获取给定命令的所有键
 * 
 * TIME 返回当前服务器时间
 * 
 * COMMAND INFO command-name [command-name ...] 获取指定 Redis 命令描述的数组
 * 
 * CONFIG GET parameter redis CONFIG GET 命令基本语法如下： 获取指定配置参数的值：CONFIG get
 * requirepass
 * 
 * CONFIG REWRITE 对启动 Redis 服务器时所指定的 redis.conf 配置文件进行改写
 * 
 * CONFIG SET parameter value 修改 redis 配置参数，无需重启
 * 
 * CONFIG RESETSTAT 重置 INFO 命令中的某些统计数据
 * 
 * DBSIZE 返回当前数据库的 key 的数量
 * 
 * DEBUG OBJECT key 获取 key 的调试信息
 * 
 * DEBUG SEGFAULT 让 Redis 服务崩溃
 * 
 * FLUSHALL 删除所有数据库的所有key
 * 
 * FLUSHDB 删除当前数据库的所有key
 * 
 * INFO [section] 获取 Redis 服务器的各种信息和统计数值
 * 
 * LASTSAVE 返回最近一次 Redis 成功将数据保存到磁盘上的时间，以 UNIX 时间戳格式表示
 * 
 * MONITOR 实时打印出 Redis 服务器接收到的命令，调试用
 * 
 * ROLE 返回主从实例所属的角色
 * 
 * SAVE 异步保存数据到硬盘 redis Save 命令基本语法如下： redis 127.0.0.1:6379> SAVE OK 该命令将在 redis
 * 安装目录中创建dump.rdb文件。 如果需要恢复数据，只需将备份文件 (dump.rdb) 移动到 redis 安装目录并启动服务即可。获取 redis
 * 目录可以使用 CONFIG 命令，如下所示： redis 127.0.0.1:6379> CONFIG GET dir 1) "dir" 2)
 * "/usr/local/redis/bin" 以上命令 CONFIG GET dir 输出的 redis 安装目录为
 * /usr/local/redis/bin。 Bgsave 创建 redis 备份文件也可以使用命令 BGSAVE，该命令在后台执行。
 * 127.0.0.1:6379> BGSAVE Background saving started
 * 
 * SHUTDOWN [NOSAVE] [SAVE] 异步保存数据到硬盘，并关闭服务器
 * 
 * SLAVEOF host port 将当前服务器转变为指定服务器的从属服务器(slave server)
 * 
 * SLOWLOG subcommand [argument] 管理 redis 的慢日志
 * 
 * SYNC 用于复制功能(replication)的内部命令
 * 
 */