-- Fixed Window: atomic increment + expire
-- KEYS[1] = rate limit key
-- ARGV[1] = window size in seconds
-- ARGV[2] = max requests
-- Returns: { current_count, ttl_remaining }

local key = KEYS[1]
local window = tonumber(ARGV[1])
local limit  = tonumber(ARGV[2])

local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, window)
end

local ttl = redis.call('TTL', key)
return { current, ttl }
