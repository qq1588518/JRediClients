package redis.clients.common.utils;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.reflect.Field;

import redis.clients.common.annotation.FieldSave;
import redis.clients.common.annotation.FieldSave2DB;
import redis.clients.common.annotation.FieldSave2Redis;
import redis.clients.entity.AbstractEntity;
import redis.clients.entity.IEntity;
import redis.clients.proxy.EntityProxy;
import redis.clients.proxy.EntityProxyWrapper;
import redis.clients.redis.RedisInterface;
import redis.clients.redis.RedisListInterface;
import redis.clients.redis.RedisService;

/**
 * Created by qq24139297 on 2017/3/22. 实体辅助类
 */
public class EntityUtils {
	/*
	 * type == 0 获取所有需要同时保存到Redis和DB数据库的字段 type == 1 获取所有需要同时保存到DB数据库的字段 type == 2
	 * 获取所有需要同时保存到Redis的字段
	 */
	public enum EntitySaveEnum {
		SaveAll, Save2DB, Save2Redis
	}

	//
	public static String ENTITY_SPLIT_STRING = "#";

	/**
	 * 获取所有需要保存到Redis和DB数据库的缓存的字段跟值
	 * 
	 * @param iEntity
	 * @return
	 */
	public static Map<String, String> getCacheValueMap(IEntity iEntity, EntitySaveEnum type) {
		Map<String, String> map = new HashMap<>();
		Field[] fields = getAllCacheFields(iEntity, type);
		for (Field field : fields) {
			String fieldName = field.getName();
			String value = ObjectUtils.getFieldsValueStr(iEntity, fieldName);
			map.put(fieldName, value);
		}
		return map;
	}

	/**
	 * 获取代理对象里面变化的值
	 * 
	 * @param iEntity
	 * @return
	 */
	public static Map<String, String> getProxyChangedCacheValueMap(AbstractEntity entity) {
		EntityProxyWrapper entityProxyWrapper = entity.getEntityProxyWrapper();
		if (entityProxyWrapper != null) {
			EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
			if (entityProxy != null) {
				Map<String, Object> changeParamSet = entityProxy.getChangeParamSet(EntitySaveEnum.Save2Redis);
				if (changeParamSet != null && (!changeParamSet.isEmpty())) {
					return ObjectUtils.getTransferMap(changeParamSet);
				}
			}
		}
		return null;
	}

	/**
	 * 获取所有缓存的需要保存到Redis和DB数据库的字段field type == 0 获取所有需要同时保存到Redis和DB数据库的字段 type == 1
	 * 获取所有需要同时保存到DB数据库的字段 type == 2 获取所有需要同时保存到Redis的字段
	 * 
	 * @param obj
	 * @return
	 */
	public static Field[] getAllCacheFields(IEntity obj, EntitySaveEnum type) {
		Class<?> clazz = obj.getClass();
		List<Field> fieldList = new ArrayList<>();
		for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				// 获取filed注解
				FieldSave fieldSave = field.getAnnotation(FieldSave.class);
				if (fieldSave != null) {
					fieldList.add(field);
				}
				if (type == EntitySaveEnum.Save2DB) {
					// 保存到DB数据库的字段
					FieldSave2DB fieldSave2DB = field.getAnnotation(FieldSave2DB.class);
					if (fieldSave2DB != null) {
						fieldList.add(field);
					}
				} else if (type == EntitySaveEnum.Save2Redis) {
					// 保存到Redis数据库的字段
					FieldSave2Redis fieldSave2Redis = field.getAnnotation(FieldSave2Redis.class);
					if (fieldSave2Redis != null) {
						fieldList.add(field);
					}
				}
			}
		}
		return fieldList.toArray(new Field[0]);
	}

	// Get rediskey
	public static String getRedisKey(RedisInterface redisInterface) {
		return redisInterface.getRedisKeyEnumString() + redisInterface.getUnionKey();
	}

	// Get rediskey by RedisListInterface
	public static String getRedisListKey(RedisListInterface redisInterface) {
		return redisInterface.getRedisKeyEnumString() + redisInterface.getShardingKey();
	}

	/**
	 * 更新变化字段
	 * 
	 * @param entity
	 */
	public static boolean updateChangedFieldEntity(RedisService redisService, AbstractEntity entity) {
		boolean ret = false;
		if (entity != null) {
			if (entity instanceof RedisInterface) {
				RedisInterface redisInterface = (RedisInterface) entity;
				EntityProxyWrapper entityProxyWrapper = entity.getEntityProxyWrapper();
				if (entityProxyWrapper != null) {
					EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
					if (entityProxy != null) {
						Map<String, Object> changeParamSet = entityProxy.getChangeParamSet(EntitySaveEnum.Save2Redis);
						if ((changeParamSet != null) && (!changeParamSet.isEmpty())) {
							ret = redisService.updateObjectHashMap(EntityUtils.getRedisKey(redisInterface),
									changeParamSet);
						}
					}
				}
			} else if (entity instanceof RedisListInterface) {
				RedisListInterface redisListInterface = (RedisListInterface) entity;
				List<RedisListInterface> redisListInterfaceList = new ArrayList<>();
				redisListInterfaceList.add(redisListInterface);
				ret = redisService.setListToHash(EntityUtils.getRedisListKey(redisListInterface),
						redisListInterfaceList);
			}
		}
		return ret;
	}

	/**
	 * 插入或更新所有字段
	 * 
	 * @param entity
	 */
	public static boolean updateAllFieldEntity(RedisService redisService, AbstractEntity entity) {
		boolean ret = false;
		if (entity != null) {
			if (entity instanceof RedisInterface) {
				RedisInterface redisInterface = (RedisInterface) entity;
				ret = redisService.setObjectToHash(EntityUtils.getRedisKey(redisInterface), entity);
			} else if (entity instanceof RedisListInterface) {
				RedisListInterface redisListInterface = (RedisListInterface) entity;
				List<RedisListInterface> redisListInterfaceList = new ArrayList<>();
				redisListInterfaceList.add(redisListInterface);
				ret = redisService.setListToHash(EntityUtils.getRedisListKey(redisListInterface),
						redisListInterfaceList);
			}
		}
		return ret;
	}

	/**
	 * 删除实体
	 * 
	 * @param abstractEntity
	 */
	public static boolean deleteEntity(RedisService redisService, AbstractEntity abstractEntity) {
		boolean ret = false;
		if (abstractEntity != null) {
			if (abstractEntity instanceof RedisInterface) {
				RedisInterface redisInterface = (RedisInterface) abstractEntity;
				ret = redisService.deleteKey(EntityUtils.getRedisKey(redisInterface));
			} else if (abstractEntity instanceof RedisListInterface) {
				RedisListInterface redisListInterface = (RedisListInterface) abstractEntity;
				Long delret = redisService.hdel(EntityUtils.getRedisListKey(redisListInterface),
						redisListInterface.getSubUniqueKey());
				if (delret > 0) {
					ret = true;
				}
			}
		}
		return ret;
	}

	/**
	 * 插入或更新所有字段实体列表
	 * 
	 * @param entityList
	 */
	public static List<Long> updateAllFieldEntityList(RedisService redisService, List<AbstractEntity> entityList) {
		// 拿到第一个，看一下类型
		boolean ret = false;
		List<Long> retlist = new ArrayList<>();
		if (entityList.size() > 0) {
			AbstractEntity entity = entityList.get(0);
			if (entity instanceof RedisInterface) {
				for (AbstractEntity abstractEntity : entityList) {
					ret = updateAllFieldEntity(redisService, entity);
					retlist.add((long) (ret == true ? 1 : 0));
				}
			} else if (entity instanceof RedisListInterface) {
				List<RedisListInterface> redisListInterfaceList = new ArrayList<>();
				for (AbstractEntity abstractEntity : entityList) {
					redisListInterfaceList.add((RedisListInterface) abstractEntity);
				}
				ret = redisService.setListToHash(EntityUtils.getRedisListKey((RedisListInterface) entity),
						redisListInterfaceList);
				retlist.add((long) (ret == true ? 1 : 0));
			}
		}
		return retlist;
	}

	/**
	 * 更新变化字段实体列表
	 * 
	 * @param entityList
	 * @return
	 */
	public static List<Long> updateChangedFieldEntityList(RedisService redisService, List<AbstractEntity> entityList) {
		boolean ret = false;
		List<Long> retlist = new ArrayList<>();
		if (entityList.size() > 0) {
			AbstractEntity entity = entityList.get(0);
			if (entity != null) {
				if (entity instanceof RedisInterface) {
					for (AbstractEntity abstractEntity : entityList) {
						ret = updateChangedFieldEntity(redisService, entity);
						retlist.add((long) (ret == true ? 1 : 0));
					}
				} else if (entity instanceof RedisListInterface) {
					List<RedisListInterface> redisListInterfaceList = new ArrayList<>();
					for (AbstractEntity abstractEntity : entityList) {
						RedisListInterface redisListInterface = (RedisListInterface) abstractEntity;
						redisListInterfaceList.add(redisListInterface);
					}
					ret = redisService.setListToHash(EntityUtils.getRedisListKey((RedisListInterface) entity),
							redisListInterfaceList);
					retlist.add((long) (ret == true ? 1 : 0));
				}
			}
		}
		return retlist;
	}

	// 删除实体列表
	public static List<Long> deleteEntityList(RedisService redisService, List<AbstractEntity> entityList) {
		boolean ret = false;
		List<Long> retlist = new ArrayList<>();
		if (entityList.size() > 0) {
			AbstractEntity entity = entityList.get(0);
			if (entity != null) {
				if (entity instanceof RedisInterface) {
					for (AbstractEntity abstractEntity : entityList) {
						ret = deleteEntity(redisService, abstractEntity);
						retlist.add((long) (ret == true ? 1 : 0));
					}
				} else if (entity instanceof RedisListInterface) {
					List<String> redisListInterfaceList = new ArrayList<>();
					for (AbstractEntity abstractEntity : entityList) {
						RedisListInterface redisListInterface = (RedisListInterface) abstractEntity;
						redisListInterfaceList.add(redisListInterface.getSubUniqueKey());
					}
					Long delret = redisService.hdel(EntityUtils.getRedisListKey((RedisListInterface) entity),
							redisListInterfaceList.toArray(new String[0]));
					retlist.add(delret);
				}
			}
		}
		return retlist;
	}
}
