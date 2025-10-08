package org.project.appointment_project.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return (T) value;
        } catch (Exception e) {
            return null;
        }
    }


    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error(" Error caching: {}", key);
        }
    }

    // Lưu giá trị với TTL
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);

        } catch (Exception e) {
            log.error(" Error caching with TTL: {}", key);
        }
    }

    // Set TTL cho key đã tồn tại
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            Boolean result = redisTemplate.expire(key, timeout, timeUnit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error setting TTL for key: {}", key);
            return false;
        }
    }

    // Lấy TTL còn lại của key (seconds)
    public long getTTL(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL: {}", key);
            return -1;
        }
    }

    // Kiểm tra key có tồn tại không
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking existence: {}", key);
            return false;
        }
    }


    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error deleting: {}", key);
            return false;
        }
    }


    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count;
        } catch (Exception e) {
            log.error("Error deleting keys", e);
            return 0;
        }
    }


    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                return count;
            }
            return 0;
        } catch (Exception e) {
            log.error("✗ Error deleting by pattern: {}", pattern);
            return 0;
        }
    }


    // Push nhiều items vào đầu list
    public void leftPushAll(String key, List<?> values) {
        try {
            redisTemplate.opsForList().leftPushAll(key, values.toArray());
        } catch (Exception e) {
            log.error("Error left pushing to list: {}", key);
            e.printStackTrace();
        }
    }

    public void leftPush(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            log.error("Error left pushing: {}", key);
        }
    }

    public void rightPush(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("Error right pushing: {}", key);
        }
    }

    public Object rightPop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            return null;
        }
    }


    public Object leftPop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            return null;
        }
    }

    public long listSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public Map<String, Object> getCacheInfo(String pattern) {
        Map<String, Object> info = new HashMap<>();
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            info.put("totalKeys", keys != null ? keys.size() : 0);
            info.put("pattern", pattern);
            info.put("keys", keys);
        } catch (Exception e) {
            log.error("Error getting cache info", e);
        }
        return info;
    }

    public void flushAll() {
        try {
            redisTemplate.getConnectionFactory()
                    .getConnection()
                    .flushAll();
        } catch (Exception e) {
        }
    }
}
