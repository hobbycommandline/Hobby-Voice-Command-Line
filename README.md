# Hobby Command Line

Hobby is a dangerous app.

This project was the result of using Vosk API to get text from speech and executing a Lua script based on the first word.

After reading the saftey warnings, see the [User Guide](./USER_GUIDE.md) for more helpful information.

## Safety Warnings

The app will be safer once the transition to Lua is complete. Lua does not allow arbitrary Java invocation. This app will still be able to interact with the rest of your phone, which still presents some challenges:
- when active anyone can control your phone in the same ways you can over voice.
- Lua can read and write files, this may present some danger

### Why Lua

Slightly safer and easier once already integrated than the original scheme was. I found it unappetizing to develop new features when the app was written in scheme, and I hope this alleviates that reaction.

Huzzah, seems like Lua fully works; just need to enhance communication between Lua and Java by adding Map <-> table conversion to complete the setup. Then in Java, I will need to make Map -> Intent conversion.

## Guide for Speaking to Hobby / Writing Hobby Actions

Just like linux command lines start with an action like "cat", Hobby commands should start with the action name. This is used to look up a Lua file by that name, and the Lua is responsible for interpreting what intent to fire off.

For instance "Play music" will be interpreted as:

Action: Play
Arguments: "Music"

and will be handled by "lua/actions/play.lua"

Actions are handled this way because it is efficient, quick, and predictable. Some exceptions can be made for filler words such as "Set an Alarm for ten PM" instead of having to say "Alarm ten PM", which would go to set.scm which would then redirect to alarm.scm. Hobby is not intended to interpret every way something can be said, but users can make aliases for common ways they say things to make dealing with Hobby easier.

## Replacing the system Assisstant

I don't have documentation on this currently but it is possible to replace the squeeze action without root, although it will require a screen tap if the phone was locked, otherwise the microphone will not enable. It did require me to run an ADB command from developer mode, but I wouldn't reccomend you do this unless you know what you're doing.

## Goals

Basically I would like this to do everything I want to speak to my phone about, or do with my eyes closed.

- [X] Offline Speech to Text
    - Vosk is an offline model, if you notice this app making network calls, file an issue.
    - As speech recognition is done offline, you will need a phone capable of running a model efficiently. I'm using a pixel 3.
- [ ] Offer speech to text via an intent?
- [ ] Settings panel
- [ ] Implement voice interaction API
    - https://developers.google.com/voice-actions/interaction/voice-interactions
- [X] Play/Pause/Stop Music (implemented common interface, but most apps ignore it).
    - "Stop"
    - "Play"
    - "Previous"
    - "Next"
- [x] Take a note
    - "Note [note body here]"
    - "Stop" to finish note
    - This also lets you tweet/discord/telegram
    - Unfortunately I had to go through the app picker as no one
    implemented the CREATE_NOTE API described by Google.
- [ ] Reminder API
- [ ] Make a calendar event
    - Calendar event on (date) [start (time) end (time)] [named (name)]
        - [from, to] unfortunately hard to parse because to could be two
- [ ] Manage Alarms/Timer
    - [X] Timer
    - [ ] Alarm
    - [ ] "Stop"
    - Timer [3 hours] [22 minutes] [30 seconds]
        - Timers have a max of 24 hours
    - "Alarm for (Time) [on (Date)|Daily|Weekly] [named (Name)] [repeat]"
    - "Alarm"
        1. "Please say the alarm time"
	2. "Do you want this alarm to repeat 'Daily', 'Weekly', or 'Not' ('Activate' to skip)"
	3. "Alarm name? ('Activate' to skip)"
- [X] Conversations of more than one line, if the Action needs more clarification, or is taking a note for you.
- [ ] User Scripts
- [X] Script execution in the background.
- [ ] Credit screen to thank Vosk / Lua
- [ ] Custom failure behavior
    - If Hobby doesn't understand you, I would like it to be a user preference whether the app will attempt to detect speech again, or simply close. Probably give an option to choose the number of retries.
- [X] Cancel words / button.
    - Say "stop stop stop" (at least 3 stops in a row) to cancel out of whatever action hobby is doing.


## Why did you name it that

I thought it would not be wise to call it Robby, and building things like this is my hobby. Feel free to call it whatever you want, it doesn't activate by trigger word. I launch it personally by squeezing my phone. If I ever allow it to recognize its name, I will include the ability to overwrite it.

## Intents

Most intents are not implemented by this app at the moment, but the list of things you could possibly implement is contained in this section.

### Music

- https://developer.android.com/guide/topics/media-apps/audio-app/media-controller-test
- https://developer.android.com/guide/topics/media-apps/interacting-with-assistant

### Other

- https://developer.android.com/guide/components/intents-common
- https://developers.google.com/voice-actions/system#system_actions_reference
- https://developers.google.com/assistant/app/reference/built-in-intents

## Licensing

- Alphacephei's Vosk API, Apache 2.0
    - They offer an enterprise edition if you think the speech to text is good for your company at https://alphacephei.com/en/
    - They offer improved accuracy too, I'm just using freely available models
- vosk-model-small-en-us-0.15, Apache 2.0 Alphacephei
    - There are better models if you want at the cost of more disk space
    - There are also other languages, I'm using english because I can only chose one language to package (doing more would waste space) so if you want your language supported, make a build using the appropriate model and redo the code that decides which intents to create
    - https://alphacephei.com/vosk/models
- Lua license (MIT)
- The sounds
    - I made them using zynaddsubfx and Ardour
- All the extra code I wrote is AGPL (GNU AFFERO GENERAL PUBLIC LICENSE Version 3, 19 November 2007) with the caveat that you are not allowed to sell this app you may distribute this app for free in compliance with AGPL on side-loading app stores as is. You may only distribute this app on non-side-loaded app stores like the Android Store if it is for free and under a different name and complies with AGPL.
- If you are the Dicio project, you may use my code under the same GPL license you use. 
