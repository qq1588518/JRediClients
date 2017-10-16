package redis.clients.redisearch.client;

import redis.clients.jedis.ProtocolCommand;
import redis.clients.util.SafeEncoder;

/**
 * TODO: Abstract auto-complete commands
 */
public class AutoCompleter {

    public enum Command implements ProtocolCommand {
        SUGADD("FT.SUGADD"),
        SUGGET("FT.SUGGET"),
        SUGDEL("FT.SUGDEL"),
        SUGLEN("FT.SUGLEN");

        private final byte[] raw;

        Command(String alt) {
            raw = SafeEncoder.encode(alt);
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }
//    SUGADD("FT.SUGADD"),
//    SUGGET("FT.SUGGET"),
//    SUGDEL("FT.SUGDEL"),
//    SUGLEN("FT.SUGLEN");
}
