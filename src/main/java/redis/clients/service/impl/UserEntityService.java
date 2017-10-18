package redis.clients.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.entity.EntityService;
import redis.clients.entity.UserEntity;
import redis.clients.service.IUserEntityService;
import redis.clients.sharding.EntityServiceShardingStrategy;

/**
 * Created by qq24139297 on 17/3/20.
 */
@Service
public class UserEntityService extends EntityService<UserEntity> implements IUserEntityService {
	final static Logger logger = LoggerFactory.getLogger(UserEntityService.class);

	@Override
	public long insertUser(UserEntity entity) {
		try {
			return insertEntity(entity);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.toString());
		}
		return -1;
	}

	@Override
	public UserEntity getUser(String acc) {
		try {
			if(acc==null || acc.isEmpty()) {
				logger.error("acc is Empty!");
				return null;
			}
			UserEntity entity = new UserEntity();
			entity.setUid("");
			entity.setAcc(acc);
			return (UserEntity) getEntity(entity);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.toString());
		}
		return null;
	}

	@Override
	public List<UserEntity> getUserList() {
		List<UserEntity> retList = new ArrayList<>();
		UserEntity entity = new UserEntity();
		try {
			entity.setUid("");
			entity.setAcc("");
			retList = (List<UserEntity>) getEntityList(entity);
			return retList;
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.toString());
		}
		return null;
	}

	@Override
	public boolean updateUser(UserEntity entity) {
		try {
			entity.setVersion(entity.getVersion()+1);
			return updateEntity(entity);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.toString());
		}
		return false;
	}

	@Override
	public boolean deleteUser(UserEntity entity) {
		try {
			return deleteEntity(entity);
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.toString());
		}
		return false;
	}

	@Override
	public EntityServiceShardingStrategy getEntityServiceShardingStrategy() {
		return getDefaultEntityServiceShardingStrategy();
	}
}
