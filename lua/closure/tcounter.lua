#!/usr/bin/lua

local counter = require "counter"

local c1 = counter.new()
local sum, mul = c1(2)
assert(sum == 2 and mul == 2, sum .. " " .. mul)

local c2 = counter.new()
sum, mul = c2(3)
assert(sum == 3 and mul == 3, sum .. " " .. mul)

sum, mul = c1(3)
assert(sum == 5 and mul == 6, sum .. " " .. mul)

print "OK"
