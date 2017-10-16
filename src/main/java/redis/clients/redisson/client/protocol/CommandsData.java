/**
 * Copyright 2016 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package redis.clients.redisson.client.protocol;

import java.util.ArrayList;
import java.util.List;

import redis.clients.redisson.misc.RPromise;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class CommandsData implements QueueCommand {

    private final List<CommandData<?, ?>> commands;
    private final RPromise<Void> promise;
    private final boolean noResult;

    public CommandsData(RPromise<Void> promise, List<CommandData<?, ?>> commands) {
        this(promise, commands, false);
    }
    
    public CommandsData(RPromise<Void> promise, List<CommandData<?, ?>> commands, boolean noResult) {
        super();
        this.promise = promise;
        this.commands = commands;
        this.noResult = noResult;
    }

    public RPromise<Void> getPromise() {
        return promise;
    }

    public boolean isNoResult() {
        return noResult;
    }
    
    public List<CommandData<?, ?>> getCommands() {
        return commands;
    }

    @Override
    public List<CommandData<Object, Object>> getPubSubOperations() {
        List<CommandData<Object, Object>> result = new ArrayList<CommandData<Object, Object>>();
        for (CommandData<?, ?> commandData : commands) {
            if (RedisCommands.PUBSUB_COMMANDS.equals(commandData.getCommand().getName())) {
                result.add((CommandData<Object, Object>)commandData);
            }
        }
        return result;
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        return promise.tryFailure(cause);
    }

    @Override
    public String toString() {
        return "CommandsData [commands=" + commands + "]";
    }

}
