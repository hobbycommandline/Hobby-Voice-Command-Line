--[[resume playing of current song
  ;; play -> resume current audio
]]
function main()
    jcall("sendMediaButtonKeyCode", 126)
    jcall("quit")
    return "ok"
end
