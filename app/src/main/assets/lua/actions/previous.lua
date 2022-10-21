-- skip to previous song
function main()
    jcall("sendMediaButtonAction", 16)
    return "ok"
end
