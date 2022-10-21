
# honestly no clue, probably a lower version ok, took what
# version android.com told me to use
cmake_minimum_required(VERSION 3.4.1)
set( LUA_PATH lua-5.4.4/src )
set( LUA_SRC ${LUA_PATH}/lapi.c ${LUA_PATH}/lcode.c ${LUA_PATH}/ldebug.c ${LUA_PATH}/ldo.c ${LUA_PATH}/ldump.c ${LUA_PATH}/lfunc.c 
  ${LUA_PATH}/lgc.c ${LUA_PATH}/llex.c ${LUA_PATH}/lmem.c ${LUA_PATH}/lobject.c ${LUA_PATH}/lopcodes.c ${LUA_PATH}/lparser.c ${LUA_PATH}/lstate.c 
  ${LUA_PATH}/lstring.c ${LUA_PATH}/ltable.c ${LUA_PATH}/ltm.c ${LUA_PATH}/lundump.c ${LUA_PATH}/lvm.c ${LUA_PATH}/lzio.c ${LUA_PATH}/lauxlib.c 
  ${LUA_PATH}/lbaselib.c ${LUA_PATH}/ldblib.c ${LUA_PATH}/liolib.c ${LUA_PATH}/lmathlib.c ${LUA_PATH}/loslib.c ${LUA_PATH}/ltablib.c 
  ${LUA_PATH}/lstrlib.c ${LUA_PATH}/linit.c ${LUA_PATH}/lutf8lib.c ${LUA_PATH}/lcorolib.c ${LUA_PATH}/loadlib.c ${LUA_PATH}/lctype.c )

add_library( # Specifies the name of the library.
             lua-bridge

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${LUA_SRC} src/bridge.cc src/lua-fix.c )
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

target_link_libraries( # Specifies the target library.
        lua-bridge
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )

add_definitions(-include lua-fix.h)
include_directories(src/include/)
include_directories(src/)
include_directories(${LUA_PATH})
