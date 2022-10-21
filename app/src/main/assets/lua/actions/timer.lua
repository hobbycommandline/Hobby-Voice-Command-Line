--[[
timer [x hours] [x minutes] [x seconds]
-> starts a timer using system default timer app
]]
require("lua/lib/say")
require("lua/lib/text-to-number")
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

    local args, extras, reverse_args = parse_args(arguments, {}, {"hours", "hour", "minutes", "minute", "seconds", "second"})
    -- adding together singular and plural form as a way of aliasing them
    -- presumably a user won't say 1 minute 30 minutes
    local hours = numbers.list_to_number(reverse_args["hours"]) + numbers.list_to_number(reverse_args["hour"])
    local minutes = numbers.list_to_number(reverse_args["minutes"]) + numbers.list_to_number(reverse_args["minute"])
    local seconds = numbers.list_to_number(reverse_args["seconds"]) + numbers.list_to_number(reverse_args["second"])
    local say_parts, i = {"Setting a timer for"}, 2
    local length = hours * 3600 + minutes * 60 + seconds
    -- recalculate how long each time segment is
    -- this is needed to convert things like 90 seconds
    -- to 1 minute and thirty seconds
    hours = length // 3600
    minutes = (length - hours * 3600) // 60
    seconds = length - hours * 3600 - minutes * 60
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
    extras = {
    ["android.intent.extra.alarm.LENGTH"]=length
    }
    })
end
