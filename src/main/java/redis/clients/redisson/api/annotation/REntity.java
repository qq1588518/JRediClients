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
package redis.clients.redisson.api.annotation;

import redis.clients.redisson.liveobject.resolver.NamingScheme;
import redis.clients.redisson.liveobject.resolver.DefaultNamingScheme;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.codec.JsonJacksonCodec;

/**
 *
 * @author Rui Gu (https://github.com/jackygurui)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface REntity {

    public enum TransformationMode {
        IMPLEMENTATION_BASED, ANNOTATION_BASED
    }
    
    Class<? extends NamingScheme> namingScheme() default DefaultNamingScheme.class;

    Class<? extends Codec> codec() default JsonJacksonCodec.class;

    TransformationMode fieldTransformation() default TransformationMode.ANNOTATION_BASED;
    
}
