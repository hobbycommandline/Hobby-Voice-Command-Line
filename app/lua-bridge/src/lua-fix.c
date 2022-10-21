#include "lua-fix.h"
#undef fopen
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
const char* jni_bridge_files_path = NULL;
const char* jni_bridge_files_path_lua = NULL;

size_t jni_bridge_files_path_length = 0;

FILE* __jni_lua_open(const char *path, const char* mode) {
  // very important, lua relies on this or require will fail
  if(path == NULL) {
    return NULL;
  }
  // starts with ./ -> a relative path; skip that part
  if(strncmp(path, "./", 2) == 0) {
    path = path + 2;
  }
  size_t path_len = strlen(path);
  size_t full_len = jni_bridge_files_path_length + path_len + 2;
  char* true_path = (char *)malloc(full_len);
  true_path[full_len - 1] = '\0';
  strcpy(true_path, jni_bridge_files_path);
  true_path[jni_bridge_files_path_length] = '/';
  strcpy(true_path + jni_bridge_files_path_length + 1, path);
  FILE* fp = fopen(true_path, mode);
  free(true_path);
  return fp;
}