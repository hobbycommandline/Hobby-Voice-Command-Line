--[[ Convert English-Numbers to number in Lua
  ;; proper numbers
  ;; numbers.string_to_number("one million seven thousand two hundred thirty seven eight") ; 10072378
  ;; phone case
  ;; numbers.string_to_number("one two three forty fifty") ; 1234050
  ;; doesn't handle
  ;; one million million
  ;; one triple zero
  ;; one point two
  ;; one eight hundred
  ;; tenths, thousanths, sixteenths, etc
]]
numbers = {}
--[[
  -- cur: current item (number *|+ zerosOf addToSum? powMultOf)

  -- number: numerical value
  -- *|+: whether to add or multiply this number to the previous one
  -- zerosOf: a relative size comparison of the number of zeros, probably could do with some adjustment but i'd have to fix the constant checks in numlst->number
  -- addToSum? whether to directly add the result of *|+ to the sum
  -- powMultOf: how many places to multiply the current sum by when doing numbers in 'phone number case' mode
]]
numbers.word_map = {
    trillion = {1000000000000, "*", 13, true, 1},
    billion = {1000000000, "*", 10, true, 1},
    million = {1000000, "*", 7, true, 1},
    thousand = {1000, "*", 4, true, 1},
    hundred = {100, "*", 3, false, 1000},

    ten = {10, "+", 2, false, 100},
    twenty = {20, "+", 2, false, 100},
    thirty = {30, "+", 2, false, 100},
    forty = {40, "+", 2, false, 100},
    fifty = {50, "+", 2, false, 100},
    sixty = {60, "+", 2, false, 100},
    seventy = {70, "+", 2, false, 100},
    eighty = {80, "+", 2, false, 100},
    ninety = {90, "+", 2, false, 100},

    one = {1, "+", 1, false, 10},
    two = {2, "+", 1, false, 10},
    three = {3, "+", 1, false, 10},
    four = {4, "+", 1, false, 10},
    five = {5, "+", 1, false, 10},
    six = {6, "+", 1, false, 10},
    seven = {7, "+", 1, false, 10},
    eight = {8, "+", 1, false, 10},
    nine = {9, "+", 1, false, 10},
    zero = {0, "+", 1, false, 10},

    eleven = {11, "+", 1, false, 100},
    twelve = {12, "+", 1, false, 100},
    thirteen = {13, "+", 1, false, 100},
    fourteen = {14, "+", 1, false, 100},
    fifteen = {15, "+", 1, false, 100},
    sixteen = {16, "+", 1, false, 100},
    seventeen = {17, "+", 1, false, 100},
    eighteen = {18, "+", 1, false, 100},
    nineteen = {19, "+", 1, false, 100},

    o = {0, "+", 1, false, 10},
    oh = {0, "+", 1, false, 10},
    ["for"] = {4, "+", 1, false, 10},
    ["to"] = {2, "+", 1, false, 10},
    too = {2, "+", 1, false, 10},
}

--[[
Converts a list of English numbers
to a lua number.

this is the only function you should
be calling from this file
]]
function numbers.list_to_number(list, start_at_first_number)
    if list == nil then
        return 0
    end
    if start_at_first_number == nil then
        start_at_first_number = true
    end
    local list, size = numbers.list_to_numlist(list, start_at_first_number)
    return numbers.numlst_to_number(list, size)
end

--[[
you can technically call this one,
but it's more for debugging purposes
as i would assume most strings you'll
come across are already split
]]
function numbers.string_to_number(s)
    local words = {}
    local i = 1
    for word in string.gmatch(s, "[^%s]+") do
       words[i] = word
       i = i + 1
    end
    return numbers.list_to_number(words)
end


function numbers.list_to_numlist(list, start_at_first_number)
    local word_map = numbers.word_map
    local new_list = {}
    local i = 1
    local skips = 0
    local current = true
    while current ~= nil and list[i + skips] ~= nil do
        local v = list[i + skips]
        current = word_map[v]
        if current ~= nil then
            start_at_first_number = false
            new_list[i] = current
            i = i + 1
        end
        if current == nil and start_at_first_number then
            current = true
            skips = skips + 1
        end
    end
    return new_list, i - 1
end

function numbers.is_mult(n)
    return n[2] == '*'
end
function numbers.add_to_sum(n)
    return n[4]
end
function numbers.num_of(n)
    return n[1]
end
function numbers.zeros_of(n)
    return n[3]
end
function numbers.pow_mult_of(n)
    return n[5]
end
function numbers.do_mult(a, b)
    if a == 0 then
        return numbers.num_of(b)
    else
        return a * numbers.num_of(b)
    end
end

-- lst {{number *|+ zerosOf addToSum? powMultOf}, ... }
function numbers.numlst_to_number(list, size)
--[[
    ;; sum: the total sum, returned value
    ;; temp: temporary sum up to about the hundreds usually
    ;; cur: current item (number *|+ zerosOf addToSum? powMultOf)
    ;; numZeros: zerosOf, a rough estimate of the numbers size relative
    ;; to others of the previous number seen
    ;; curZeros: zerosOf but for the current number
    ;; numNum: the actual number value of the current item
]]
    local sum = 0
    local temp = 0
    local cur = 0
    local num_zeros = 0
    local cur_zeros = 0
    local num_num = 0

    for i = 1,size do
        -- current item
        local cur = list[i]
	    -- current zeros
	    local cur_zeros = numbers.zeros_of(cur)
	    -- the actual
	    local num_num = numbers.num_of(cur)
	    local is_cur_mult = numbers.is_mult(cur)

	    -- This is the 'phone number case' handler
        -- handles things like ten twenty two thirty
        if cur_zeros >= num_zeros and num_zeros ~= 0 and not is_cur_mult then
            sum = sum + temp
            sum = sum * numbers.pow_mult_of(cur)
            temp = 0
            num_zeros = 0
        end

        -- non-phone number handler
        num_zeros = cur_zeros
        if is_cur_mult then
            -- things like 2 * 100 or 37 * 1000
            temp = numbers.do_mult(temp, cur)
        else
            -- things like 30 + 7 or 100 + 30
            temp = temp + num_num
        end
        -- numbers like thousand, million, billion etc
        -- are safe to immediately add to the sum
        -- and the temp count begins again so that
        -- numbers like one million (followed by) seven hundred work
        if numbers.add_to_sum(cur) then
            sum = sum + temp
            num_zeros = 0
            temp = 0
        end
    end
    return sum + temp
end

--[=[
function numbers.test()
    local expected, result
    result = numbers.string_to_number("one million seven thousand two hundred thirty seven eight")
    expected = 10072378
    assert(result == expected, string.format("%i ~= %i", result, expected))

    result = numbers.string_to_number("one two three forty fifty")
    expected = 1234050
    assert(result == expected, string.format("%i ~= %i", result, expected))

    --[[result = numbers.string_to_number("one million million")
    expected = 1000000000000
    assert(result == expected, string.format("%i ~= %i", result, expected))]]
end
numbers.test()
]=]