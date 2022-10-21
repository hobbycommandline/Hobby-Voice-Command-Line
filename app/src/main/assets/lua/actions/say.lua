function main(arguments)
    table.remove(arguments, 1)
    jcall("say", table.concat(arguments, " "))
    return "ok"
end
