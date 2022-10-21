--[[
Turns on or off wake word watching mode
]]
function main(arguments)
    if arguments[2] == "stop" then
        jcall("wakeWordStop")
    else
        jcall("wakeWordWatch", arguments[2] or "computer")
    end
    return "ok"
end