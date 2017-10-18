package redis.clients.entity;

import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sunmosh on 2017/4/5.
 */

public class BaseStringIDEntity extends AbstractEntity {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5072464272846321687L;
	private static Logger logger = LoggerFactory.getLogger(BaseStringIDEntity.class);
	/*
	 * beanId的后两位用于区分对Bean做的操作， 00为插入；01为更新；10为删除， 每次生成一个新对象时会把beanId加4
	 */

    private String uid;

	public BaseStringIDEntity() {
		String guid = UUID.randomUUID().toString();
		uid = guid.substring(0, 8) + guid.substring(9, 13) + guid.substring(14, 18) + guid.substring(19, 23)
				+ guid.substring(24);
		//logger.error("BeanId of type long is out of range. ");
	}

	@Override
	public long getId() {
		return 0;
	}

	@Override
	public void setId(long id) {

	}

	@Override
	public String getUid() {
		return uid;
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
