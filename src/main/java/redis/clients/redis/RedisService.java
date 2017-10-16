package redis.clients.redis;

import java.util.*;
import java.util.Map.Entry;
import org.slf4j.Logger;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import redis.clients.common.utils.Loggers;
import redis.clients.common.utils.EntityUtils;
import redis.clients.common.utils.ObjectUtils;
import redis.clients.common.utils.PageUtils;
import redis.clients.common.utils.StringUtils;
import redis.clients.common.utils.TimeUtils;
import redis.clients.common.utils.EntityUtils.EntitySaveEnum;
import redis.clients.common.constant.GlobalConstants;
import redis.clients.entity.IEntity;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

/**
 * Created by qq24139297 on 17/3/16. 缓存服务
 */

@Service
public class RedisService {

	protected static Logger logger = Loggers.redisLogger;
	/*
	 * 数据源
	 */
	private JedisPool jedisPool;

	/**
	 * 设置连接池
	 */
	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	/*
	 * 正常返还链接
	 */
	@SuppressWarnings("deprecation")
	private void returnResource(Jedis jedis) {
		if (jedis != null) {
			try {
				jedisPool.returnResource(jedis);
			} catch (Exception e) {
				logger.error("RedisService.returnResource() Exception err={}", e.toString());
			}
		}
	}

	/*
	 * 释放错误链接
	 */
	@SuppressWarnings("deprecation")
	private void returnBrokenResource(Jedis jedis, String name, Exception msge) {
		logger.error(TimeUtils.dateToString(new Date()) + ":::::" + name + ":::::" + msge.getMessage());
		if (jedis != null) {
			try {
				jedisPool.returnBrokenResource(jedis);
			} catch (Exception e) {
				logger.error("RedisService.returnBrokenResource() Exception err={}", e.toString());
			}
		}
	}

	/**
	 * 设置缓存生命周期
	 * 
	 * @param key
	 * @param seconds
	 */
	public boolean expire(String key, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			if (seconds >= 0) {
				ret = (jedis.expire(key, seconds) > 0);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "expire:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 将对象保存到hash中,并且设置默认生命周期
	 * 
	 * @param key
	 * @param bean
	 */
	public boolean setObjectToHash(String key, IEntity entity) {
		return setObjectToHash(key, entity, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	/**
	 * 将对象保存到hash中,并且设置生命周期
	 * 
	 * @param key
	 * @param entity
	 * @param seconds
	 */
	public boolean setObjectToHash(String key, IEntity entity, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			Map<String, String> map = EntityUtils.getCacheValueMap(entity, EntitySaveEnum.Save2Redis);
			if (map != null && map.size() > 0) {
				String result = jedis.hmset(key, map);
				ret = "OK".equalsIgnoreCase(result);
				if (ret && (seconds >= 0)) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "setObjectToHash:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/*
	 * 更新缓存里的hash值
	 * 
	 * @param key
	 * 
	 * @param map
	 * 
	 * @param unique 此key是由哪个字段拼接而成的
	 */
	public boolean updateObjectHashMap(String key, Map<String, Object> map) {
		return updateObjectHashMap(key, map, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	public boolean updateObjectHashMap(String key, Map<String, Object> mapToUpdate, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			Map<String, String> map = new HashMap<String, String>();
			for (Entry<String, Object> entry : mapToUpdate.entrySet()) {
				String temp = entry.getKey();
				Object obj = entry.getValue();
				if (obj instanceof Date) {
					map.put(temp, TimeUtils.dateToString((Date) obj));
				}
				else if((obj instanceof Map)||(obj instanceof HashMap)) {
					map.put(temp, JSON.toJSONString(obj));
				}
				else if((obj instanceof List)||(obj instanceof ArrayList)) {
					map.put(temp, JSON.toJSONString(obj));
				}
				else {
					map.put(temp, obj.toString());
				}
			}
			if (map != null && map.size() > 0) {
				String result = jedis.hmset(key, map);
				ret = "OK".equalsIgnoreCase(result);
				if (ret && (seconds >= 0)) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "updateHashMap:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}
	/*
	 * 更新缓存里的field字段值
	 * 
	 * @param key
	 * 
	 * @param field
	 * 
	 * @param value 此key是由哪个字段拼接而成的
	 */

	public Long hincrBy(String key, String field, int value) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = -1L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hincrBy(key, field, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hincrBy:" + key + ":field" + field, e);
		} finally {
			releaseReidsSource(success, jedis);
		}

		return ret;
	}

	/**
	 * 通过反射从缓存里获取一个对象 缺省默认时间
	 * 
	 * @param <T>
	 * @param key
	 * @param clazz
	 * @return
	 */

	@SuppressWarnings("unchecked")
	public <T> T getObjectFromHash(String key, Class<?> clazz) {
		return (T) getObjectFromHash(key, clazz, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	/*
	 * 通过反射从缓存里获取一个对象
	 * 
	 * @param key
	 * 
	 * @param clazz
	 * 
	 * @param uniqueKey 此key由哪个字段拼接而成的
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObjectFromHash(String key, Class<?> clazz, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		Object ret = null;
		try {
			if (clazz == null) {
				success = false;
				return null;
			}
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return null;
			}
			Map<String, String> map = jedis.hgetAll(key);
			if (map != null && map.size() > 0) {
				Object obj = clazz.newInstance();
				if (obj != null) {
					ret = ObjectUtils.getObjFromMap(map, obj);
				}
				if (seconds >= 0) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getObjectFromHash:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return (T) ret;
	}

	/**
	 * 将一个列表对象放入缓存
	 * 
	 * @param key
	 * @param list
	 */

	public boolean setListToHash(String key, List<RedisListInterface> list) {
		return setListToHash(key, list, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	/*
	 * 将一个列表对象放入缓存，并设置有效期
	 * 
	 * @param key
	 * 
	 * @param list
	 * 
	 * @param seconds
	 */
	public boolean setListToHash(String key, List<RedisListInterface> list, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			Map<String, String> map = new HashMap<String, String>();
			for (RedisListInterface po : list) {
				Map<String, String> cacheMap = EntityUtils.getCacheValueMap((IEntity) po, EntitySaveEnum.Save2Redis);
				if (cacheMap != null && (!cacheMap.isEmpty())) {
					map.put(po.getSubUniqueKey(), JSON.toJSONString(cacheMap));
				}
			}
			if (map != null && map.size() > 0) {
				String result = jedis.hmset(key, map);
				ret = "OK".equalsIgnoreCase(result);
				if (ret && (seconds >= 0)) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "setListToHash:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 从缓存里还原一个列表对象
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	public <T> List<T> getListFromHash(String key, Class<?> clazz) {
		return getListFromHash(key, clazz, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	/**
	 * 从缓存里还原一个列表对象
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getListFromHash(String key, Class<?> clazz, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		List<T> ret = new ArrayList<T>();
		try {
			if (clazz == null) {
				success = false;
				return null;
			}
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return null;
			}
			Map<String, String> map = jedis.hgetAll(key);
			if (map != null && map.size() > 0) {
				RedisListInterface po = null;
				Map<String, String> mapFields = null;
				for (Entry<String, String> entry : map.entrySet()) {
					mapFields = JSON.parseObject(entry.getValue().replaceAll("=", ":"), HashMap.class);
					po = (RedisListInterface) clazz.newInstance();
					ObjectUtils.getObjFromMap(mapFields, po);
					ret.add((T) po);
				}
				if (seconds >= 0) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getListFromHash:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public <T> T getObjectFromList(String key, String subUnionkey, Class<?> clazz) {
		return (T) getObjectFromList(key, subUnionkey, clazz, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	/**
	 * 从缓存里还原一个列表对象
	 * 
	 * @param key
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getObjectFromList(String key, String subUnionkey, Class<?> clazz, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		RedisListInterface po = null;
		Map<String, String> mapFields = null;
		try {
			if (clazz == null) {
				success = false;
				return null;
			}
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return null;
			}
			String value = jedis.hget(key, subUnionkey);
			if (!StringUtils.isEmpty(value)) {
				mapFields = JSON.parseObject(value.replaceAll("=", ":"), HashMap.class);
				po = (RedisListInterface) clazz.newInstance();
				if (po != null) {
					ObjectUtils.getObjFromMap(mapFields, po);
				}
				if (seconds >= 0) {
					jedis.expire(key, seconds);
				}
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getObjectFromList:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return (T) po;
	}

	/**
	 * 批量删除对象
	 * 
	 * @param key
	 * @param list
	 */

	public boolean deleteList(String key, List<RedisListInterface> list) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			String keys[] = new String[list.size()];
			// String keyNames[] = null;
			// Map<String, String> keyMap = null;
			int index = 0;
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			for (RedisListInterface po : list) {
				keys[index++] = ObjectUtils.getFieldsValueStr(po, po.getSubUniqueKey());
			}
			ret = (jedis.hdel(key, keys) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "deleteList:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public boolean setString(String key, String object) {
		return setString(key, object, -1);
	}

	/**
	 * 设置
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean setNxString(String key, String value, int seconds) throws Exception {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.setnx(key, value) > 0);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setNxString", e, false);
			// throw e;
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 设置
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean setHnxString(String key, String field, String value) throws Exception {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.hsetnx(key, field, value) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setHnxString", e, false);
			// throw e;
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;

	}

	/**
	 * 必须强制获取成功状态
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	public String getHgetString(String key, String field) throws Exception {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hget(key, field);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "getHString", e, false);
			// throw e;
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 删除key
	 * 
	 * @param key
	 */
	public boolean deleteHField(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.hdel(key, field) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "deleteHField", e, false);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 删除key
	 * 
	 * @param key
	 */
	public boolean deleteKey(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.del(key) > 0);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "deleteKey", e, false);
		} finally {
			releaseReidsSource(success, jedis);
		}

		return ret;
	}

	/**
	 * 获取所有成员及分数
	 * 
	 * @param key
	 */
	public Set<Tuple> zAllMemberWithScore(String key) {
		Jedis jedis = null;
		boolean success = true;
		Set<Tuple> set = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return set;
			}
			set = jedis.zrevrangeWithScores(key, 0, -1);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "zAllMemberWithScore", e, false);
		} finally {
			releaseReidsSource(success, jedis);
		}

		return set;
	}

	/**
	 * 排序
	 * 
	 * @param key
	 */
	public Set<String> zRevRange(String key, long start, long end) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> set = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return set;
			}
			set = jedis.zrevrange(key, start, end);
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "deleteKey", e, false);
		} finally {
			releaseReidsSource(success, jedis);
		}

		return set;
	}

	/**
	 * 删除key
	 */
	public void deleteKeys(String... keys) {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				return;
			}
			jedis.del(keys);
		} catch (Exception e) {
			returnBrokenResource(jedis, "deleteKey" + keys, e);
		} finally {
			releaseReidsSource(true, jedis);
		}

	}

	/**
	 * 释放非正常链接
	 * 
	 * @param jedis
	 * @param key
	 * @param string
	 * @param e
	 */
	private void releaseBrokenReidsSource(Jedis jedis, String key, String string, Exception e, boolean deleteKeyFlag) {
		returnBrokenResource(jedis, string, e);
		if (deleteKeyFlag) {
			expire(key, 0);
		}
	}

	/**
	 * 释放成功链接
	 * 
	 * @param success
	 * @param jedis
	 */
	private void releaseReidsSource(boolean success, Jedis jedis) {
		if (success && jedis != null) {
			returnResource(jedis);
		}
	}

	public boolean setString(String key, String value, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = (jedis.set(key, value) != null);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setString", e, true);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String getString(String key) {
		return getString(key, -1);
	}

	public String getString(String key, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.get(key);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getString", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 需要知道是否是成功获取的
	 * 
	 * @param key
	 * @return
	 */
	public Object[] getStringAndSuccess(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = "";
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return null;
			}
			ret = jedis.get(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "getString", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		Object[] object = new Object[2];
		object[0] = success;
		object[1] = ret;
		return object;
	}

	public List<String> hvals(String key) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return null;
			}
			return jedis.hvals(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hvals", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return null;
	}

	public boolean hexists(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			return jedis.hexists(key, field);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hexists", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return false;
	}

	/**
	 * 检测是否是成员
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public boolean sexists(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			return jedis.sismember(key, member);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hexists", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return false;
	}

	public Long hdel(String key, String... fields) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = -1L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hdel(key, fields);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hdel", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public List<String> mgetString(List<String> keys) {
		Jedis jedis = null;
		boolean success = true;
		List<String> ret = new ArrayList<String>();
		if (ObjectUtils.isEmpityList(keys)) {
			return ret;
		}
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			if (keys.size() > GlobalConstants.RedisKeyConfig.MGET_MAX_KEY) {
				List<String> tmp = new ArrayList<String>();
				int size = keys.size();
				int page = size / GlobalConstants.RedisKeyConfig.MGET_MAX_KEY
						+ ((size % GlobalConstants.RedisKeyConfig.MGET_MAX_KEY) > 0 ? 1 : 0);
				for (int i = 0; i < page; i++) {
					tmp.addAll(PageUtils.getSubListPage(keys, i * GlobalConstants.RedisKeyConfig.MGET_MAX_KEY,
							GlobalConstants.RedisKeyConfig.MGET_MAX_KEY));
					ret.addAll(jedis.mget(tmp.toArray(new String[0])));
					tmp.clear();
				}
			} else {
				String[] keys2 = keys.toArray(new String[0]);
				ret = jedis.mget(keys2);
			}
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "mgetString" + keys, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public void hsetString(String key, String field, String value) {
		hsetString(key, field, value, GlobalConstants.RedisKeyConfig.NORMAL_LIFECYCLE);
	}

	public void hsetString(String key, String field, String value, int seconds) {
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return;
			}
			jedis.hset(key, field, value);
			if (seconds >= 0) {
				jedis.expire(key, seconds);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "hsetString", e, true);
		} finally {
			releaseReidsSource(success, jedis);
		}
	}

	public Map<String, String> hmgetAllString(String key) {
		Jedis jedis = null;
		boolean success = true;
		Map<String, String> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hgetAll(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hmgetAllString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String hget(String key, String field) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hget(key, field);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hmgetString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long hLen(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.hlen(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "hLen" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/** 返回的是列表的剩余个数 */
	public long lpushString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.lpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpushString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/** 返回的是列表的剩余个数 */
	public long rPushString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.rpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "rpushString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 返回集合key的基数(集合中元素的数量)。
	 * 
	 * @param key
	 * @return
	 */
	public long scardString(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.scard(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "scardString" + key, e);
			System.out.println(e.getMessage().toString());
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long saddString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		Long ret = 0L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.sadd(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "saddString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long saddStrings(String key, String... values) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.sadd(key, values);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "saddStrings" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long sremString(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.srem(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "sremString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long sremStrings(String key, String... values) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.srem(key, values);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "sremStrings" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String spopString(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.spop(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "spopString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public Set<String> smembersString(String key) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.smembers(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "smembersString" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 删除zset 的成员
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public long zRemByMember(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0L;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.zrem(key, member);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScoreWithScores", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 删除zset 的成员
	 * 
	 * @param key
	 * @param member
	 * @return
	 */
	public boolean zRemByMemberReturnBoolean(String key, String member) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = (jedis.zrem(key, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScoreWithScores", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public Set<String> zrangeByScore(String key, long min, long max, int limit) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.zrangeByScore(key, min, max, 0, limit);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScore", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 增加成员
	 * 
	 * @param key
	 * @return
	 */
	public boolean zAdd(String key, String member, long value) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.zadd(key, value, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zAdd key:" + key + "member:" + member + "value:" + value, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 增加值
	 * 
	 * @param key
	 * @param member
	 * @param value
	 * @return
	 */
	public boolean zIncrBy(String key, String member, long value) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			// 记录最后一个心跳时间
			ret = (jedis.zincrby(key, value, member) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zIncrBy key:" + key + "member:" + member + "value:" + value, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 增加成员
	 * 
	 * @return
	 */
	public boolean zAddMap(String key, Map<String, Double> scoreMembers) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			// 记录最后一个心跳时间
			ret = (jedis.zadd(key, scoreMembers) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zAddMap key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public Long incr(String key) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.incr(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "incr:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public Long incrBy(String key, long value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.incrBy(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "incrBy:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 返回是否成功
	 * 
	 * @param key
	 * @return
	 */
	public boolean decr(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.decr(key) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "decr:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 返回是否成功
	 * 
	 * @param key
	 * @return
	 */
	public boolean decrBy(String key, int size) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.decrBy(key, size) > 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "decrBy:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public boolean exists(String key) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = jedis.exists(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "exists:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 返回在分数之类的所有的成员以及分数.
	 */
	public Set<Tuple> zrangeByScoreWithScores(String key, long min, long max, int offset, int limit) {
		Jedis jedis = null;
		boolean success = true;
		Set<Tuple> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.zrangeByScoreWithScores(key, min, max, offset, limit);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zrangeByScoreWithScores limit", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 获取包含这个key的所有redis key
	 * 
	 * @param key
	 * @return
	 */
	public Set<String> keys(String key) {
		Jedis jedis = null;
		boolean success = true;
		Set<String> keys = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return keys;
			}
			keys = jedis.keys("*" + key + "*");
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "keys", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return keys;
	}

	/**
	 * 获取key的剩余时间
	 * 
	 * @param key
	 * @return
	 */
	public long getKeyTTL(String key) {
		Jedis jedis = null;
		long ret = 0;
		// Set<String> keys = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.ttl(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "key ttl", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	/**
	 * 设置
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public boolean setNxStringByMillisec(String key, String value, int millisec) {
		Jedis jedis = null;
		boolean success = true;
		boolean ret = false;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return false;
			}
			ret = (jedis.setnx(key, value) > 0);
			if (millisec > -1) {
				jedis.pexpire(key, millisec);
			}
		} catch (Exception e) {
			success = false;
			releaseBrokenReidsSource(jedis, key, "setNxStringByMillisec", e, false);
		} finally {
			releaseReidsSource(success, jedis);
		}

		return ret;

	}

	public List<String> lrange(String key, int start, int stop) {
		Jedis jedis = null;
		boolean success = true;
		List<String> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.lrange(key, start, stop);

		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lrange", e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long rpush(String key, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = -1;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.rpush(key, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "rpush key:" + key + "value:" + value, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String lpop(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.lpop(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String lindex(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.lindex(key, 0);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public long zcount(String key) {
		long ret = 0L;
		Jedis jedis = null;
		boolean success = true;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.zcount(key, 0, Long.MAX_VALUE);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "zcount key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public String sRandMember(String key) {
		Jedis jedis = null;
		boolean success = true;
		String ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.srandmember(key);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lpop key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	public List<String> sRandMember(String key, int count) {
		Jedis jedis = null;
		boolean success = true;
		List<String> ret = null;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.srandmember(key, count);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "sRandMember key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}

	// 删除集合里的元素
	public Long lRem(String key, int count, String value) {
		Jedis jedis = null;
		boolean success = true;
		long ret = 0;
		try {
			jedis = jedisPool.getResource();
			if (jedis == null) {
				success = false;
				return ret;
			}
			ret = jedis.lrem(key, count, value);
		} catch (Exception e) {
			success = false;
			returnBrokenResource(jedis, "lRem key:" + key, e);
		} finally {
			releaseReidsSource(success, jedis);
		}
		return ret;
	}
}
