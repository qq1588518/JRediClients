package redis.clients.entity;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import redis.clients.common.annotation.DbMapper;
import redis.clients.common.annotation.DbOperation;
import redis.clients.common.constant.DbOperationEnum;
import redis.clients.common.utils.Loggers;
import redis.clients.common.utils.EntityUtils.EntitySaveEnum;
import redis.clients.entity.AbstractEntity;
import redis.clients.entity.IEntity;
import redis.clients.proxy.EntityProxy;
import redis.clients.proxy.EntityProxyWrapper;
import redis.clients.sharding.CustomerContextHolder;
import redis.clients.sharding.EntityServiceShardingStrategy;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import java.lang.reflect.ParameterizedType;
import com.github.pagehelper.PageRowBounds;




/**
 * Created by qq24139297 on 17/3/21. 模版实体数据提服务 批量应该保证它们在同一个数据库中
 */
public abstract class EntityService<T extends AbstractEntity> implements IEntityService<T> {

	private static final Logger logger = Loggers.dbLogger;

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Autowired
	private SqlSessionTemplate sqlSessionBatchTemplate;

	@Autowired
	private EntityServiceShardingStrategy defaultEntityServiceShardingStrategy;

	private static ThreadLocal<SqlSession> threadLocal = new ThreadLocal<SqlSession>();

	/**
	 * 插入实体
	 *
	 * @param entity
	 * @return
	 */
	@Override
	@DbOperation(operation = DbOperationEnum.insert)
	public long insertEntity(T entity) {
		long result = -1;
		IDBMapper<T> idbMapper = getTemplateMapper(entity);
		if (idbMapper != null) {
			long selectId = getShardingId(entity);
			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
			// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
			try {
				result = idbMapper.insertEntity(entity);
			} catch (Exception e) {
				logger.error("EntityService.insertEntity Error! entity={} Exception err={}" , entity.toString() , e.toString());
			} finally {
			}
		}
		return result;
	}
	
	@Override
	@DbOperation(operation = DbOperationEnum.insertBatch)
	public List<Long> insertEntityBatch(List<T> entityList) {
		List<Long> result = new ArrayList<>();
		SqlSession sqlSession = getBatchSession();
		try {
			for (T entity : entityList) {
				IDBMapper<T> idbMapper = getBatchTemplateMapper(sqlSession, entity);
				if(idbMapper!=null) {
					long selectId = getShardingId(entity);
					CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
					// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
					long insertReuslt = idbMapper.insertEntity(entity);
					result.add(insertReuslt);
				}
			}
			commitBatchSession();
		} catch (Exception e) {
			logger.error("EntityService.insertEntityBatch Error! entityList={} Exception err={}" , entityList.toString() , e.toString());
			rollbackBatchSession();
		} finally {
			closeBatchSession();
		}
		return result;
	}
	
	/**
	 * 查询实体
	 *
	 * @return
	 */
	@Override
	@DbOperation(operation = DbOperationEnum.query)
	public IEntity getEntity(T entity) {
		IEntity result = null;
		IDBMapper<T> idbMapper = getTemplateMapper(entity);
		if (idbMapper != null) {
			long selectId = getShardingId(entity);
			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
			// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
			String uid = entity.getUid();
			try {
				if(uid == null || uid.isEmpty()) {
					result = idbMapper.getEntity(entity);
				}else {
					result = idbMapper.getEntityByUID(entity);
				}
			} catch (Exception e) {
				logger.error("EntityService.getEntity Error! entity={} Exception err={}" , entity.toString() , e.toString());
			} finally {
			}
		}
		return result;
	}

	@Override
	@DbOperation(operation = DbOperationEnum.queryList)
	public List<T> getEntityList(T entity) {
		List<T> result = null;
		IDBMapper<T> idbMapper = getTemplateMapper(entity);
		if (idbMapper != null) {
			long selectId = getShardingId(entity);
			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
			// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
			EntityServiceShardingStrategy entityServiceShardingStrategy = getDefaultEntityServiceShardingStrategy();
			try {
				if (!entityServiceShardingStrategy.isPageFlag()) {
					result = idbMapper.getEntityList(entity);
				} else {
					int pageLimit = entityServiceShardingStrategy.getPageLimit();
					PageRowBounds pageRowBounds = new PageRowBounds(0, pageLimit);
					result = idbMapper.getEntityList(entity, pageRowBounds);
					long count = pageRowBounds.getTotal().longValue();
					if (count > pageLimit) {
						int offset = pageLimit;
						while (offset < count) {
							pageRowBounds = new PageRowBounds(offset, pageLimit);
							result.addAll(idbMapper.getEntityList(entity, pageRowBounds));
							offset += pageLimit;
						}
					}
				}
	
			} catch (Exception e) {
				logger.error("EntityService.getEntityList Error! entity={} Exception err={}" , entity.toString() , e.toString());
			} finally {
			}
		}
		return result;
	}


	/**
	 * 修改实体
	 *
	 * @param entity
	 */
	@Override
	@DbOperation(operation = DbOperationEnum.update)
	public boolean updateEntity(T entity) {
		IDBMapper<T> idbMapper = getTemplateMapper(entity);
		if (idbMapper != null) {
			long selectId = getShardingId(entity);
			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
			// int sharding_table_index = getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId);
			Map<Object, Object> hashMap = new HashMap<>();
			// hashMap.put("sharding_table_index", sharding_table_index);
			hashMap.put("id", entity.getId());
			hashMap.put("uid", entity.getUid());
			EntityProxyWrapper entityProxyWrapper = entity.getEntityProxyWrapper();
			// 只有数据变化的时候才会更新
			if (entityProxyWrapper != null) {
				EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
				if (entityProxy != null && entityProxy.isDirtyFlag()) {
					Map<String, Object> changeParamSet = entityProxy.getChangeParamSet(EntitySaveEnum.Save2DB);
					if (changeParamSet != null && (!changeParamSet.isEmpty())) {
						hashMap.putAll(changeParamSet);
						if (hashMap != null && hashMap.size() > 0) {
							try {
								idbMapper.updateEntityByMap(hashMap);
								return true;
							} catch (Exception e) {
								logger.error("EntityService.updateEntityByMap Error! entity={} hashMap={} Exception err={}",entity.toString(),hashMap.toString(),e.toString());
								return false;
							} finally {
							}
						} else {
							logger.info("EntityService.updateEntity not excute! hashMap is NULL nothing is changed! entity={}",entity.toString());
						}
					}
				} else {
					logger.info("EntityService.updateEntity not excute! entityProxy is NULL or is DirtyFlag nothing is changed! entity={}",entity.toString());
				}
			} else {
				logger.error("EntityService.updateEntity Error! entityProxyWrapper is NULL! entity={}",entity.toString());
			}
		}
		return false;
	}
	
	@Override
	@DbOperation(operation = DbOperationEnum.updateBatch)
	public List<Long> updateEntityBatch(List<T> entityList) {
		List<Long> result = new ArrayList<>();
		SqlSession sqlSession = getBatchSession();
		try {
			for (T entity : entityList) {
				IDBMapper<T> idbMapper = getBatchTemplateMapper(sqlSession, entity);
				if(idbMapper != null) {
					long selectId = getShardingId(entity);
					CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
					// int sharding_table_index = getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId);
					// entity.setSharding_table_index(sharding_table_index);					
					Map<Object, Object> hashMap = new HashMap<>();
					// hashMap.put("sharding_table_index", sharding_table_index);
					hashMap.put("id", entity.getId());
					hashMap.put("uid", entity.getUid());
					EntityProxyWrapper entityProxyWrapper = entity.getEntityProxyWrapper();
					// 只有数据变化的时候才会更新
					if (entityProxyWrapper != null) {
						EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
						if (entityProxy != null && entityProxy.isDirtyFlag()) {
							Map<String, Object> changeParamSet = entityProxy.getChangeParamSet(EntitySaveEnum.Save2DB);	
							if (changeParamSet != null && (!changeParamSet.isEmpty())) {
								hashMap.putAll(changeParamSet);
								if (hashMap != null && hashMap.size() > 0) {
									try {
										idbMapper.updateEntityByMap(hashMap);
										result.add(1L);
									} catch (Exception e) {
										logger.error("EntityService.updateEntityBatch updateEntityByMap Error! entity={} Exception err={}",entity.toString(),e.toString());
										result.add(0L);
									} finally {
									}
								}
							}
						} else {
							logger.error("updateEntityBatch cancel entityProxy == null! entity.simpleName={} id={}",entity.getClass().getSimpleName(),entity.getId());
							result.add(0L);
						}
					} else {
						logger.error("updateEntityBatch cancel entityProxyWrapper == null! entity.simpleName={} id={}",entity.getClass().getSimpleName(),entity.getId());
						result.add(0L);
					}
				}
			}
			commitBatchSession();
		} catch (Exception e) {
			logger.error("EntityService.updateEntityBatch Error! entityList={} Exception err={}",entityList.toString(),e.toString());
			rollbackBatchSession();
			result.add(0l);
		} finally {
			closeBatchSession();
		}
		return result;
	}
	
	/**
	 * 删除实体
	 *
	 * @param entity
	 */
	@Override
	@DbOperation(operation = DbOperationEnum.delete)
	public boolean deleteEntity(T entity) {
		IDBMapper<T> idbMapper = getTemplateMapper(entity);
		if (idbMapper != null) {
			long selectId = getShardingId(entity);
			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
			// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
			String uid = entity.getUid();
			try {
				if(uid == null || uid.isEmpty()) {
					idbMapper.deleteEntity(entity);
				}else {
					idbMapper.deleteEntityByUID(entity);
				}
				return true;
			} catch (Exception e) {
				logger.error("EntityService.deleteEntity Error! entity={} Exception err={}" , entity.toString() , e.toString());
				return false;
			} finally {
			}
		}
		return false;
	}

	@Override
	@DbOperation(operation = DbOperationEnum.deleteBatch)
	public List<Long> deleteEntityBatch(List<T> entityList) {
		List<Long> result = new ArrayList<>();
		SqlSession sqlSession = getBatchSession();
		try {
			for (T iEntity : entityList) {
				IDBMapper<T> idbMapper = getBatchTemplateMapper(sqlSession, iEntity);
				if (idbMapper != null) {
					long selectId = getShardingId(iEntity);
					CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
					// iEntity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
					idbMapper.deleteEntity(iEntity);
					result.add(1l);
				}
			}
			commitBatchSession();
		} catch (Exception e) {
			logger.error("EntityService.deleteEntityBatch Error! entityList={} Exception err={}",entityList.toString(),e.toString());
			rollbackBatchSession();
			result.add(0l);
		} finally {
			closeBatchSession();
		}
		return result;
	}
	
	/**
	 * 获取分库主键
	 * 
	 * @param entity
	 * @return
	 */
	@Override
	public long getShardingId(T entity) {
		long shardingId = entity.getId();
		// if
		// (entity.getEntityKeyShardingStrategyEnum().equals(EntityKeyShardingStrategyEnum.ID))
		// {
		// if(entity instanceof AbstractEntity) {
		// //BaseLongIDEntity baseLongIDEntity = (BaseLongIDEntity) entity;
		// shardingId = entity.getId();
		// }else if(entity instanceof BaseStringIDEntity){
		// BaseStringIDEntity baseStringIDEntity = (BaseStringIDEntity)entity;
		// shardingId = baseStringIDEntity.getId().hashCode();
		// }
		// }
		return shardingId;
	}

	/**
	 * RedisInterface直接查询db的list接口
	 * 
	 * @param entity
	 *            需要实现代理
	 * @return
	 */
//	public List<T> filterList(T entity) {
//		List<T> result = null;
//		IDBMapper<T> idbMapper = getTemplateMapper(entity);
//		if (idbMapper != null) {
//			long selectId = getShardingId(entity);
//			CustomerContextHolder.setCustomerType(getEntityServiceShardingStrategy().getShardingDBKeyByUserId(selectId));
//			// entity.setSharding_table_index(getEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId));
//			Map<Object, Object> hashMap = new HashMap<>();
//			// hashMap.put("sharding_table_index", entity.getSharding_table_index());
//			hashMap.put("id", entity.getId());
//			hashMap.put("uid", entity.getUid());
//			EntityProxyWrapper entityProxyWrapper = entity.getEntityProxyWrapper();
//			if (entityProxyWrapper != null) {
//				EntityProxy entityProxy = entityProxyWrapper.getEntityProxy();
//				if (entityProxy != null) {
//					Map<String, Object> changeParamSet = entityProxy.getChangeParamSet(1);	
//					if (changeParamSet != null && (!changeParamSet.isEmpty())) {
//						hashMap.putAll(changeParamSet);
//					}
//				}
//			}
//	
//			EntityServiceShardingStrategy entityServiceShardingStrategy = getDefaultEntityServiceShardingStrategy();
//			try {
//				if (!entityServiceShardingStrategy.isPageFlag()) {
//					result = idbMapper.filterList(hashMap);
//				} else {
//					int pageLimit = entityServiceShardingStrategy.getPageLimit();
//					PageRowBounds pageRowBounds = new PageRowBounds(0, pageLimit);
//					result = idbMapper.filterList(hashMap, pageRowBounds);
//					long count = pageRowBounds.getTotal().longValue();
//					if (count > pageLimit) {
//						int offset = pageLimit;
//						while (offset < count) {
//							pageRowBounds = new PageRowBounds(offset, pageLimit);
//							result.addAll(idbMapper.filterList(hashMap, pageRowBounds));
//							offset += pageLimit;
//						}
//					}
//				}
//	
//			} catch (Exception e) {
//				logger.error("EntityService.filterList Error! entity={} Exception err={}",entity.toString(),e.toString());
//			} finally {
//			}
//		}
//		return result;
//	}

	/**
	 * Function : 获取sqlSession
	 */
	public SqlSession getBatchSession() {
		SqlSession session = threadLocal.get();

		if (session == null) {
			// 如果sqlSessionFactory不为空则获取sqlSession，否则返回null
			session = (sqlSessionBatchTemplate.getSqlSessionFactory() != null)
					? sqlSessionBatchTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, true)
					: null;
			threadLocal.set(session);
		}
		return session;
	}

	/**
	 * Function : 关闭sqlSession
	 */
	public void closeBatchSession() {
		SqlSession session = threadLocal.get();
		if (session != null) {
			if (logger.isDebugEnabled()) {
				logger.info("销毁关闭sqlSession:{}",session.toString());
			}
			session.close();
			threadLocal.set(null);
		}
	}

	/**
	 * Function : 关闭sqlSession
	 */
	public void rollbackBatchSession() {
		SqlSession session = threadLocal.get();
		if (session != null) {
			session.rollback();
		}
	}

	public void commitBatchSession() {
		SqlSession session = threadLocal.get();
		if (session != null) {
			session.commit();
		}
	}

	public IDBMapper<T> getTemplateMapper(T entity) {
		DbMapper mapper = entity.getClass().getAnnotation(DbMapper.class);
		if ((mapper != null)&&(sqlSessionTemplate != null)) {
			return (IDBMapper<T>) sqlSessionTemplate.getMapper(mapper.mapper());
		}
		return null;
	}


	public IDBMapper<T> getBatchTemplateMapper(SqlSession sqlSession, T entity) {
		DbMapper mapper = entity.getClass().getAnnotation(DbMapper.class);
		if ((mapper != null)&&(sqlSessionTemplate != null)) {
			return (IDBMapper<T>) sqlSession.getMapper(mapper.mapper());
		}
		return null;
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		return sqlSessionTemplate;
	}

	public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	public SqlSessionTemplate getSqlSessionBatchTemplate() {
		return sqlSessionBatchTemplate;
	}

	public void setSqlSessionBatchTemplate(SqlSessionTemplate sqlSessionBatchTemplate) {
		this.sqlSessionBatchTemplate = sqlSessionBatchTemplate;
	}

	public EntityServiceShardingStrategy getDefaultEntityServiceShardingStrategy() {
		return defaultEntityServiceShardingStrategy;
	}

	public void setDefaultEntityServiceShardingStrategy(
			EntityServiceShardingStrategy defaultEntityServiceShardingStrategy) {
		this.defaultEntityServiceShardingStrategy = defaultEntityServiceShardingStrategy;
	}

	public abstract EntityServiceShardingStrategy getEntityServiceShardingStrategy();

	// 获取模版参数类
	@SuppressWarnings("unchecked")
	public Class<T> getEntityTClass() {
		Class<? extends EntityService> classes = getClass();
		Class<T> result = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];
		return result;
	}
}
