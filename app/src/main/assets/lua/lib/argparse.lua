--[[ Parse vocal arguments by keywords
return table of keywords associated with values, unused arguments

e.g. "from (nine thirty eight) to (ten fifty seven) at (tweleve monroe ave)"
e.g. "(smooth jazz) by (louis)" -- "smooth jazz" is unused, by="louis"
]]
function parse_args(words, keywords, reverse_keywords, single_keywords)
    keywords = keywords or {}
    reverse_keywords = reverse_keywords or {}
    single_keywords = single_keywords or {}
    local start = -1
    local extra_args = {}
    local args = {}
    local last_keyword = nil
    local handled
    for word_index, word in ipairs(words) do
        handled = false
        for ki, keyword in ipairs(single_keywords) do
            if keyword == word then
                args[keyword] = true
                last_keyword = nil
                handled = true
                break
            end
        end
        for ki, keyword in ipairs(reverse_keywords) do
            if keyword == word then
                handle_match(start, words, word_index, args, extra_args, last_keyword, keyword)
                last_keyword = nil
                handled = true
                break
            end
        end
        for ki, keyword in ipairs(keywords) do
            if keyword == word then
                handle_match(start, words, word_index, args, extra_args, last_keyword, nil)
                last_keyword = keyword
                handled = true
                break
            end
        end
        if handled then
            start = word_index
        end
    end
    handle_match(start, words, #words + 1, args, extra_args, last_keyword, nil)
    return args, extra_args
end

function merge_args(source, target)
    for k,v in pairs(source) do
        target[k] = v
    end
    return target
end

function handle_match(start, words, word_index, args, extra_args, last_keyword, reverse_keyword)
    if start == -1 then
        if reverse_keyword ~= nil then
            insert_match(0, words, word_index, args, reverse_keyword)
        else
            -- copy unused arguments
            if word_index > 1 then
                table.move(words,1,word_index - 1,1,extra_args)
            end
        end
    else
        if last_keyword ~= nil then
            insert_match(start, words, word_index, args, last_keyword)
        end
        if reverse_keyword ~= nil then
            insert_match(start, words, word_index, args, reverse_keyword)
        end
    end
end

function insert_match(start, words, word_index, args, keyword)
    local temp_table = {}
    if start + 1 > word_index - 1 then
        args[keyword] = true
    else
        table.move(words,start + 1,word_index - 1,1,temp_table)
        args[keyword] = temp_table
    end
end
