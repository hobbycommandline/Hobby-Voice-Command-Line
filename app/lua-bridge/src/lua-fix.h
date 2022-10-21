#pragma once
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define lua_writestring(s,l) __android_log_write(ANDROID_LOG_DEBUG, "LUA", s)
#define lua_writestringerror(s,p) __android_log_print(ANDROID_LOG_ERROR, "LUA", (s), (p))
extern const char* jni_bridge_files_path;
extern const char* jni_bridge_files_path_lua;
extern size_t jni_bridge_files_path_length;

# define LUA_PATH_DEFAULT \
	jni_bridge_files_path_lua


FILE* __jni_lua_open(const char *path, const char* mode);

//#define fopen __jni_lua_open
