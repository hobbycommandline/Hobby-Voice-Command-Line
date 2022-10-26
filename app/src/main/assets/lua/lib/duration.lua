require("lua/lib/text-to-number")
function duration_args()
    return {"hours", "hour", "minutes", "minute", "seconds", "second"}
end

function duration_calculate(args)
    -- adding together singular and plural form as a way of aliasing them
    -- presumably a user won't say 1 minute 30 minutes
    local hours = numbers.list_to_number(args["hours"]) + numbers.list_to_number(args["hour"])
    local minutes = numbers.list_to_number(args["minutes"]) + numbers.list_to_number(args["minute"])
    local seconds = numbers.list_to_number(args["seconds"]) + numbers.list_to_number(args["second"])
    local length = hours * 3600 + minutes * 60 + seconds
    -- recalculate how long each time segment is
    -- this is needed to convert things like 90 seconds
    -- to 1 minute and thirty seconds
    hours = length // 3600
    minutes = (length - hours * 3600) // 60
    seconds = length - hours * 3600 - minutes * 60
    return length, hours, minutes, seconds
end