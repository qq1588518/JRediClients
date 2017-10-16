package redis.clients.entity;

import java.util.Date;
import java.io.Serializable;

/**
 * Created by qq24139297 on 17/3/16. 基本的数据存储对象
 * 所有对象都是先设置删除时间，删除标志，设置缓存，然后同步db异步操作执行删除后，执行回调，然后删除缓存
 * 缓存获取对象的时候，需要过滤已经删除的对象
 */
public interface IEntity extends Serializable {
	public long getId();

	public void setId(long id);

	public String getUid();

	public void setUid(String uid);

	public boolean isDeleted();

	public void setDeleted(boolean deleted);

	public Date getDeleteTime();

	public void setDeleteTime(Date deleteTime);
}
