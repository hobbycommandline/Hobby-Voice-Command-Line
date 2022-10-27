-- skip to previous song
function main()
    jcall("unmuteBackground")
    jcall("sendMediaButtonKeyCode", 88)
    jcall("quit")
    return "ok"
end
