/**
 * UserEntity.java
 *
 *
 * $LastChangedBy:  $
 * $LastChangedDate:  $
 * $Revision:  $
 */
package redis.clients.entity;

import redis.clients.mapper.UserEntityMapper;
import redis.clients.common.annotation.DbMapper;
import redis.clients.common.annotation.FieldSave;
import redis.clients.common.annotation.FieldSave2Redis;
import redis.clients.common.annotation.MethodSaveProxy;
import redis.clients.entity.BaseStringIDEntity;
import redis.clients.redis.RedisInterface;
import redis.clients.redis.RedisKeyEnum;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.Date;

/**
 * Domain object class for UserEntity
 *
 * @author CodeMaster v1.0
 */
@DbMapper(mapper = UserEntityMapper.class)
public class UserEntity extends BaseStringIDEntity implements RedisInterface, Cloneable {
	/**
	 * 默认生成的账号实体的ID，每次创建自动加 1 *
	 */
	@FieldSave
	private long id;
	/**
	 * 创建唯一关键字索引UUID *
	 */
	@FieldSave
	private String uid;
	/**
	 * 该实体是否在过有效期后需要需要自动从缓存中删除 *
	 */
	@FieldSave
	private boolean deleted;
	/**
	 *  设置有效期时间，默认从当前创建时间加24小时合86400秒 *
	 */
	@FieldSave
	private Date deleteTime;
	/**
	 * 玩家账号(可能是手机号) *
	 */
	@FieldSave
	private String acc;
	/**
	 * 玩家名称 *
	 */
	@FieldSave
	private String name;
	/**
	 * 玩家外形 * 
	 */
	@FieldSave
	private String appearance;
	/**
	 * 玩家性别 1 男性 0 女性 * 
	 */
	@FieldSave
	private int sex;
	/**
	 * 玩家等级 * 
	 */
	@FieldSave
	private int level;
	/**
	 * 1.经验 * 
	 */
	@FieldSave
	private int experience;
	/**
	 * 2.金币 * 
	 */
	@FieldSave
	private int goldCoin;
	/**
	 * 3.钻石 * 
	 */
	@FieldSave
	private int diamonds;
	/**
	 * 4.礼券* 
	 */
	@FieldSave
	private int giftCert;
	/**
	 * 5.功勋* 
	 */
	@FieldSave
	private int feats;

	/**
	 * 玩家武器设置 * 
	 */
	@FieldSave
	private int weaponSet;
	/**
	 * 玩家账号创建时间 * 
	 */
	@FieldSave
	private long createTime;
	/**
	 * 玩家账号最后一次登陆时间 * 
	 */
	@FieldSave
	private long lastLoginTime;

	/**
	 * 关卡或任务ID *
	 */
	@FieldSave2Redis
	private int missionId = 0;
	/**
	 * 默认生成的队伍ID，每次创建自动加 1 *
	 */
	@FieldSave2Redis
	private long teamId = 0;
	
	@FieldSave
	private long version;
	
	public UserEntity() {
		init();

	}
	
	public UserEntity(long id) {
		this.id = id;
		init();
	}
	
	public UserEntity(String uid) {
		this.uid = uid;
		init();
	}
	/** Gets */
	@Override
	public long getId() {
		return this.id;
	}

	/** Gets */
	@Override
	public String getUid() {
		return this.uid;
	}

	/** Gets */
	@Override
	public boolean isDeleted() {
		return this.deleted;
	}

	/** Gets */
	@Override
	public Date getDeleteTime() {
		return this.deleteTime;
	}

	/** Gets */
	public String getAcc() {
		return this.acc;
	}

	/** Gets */
	public int getSex() {
		return this.sex;
	}

	/** Gets */
	public String getName() {
		return this.name;
	}

	/** Gets */
	public String getAppearance() {
		return this.appearance;
	}

	/** Gets */
	public int getLevel() {
		return this.level;
	}

	/** Gets */
	public int getExperience() {
		return experience;
	}

	/** Gets */
	public int getGoldCoin() {
		return goldCoin;
	}

	/** Gets */
	public int getDiamonds() {
		return diamonds;
	}

	/** Gets */
	public int getGiftCert() {
		return giftCert;
	}

	/** Gets */
	public int getFeats() {
		return feats;
	}

	/** Gets */
	public int getWeaponSet() {
		return this.weaponSet;
	}

	/** Gets */
	public long getCreateTime() {
		return this.createTime;
	}

	/** Gets */
	public long getLastLoginTime() {
		return this.lastLoginTime;
	}

	/** Gets */
	public int getMissionId() {
		return this.missionId;
	}
	
	/** Gets */
	public long getTeamId() {
		return this.teamId;
	}
	
	/** Gets */
	@Override
	public long getVersion() {
		return version;
	}
	
	/** Initializes the values */
	public void init() {
		if (this.id == 0) {
			this.id = super.getId();
		}
		if (this.uid == null || this.uid == "") {
			this.uid = super.getUid();
		}
		this.deleted = false;
		this.deleteTime = new Date();
		this.acc = "";
		this.name = "";
		this.appearance = "";
		this.sex = 0;
		this.level = 0;
		this.experience = 0;
		this.goldCoin = 0;
		this.diamonds = 0;
		this.giftCert = 0;
		this.feats = 0;
		this.weaponSet = 0;
		this.createTime = System.currentTimeMillis();
		this.lastLoginTime = System.currentTimeMillis();
		this.missionId = 0;
		this.teamId = 0;
		this.version = 0;
	}

	/** Sets */
	@Override
	@MethodSaveProxy(proxy = "id")
	public void setId(long id) {
		this.id = id;
	}

	/** Sets */
	@Override
	@MethodSaveProxy(proxy = "uid")
	public void setUid(String uid) {
		this.uid = uid;
	}

	/** Sets */
	@Override
	@MethodSaveProxy(proxy = "deleted")
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/** Sets */
	@Override
	@MethodSaveProxy(proxy = "deleteTime")
	public void setDeleteTime(Date deleteTime) {
		this.deleteTime = deleteTime;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "acc")
	public void setAcc(String acc) {
		this.acc = acc;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "name")
	public void setName(String name) {
		this.name = name;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "appearance")
	public void setAppearance(String appearance) {
		this.appearance = appearance;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "sex")
	public void setSex(int sex) {
		this.sex = sex;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "level")
	public void setLevel(int level) {
		this.level = level;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "experience")
	public void setExperience(int experience) {
		this.experience = experience;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "goldCoin")
	public void setGoldCoin(int goldCoin) {
		this.goldCoin = goldCoin;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "diamonds")
	public void setDiamonds(int diamonds) {
		this.diamonds = diamonds;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "giftCert")
	public void setGiftCert(int giftCert) {
		this.giftCert = giftCert;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "feats")
	public void setFeats(int feats) {
		this.feats = feats;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "weaponSet")
	public void setWeaponSet(int weaponSet) {
		this.weaponSet = weaponSet;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "createTime")
	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	/** Sets */
	@MethodSaveProxy(proxy = "lastLoginTime")
	public void setLastLoginTime(long lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}
	
	/** Sets */
	@MethodSaveProxy(proxy = "missionId")
	public void setMissionId(int missionId) {
		this.missionId = missionId;
	}
	
	/** Sets */
	@MethodSaveProxy(proxy = "teamId")
	public void setTeamId(long teamId) {
		this.teamId = teamId;
	}

	/** Sets */
	@Override
	@MethodSaveProxy(proxy = "version")
	public void setVersion(long version) {
		this.version = version;
	}
	/** depth clone **/
	public Object clone() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(this);
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			return ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Returns the String representation */
	public String toString() {
		return "(UserEntity) " 
				+ "id='" + id + "', "
				+ "uid='" + uid + "', "
				+ "acc='" + acc + "', "
				+ "name='" + name + "', "
				+ "appearance='" + appearance + "', "
				+ "sex='" + sex	+ "', "
				+ "level='" + level + "', "
				+ "experience='" + experience + "', "
				+ "goldCoin='" + goldCoin + "', "
				+ "diamonds='" + diamonds + "', "
				+ "giftCert='" + giftCert + "', "
				+ "feats='" + feats + "', "
				+ "weaponSet='" + weaponSet + "', "
				+ "createTime='" + createTime + "', "
				+ "lastLoginTime='" + lastLoginTime + "', "
				+ "missionId='" + missionId + "',"
				+ "teamId='" + teamId + "',"
				+ "version='" + version + "'";
	}

	/** Returns the CSV String */
	public String toCSVLine() {
		return "\"" 
				+ id + "\",\""
				+ uid + "\",\""
				+ acc + "\",\""
				+ name + "\",\""
				+ appearance + "\",\""
				+ sex + "\",\""
				+ level + "\",\""
				+ experience + "\",\""
				+ goldCoin + "\",\""
				+ diamonds + "\",\""
				+ giftCert + "\",\""
				+ feats + "\",\""
				+ weaponSet + "\",\""
				+ createTime + "\",\""
				+ lastLoginTime + "\",\""
				+ missionId + "\",\""
				+ teamId + "\",\""
				+ version + "\"";
	}

	@Override
	public String getUnionKey() {
		return String.valueOf(getAcc());
	}

	@Override
	public String getRedisKeyEnumString() {
		return RedisKeyEnum.USER.getKey();
	}

}