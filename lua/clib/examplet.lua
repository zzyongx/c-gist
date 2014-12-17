#!/usr/bin/lua

local example = require "example"

print(example.retInt1(10))
print(example.retInt1(20,30))

print(pcall(example.retInt2, 10))
print(example.retInt2(10, 20, 30))

print(example.retIntN(10, 20));
print(example.retIntN(10, 20, 30, 40));

print(example.retUpper(12));
print(example.retUpper("12abcd"));

print(example.retHex("12@#$%ef"));

print(pcall(example.retIntList, 10, 20, 30))
print(pcall(example.retIntList, {10, "a"}))
print(example.retIntList({10, 20, 30, 40}))

local array = example.retIntArray({10, 20, 30});
print(array[1], array[2], array[3]);

local hash = example.retHash({red=128, blue="255"});
print(hash.red, hash.green, hash.blue);
