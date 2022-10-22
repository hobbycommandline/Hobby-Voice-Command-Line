--[[resume playing of current song
  ;; play -> resume current audio
]]
function main()
    jcall("sendMediaButtonAction", 4)
    jcall("quit")
    return "ok"
end
