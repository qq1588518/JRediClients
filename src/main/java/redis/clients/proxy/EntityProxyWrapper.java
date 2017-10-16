package redis.clients.proxy;

import redis.clients.entity.AbstractEntity;

/**
 * Created by qq24139297 on 2017/3/16. 
 * 代理封装
 */
public class EntityProxyWrapper<T extends AbstractEntity> {

	private EntityProxy entityProxy;

	public EntityProxyWrapper(EntityProxy entityProxy) {
		this.entityProxy = entityProxy;
	}

	public EntityProxy getEntityProxy() {
		return entityProxy;
	}

	public void setEntityProxy(EntityProxy entityProxy) {
		this.entityProxy = entityProxy;
	}
}
