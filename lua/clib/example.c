#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <lua.h>
#include <lauxlib.h>

/* gcc -shared -o example.so -fPIC -Wall -Werror example.c */

static int ExRetInt1(lua_State *L) {
  int n1 = luaL_checkinteger(L, 1);
  lua_pushinteger(L, n1);
  return 1;
}

static int ExRetInt2(lua_State *L) {
  int n1 = luaL_checkinteger(L, 1);
  int n2 = luaL_checkinteger(L, 2);
  lua_pushinteger(L, n1);
  lua_pushinteger(L, n2);
  return 2;
}

static int ExRetIntN(lua_State *L) {
  int n = lua_gettop(L);
  int i;
  for (i = 0; i < n; ++i) {
    lua_pushinteger(L, luaL_checkinteger(L, i+1));
  }
  return n;
}

static int ExRetUpper(lua_State *L) {
  const char *p = luaL_checkstring(L, 1);
  size_t size = lua_strlen(L, 1);
  
  char *array = malloc(sizeof(char) * (size+1));
  size_t i;
  for (i = 0; i < size; ++i) {
    array[i] = toupper(p[i]);
  }
  array[size] = '\0';

  lua_pushstring(L, array);
  free(array);
  return 1;
}

static int ExRetHex(lua_State *L) {
  const char *p = luaL_checkstring(L, 1);
  size_t size = lua_strlen(L, 1);

  static const char *hextbl = "0123456789ABCDEF";

  char *array = malloc(sizeof(char) * size * 2);
  size_t i;
  for (i = 0; i < size; ++i) {
    array[2*i] = hextbl[p[i]/16];
    array[2*i+1] = hextbl[p[i]%16];
  }

  lua_pushlstring(L, array, size * 2);
  free(array);
  return 1;
}

static int ExRetIntList(lua_State *L) {
  if (!lua_istable(L, 1)) {
    luaL_error(L, "#1 must be array");
  }

  int size = luaL_getn(L, 1);
  if (size) {
    int *array = malloc(sizeof(int) * size);
    int i;
    for (i = 0; i < size; ++i) {
      lua_pushinteger(L, i+1);
      lua_gettable(L, 1);        // get t[i+1], push stack
      if (!lua_isnumber(L, -1)) { // check stack top
        free(array);
        luaL_error(L, "#%d is not number", i+1);
      }
      array[i] = lua_tonumber(L, -1);
      // here we must first check and then get
      // luaL_checkinteger may case memory leak
      
      // lua_pop(L); neithor call or not is ok
    }

    for (i = 0; i < size; ++i) {
      lua_pushinteger(L, array[i]);
    }
    free(array);
    return size;
  } else {
    lua_pushnil(L);
    return 1;
  }
}

static int ExRetIntArray(lua_State *L) {
  if (!lua_istable(L, 1)) {
    luaL_error(L, "#1 must be array");
  }

  int size = luaL_getn(L, 1);
  if (!size) {
    lua_pushnil(L);
    return 1;
  }

  int *array = malloc(sizeof(int) * size);
  int i;
  for (i = 0; i < size; ++i) {
    lua_pushinteger(L, i+1);
    lua_gettable(L, 1);
    array[i] = luaL_checkinteger(L, -1); // mem leak may happen
  }

  lua_newtable(L);
  int table = lua_gettop(L);
  for (i = 0; i < size; ++i) {
    lua_pushinteger(L, i+1);         // index
    lua_pushinteger(L, array[i]);    // value
    lua_settable(L, table);
  }
  free(array);
  return 1;
}

typedef struct kv {
  const char *k;
  int v;
} kv_t;

static int ExRetHash(lua_State *L) {
  if (!lua_istable(L, 1)) {
    luaL_error(L, "#1 must be hash");
  }

  kv_t kvs[] = {
    {"red",   0},
    {"green", 0},
    {"blue",  0},
    {NULL, 0}
  };

  int i;
  for (i = 0; kvs[i].k; i++) {
    lua_getfield(L, 1, kvs[i].k);
    // printf("top type %s\n", lua_typename(L, lua_type(L, -1)));
    if (lua_isnumber(L, -1)) {
      kvs[i].v = lua_tonumber(L, -1);
    } else if (!lua_isnil(L, -1)) {
      luaL_error(L, "%s's value must be number", kvs[i].k);
    }
  }

  lua_newtable(L);
  int table = lua_gettop(L);
  for (i = 0; kvs[i].k; i++) {
    lua_pushinteger(L, kvs[i].v);
    lua_setfield(L, table, kvs[i].k);
  }
  return 1;
}  

typedef struct rect {
  int x, y, w, h;
} ExRect;

#define EX_RECT_PTR_NAM "ExRect"

static void pushRectPtr(lua_State *L, ExRect *p) {
  (*(void **)lua_newuserdata(L, sizeof(void *))) = p;
  luaL_getmetatable(L, EX_RECT_PTR_NAM);
  lua_setmetatable(L, -2);
}

static int ExAllocRect(lua_State *L) {
  int num = luaL_checkinteger(L, 1);
  if (num <= 0) {
    luaL_error(L, "ExAllocRect #1 must > 0");
  }

  ExRect rect = {0};
  if (lua_gettop(L) >= 2) {
    if (lua_istable(L, 2)) {
      luaL_error(L, "ExAllocRect #2 must be array");
    }

    lua_getfield(L, 2, "x");
    if (lua_isnumber(L, -1)) rect.x = lua_tonumber(L, -1);

    lua_getfield(L, 2, "y");
    if (lua_isnumber(L, -1)) rect.y = lua_tonumber(L, -1);

    lua_getfield(L, 2, "w");
    if (lua_isnumber(L, -1)) rect.w = lua_tonumber(L, -1);

    lua_getfields(L, 2, "h");
    if (lua_isnumber(L, -1)) rect.h = lua_tonumber(L, -1);
  }

  ExRect *rects = malloc(sizeof(ExRect) * num);
  int i;
  for (i = 0; i < size; ++i) {
    memcpy(rects[i], &rect, sizeof(rect));
  }

  pushRectPtr(L, rects);
  return 1;
}

static const luaL_reg ExFunctions[] = {
  {"allocRect", ExAllocRect},
  {"retInt1",   ExRetInt1},
  {"retInt2",   ExRetInt2},
  {"retIntN",   ExRetIntN},
  {"retUpper",   ExRetUpper},
  {"retHex",     ExRetHex},
  {"retIntList", ExRetIntList},
  {"retIntArray", ExRetIntArray},
  {"retHash",     ExRetHash},
  {NULL, NULL},
};

int luaopen_example(lua_State *L) {
  luaL_register(L, "example", ExFunctions);

  luaL_newmetatable(L, EX_RECT_PTR_NAM);
  lua_pushliteral(L, "__index");
  
  return 1;
}
