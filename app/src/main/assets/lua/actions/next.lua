-- skip to next song
function main()
    jcall("unmuteBackground")
    jcall("sendMediaButtonKeyCode", 87)
    jcall("quit")
    return "ok"
end
