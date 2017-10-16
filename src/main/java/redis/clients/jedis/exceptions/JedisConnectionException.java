package redis.clients.jedis.exceptions;

public class JedisConnectionException extends JedisException {
	private static final long serialVersionUID = 1L;

	public JedisConnectionException(String message) {
		super(message);
	}

	public JedisConnectionException(Throwable cause) {
		super(cause);
	}

	public JedisConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
