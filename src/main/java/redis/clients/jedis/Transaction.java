package redis.clients.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import redis.clients.common.utils.Loggers;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * Transaction is nearly identical to Pipeline, only differences are the
 * multi/discard behaviors
 */
public class Transaction extends MultiKeyPipelineBase implements Closeable {

	protected static Logger logger = Loggers.jedisLogger;
	protected boolean inTransaction = true;

	protected Transaction() {
		// client will be set later in transaction block
	}

	public Transaction(final Client client) {
		this.client = client;
	}

	@Override
	protected Client getClient(String key) {
		return client;
	}

	@Override
	protected Client getClient(byte[] key) {
		return client;
	}

	public void clear() {
		if (inTransaction) {
			discard();
		}
	}

	public List<Object> exec() {
    	// Discard QUEUED or ERROR
    	client.getMany(getPipelinedResponseLength());
		client.exec();
		//client.getAll(1); // Discard all but the last reply
		inTransaction = false;

		List<Object> unformatted = client.getObjectMultiBulkReply();
		if (unformatted == null) {
			return null;
		}
		List<Object> formatted = new ArrayList<Object>();
		for (Object o : unformatted) {
			try {
				formatted.add(generateResponse(o).get());
			} catch (JedisDataException e) {
				formatted.add(e);
			}
		}
		return formatted;
	}

	public List<Response<?>> execGetResponse() {
    	// Discard QUEUED or ERROR
    	client.getMany(getPipelinedResponseLength());
		client.exec();
		//client.getAll(1); // Discard all but the last reply
		inTransaction = false;

		List<Object> unformatted = client.getObjectMultiBulkReply();
		if (unformatted == null) {
			return null;
		}
		List<Response<?>> response = new ArrayList<Response<?>>();
		for (Object o : unformatted) {
			response.add(generateResponse(o));
		}
		return response;
	}

	public String discard() {
		// Discard QUEUED or ERROR
		client.getMany(getPipelinedResponseLength());
		client.discard();
		//client.getAll(1); // Discard all but the last reply
		inTransaction = false;
		clean();
		String reply = client.getStatusCodeReply();
		if (!"OK".equalsIgnoreCase(reply)) {
			logger.error("Send DISCARD Command Error! reply={}" , reply);
		}
		return reply;
	}

	@Override
	public void close() throws IOException {
		clear();
	}
}