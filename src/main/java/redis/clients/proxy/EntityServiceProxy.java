package redis.clients.proxy;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import redis.clients.common.utils.Loggers;
import redis.clients.common.utils.EntityUtils;
import redis.clients.common.annotation.DbOperation;
import redis.clients.common.constant.DbOperationEnum;
import redis.clients.entity.AbstractEntity;
import redis.clients.entity.EntityService;
import redis.clients.redis.RedisInterface;
import redis.clients.redis.RedisListInterface;
import redis.clients.redis.RedisService;

/**
 * Created by qq24139297 on 2017/3/23. 实体存储服务代理 同步存储
 *
 * 存储策略为 insert的时候插入db,然后更新缓存。query的时候优先缓存，找不到的时候查询db,更新缓存。delete的时候删除db，删除缓存，
 * useRedisFlag 为是否使用缓存redis标志
 */
public class EntityServiceProxy<T extends EntityService> implements MethodInterceptor {

	private static final Logger proxyLogger = Loggers.dbServiceProxyLogger;

	@Autowired
	private RedisService redisService;

	private boolean useRedisFlag;

	public EntityServiceProxy(RedisService redisService, boolean useRedisFlag) {
		this.redisService = redisService;
		this.useRedisFlag = useRedisFlag;
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
		Object result = null;
		boolean ret = false;
		List<Long> retlist = new ArrayList<>();
		AbstractEntity abstractEntity = null;
		List<AbstractEntity> entityList = null;
		if (!useRedisFlag) { // 如果使用redis，直接进行返回
			result = methodProxy.invokeSuper(obj, args);
			return result;
		} 
		else {
			// 进行数据库操作
			DbOperation dbOperation = method.getAnnotation(DbOperation.class);
			if (dbOperation == null) { // 如果没有进行注解，直接进行返回
				result = methodProxy.invokeSuper(obj, args);
				return result;
			}
			DbOperationEnum dbOperationEnum = dbOperation.operation();
			switch (dbOperationEnum) {
			case insert:
				result = methodProxy.invokeSuper(obj, args);
				abstractEntity = (AbstractEntity) args[0];
				if(abstractEntity != null) {
					ret = EntityUtils.updateAllFieldEntity(redisService, abstractEntity);
					result = ret ? 1:result;
				}
				break;
			case insertBatch:
				result = methodProxy.invokeSuper(obj, args);
				entityList = (List<AbstractEntity>) args[0];
				if(entityList != null) {
					retlist = EntityUtils.updateAllFieldEntityList(redisService, entityList);
					if (((retlist != null)&&(retlist.size() > 0))
							&&((result == null)||((List<Long>)result).size() == 0)) {
						result = retlist; 
					}
				}
				break;
			case update:
				result = methodProxy.invokeSuper(obj, args);
				abstractEntity = (AbstractEntity) args[0];
				if(abstractEntity != null) {
					ret = EntityUtils.updateChangedFieldEntity(redisService, abstractEntity);
					result = ret ? true:result;
				}
				break;
			case updateBatch:
				result = methodProxy.invokeSuper(obj, args);
				entityList = (List<AbstractEntity>) args[0];
				if(entityList != null) {
					retlist = EntityUtils.updateChangedFieldEntityList(redisService, entityList);
					if (((retlist != null)&&(retlist.size() > 0))
							&&((result == null)||((List<Long>)result).size() == 0)) {
						result = retlist; 
					}
				}
				break;
			case delete:
				result = methodProxy.invokeSuper(obj, args);
				abstractEntity = (AbstractEntity) args[0];
				if(abstractEntity != null) {
					ret = EntityUtils.deleteEntity(redisService, abstractEntity);
					result = ret ? true:result;
				}
				break;
			case deleteBatch:
				result = methodProxy.invokeSuper(obj, args);
				entityList = (List<AbstractEntity>) args[0];
				if(entityList != null) {
					retlist = EntityUtils.deleteEntityList(redisService, entityList);
					if (((retlist != null)&&(retlist.size() > 0))
							&&((result == null)||((List<Long>)result).size() == 0)) {
						result = retlist; 
					}
				}
				break;
			case query:
				abstractEntity = (AbstractEntity) args[0];
				if (abstractEntity != null) {
					if (abstractEntity instanceof RedisInterface) {
						RedisInterface redisInterface = (RedisInterface) abstractEntity;
						result = redisService.getObjectFromHash(EntityUtils.getRedisKey(redisInterface),
								abstractEntity.getClass());
					} else if (abstractEntity instanceof RedisListInterface) {
						RedisListInterface redisInterface = (RedisListInterface) abstractEntity;
						result = redisService.getObjectFromList(EntityUtils.getRedisListKey(redisInterface),
								redisInterface.getSubUniqueKey(), abstractEntity.getClass());
					} else {
						proxyLogger.error(
								"EntityServiceProxy query interface error! " 
								+ abstractEntity.getClass().getSimpleName()
								+ " abstractEntity: " + abstractEntity.toString());
					}
				}
				if (result == null) {
					result = methodProxy.invokeSuper(obj, args);
					if (result != null) {
						abstractEntity = (AbstractEntity) result;
						ret = EntityUtils.updateAllFieldEntity(redisService, abstractEntity);
					}
				}
				break;
			case queryList:
				abstractEntity = (AbstractEntity) args[0];
				if (abstractEntity != null) {
					if (abstractEntity instanceof RedisListInterface) {
						RedisListInterface redisInterface = (RedisListInterface) abstractEntity;
						result = redisService.getListFromHash(EntityUtils.getRedisListKey(redisInterface),
								abstractEntity.getClass());
						// if (result != null) {
						// result = filterEntity((List<IEntity>) result, abstractEntity);
						// }
					}
					//else {
					//	proxyLogger.error("EntityServiceProxy query RedisListInterface Error! "
					//			+ abstractEntity.getClass().getSimpleName() + " use RedisInterface "
					//			+ abstractEntity.toString());
					//}
				}
				if (result == null) {
					result = methodProxy.invokeSuper(obj, args);
					if (result != null) {
						retlist = EntityUtils.updateAllFieldEntityList(redisService, (List<AbstractEntity>) result);
					}
				}
				break;
				default:
					break;
			}
		}
		return result;
	}

	/**
	 * 根据封装的条件判断查找出相同的对象
	 * 
	 * @param list
	 * @param abstractEntity
	 * @return
	 */
//	public List<IEntity> filterEntity(List<IEntity> list, AbstractEntity abstractEntity) {
//		List<IEntity> result = new ArrayList<>();
//		// 开始进行filter
//		EntityProxyWrapper entityProxyWrapper = abstractEntity.getEntityProxyWrapper();
//		if (entityProxyWrapper != null) {
//			EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
//			if (entityProxy != null) {
//				Map<String, Object> changeParamSet = entityProxy.getChangeParamSet();
//				if (changeParamSet != null) {
//					for (IEntity iEntity : list) {
//						boolean equalFlag = false;
//						for (String fieldName : changeParamSet.keySet()) {
//							String value = ObjectUtils.getFieldsValueStr(iEntity, fieldName);
//
//							Object object = changeParamSet.get(fieldName);
//							if (value.equals(object.toString())) {
//								equalFlag = true;
//							} else {
//								equalFlag = false;
//								break;
//							}
//						}
//						if (equalFlag) {
//							result.add(iEntity);
//						}
//					}
//				}
//			}
//		}
//		return result;
//	}
}
