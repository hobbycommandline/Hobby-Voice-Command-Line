-- skip to next song
function main()
    jcall("unmuteBackground")
    jcall("sendMediaButtonAction", 32)
    jcall("quit")
    return "ok"
end
