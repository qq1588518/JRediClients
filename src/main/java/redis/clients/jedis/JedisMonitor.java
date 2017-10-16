package redis.clients.jedis;

import org.slf4j.Logger;
import redis.clients.common.utils.Loggers;

public abstract class JedisMonitor {
	protected static Logger logger = Loggers.jedisLogger;
	protected Client client;

	public void proceed(Client client) {
		this.client = client;
		this.client.setTimeoutInfinite();
		do {
			String command = client.getBulkReply();
			onCommand(command);
		} while (client.isConnected());
	}

	public abstract void onCommand(String command);
}