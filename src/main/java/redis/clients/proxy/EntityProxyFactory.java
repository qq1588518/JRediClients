package redis.clients.proxy;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.stereotype.Service;
import redis.clients.entity.AbstractEntity;
import redis.clients.entity.IEntity;

/**
 * Created by qq24139297 on 2017/3/16. 
 * 实体代理服务
 */
@Service
public class EntityProxyFactory {

	private EntityProxy createProxy(IEntity entity) {
		return new EntityProxy(entity);
	}

	@SuppressWarnings("unchecked")
	private <T extends IEntity> T createProxyEntity(EntityProxy entityProxy) {
		if (entityProxy != null) {
			Enhancer enhancer = new Enhancer();
			if (enhancer != null) {
				// 设置需要创建子类的类
				enhancer.setSuperclass(entityProxy.getEntity().getClass());
				enhancer.setCallback(entityProxy);
				// 通过字节码技术动态创建子类实例
				return (T) enhancer.create();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends IEntity> T createProxyEntity(T entity) throws Exception {
		EntityProxy entityProxy = createProxy(entity);
		if (entityProxy != null) {
			EntityProxyWrapper entityProxyWrapper = new EntityProxyWrapper(entityProxy);			
			AbstractEntity proxyEntity = createProxyEntity(entityProxy);
			if(proxyEntity!=null) {
				// 注入对象 数值
				BeanUtils.copyProperties(proxyEntity, entity);
				entityProxy.setCollectFlag(true);
				if(entityProxyWrapper != null) {
					proxyEntity.setEntityProxyWrapper(entityProxyWrapper);
					return (T) proxyEntity;
				}
			}
		}
		return null;
	}
}
