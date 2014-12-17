#!/usr/bin/lua

local floor = math.floor

local function image_ramp_green(n)
  local img = {}
  local f = 255/(n-1)
  for i=1,n do
    img[i] = { red = 0, green = floor((i-1)*f), blue = 0, alpha = 255 }
  end
  return img
end

local function image_to_grey(img, n)
  for i=1,n do
    local y = floor(0.3*img[i].red + 0.59*img[i].green + 0.11*img[i].blue)
    img[i].red = y; img[i].green = y; img[i].blue = y
  end
end

local N = 400*400
local img = image_ramp_green(N)
for i=1,1000 do
  image_to_grey(img, N)
end

print(10, img[10].red, img[10].green, img[10].blue)
print(100, img[100].red, img[100].green, img[100].blue)
