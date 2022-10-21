--[[
parse a number and print it to the screen
]]
require("lua/lib/text-to-number")
require("lua/lib/say")

function main(words)
    -- pop the word "number"
    table.remove(words, 1)
    say(numbers.list_to_number(words))
    return "ok"
end
