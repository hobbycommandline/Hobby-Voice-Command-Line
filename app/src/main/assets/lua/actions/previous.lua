-- skip to previous song
function main()
    jcall("unmuteBackground")
    jcall("sendMediaButtonAction", 16)
    jcall("quit")
    return "ok"
end
