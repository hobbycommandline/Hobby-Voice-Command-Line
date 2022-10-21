require("lua/lib/argparse")
require("lua/lib/say")
require("lua/lib/util")

function main(arguments)
    -- pop name
    table.remove(arguments, 1)
    local args, extras = parse_args(arguments, {"from", "to", "by"})
    say("args:", table.tostring(args))
    say("extra args:", table.tostring(extras))
    return "ok"
end
