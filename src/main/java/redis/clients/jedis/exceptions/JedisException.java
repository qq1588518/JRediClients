package redis.clients.jedis.exceptions;

import org.slf4j.Logger;
import redis.clients.common.utils.Loggers;

public class JedisException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	protected static Logger logger = Loggers.jedisLogger;

	public JedisException(String message) {
		super(message);
		logger.error(message);
	}

	public JedisException(Throwable e) {
		super(e);
		logger.error(e.toString());
	}

	public JedisException(String message, Throwable cause) {
		super(message, cause);
		logger.error(message + cause.toString());
	}
}
