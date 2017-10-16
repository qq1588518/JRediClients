package redis.clients.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import redis.clients.common.utils.Loggers;
import redis.clients.common.utils.EntityUtils;
import redis.clients.common.utils.ObjectUtils;
import redis.clients.common.utils.EntityUtils.EntitySaveEnum;
import redis.clients.common.annotation.MethodSaveProxy;
import redis.clients.entity.IEntity;

/**
 * Created by qq24139297 on 17/3/16. db存储的实体代理对象
 */
public class EntityProxy<T extends IEntity> implements MethodInterceptor {

	private Logger logger = Loggers.dbProxyLogger;
	// 实体对象
	private T entity;

	// 是否需要存储
	private boolean dirtyFlag;

	// 那些字段存在变化
	private Map<String, Object> changeParamSet;

	// 初始化标志，只有初始化之后才会采集变化字段
	private boolean collectFlag;

	public EntityProxy(T entity) {
		this.changeParamSet = new ConcurrentHashMap<>();
		this.entity = entity;
	}

	// 实现MethodInterceptor接口方法
	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
		// 通过代理类调用父类中的方法
		Object result = null;
		if (!collectFlag) {
			result = methodProxy.invokeSuper(obj, args);
		} else {
			// 检查MethodProxy注解
			MethodSaveProxy methodSaveProxyAnnotation = (MethodSaveProxy) method.getAnnotation(MethodSaveProxy.class);
			if (methodSaveProxyAnnotation != null) {
				// 检查对象原来数值
				String filedName = methodSaveProxyAnnotation.proxy();
				Object oldObject = ObjectUtils.getFieldsValueObj(entity, filedName);
				// 获取新参数
				Object newObject = args[0];
				result = methodProxy.invokeSuper(obj, args);
				if ((oldObject == null) || (!oldObject.equals(newObject))) {
					dirtyFlag = true;
					changeParamSet.put(filedName, newObject);
					if (logger.isDebugEnabled()) {
						logger.debug(filedName + "更新替换前为" + oldObject);
						logger.debug(filedName + "更新替换后为" + newObject);
					}
				}
			} else {
				result = methodProxy.invokeSuper(obj, args);
			}
		}

		return result;
	}

	public IEntity getEntity() {
		return entity;
	}

	public void setEntity(T entity) {
		this.entity = entity;
	}

	public boolean isDirtyFlag() {
		return dirtyFlag;
	}

	public void setDirtyFlag(boolean dirtyFlag) {
		this.dirtyFlag = dirtyFlag;
	}

	/*
	 * type == 0 获取所有需要同时保存到Redis和DB数据库的字段 type == 1 获取所有需要同时保存到DB数据库的字段 type == 2
	 * 获取所有需要同时保存到Redis的字段
	 */
	public Map<String, Object> getChangeParamSet(EntitySaveEnum type) {
		Map<String, Object> map = new HashMap<>();
		Field[] fields = null;
		if (changeParamSet != null && (!changeParamSet.isEmpty())) {
			fields = EntityUtils.getAllCacheFields(this.getEntity(), type);
			for (Field field : fields) {
				String key = field.getName();
				if (key == null || key.isEmpty()) {
					continue;
				}
				if (changeParamSet.containsKey(key)) {
					map.put(key, changeParamSet.get(key));
				}
			}
		}
		return map;
	}

	public void setChangeParamSet(Map<String, Object> changeParamSet) {
		this.changeParamSet = changeParamSet;
	}

	public boolean isCollectFlag() {
		return collectFlag;
	}

	public void setCollectFlag(boolean collectFlag) {
		this.collectFlag = collectFlag;
	}
}
