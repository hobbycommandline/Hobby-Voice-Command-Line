#include <jni.h>
#include <string>
#include <vector>

#include <android/log.h>
#include "lua-fix.h"
extern "C" {
#include "lua-fix.h"
#include <lauxlib.h>
#include <lua.h>
#include <lualib.h>
};

#include <android/log.h>
namespace jni_bridge {
	jclass Integer = NULL;
	jclass Float = NULL;
	jmethodID float_value = 0;
	jclass Double = NULL;
	jmethodID double_value = 0;
	jmethodID new_double = 0;
	jclass Boolean = NULL;
	jmethodID boolean_value = 0;
	jmethodID new_boolean = 0;
	jclass Class = NULL;
	jclass Object = NULL;
	jclass HashMap = NULL;
	jmethodID new_hashmap = 0;
	jmethodID hashmap_put = 0;
	jclass Map = NULL;
	jmethodID map_entry_set = 0;
	jclass Set = NULL;
	jmethodID iterator = 0;
	jclass Map_Entry = NULL;
	jmethodID map_entry_get_value = 0;
	jmethodID map_entry_get_key = 0;
	jclass Iterator = NULL;
	jmethodID has_next = 0;
	jmethodID next = 0;
	jclass String = NULL;
	jmethodID is_array = 0;
	jmethodID int_value = 0;
	jmethodID new_integer = 0;
	jclass LuaDispatcher = NULL;
	jmethodID j_call = 0;
	jclass Callback = NULL;
	jmethodID new_callback = 0;
	std::string* files_path_lua = NULL;
	std::string* files_path = NULL;
};

static int lua_j_call (lua_State *L);
static int lua_unwind_error_stack(JNIEnv* env, jobject lua_dispatcher, lua_State* L);

inline bool is_java_array(JNIEnv *env, jobject object) {
	jclass obj_class = env->GetObjectClass(object);
	return env->CallBooleanMethod(obj_class, jni_bridge::is_array);
}

/**
 * This must be called synchronously
 */
class _LuaInstance {
	lua_State *L;
	int id;
	// since we're synchronous, we can store for the
	// length of call_top
	jobject lua_dispatcher = NULL;
	// since we're synchronous, we can store for the
	// length of call_top
	JNIEnv* env = NULL;
public:
	_LuaInstance(int id):id(id), L(luaL_newstate()) {
		luaL_openlibs(L);
		lua_pushlightuserdata(L, (void*)this);
		lua_pushcclosure(L, &lua_j_call, 1);
		lua_setglobal(L, "jcall");
	}
	~_LuaInstance() {
		lua_close(L);
	}
	void run(const char* script) {
		std::string callback_path(*jni_bridge::files_path + "/lua/lib/base.lua");
		luaL_dofile(L, callback_path.c_str());
		luaL_dofile(L, script);
	}

	/**
	 * Calls a lua method by name with java arguments
	 * @param env
	 * @param j_function_name name of the function in lua
	 * @param arguments arguments to the lua funciton, as java arguments
	 * @return result of the lua function as a java object.
	 */
	jobject call(JNIEnv* env, jobject lua_dispatcher, jstring j_function_name, jobjectArray arguments) {
		// push function to stack
		const char *function_name = env->GetStringUTFChars(j_function_name, NULL);
		lua_getglobal(L, function_name);
		env->ReleaseStringUTFChars(j_function_name, function_name);

		return call_top(env, lua_dispatcher, arguments);
	}

	/**
	 * calls a callback, returns the result as a java object
	 * @param env
	 * @param callback_id
	 * @param arguments
	 * @return result of callback
	 */
	jobject callback(JNIEnv* env, jobject lua_dispatcher, lua_Integer callback_id, jobjectArray arguments) {
		lua_getglobal(L, "_Gcallbacks");
		lua_geti(L, -1, callback_id);
		return call_top(env, lua_dispatcher, arguments);
	}

	/**
	 * Deletes callback with a known id
	 *
	 * Stack unaffected.
	 * @param env
	 * @param callback_id
	 */
	void delete_callback(lua_Integer callback_id) {
		lua_getglobal(L, "_Gdelete_callback");
		lua_pushinteger(L, callback_id);
		lua_pcall(L, 1, 0,0);
	}

	/**
	 * pops the L stack, converts it into a callback
	 * @param env
	 * @return id of the new callback
	 */
	lua_Integer pop_callback(JNIEnv* env) {
		//start with L: function
		lua_getglobal(L, "_Gmake_callback");
		// L: function, _Gdelete_callback
		lua_rotate(L, -2, 1);
		// L: _Gdelete_callback, function
		lua_pcall(L, 1, 1,0);
		lua_Integer id = lua_tointeger(L, -1);
		lua_pop(L, 1);
		return id;
	}

	/**
	 * Assumes top of L is a function. Pops the stack, calls the function with java object arguments
	 * @param env
	 * @param arguments
	 * @return result of lua function converted to a java object
	 */
	jobject call_top(JNIEnv* env, jobject lua_dispatcher, jobjectArray arguments) {
		// only ok because we're being called synchronously
		this->lua_dispatcher = lua_dispatcher;
		// only ok because we're being called synchronously
		this->env = env;
		// push args
		lua_Integer n_args = unpack_object_array(env, arguments);
		// call function
		if(lua_pcall(L, n_args, 1, NULL) != LUA_OK) {
			lua_unwind_error_stack(env, lua_dispatcher, L);
		}

		// convert result to java and return
		auto result = pop_object(env, lua_dispatcher);
		this->lua_dispatcher = NULL;
		this->env = NULL;
		return result;
	}

	/**
	 * Unpacks an object array onto the lua stack, ready for calling a function.
	 *
	 * @param env
	 * @param object_array
	 */
	lua_Integer unpack_object_array(JNIEnv* env, jobjectArray object_array) {
		jsize size = env->GetArrayLength(object_array);
		for (jsize i = 0; i < size; ++i) {
			jobject object = env->GetObjectArrayElement(object_array, i);
			push_object(env, object);
		}
		return (lua_Integer)(size);
	}

	/**
	 * Convert a java object array to a lua table. put the table on the
	 * lua stack.
	 *
	 * @param env
	 * @param object_array
	 */
	void push_object_array(JNIEnv* env, jobjectArray object_array) {
		jsize size = env->GetArrayLength(object_array);
		lua_newtable(L);
		lua_Integer table_index = 0;
		for (jsize i = 0; i < size; ++i) {
			jobject object = env->GetObjectArrayElement(object_array, i);
			push_object(env, object);
			lua_rawseti(L, -2, ++table_index);
		}
	}

/**
	 * push a java object onto the lua stack, converting it to lua
	 * and pushing it onto the lua stack @ L-1
	 *
	 * Currently handles: null, Integer, Float, Double, String, Array, Map
	 *
	 * Default: nil
	 *
	 * @param env
	 * @param object
	 */
	void push_object(JNIEnv* env, jobject object) {
		if (object == NULL) {
			lua_pushnil(L);
			return;
		}
		jclass objectClass = env->GetObjectClass(object);
		// no clue if this or a map will be faster in practice
		// but its probably this
		if (env->IsSameObject(objectClass, jni_bridge::Integer)) {
			lua_pushinteger(L, env->CallIntMethod(object, jni_bridge::int_value));
		} else if (env->IsSameObject(objectClass, jni_bridge::Double)) {
			lua_pushnumber(L, env->CallDoubleMethod(object, jni_bridge::double_value));
		} else if (env->IsSameObject(objectClass, jni_bridge::Float)) {
			lua_pushnumber(L, env->CallFloatMethod(object, jni_bridge::float_value));
		}  else if (env->IsSameObject(objectClass, jni_bridge::Boolean)) {
			lua_pushboolean(L, env->CallBooleanMethod(object, jni_bridge::boolean_value));
		} else if (env->IsSameObject(objectClass, jni_bridge::String)) {
			jstring j_string = (jstring)object;
			const long l_string = env->GetStringUTFLength(j_string);
			const char* string = env->GetStringUTFChars(j_string, NULL);

			lua_pushlstring(L,string, l_string);

			env->ReleaseStringUTFChars(j_string, string);

		} else if(is_java_array(env, object)){
			push_object_array(env, (jobjectArray)object);
		} else if (env->IsInstanceOf(object, jni_bridge::Map)) {
			jobject map_entry_set = env->CallObjectMethod(object, jni_bridge::map_entry_set);
			jobject iterator = env->CallObjectMethod(map_entry_set, jni_bridge::iterator);
			lua_newtable(L);
			while (env->CallBooleanMethod(iterator, jni_bridge::has_next)) {
				jobject entry = env->CallObjectMethod(iterator, jni_bridge::next);
				jobject key = env->CallObjectMethod(entry, jni_bridge::map_entry_get_key);
				jobject value = env->CallObjectMethod(entry, jni_bridge::map_entry_get_value);
				push_object(env, key);
				push_object(env, value);
				lua_rawset(L, -3);
			}
		} else { // if couldn't convert, push nil to maintain indexes
			lua_pushnil(L);
		}
	}

	/**
	 * Convert the top lua object to a java object. Pops lua stack.
	 *
	 * handles: int, string, function, array, map, double (64 bit float), boolean
	 *
	 * @param env
	 * @return new java object
	 */
	jobject pop_object(JNIEnv* env, jobject lua_dispatcher) {
		if (lua_isinteger(L, -1)) {
			lua_Integer i = lua_tointeger(L, -1);
			lua_pop(L, 1);
			return env->NewObject(jni_bridge::Integer, jni_bridge::new_integer, (int)i);
		} else if (lua_isboolean(L, -1)) { // must come after string, integer
			lua_Integer b = lua_toboolean(L, -1);
			lua_pop(L, 1);
			return env->NewObject(jni_bridge::Boolean, jni_bridge::new_boolean, (jboolean)b);
		} else if(lua_isstring(L, -1)) {
			const char* l_string = lua_tostring(L, -1);
			lua_pop(L, 1);
			return env->NewStringUTF(l_string);
		} else if (lua_isnumber(L, -1)) { // must come after string, integer
			lua_Number d = lua_tonumber(L, -1);
			lua_pop(L, 1);
			return env->NewObject(jni_bridge::Double, jni_bridge::new_double, (double)d);
		} else if (lua_isfunction(L, -1)) {
			lua_Integer callback_id = pop_callback(env);
			return env->NewObject(jni_bridge::Callback, jni_bridge::new_callback, lua_dispatcher, (int)callback_id);
		} else if(lua_istable(L, -1)) {
			lua_getglobal(L, "table");
			lua_getfield(L, -1, "is_array");
			// remove global["table"]
			lua_remove(L, -2);
			// copy the table to use as an argument
			lua_pushvalue(L, -2);
			if(lua_pcall(L, 1, 1, NULL) != LUA_OK) {
				lua_unwind_error_stack(env, lua_dispatcher, L);
				return NULL;
			}
			lua_Integer is_array = lua_toboolean(L, -1);
			lua_Integer array_len = 0;
			lua_pop(L, 1);
			jobjectArray array = NULL;
			jobject map = NULL;
			if (is_array) {
				lua_len(L, -1);
				array_len = lua_tointeger(L, -1);
				array = env->NewObjectArray(array_len, jni_bridge::Object, NULL);
				lua_pop(L, 1);
			} else {
				// init map here
				map = env->NewObject(jni_bridge::HashMap, jni_bridge::new_hashmap);
			}
			// iterate table
			lua_pushnil(L);  /* first key */
			while (lua_next(L, -2) != 0) {
				/* uses 'key' (at index -2) and 'value' (at index -1) */
				if (is_array) {
					if (lua_type(L, -2) == LUA_TNUMBER) {
						// value
						// key
						jobject value = pop_object(env, lua_dispatcher);
						// key
						// convert from 1 index to zero index
						lua_Integer index = lua_tointeger(L, -1) - 1;
						env->SetObjectArrayElement(array, index, value);
					} else {
						lua_pop(L, 1);
					}
				} else {
					if (lua_type(L, -2) == LUA_TSTRING) {
						// value
						// key
						jobject value = pop_object(env, lua_dispatcher);
						// key
						// clone key
						lua_pushvalue(L, -1);
						// key
						// key
						// pop key
						jstring key = (jstring)pop_object(env, lua_dispatcher);
						// key
						env->CallObjectMethod(map, jni_bridge::hashmap_put, key, value);
					} else {
						lua_pop(L, 1);
					}
				}
			}
			// table
			lua_pop(L, 1);
			if (is_array) {
				return array;
			} else {
				return map;
			}
		}
		lua_pop(L, 1);
		return NULL;
	}

	int j_call(lua_State *L) {
		if (env == NULL || lua_dispatcher == NULL) {
			lua_pushnil(L);
			return 1;
		}
		lua_Integer function_name_index = -2;
		if (!lua_isstring(L, function_name_index)) {
			function_name_index = -1;
			if (!lua_isstring(L, function_name_index)) {
				lua_pushnil(L);
				return 1;
			}
		}
		jobject argument = NULL;
		if (function_name_index == -2) {
			argument = pop_object(env, lua_dispatcher);
		}

		const char* l_string = lua_tostring(L, -1);
		jstring functionName = env->NewStringUTF(l_string);
		lua_pop(L, 1);

		jobject result = env->CallObjectMethod(lua_dispatcher, jni_bridge::j_call, functionName, argument);
		push_object(env, result);
		return 1;
	}
};

typedef		 _LuaInstance*	LuaInstance;


std::vector<LuaInstance> LUA_INSTANCES;
static inline LuaInstance get_instance(jint scriptId) {
	if (jni_bridge::LuaDispatcher == NULL) {
		return NULL;
	}
	if (scriptId < 0 || scriptId >= LUA_INSTANCES.size()) {
		return NULL;
	}
	LuaInstance instance = LUA_INSTANCES[scriptId];
	return instance;
}

void setup(JNIEnv* env, jstring files_path) {
	jni_bridge_files_path_length = env->GetStringUTFLength(files_path);

	jni_bridge_files_path = env->GetStringUTFChars(files_path, NULL);
	jni_bridge::files_path = new std::string(jni_bridge_files_path);
	env->ReleaseStringUTFChars(files_path, jni_bridge_files_path);
	jni_bridge_files_path = jni_bridge::files_path->c_str();

	jobject local = env->FindClass("org/hobby/luabridge/LuaDispatcher");
	jni_bridge::LuaDispatcher = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("org/hobby/luabridge/LuaDispatcher$Callback");
	jni_bridge::Callback = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/lang/Integer");
	jni_bridge::Integer = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/lang/Float");
	jni_bridge::Float = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);
	jni_bridge::float_value = env->GetMethodID(jni_bridge::Float, "floatValue", "()F");

	local = env->FindClass("java/lang/Double");
	jni_bridge::Double = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);
	jni_bridge::double_value = env->GetMethodID(jni_bridge::Double, "doubleValue", "()D");
	jni_bridge::new_double = env->GetMethodID(jni_bridge::Double, "<init>", "(D)V");

	local = env->FindClass("java/lang/Boolean");
	jni_bridge::Boolean = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);
	jni_bridge::boolean_value = env->GetMethodID(jni_bridge::Boolean, "booleanValue", "()Z");
	jni_bridge::new_boolean = env->GetMethodID(jni_bridge::Boolean, "<init>", "(Z)V");

	local = env->FindClass("java/lang/String");
	jni_bridge::String = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/lang/Object");
	jni_bridge::Object = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/util/HashMap");
	jni_bridge::HashMap = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/util/Map$Entry");
	jni_bridge::Map_Entry = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/util/Set");
	jni_bridge::Set = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	jni_bridge::iterator = env->GetMethodID(jni_bridge::Set, "iterator",
	                                        "()Ljava/util/Iterator;");
	local = env->FindClass("java/util/Iterator");
	jni_bridge::Iterator = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	jni_bridge::has_next = env->GetMethodID(jni_bridge::Iterator, "hasNext",
	                                        "()Z");
	jni_bridge::next = env->GetMethodID(jni_bridge::Iterator, "next",
	                                    "()Ljava/lang/Object;");


	local = env->FindClass("java/util/Map");
	jni_bridge::Map = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	local = env->FindClass("java/lang/Class");
	jni_bridge::Class = (jclass)env->NewGlobalRef(local);
	env->DeleteLocalRef(local);

	jni_bridge::map_entry_set = env->GetMethodID(jni_bridge::Map, "entrySet",
	                                             "()Ljava/util/Set;");
	jni_bridge::map_entry_get_key = env->GetMethodID(jni_bridge::Map_Entry, "getKey",
	                                                 "()Ljava/lang/Object;");
	jni_bridge::map_entry_get_value = env->GetMethodID(jni_bridge::Map_Entry, "getValue",
	                                                   "()Ljava/lang/Object;");


	jni_bridge::new_hashmap = env->GetMethodID(jni_bridge::HashMap, "<init>", "()V");
	jni_bridge::hashmap_put = env->GetMethodID(jni_bridge::HashMap, "put",
	                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

	jni_bridge::is_array = env->GetMethodID(jni_bridge::Class, "isArray", "()Z");

	jni_bridge::int_value = env->GetMethodID(jni_bridge::Integer, "intValue", "()I");
	jni_bridge::new_integer = env->GetMethodID(jni_bridge::Integer, "<init>", "(I)V");

	jni_bridge::new_callback = env->GetMethodID(jni_bridge::Callback, "<init>",
																							"(Lorg/hobby/luabridge/LuaDispatcher;I)V");
	jni_bridge::j_call = env->GetMethodID(jni_bridge::LuaDispatcher, "execJvm",
	                                      "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");

	jni_bridge::files_path_lua = new std::string(std::string(jni_bridge_files_path) + std::string("/?.lua"));
	jni_bridge_files_path_lua = jni_bridge::files_path_lua->c_str();
}

extern "C" JNIEXPORT void JNICALL
Java_org_hobby_luabridge_LuaDispatcher_setup(
																							JNIEnv* env,
																							jclass LuaDispatcher, jstring files_path) {
	setup(env, files_path);
}

extern "C" JNIEXPORT jint JNICALL
Java_org_hobby_luabridge_LuaDispatcher_runScript(
																									JNIEnv* env,
																									jclass LuaDispatcher, jstring jscript) {
	if (jscript == NULL) {
		return -1;
	}
	if (jni_bridge::LuaDispatcher == NULL) {
		return -1;
	}
	int id = LUA_INSTANCES.size();
	auto instance = new _LuaInstance(id);
	LUA_INSTANCES.push_back(instance);
	const char* script = env->GetStringUTFChars(jscript, NULL);

	instance->run(script);

	env->ReleaseStringUTFChars(jscript, script);

	return id;
}

extern "C" JNIEXPORT void JNICALL
Java_org_hobby_luabridge_LuaDispatcher_destroyLuaInstance(
																													 JNIEnv* env,
																													 jclass LuaDispatcher, jint scriptId) {
	if (scriptId >= LUA_INSTANCES.size()) {
		return;
	}
	auto instance = LUA_INSTANCES[scriptId];
	if (instance != NULL) {
		delete instance;
	}
	if (scriptId == LUA_INSTANCES.size() - 1) {
		LUA_INSTANCES.pop_back();
	} else {
		LUA_INSTANCES[scriptId] = NULL;
	}
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_hobby_luabridge_LuaDispatcher_executeCallback(
																												JNIEnv* env,
																												jobject dispatcher, jint scriptId, jint callbackId, jobjectArray arguments) {
	if(auto instance = get_instance(scriptId)){
		return instance->callback(env, dispatcher, callbackId, arguments);
	}
	return NULL;
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_hobby_luabridge_LuaDispatcher_executeFunction(
																												JNIEnv* env,
																												jobject dispatcher, jint scriptId, jstring functionName, jobjectArray arguments) {
	if(auto instance = get_instance(scriptId)){
		return instance->call(env, dispatcher, functionName, arguments);
	}
	return NULL;
}

extern "C" JNIEXPORT void JNICALL
Java_org_hobby_luabridge_LuaDispatcher_destroyCallback(
																												JNIEnv* env,
																												jclass LuaDispatcher, jint scriptId, jint callback_id) {
	if(auto instance = get_instance(scriptId)){
		instance->delete_callback(callback_id);
	}
}

static int lua_j_call (lua_State *L) {
	LuaInstance lua_instance = (LuaInstance)lua_touserdata(L, lua_upvalueindex(1));
	return lua_instance->j_call(L);
}

static void j_say_top(lua_State* L, JNIEnv* env, jobject lua_dispatcher) {
	if (L == NULL || env == NULL || lua_dispatcher == NULL) {
		return;
	}
	const char* say = "say";
	jstring j_say = env->NewStringUTF(say);
	const char* error = lua_tostring(L, -1);
	jstring j_error = env->NewStringUTF(error);
	env->CallObjectMethod(lua_dispatcher, jni_bridge::j_call, j_say, j_error);
}

static int lua_unwind_error_stack(JNIEnv* env, jobject lua_dispatcher, lua_State* L) {
	lua_writestringerror("Unwinding error stack", NULL);
	// pop everything and print it.
	for(lua_Integer top = lua_gettop(L); top > 0; top--) {
		const char* error = lua_tostring(L, -1);
		lua_writestringerror("Protected error: (%s)", error);
		j_say_top(L, env, lua_dispatcher);
		lua_pop(L, 1);
	}

	lua_pushnil(L);
	return 1;
}
