
table.tostring = function (object)
    local keys = {"{"}
    local i = 2
    local need_comma = false
    if #object > 0 then
        keys[i] = table.concat(object,", ")
        i = i + 1
        need_comma = true
    end
    for k,v in pairs(object) do
        if type(k) ~= "number" then
            if need_comma then
                keys[i] = ","
                i = i + 1
            end
            keys[i] = k .. " ="
            i = i + 1
            if type(v) == "table" then
                keys[i] = table.tostring(v)
                i = i + 1
            else
                keys[i] = tostring(v)
                i = i + 1
            end
            need_comma = true
        end
    end
    keys[i] = "}"
    i = i + 1
    return table.concat(keys, " ")
end

function printf(format, ...)
    print(string.format(format, ...))
end