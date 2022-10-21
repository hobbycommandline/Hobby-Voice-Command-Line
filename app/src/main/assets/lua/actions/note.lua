--[[
;; Note [Name of note]
;; Note body
;; stop|send

; Edit is append, not overwrite.
]]
require("lua/lib/say")
require("lua/lib/intents")
require("lua/lib/util")
function main(arguments)
    table.remove(arguments, 1)
    lines = {}
    lines_i = 1

    title = nil
    if #arguments > 0 then
        title = table.concat(arguments, " ")
    end
    jcall("setSpeechCallback", tell_me_more)
    return "say \"stop\" or \"send\" to send your note to your note app"
end

function tell_me_more(text, is_end)
    if #text == 1 and text[1] == "send" then
        is_end = true
        jcall("clearSpeechCallback")
    end
    if is_end then
        finish_note()
    else
        lines[lines_i] = table.concat(text, " ")
        lines_i = lines_i + 1
    end
end

function finish_note()
    local intent = {}
    local extras = {}
    intent.action = "android.intent.action.SEND"
    intent.extras = extras

    intent.type = "text/plain"
    if title == nil then
        title = lines[1] or "Untitled Note"
    end
    extras["android.intent.extra.TITLE"] = title
    extras["android.intent.extra.TEXT"] = table.concat(lines, "\n")
    if startActivity(intent) == nil then
        say("No Activity found to handle notes")
    end
end
