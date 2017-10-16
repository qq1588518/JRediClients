package redis.clients.entity;

import java.util.Map;
import java.util.List;
import redis.clients.entity.IEntity;
import org.apache.ibatis.session.RowBounds;

/**
 * Created by qq24139297 on 17/3/21. 
 * 基础mapper
 */

public interface IDBMapper<T extends IEntity> {

	public long insertEntity(T entity);

	public IEntity getEntity(T entity);
	
	public IEntity getEntityByUID(T entity);
	
	public List<T> getEntityList(T entity);

	public List<T> getEntityList(T entity, RowBounds rowBounds);

	public void updateEntityByMap(Map map);

	public void deleteEntity(T entity);

	public void deleteEntityByUID(T entity);
	
	/**
	 * 直接查找db，无缓存
	 * 
	 * @param map
	 * @return
	 */
	//public List<T> filterList(Map map);
	//public List<T> filterList(Map map, RowBounds rowBounds);

}
