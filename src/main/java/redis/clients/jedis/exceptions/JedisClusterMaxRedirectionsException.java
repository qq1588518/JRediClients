package redis.clients.jedis.exceptions;

public class JedisClusterMaxRedirectionsException extends JedisDataException {
	private static final long serialVersionUID = 1L;

	public JedisClusterMaxRedirectionsException(Throwable cause) {
		super(cause);
	}

	public JedisClusterMaxRedirectionsException(String message, Throwable cause) {
		super(message, cause);
	}

	public JedisClusterMaxRedirectionsException(String message) {
		super(message);
	}
}
