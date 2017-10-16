package redis.clients.entity;

import java.util.List;
import redis.clients.entity.AbstractEntity;
import redis.clients.entity.IEntity;

/**
 * Created by qq24139297 on 2017/3/23. 
 * 基础服务
 */
public interface IEntityService<T extends AbstractEntity> {
	/**
	 * 插入实体
	 * 
	 * @param entity
	 * @return
	 */
	public long insertEntity(T entity);

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @return
	 */
	public IEntity getEntity(T entity);

	/**
	 * 查询实体列表
	 * 
	 * @param entity
	 *            需要实现代理，才能拼写sql map
	 * @return
	 */
	public List<T> getEntityList(T entity);

	/**
	 * 更新实体
	 * 
	 * @param entity
	 *            需要实现代理
	 * @return
	 */
	public boolean updateEntity(T entity);

	/**
	 * 删除实体
	 * 
	 * @param entity
	 */
	public boolean deleteEntity(T entity);

	/**
	 * 批量插入实体列表
	 * 
	 * @param entityList
	 * @return
	 */
	public List<Long> insertEntityBatch(List<T> entityList);

	/**
	 * 批量更新实体列表
	 * 
	 * @param entityList
	 */
	public List<Long> updateEntityBatch(List<T> entityList);

	/**
	 * 批量删除实体列表
	 * 
	 * @param entityList
	 */
	public List<Long> deleteEntityBatch(List<T> entityList);

	/**
	 * 获取sharding后的结果
	 * 
	 * @param entity
	 * @return
	 */
	public long getShardingId(T entity);
}
