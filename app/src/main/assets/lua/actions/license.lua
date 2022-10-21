require("lua/lib/say")
function main(arguments)
    return [[The licenses are stored in the assets folder under license/...
If you really need to read up on it check in there. I don't think
this app can handle reading out all the licenses at once, so here's
a summary.

Vosk API (Speech to text AI library) - Apache License Version 2.0
vosk-model-small-en-us-0.15 (AI core model) - Apache License Version 2.0

Lua (Scripting language you can use to configure this app,
see your /Android/data/org.hobby.voicecommandline/files/lua folder)
Lua is MIT License

The other code for this app written by me, the author, is under AGPL
(called main.LICENSE.txt in the license/ folder). This license is
for ensuring any products made with this code come with source code.
That includes this one, get the source at
https://github.com/hobbycommandline/Hobby-Scheme-Command-Line
]]
end
