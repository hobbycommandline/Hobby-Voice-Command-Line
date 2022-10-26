require("lua/lib/argparse")
require("lua/lib/duration")
require("lua/lib/say")
require("lua/lib/util")
require("lua/lib/intents")
-- skip backward in time by a set amount
function main(arguments)
    local directions = {"forward", "forwards", "backward", "backwards", "ford"}
    local args, extras = parse_args(arguments, {}, merge_args(duration_args(), {"times"}), directions)
    local length, hours, minutes, seconds = duration_calculate(args)
    local forward = args.forward or args.forwards or args.ford
    local backward = args.backward or args.backwards
    local direction = true
    if backward then
        direction = false
    end
    if length > 0 then
        return time_skip(length, direction)
    end
    local times = 1
    if args.times ~= nil then
        times = numbers.list_to_number(args.times)
    end
    local key = 0
    if forward then
        key = 64
    elseif backward then
        key = 8
    end
    if key == 0 then
        return "specify forward or backward"
    end
    jcall("unmuteBackground")
    for i = 1,times do
        jcall("sendMediaButtonAction", key)
    end
    jcall("quit")
    return "ok"
end

function time_skip(length, direction)
    if direction ~= true then
        length = -length
    end
    local br = function(intent)
        if not (intent.extras and type(intent.extras.position) == "number") then
            say("Could not find media position")
        else
            local new_pos = length + intent.extras.position
            if new_pos < 0 then
                new_pos = 0
            end
            setMediaTime(new_pos)
        end
    end
    jcall("observeMusicState", br)
    return "ok"
end

function setMediaTime(new_pos)
    -- trying to mimic what carbon player expects but idk
    -- definitely not official
    sendIntent(
    {action="android.media.browse.MediaBrowserService",
        ["android.intent.extra.INTENT"]={action="SEEK",
           extras={
           POSITION=new_pos
           }
       }
    })
end
