package redis.clients.entity;

import redis.clients.proxy.EntityProxyWrapper;

/**
 * Created by qq24139297 on 17/3/16.
 */
public abstract class AbstractEntity implements IEntity, AsyncSave {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6354364334506583280L;
	/**
	 * 
	 */
	// 用于记录数据库封装对象
	private EntityProxyWrapper entityProxyWrapper;

	@Override
	public EntityProxyWrapper getEntityProxyWrapper() {
		return entityProxyWrapper;
	}

	@Override
	public void setEntityProxyWrapper(EntityProxyWrapper entityProxyWrapper) {
		this.entityProxyWrapper = entityProxyWrapper;
	}
}
