#!/usr/bin/luajit

local ffi =  require "ffi"

ffi.cdef [[
unsigned int sleep(unsigned int seconds);
int poll(struct pollfd *fds, int nfds, int timeout);

struct timeval {
  long tv_sec;    /* typedef long time_t */
  long tv_usec;
};
int gettimeofday(struct timeval *tv, void *tz);
]]

local sleep = function(second)
  ffi.C.sleep(second)
end
local msleep = function(milli)
  ffi.C.poll(nil, 0, milli)
end

local tvtype = ffi.typeof("struct timeval");
local mstime = function()
  --  local tv = ffi.new("struct timeval")
  local tv = tvtype()
  ffi.C.gettimeofday(tv, nil)
  return tonumber(tv.tv_sec + (tv.tv_usec/1e6))
end

local begin = mstime()
for i = 1,1000 do
  msleep(1);
end
print(mstime() - begin)

begin = mstime()
sleep(1)
print(mstime() - begin)
