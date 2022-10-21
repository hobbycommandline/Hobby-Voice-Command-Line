--[[
Imported automatically whenever a script runs; you do
not need to manually import this file.

These functions are required to make lua <-> kotlin
communication work, and you can modify them
at the start of your action if you need to.

Each action is launched in its own lua environment
so you won't mess anything else up if you do so
in an action file
]]
_Gcallbacks = {}
-- get_callback is just lua_geti on _Gcallbacks

function _Gdelete_callback(id)
	 _Gcallbacks[id] = nil
end

function _Gmake_callback(fn)
	 local len = #_Gcallbacks + 1 -- 1 indexed
	 _Gcallbacks[len] = fn
	 return len
end

--[[
this function will be called to determine whether a
table passed to java via jcall should be treated
as an array or as a map<string, object>
@param t: the table to check
returns is_table
]]
function table.is_array(t)
    if type(t.__isarray) == "boolean" then
        return t.__isarray
    end
    if #t > 0 then
        return true
    end
    return false
end