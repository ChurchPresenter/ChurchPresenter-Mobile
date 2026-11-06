# signage-core

A copy of [`ChurchPresenter-Signage`](https://github.com/ChurchPresenter/ChurchPresenter-Signage)'s
`:core` module: the manifest, the schedule, the messages, and `decide()`, which says what a screen
shows. The TV's Signage mode uses it to speak the same protocol as the Raspberry Pi player.

The desktop keeps its own copy (`ChurchPresenter/signage-core`) the same way. **Change it in the
Signage repo first**, then copy the files here and to the desktop; a change made only here is lost
at the next copy.

Copied from Signage `0178bf6`, plus `Platform.ANDROID_TV`.
