# User guide

## Start a timer

Say

    Timer [<number> hour(s)] [<number> minute(s)] [<number> second(s)]

Timers can be up to 24 hours in length

## Create a note / dictation / send tweet
Say

    Note [New|Edit] [Note title]

Then pause, and the action will begin recording your note. Speak your note and Hobby will do text to speech.

When you are done with your note, pause, then say stop.

You will be directed to the user interface for your selected app.

Hobby will close automatically after this action is complete.

## Stop Music

Say

    Stop

This will stop your music. We first ask nicely, then force the app to stop if it didn't comply.

Hobby will close automatically after this action is complete.

## Resume current media

Say

    Play

We can only ask nicely to play music, if the app refuses, we can't do anything about it. Music Player GO respects this command.

## [Currently Broken] Skip to next song

Say

    Next

We can only ask nicely, if the app refuses, we can't do anything about it. Music Player GO respects this command.

## [Currently Broken] Skip to previous song

Say

    Previous

We can only ask nicely, if the app refuses, we can't do anything about it. Music Player GO respects this command.

## Launch a music player from search

Say

    Music ([by <Artist>]|[genre <Genre>]|[albulm <albulm>]|[song <song>]|[playlist <playlist>])|[say some unstructured query here or don't]

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
