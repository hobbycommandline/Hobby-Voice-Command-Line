require("lua/lib/argparse")
require("lua/lib/say")
require("lua/lib/intents")

function main(arguments)
    -- pop action name
    table.remove(arguments, 1)

    local intent = {
        action="android.media.action.MEDIA_PLAY_FROM_SEARCH"
    }
    local query = nil
    local focus = "vnd.android.cursor.item/*"
    local args, extra_args, args_reverse = parse_args(arguments, {"by", "artist", "genre", "album", "title", "playlist"})
    local extras = {}
    if args.by ~= nil then
        focus = "vnd.android.cursor.item/artist"
        query = table.concat(args.by, " ")
        extras["android.intent.extra.artist"] = query
    end
    if args.artist ~= nil then
        focus = "vnd.android.cursor.item/artist"
        query = table.concat(args.artist, " ")
        extras["android.intent.extra.artist"] = query
    end
    if args.genre ~= nil then
        focus = "vnd.android.cursor.item/genre"
        query = table.concat(args.genre, " ")
        extras["android.intent.extra.genre"] = query
    end
    if args.album ~= nil then
        focus = "vnd.android.cursor.item/album"
        query = table.concat(args.album, " ")
        extras["android.intent.extra.album"] = query
    end
    if args.title ~= nil then
        focus = "vnd.android.cursor.item/audio"
        query = table.concat(args.title, " ")
        extras["android.intent.extra.title"] = query
    end
    if args.playlist ~= nil then
        focus = "vnd.android.cursor.item/playlist"
        query = table.concat(args.playlist, " ")
        extras["android.intent.extra.playlist"] = query
    end
    if #extra_args > 0 then
        query = table.concat(extra_args, " ")
    end
    extras["android.intent.extra.focus"] = focus
    extras["query"] = query or ""
    intent.extras = extras
    startActivity(intent)
    jcall("quit")
    return "ok"
end
