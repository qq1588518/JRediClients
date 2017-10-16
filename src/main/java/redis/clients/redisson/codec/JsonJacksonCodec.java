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
package redis.clients.redisson.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.datatype.XMLGregorianCalendar;

import redis.clients.redisson.client.codec.Codec;
import redis.clients.redisson.client.handler.State;
import redis.clients.redisson.client.protocol.Decoder;
import redis.clients.redisson.client.protocol.Encoder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 *
 * @see redis.clients.redisson.codec.CborJacksonCodec
 * @see redis.clients.redisson.codec.MsgPackJacksonCodec
 *
 * @author Nikita Koksharov
 *
 */
public class JsonJacksonCodec implements Codec {

    public static final JsonJacksonCodec INSTANCE = new JsonJacksonCodec();

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    @JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.PUBLIC_ONLY, setterVisibility = Visibility.PUBLIC_ONLY, isGetterVisibility = Visibility.PUBLIC_ONLY)
    public static class ThrowableMixIn {
        
    }
    
    private final ObjectMapper mapObjectMapper;

    private final Encoder encoder = new Encoder() {
        @Override
        public ByteBuf encode(Object in) throws IOException {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream os = new ByteBufOutputStream(out);
                mapObjectMapper.writeValue((OutputStream)os, in);
                return os.buffer();
            } catch (IOException e) {
                out.release();
                throw e;
            }
        }
    };

    private final Decoder<Object> decoder = new Decoder<Object>() {
        @Override
        public Object decode(ByteBuf buf, State state) throws IOException {
            return mapObjectMapper.readValue((InputStream)new ByteBufInputStream(buf), Object.class);
        }
    };
    
    public JsonJacksonCodec() {
        this(new ObjectMapper());
    }
    
    public JsonJacksonCodec(ClassLoader classLoader) {
        this(createObjectMapper(classLoader, new ObjectMapper()));
    }
    
    protected static ObjectMapper createObjectMapper(ClassLoader classLoader, ObjectMapper om) {
        TypeFactory tf = TypeFactory.defaultInstance().withClassLoader(classLoader);
        om.setTypeFactory(tf);
        return om;
    }

    public JsonJacksonCodec(ObjectMapper mapObjectMapper) {
        this.mapObjectMapper = mapObjectMapper;
        init(mapObjectMapper);
        initTypeInclusion(mapObjectMapper);
    }

    protected void initTypeInclusion(ObjectMapper mapObjectMapper) {
        TypeResolverBuilder<?> mapTyper = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL) {
            public boolean useForType(JavaType t) {
                switch (_appliesFor) {
                case NON_CONCRETE_AND_ARRAYS:
                    while (t.isArrayType()) {
                        t = t.getContentType();
                    }
                    // fall through
                case OBJECT_AND_NON_CONCRETE:
                    return (t.getRawClass() == Object.class) || !t.isConcrete();
                case NON_FINAL:
                    while (t.isArrayType()) {
                        t = t.getContentType();
                    }
                    // to fix problem with wrong long to int conversion
                    if (t.getRawClass() == Long.class) {
                        return true;
                    }
                    if (t.getRawClass() == XMLGregorianCalendar.class) {
                        return false;
                    }
                    return !t.isFinal(); // includes Object.class
                default:
                    // case JAVA_LANG_OBJECT:
                    return (t.getRawClass() == Object.class);
                }
            }
        };
        mapTyper.init(JsonTypeInfo.Id.CLASS, null);
        mapTyper.inclusion(JsonTypeInfo.As.PROPERTY);
        mapObjectMapper.setDefaultTyping(mapTyper);
        
        // warm up codec
        try {
            byte[] s = mapObjectMapper.writeValueAsBytes(1);
            mapObjectMapper.readValue(s, Object.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void init(ObjectMapper objectMapper) {
        objectMapper.registerModule(new DefenceModule());
        
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        objectMapper.addMixIn(Throwable.class, ThrowableMixIn.class);
    }

    @Override
    public Decoder<Object> getMapValueDecoder() {
        return decoder;
    }

    @Override
    public Encoder getMapValueEncoder() {
        return encoder;
    }

    @Override
    public Decoder<Object> getMapKeyDecoder() {
        return decoder;
    }

    @Override
    public Encoder getMapKeyEncoder() {
        return encoder;
    }

    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }

    @Override
    public Encoder getValueEncoder() {
        return encoder;
    }

    public ObjectMapper getObjectMapper() {
        return mapObjectMapper;
    }
}
