package redis.clients.redisearch.client;

import redis.clients.jedis.ProtocolCommand;
import redis.clients.util.SafeEncoder;

/**
 * Jedis enum for command encapsulation
 */
 public class Commands {


    // TODO: Move this to the client and autocompleter as two different enums
    public enum Command implements ProtocolCommand{

        CREATE("FT.CREATE"),
        ADD("FT.ADD"),
        ADDHASH("FT.ADDHASH"),
        INFO("FT.INFO"),
        SEARCH("FT.SEARCH"),
        DEL("FT.DEL"),
        DROP("FT.DROP"),
        OPTIMIZE("FT.OPTIMIZE"),
    	BROADCAST("FT.BROADCAST");
    	
        private final byte[] raw;

        Command(String alt) {
            raw = SafeEncoder.encode(alt);
        }

		@Override
		public byte[] getRaw() {
            return raw;
        }
    }

    public interface CommandProvider {
    	Command getCreateCommand();
    	Command getAddCommand();
    	Command getAddHashCommand();
    	Command getDelCommand();
    	Command getInfoCommand();
    	Command getDropCommand();
    	Command getSearchCommand();
    	Command getOptimizeCommand();
    	Command getBroadcastCommand();
    }

    public static class SingleNodeCommands implements CommandProvider {

        @Override
        public Command getCreateCommand() {
            return Command.CREATE;
        }

        @Override
        public Command getAddCommand() {
            return Command.ADD;
        }

        @Override
        public Command getAddHashCommand() {
            return Command.ADDHASH;
        }

        @Override
        public Command getDelCommand() {
            return Command.DEL;
        }

        @Override
        public Command getInfoCommand() {
            return Command.INFO;
        }

        @Override
        public Command getDropCommand() {
            return Command.DROP;
        }

        @Override
        public Command getSearchCommand() {
            return Command.SEARCH;
        }

        @Override
        public Command getOptimizeCommand() {
            return Command.OPTIMIZE;
        }

		@Override
		public Command getBroadcastCommand() {
			return Command.BROADCAST;
		}
    }

}
