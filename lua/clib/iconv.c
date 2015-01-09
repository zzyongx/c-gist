#include <stdlib.h>
#include <string.h>
#include <iconv.h>
#include <lua.h>
#include <lauxlib.h>

/* gcc -shared -o iconv.so -g -fPIC -Wall -Werror iconv.c */

static int Lexec(lua_State *L)
{
  size_t innum = lua_strlen(L, 1);
  char *inbuf = (char *) luaL_checkstring(L, 1);

  const char *fcode = luaL_checkstring(L, 2);
  const char *tcode = luaL_checkstring(L, 3);

  iconv_t cd = iconv_open(tcode, fcode);
  if (cd ==  (iconv_t) -1) {
    lua_pushnil(L);
    return 1;
  }

  size_t outnum = innum * 4;
  char *p = malloc(outnum);
  char *outbuf = p;
  
  size_t rc = iconv(cd, &inbuf, &innum, &outbuf, &outnum);

  if (rc == (size_t) -1) {
    lua_pushnil(L);
  } else {
    lua_pushlstring(L, p, outbuf - p);
  }

  free(p);
  iconv_close(cd);

  return 1;
}

static const luaL_reg Lfunctions[] = {
  {"exec", Lexec},
  {NULL, NULL},
};

int luaopen_iconv(lua_State *L) {
  luaL_register(L, "iconv", Lfunctions);
  return 1;
}
