-- Token Bucket: refill + consume
-- KEYS[1] = bucket key (tokens)
-- KEYS[2] = timestamp key
-- ARGV[1] = bucket capacity (burst)
-- ARGV[2] = refill rate per ms
-- ARGV[3] = current timestamp (ms)
-- ARGV[4] = window ms (for expire)
-- Returns: { allowed (1/0), remaining_tokens }

local tokenKey = KEYS[1]
local tsKey    = KEYS[2]
local capacity = tonumber(ARGV[1])
local rate     = tonumber(ARGV[2])
local now      = tonumber(ARGV[3])
local windowMs = tonumber(ARGV[4])

local tokens    = tonumber(redis.call('GET', tokenKey) or capacity)
local lastTime  = tonumber(redis.call('GET', tsKey) or now)

-- Refill
local elapsed   = math.max(now - lastTime, 0)
local newTokens = math.min(capacity, tokens + elapsed * rate)

if newTokens >= 1 then
    newTokens = newTokens - 1
    redis.call('SET', tokenKey, tostring(newTokens), 'PX', windowMs)
    redis.call('SET', tsKey, tostring(now), 'PX', windowMs)
    return { 1, math.floor(newTokens) }
else
    redis.call('SET', tokenKey, tostring(newTokens), 'PX', windowMs)
    redis.call('SET', tsKey, tostring(now), 'PX', windowMs)
    return { 0, 0 }
end
