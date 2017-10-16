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
package redis.clients.redisson.reactive;

import java.util.UUID;

import redis.clients.redisson.RedissonReadWriteLock;
import redis.clients.redisson.api.RLockAsync;
import redis.clients.redisson.api.RLockReactive;
import redis.clients.redisson.api.RReadWriteLock;
import redis.clients.redisson.api.RReadWriteLockReactive;
import redis.clients.redisson.command.CommandAsyncExecutor;
import redis.clients.redisson.command.CommandReactiveExecutor;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public class RedissonReadWriteLockReactive extends RedissonExpirableReactive implements RReadWriteLockReactive {

    private final RReadWriteLock instance;
    private final UUID id;

    public RedissonReadWriteLockReactive(CommandReactiveExecutor commandExecutor, String name, UUID id) {
        super(commandExecutor, name);
        this.id = id;
        this.instance = new RedissonReadWriteLock(commandExecutor, name, id);
    }

    @Override
    public RLockReactive readLock() {
        return new RedissonLockReactive(commandExecutor, getName(), id) {
            @Override
            protected RLockAsync createLock(CommandAsyncExecutor connectionManager, String name, UUID id) {
                return instance.readLock();
            }
        };
    }

    @Override
    public RLockReactive writeLock() {
        return new RedissonLockReactive(commandExecutor, getName(), id) {
            @Override
            protected RLockAsync createLock(CommandAsyncExecutor connectionManager, String name, UUID id) {
                return instance.writeLock();
            }
        };
    }

    
}
