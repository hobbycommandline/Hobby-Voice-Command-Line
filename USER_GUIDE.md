# User guide

## Start a timer

Say

    Timer [for] [<number> hour(s)] [<number> minute(s)] [<number> second(s)]

- Timers can be up to 24 hours in length
- Arguments can be in any order or omitted
- Hobby will close automatically after this action is complete.

## Create a note / dictation / send tweet
Say

    Note [Note title]

Then pause, and the action will begin recording your note. Speak your note and Hobby will do text to speech.

- If you do not say a title, the first line of your note will
also be your title.
- When you are done with your note, pause, then say stop.
- You will be directed to the user interface for your selected app.

## Resend action (like resend note)
Say

    Again

If hobby has not been closed since sending an action or intent,
you can resend the last intent. This is intended so you
don't accidentally send a note to the wrong app and
lose the ability to resend it to a different application.

## Stop Music

Say

    Stop

This will stop your music.

- We first ask nicely, then force the app to stop if it didn't comply.
- Hobby will close automatically after this action is complete.

## Resume current media

Say

    Play

- resumes playback of last played media
- Hobby quits on complete

## Skip to next song

Say

    Next

- Plays next media
- Hobby quits on complete

## Skip to previous song

Say

    Previous

- Plays previous media
- Hobby quits on complete

## Launch a music player from search

Say

    Music ([by <Artist>][artist <Artist>]|[genre <Genre>]|[albulm <albulm>]|[title <song title>]|[playlist <playlist>])|[say some unstructured query here or don't]

Who knows how many apps actually support `INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH`,
which is what this relies on, but if you have an app that does here's some samples of what you can request.

I plan to in the future add the ability to specify multiple query parts such as like both song and artist etc, but haven't built out the argument->dictionary tech for that yet.

Examples:
- Music
    - Play where you left off, probably. (it's up to your music app)
- Music by left at london
- Music genre rock
- Music song supermassive black hole
- Music album T I A P F Y H
- Music playlist happy tunes
- Music give me those beats
    - This is unstructured search mode, who knows what will happen if you say this, but it is an option

## License

Say

    license

Speaks a brief overview of the code licensed in the app

## Wake word mode

Say

    watch [wakeword] [stop]

Uses the first word said after watch as a wake word. Default is
"Computer". Puts the AI to sleep in a listening mode that
watches for the word you specified. If the word is said,
the app wakes up and accepts new commands again. It seems the
app must be on screen for this to work so splitscreen must
be used. It also has a real hard time interpreting voices
over music or other sound, so this is not the best.

- Saying "watch stop" turns this mode off

## Number

Say

    number some english number here

- Demo of number parsing

## Quit

Say

    quit

The app will shut down, even in wake word mode

## Say

Say

    say some words here

A repeat after me mode

