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
package redis.clients.redisson.connection.pool;

import redis.clients.redisson.client.RedisConnection;
import redis.clients.redisson.config.MasterSlaveServersConfig;
import redis.clients.redisson.connection.ClientConnectionsEntry;
import redis.clients.redisson.connection.ConnectionManager;
import redis.clients.redisson.connection.MasterSlaveEntry;

/**
 * Connection pool for slave node
 * 
 * @author Nikita Koksharov
 *
 */
public class SlaveConnectionPool extends ConnectionPool<RedisConnection> {

    public SlaveConnectionPool(MasterSlaveServersConfig config, ConnectionManager connectionManager,
            MasterSlaveEntry masterSlaveEntry) {
        super(config, connectionManager, masterSlaveEntry);
    }

    protected int getMinimumIdleSize(ClientConnectionsEntry entry) {
        return config.getSlaveConnectionMinimumIdleSize();
    }

}
