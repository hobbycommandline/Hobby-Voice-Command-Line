function say(...)
    local args = {...}
    jcall("say", table.concat(args, " "))
end

function sayf(format, ...)
    say(string.format(format, ...))
end
