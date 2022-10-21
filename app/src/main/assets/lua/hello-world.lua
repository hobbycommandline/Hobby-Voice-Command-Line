require("lua/lib/util")

print("hello friends!")
function addition_test(first, second)
	 print("hello world")
	 return first + second
end

function ordering_test(first, second)
	 return tostring(first) .. ", " .. tostring(second)
end

function string_roundtrip_test(first, second)
	 return first .. ", " .. second
end

function test_closure(word)
	 return function (second_word)
	    print(table.concat({"in a closure! " , word , " " , second_word}))
	 end
end

function test_array_in(array)
    print(table.concat(array, " "))
end

function test_array_out()
    return {1,2,3, "a", "b", "c"}
end

function test_map_in(map)
    print(table.tostring(map))
end

function test_map(array)
    return {a=1, b=2}
end

function test_map_out_complex()
    return {
        action="hello!",
        extras={
            ["how.are.you"]=2
        }
    }
end