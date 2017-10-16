package redis.clients.service;

import java.util.List;
import redis.clients.entity.UserEntity;

/**
 * Created by qq24139297 on 17/3/20.
 */
public interface IUserEntityService {

	public long insertUser(UserEntity entity);

	public UserEntity getUser(String acc);

	public List<UserEntity> getUserList();

	public boolean updateUser(UserEntity entity);

	public boolean deleteUser(UserEntity entity);

}
