package redis.clients.jedis.exceptions;

public class JedisNoScriptException extends JedisDataException {
	private static final long serialVersionUID = 1L;

	public JedisNoScriptException(final String message) {
		super(message);
	}

	public JedisNoScriptException(final Throwable cause) {
		super(cause);
	}

	public JedisNoScriptException(final String message, final Throwable cause) {
		super(message, cause);
	}
}