package redis.clients.entity;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jiangwenping on 17/4/5.
 */
public class BaseLongIDEntity extends AbstractEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3100119277253481640L;
	private static Logger logger = LoggerFactory.getLogger(BaseLongIDEntity.class);
	/*
	 * beanId的后两位用于区分对Bean做的操作， 00为插入；01为更新；10为删除， 每次生成一个新对象时会把beanId加4
	 */
	private static long beanId = 0l;
	
	public BaseLongIDEntity() {
		if (beanId <= 0x0ffffffffffffffbl) {
			beanId++;
		} else {
			logger.error("BeanId of type long is out of range. ");
			beanId = 0;
		}
	}

	@Override
	public long getId() {
		return beanId;
	}

	@Override
	public void setId(long id) {

	}

	@Override
	public String getUid() {
		return "";
	}

	@Override
	public void setUid(String uid) {
		
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public void setDeleted(boolean deleted) {
		
	}

	@Override
	public Date getDeleteTime() {
		return null;
	}

	@Override
	public void setDeleteTime(Date deleteTime) {
		
	}

	@Override
	public long getVersion() {
		return 0;
	}

	@Override
	public void setVersion(long version) {
		
	}

}
