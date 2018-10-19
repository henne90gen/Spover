# ApplicationDevelopment

App to add speed limit information as an overlay to navigation apps.

Challenges to be tackled:

- connectivity/offline: might not have the best internet 'on the go'
- form factor: navigation app should still be visible/usable

Adaptions:

- automatic detection of navigation start
- prefetching speedlimit data for areas without network connection
- overlay instead of whole screen
- sound when user is driving over speedlimit
- different overlay color depending on how much faster/slower than the speedlimit the user is driving

Architecture:

- android app
- local database on device for cache
- Android SDK (detect navigation start)
- OSM server connection

Work plan until 31.10.2018:

- check out existing APIs to do the adaptations
- make yourself familiar with platform and development tools
- project setup
- evaluate data sources for speed limit data
- create UI mockups
- finish first presentation

## **01.11.2018/02.11.2018** First Presentation

- app scenario
- use cases (mockups?)
- challenges of mobile computing
- adaptions of app
- architecture proposal

## **07.12.2018/14.12.2018** Second Presentation

- adaptation concepts
- 2 specific adaptation mechanisms
- method to map context features to app parameters
- detailed architecture
- tech stack

## **31.01.2019/01.02.2018** Final Presentation

- app screens of use cases
- final architecture
- mobile computing challenges (again...)
- lessons learned
- ~~open issues~~ ;)
