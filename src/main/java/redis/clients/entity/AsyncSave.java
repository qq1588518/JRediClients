package redis.clients.entity;

import redis.clients.proxy.EntityProxyWrapper;

/**
 * Created by qq24139297 on 17/3/29. 
 * 异步存储
 */
public interface AsyncSave {
	// 用于记录数据库封装对象
	public EntityProxyWrapper getEntityProxyWrapper();

	public void setEntityProxyWrapper(EntityProxyWrapper entityProxyWrapper);
}
