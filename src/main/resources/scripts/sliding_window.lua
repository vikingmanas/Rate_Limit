-- Sliding Window Log: sorted set with timestamp scores
-- KEYS[1] = rate limit key
-- ARGV[1] = current timestamp (ms)
-- ARGV[2] = window size (ms)
-- ARGV[3] = max requests
-- ARGV[4] = unique request id (to avoid duplicates)
-- Returns: { current_count, allowed (1/0) }

local key       = KEYS[1]
local now       = tonumber(ARGV[1])
local windowMs  = tonumber(ARGV[2])
local limit     = tonumber(ARGV[3])
local requestId = ARGV[4]

local windowStart = now - windowMs

-- Remove expired entries
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

local count = redis.call('ZCARD', key)

if count < limit then
    redis.call('ZADD', key, now, requestId)
    redis.call('PEXPIRE', key, windowMs)
    return { count + 1, 1 }
else
    return { count, 0 }
end
