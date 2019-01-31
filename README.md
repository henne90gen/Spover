# Summary
Spover offers an overlay for navigation apps (or anythin else) showing the speed limit information and current speed.

Adaptations:
- automatic detection of navigation start
- manual caching speedlimit data for areas without network connection
- overlay instead of whole screen
- sound when user is driving over speedlimit
- different overlay color depending on how much faster/slower than the speedlimit the user is driving

Architecture:
- local database with Room on device for caching
- Android SDK (detect navigation start)
- OSM server connection

## Notes

To build the app, a file called keys.xml is required to be in app/src/main/res/values. This file should have the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_api_key">--- Valid Google Maps API Key ---</string>
</resources>
```

## CREDITS

Sounds from:
(https://freesound.org/people/tim.kahn/ not anymore)
https://freesound.org/people/thisusernameis/sounds/426888/