package com.amg.digitalitzacio.shared.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test configuration that replaces Redis with a mock connection factory.
 * The mock connection stores data in a ConcurrentHashMap (keyed by String)
 * so that basic get/set/del operations work across calls.
 */
@TestConfiguration
public class TestRedisConfig {

    private static String str(byte[] bytes) {
        return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }

    private static byte[] bytes(String s) {
        return s != null ? s.getBytes(StandardCharsets.UTF_8) : null;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        var data = new ConcurrentHashMap<String, String>();

        var connection = mock(RedisConnection.class, withSettings().lenient());

        // get(byte[]) -> byte[]
        lenient().when(connection.get(any())).thenAnswer(invocation ->
            bytes(data.get(str(invocation.<byte[]>getArgument(0)))));

        // set(byte[], byte[]) -> void
        lenient().doAnswer(invocation -> {
            data.put(str(invocation.<byte[]>getArgument(0)),
                     str(invocation.<byte[]>getArgument(1)));
            return null;
        }).when(connection).set(any(), any());

        // set(byte[], byte[], Expiration, SetOption) -> void
        lenient().doAnswer(invocation -> {
            data.put(str(invocation.<byte[]>getArgument(0)),
                     str(invocation.<byte[]>getArgument(1)));
            return null;
        }).when(connection).set(any(), any(), any(Expiration.class), any());

        // setEx(byte[], long, byte[]) -> void
        lenient().doAnswer(invocation -> {
            data.put(str(invocation.<byte[]>getArgument(0)),
                     str(invocation.<byte[]>getArgument(2)));
            return null;
        }).when(connection).setEx(any(), anyLong(), any());

        // del(byte[]) -> Long
        lenient().when(connection.del(any())).thenAnswer(invocation -> {
            var prev = data.remove(str(invocation.<byte[]>getArgument(0)));
            return prev != null ? 1L : 0L;
        });

        // incr(byte[]) -> Long
        lenient().when(connection.incr(any())).thenAnswer(invocation -> {
            var key = str(invocation.<byte[]>getArgument(0));
            var val = data.get(key);
            if (val == null) {
                data.put(key, "1");
                return 1L;
            }
            var next = Long.parseLong(val) + 1;
            data.put(key, String.valueOf(next));
            return next;
        });

        // expire(byte[], long) -> Boolean
        lenient().when(connection.expire(any(), anyLong())).thenReturn(true);

        // keys(byte[]) -> Set<byte[]>
        lenient().when(connection.keys(any())).thenAnswer(invocation -> {
            var pattern = str(invocation.<byte[]>getArgument(0));
            var results = new java.util.HashSet<byte[]>();
            if (pattern.contains("*")) {
                var prefix = pattern.substring(0, pattern.indexOf('*'));
                for (var key : data.keySet()) {
                    if (key.startsWith(prefix)) {
                        results.add(bytes(key));
                    }
                }
            }
            return results;
        });

        // exists(byte[]) -> Boolean
        lenient().when(connection.exists(any(byte[].class))).thenAnswer(invocation ->
            data.containsKey(str(invocation.<byte[]>getArgument(0))));

        // close() -> void
        lenient().doNothing().when(connection).close();

        var factory = mock(RedisConnectionFactory.class, withSettings().lenient());
        lenient().when(factory.getConnection()).thenReturn(connection);
        lenient().when(factory.getClusterConnection())
                .thenThrow(new UnsupportedOperationException("No cluster in tests"));

        return factory;
    }
}
