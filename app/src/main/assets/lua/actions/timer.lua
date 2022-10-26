--[[
timer [x hours] [x minutes] [x seconds]
-> starts a timer using system default timer app
]]
require("lua/lib/say")
require("lua/lib/duration")
require("lua/lib/argparse")
require("lua/lib/util")
require("lua/lib/intents")

function main(arguments)
    -- remove word "timer"
    table.remove(arguments, 1)
    if arguments[1] == "for" then
        -- remove word "for"
        -- seems timer four minutes and timer for three minutes
        -- are distinct enough to not cause issues
        table.remove(arguments, 1)
    end

    local args, extras = parse_args(arguments, {}, duration_args())
    local length, hours, minutes, seconds = duration_calculate(args)
    local say_parts, i = {"Setting a timer for"}, 2
    -- building up a string to tell the user how long we're setting the timer for
    if hours > 0 then
        say_parts[i] = tostring(hours)
        i = i + 1
        say_parts[i] = "hours"
        i = i + 1
    end
    if minutes > 0 then
        say_parts[i] = tostring(minutes)
        i = i + 1
        say_parts[i] = "minutes"
        i = i + 1
    end
    if seconds > 0 then
        say_parts[i] = tostring(seconds)
        i = i + 1
        say_parts[i] = "seconds"
        i = i + 1
    end
    if #say_parts > 1 then
        launch_timer(hours, minutes, seconds)
        say(table.concat(say_parts, " "))
        jcall("quit")
        return "ok"
    else
        return "Could not tell how long to set timer for."
    end
end

function launch_timer(hours, minutes, seconds)
    local length = hours * 3600 + minutes * 60 + seconds
    startActivity({
    action = "android.intent.action.SET_TIMER",
    intExtras = {
    ["android.intent.extra.alarm.LENGTH"]=length
    }
    })
end
