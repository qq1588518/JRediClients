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
package redis.clients.redisson.misc;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.redisson.RedissonReference;
import redis.clients.redisson.api.RLiveObject;
import redis.clients.redisson.api.RLiveObjectService;
import redis.clients.redisson.api.RObject;
import redis.clients.redisson.api.RObjectReactive;
import redis.clients.redisson.api.RedissonClient;
import redis.clients.redisson.api.RedissonReactiveClient;
import redis.clients.redisson.api.annotation.REntity;
import redis.clients.redisson.api.annotation.RId;
import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.codec.CodecProvider;
import redis.clients.redisson.config.Config;
import redis.clients.redisson.liveobject.misc.ClassUtils;
import redis.clients.redisson.liveobject.misc.Introspectior;
import redis.clients.redisson.liveobject.resolver.NamingScheme;

/**
 *
 * @author Rui Gu (https://github.com/jackygurui)
 */
public class RedissonObjectFactory {

    public static class RedissonObjectBuilder {

        Method defaultCodecMethod;
        Method customCodecMethod;

        Method get(boolean value) {
            if (value) {
                return defaultCodecMethod;
            }
            return customCodecMethod;
        }
    }
    
    private static final Map<Class<?>, RedissonObjectBuilder> builders;
    
    static {
        HashMap<Class<?>, RedissonObjectBuilder> b = new HashMap<Class<?>, RedissonObjectBuilder>();
        for (Method method : RedissonClient.class.getDeclaredMethods()) {
            if (!method.getReturnType().equals(Void.TYPE)
                    && RObject.class.isAssignableFrom(method.getReturnType())
                    && method.getName().startsWith("get")) {
                Class<?> cls = method.getReturnType();
                if (!b.containsKey(cls)) {
                    b.put(cls, new RedissonObjectBuilder());
                }
                RedissonObjectBuilder builder = b.get(cls);
                if (method.getParameterTypes().length == 2 //first param is name, second param is codec.
                        && Codec.class.isAssignableFrom(method.getParameterTypes()[1])) {
                    builder.customCodecMethod = method;
                } else if (method.getParameterTypes().length == 1) {
                    builder.defaultCodecMethod = method;
                }
            }
        }
        
        for (Method method : RedissonReactiveClient.class.getDeclaredMethods()) {
            if (!method.getReturnType().equals(Void.TYPE)
                    && RObjectReactive.class.isAssignableFrom(method.getReturnType())
                    && method.getName().startsWith("get")) {
                Class<?> cls = method.getReturnType();
                if (!b.containsKey(cls)) {
                    b.put(cls, new RedissonObjectBuilder());
                }
                RedissonObjectBuilder builder = b.get(cls);
                if (method.getParameterTypes().length == 2 //first param is name, second param is codec.
                        && Codec.class.isAssignableFrom(method.getParameterTypes()[1])) {
                    builder.customCodecMethod = method;
                } else if (method.getParameterTypes().length == 1) {
                    builder.defaultCodecMethod = method;
                }
            }
        }
        
        builders = Collections.unmodifiableMap(b);
        
    }

    public static <T> T fromReference(RedissonClient redisson, RedissonReference rr) throws Exception {
        Class<? extends Object> type = rr.getType();
        CodecProvider codecProvider = redisson.getConfig().getCodecProvider();
        if (type != null) {
            if (ClassUtils.isAnnotationPresent(type, REntity.class)) {
                RLiveObjectService liveObjectService = redisson.getLiveObjectService();
                REntity anno = ClassUtils.getAnnotation(type, REntity.class);
                NamingScheme ns = anno.namingScheme()
                        .getDeclaredConstructor(Codec.class)
                        .newInstance(codecProvider.getCodec(anno, type));
                return (T) liveObjectService.get(type, ns.resolveId(rr.getKeyName()));
            }
            List<Class<?>> interfaces = Arrays.asList(type.getInterfaces());
            for (Class<?> iType : interfaces) {
                if (builders.containsKey(iType)) {// user cache to speed up things a little.
                    Method builder = builders.get(iType).get(isDefaultCodec(rr));
                    return (T) (isDefaultCodec(rr)
                            ? builder.invoke(redisson, rr.getKeyName())
                            : builder.invoke(redisson, rr.getKeyName(), codecProvider.getCodec(rr.getCodecType())));
                }
            }
        }
        throw new ClassNotFoundException("No RObject is found to match class type of " + rr.getTypeName() + " with codec type of " + rr.getCodecName());
    }
    
    private static boolean isDefaultCodec(RedissonReference rr) {
        return rr.getCodec() == null;
    }

    
    public static <T> T fromReference(RedissonReactiveClient redisson, RedissonReference rr) throws Exception {
        Class<? extends Object> type = rr.getReactiveType();
        CodecProvider codecProvider = redisson.getConfig().getCodecProvider();
        /**
         * Live Object from reference in reactive client is not supported yet.
         */
        if (type != null) {
            List<Class<?>> interfaces = Arrays.asList(type.getInterfaces());
            for (Class<?> iType : interfaces) {
                if (builders.containsKey(iType)) {// user cache to speed up things a little.
                    Method builder = builders.get(iType).get(isDefaultCodec(rr));
                    return (T) (isDefaultCodec(rr)
                            ? builder.invoke(redisson, rr.getKeyName())
                            : builder.invoke(redisson, rr.getKeyName(), codecProvider.getCodec(rr.getCodecType())));
                }
            }
        }
        throw new ClassNotFoundException("No RObjectReactive is found to match class type of " + rr.getReactiveTypeName()+ " with codec type of " + rr.getCodecName());
    }

    public static RedissonReference toReference(Config config, Object object) {
        if (object != null && ClassUtils.isAnnotationPresent(object.getClass(), REntity.class)) {
            throw new IllegalArgumentException("REntity should be attached to Redisson before save");
        }
        
        if (object instanceof RObject && !(object instanceof RLiveObject)) {
            RObject rObject = ((RObject) object);
            config.getCodecProvider().registerCodec((Class) rObject.getCodec().getClass(), (Class) rObject.getClass(), rObject.getName(), rObject.getCodec());
            return new RedissonReference(object.getClass(), ((RObject) object).getName(), ((RObject) object).getCodec());
        }
        if (object instanceof RObjectReactive && !(object instanceof RLiveObject)) {
            RObjectReactive rObject = ((RObjectReactive) object);
            config.getCodecProvider().registerCodec((Class) rObject.getCodec().getClass(), (Class) rObject.getClass(), rObject.getName(), rObject.getCodec());
            return new RedissonReference(object.getClass(), ((RObjectReactive) object).getName(), ((RObjectReactive) object).getCodec());
        }
        
        try {
            if (object instanceof RLiveObject) {
                Class<? extends Object> rEntity = object.getClass().getSuperclass();
                REntity anno = ClassUtils.getAnnotation(rEntity, REntity.class);
                NamingScheme ns = anno.namingScheme()
                        .getDeclaredConstructor(Codec.class)
                        .newInstance(config.getCodecProvider().getCodec(anno, (Class) rEntity));
                String name = Introspectior
                        .getFieldsWithAnnotation(rEntity, RId.class)
                        .getOnly().getName();
                Class<?> type = ClassUtils.getDeclaredField(rEntity, name).getType();
                return new RedissonReference(rEntity,
                        ns.getName(rEntity, type, name, ((RLiveObject) object).getLiveObjectId()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return null;
    }

    public static <T extends RObject, K extends Codec> T createRObject(RedissonClient redisson, Class<T> expectedType, String name, K codec) throws Exception {
        List<Class<?>> interfaces = Arrays.asList(expectedType.getInterfaces());
        for (Class<?> iType : interfaces) {
            if (builders.containsKey(iType)) {// user cache to speed up things a little.
                Method builder = builders.get(iType).get(codec != null);
                return (T) (codec != null
                        ? builder.invoke(redisson, name)
                        : builder.invoke(redisson, name, codec));
            }
        }
        throw new ClassNotFoundException("No RObject is found to match class type of " + (expectedType != null ? expectedType.getName() : "null") + " with codec type of " + (codec != null ? codec.getClass().getName() : "null"));
    }

    public static void warmUp() {}
}
