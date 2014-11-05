## Random Timer

Random Timer is an app designed for party games and other situations where you need a countdown
that will expire after a randomized amount of time and without the participants knowing how much
time is remaining. Current features include:

 - Configurable maximum and minimum values, that will be used when randomly picking a starting time
 - A choice of notification methods, including an alert sound or vibration
 - Continuous mode, for quick back-to-back rounds of gaming
 - Easy to use, one-button interface
 - Clear visual notification when time is running or stopped
 - Dark or light theme
 
The most recent version of Random Timer is available [on Google Play](https://play.google.com/store/apps/details?id=com.eldarerathis.randomtimer).

## Building the source

For now, Random Timer is still using Eclipse/ADT for its build system. It requires these libraries:

 - android-support-v7-appcompat
 - android-support-v4
 - [Android Bootstrap](https://github.com/Bearded-Hen/Android-Bootstrap)
 
You should be able to either use pre-built .jar files or the sources for the project libraries, but
the build has only been tested with the library sources included as projects in the build.

Migration to Android Studio is still a to-do item.

## License

Random Timer is licenced under the GPLv3. See the LICENSE file for the full text of the license.