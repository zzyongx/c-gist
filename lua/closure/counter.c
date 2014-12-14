#include <lua.h>
#include <lauxlib.h>

static int counter(lua_State *L)
{
  int i = luaL_checkinteger(L, 1);
  int sum = lua_tonumber(L, lua_upvalueindex(1));
  int mul = lua_tonumber(L, lua_upvalueindex(2));

  lua_pushnumber(L, sum+i);
  lua_replace(L, lua_upvalueindex(1));

  lua_pushnumber(L, mul * i);
  lua_replace(L, lua_upvalueindex(2));

  lua_pushnumber(L, sum + i);
  lua_pushnumber(L, mul * i);

  return 2;
}

static int new_counter(lua_State *L)
{
  lua_pushnumber(L, 0);
  lua_pushnumber(L, 1);
  lua_pushcclosure(L, &counter, 2);  /* two upvalues */
  return 1;
}

static const struct luaL_reg functions[] = {
  {"new", new_counter},
  {NULL, NULL},
};

int luaopen_counter(lua_State *L)
{
  lua_newtable(L);
  luaL_register(L, "counter", functions);
  return 1;
}
