--[[resume playing of current song
  ;; play -> resume current audio
  ;; possibly in future I will redirect to the "music" command as default if
  ;; play fails?
]]
require("lua/lib/say")
require("lua/lib/intents")
require("lua/lib/util")
did_ok = false
first = false
function main()
    jcall("sendMediaButtonActionCallback", {button=4, callback=broadcastReceiver})
    return "ok"
end

function launchPlay()
    say("no app responded to play press, starting music activity")
    local intent = {
    action="android.intent.action.MAIN",
    categories={"android.intent.category.LAUNCHER", "android.intent.category.APP_MUSIC"},
    }
    startActivity(intent)
    jcall("quit")
end

function broadcastReceiver(code, data, intent)
    if code == -1 then
        did_ok = true
    elseif first then
        first = false
    else
        launchPlay()
    end
end
