#!/usr/bin/luajit

--[[
luajit with ffi 1s 400*400*4=640K
luajit          4s 22M
lua            63s 40M
]]--

local ffi = require "ffi"

ffi.cdef [[
typedef struct rgba_pixel {
  char r; char g; char b; char a;
} rgba_pixel_t;
]]

local image_ramp_green = function(n)
  local img = ffi.new("rgba_pixel_t[?]", n)
  local f = 255/(n-1)
  for i = 0,n-1 do
    img[i].g = i*f
    img[i].a = 255
  end
  return img
end

local image_to_grey = function(img, n)
  local floor = math.floor
  for i = 0,n-1 do
    local y = floor(0.3 * img[i].r + 0.59 * img[i].g + 0.11 * img[i].b)
    img[i].r, img[i].g, img[i].b = y, y, y
  end
end

local N = 400 * 400
local img = image_ramp_green(N)
for i = 1,1000 do
  image_to_grey(img, N)
end

print(9, img[9].r, img[9].g, img[9].b)
print(99, img[99].r, img[99].g, img[99].b)
