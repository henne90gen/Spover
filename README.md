# Spover

<img src="https://raw.githubusercontent.com/henne90gen/Spover/master/images/app_icon.png" align="left" width="50px"/>

Display an overlay on navigation apps showing the speed limit information and current speed.

| light mode | dark mode |
| ---------- | --------- |
| <img src="https://raw.githubusercontent.com/henne90gen/Spover/master/images/overlay_screenshot.png" height="600px" /> | <img src="https://raw.githubusercontent.com/henne90gen/Spover/master/images/nightmode.png" height="600px" /> |

## Features

### Autostart

- Spover starts the overlay automatically as soon as you start the navigation on GMaps

### Nightmode

- adapt theme to current environmental brightness

### Caching speed limit data

- manual caching speedlimit data for areas without network connection

### Warnings
- sound when user is driving over speedlimit
- different overlay color depending on how much faster/slower than the speedlimit the user is driving

## Architecture:

- Android SDK (detect navigation start)
    - Service to display an UI over other apps
    - HttpsUrlConnection to fetch data from OpenStreetMaps API
    - Room database library to save speed limit data locally
- Jackson library to parse XML response
- JUnit for testing


## Way mapping

<img src="https://raw.githubusercontent.com/henne90gen/Spover/master/images/way_mapping.png" />

## Notes

To build the app, a file called keys.xml is required to be in app/src/main/res/values. This file should have the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_api_key">--- Valid Google Maps API Key ---</string>
</resources>
```

## CREDITS

warning sound: https://freesound.org/people/thisusernameis/sounds/426888/

open street map: https://www.openstreetmap.org
