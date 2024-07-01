# Mnemosyne

> Attempt to write some plugins for cloudstream

* Sflix (from scratch) :white_check_mark:
* HiAnime (from Rowdy-Avocado) :white_check_mark:
* Aniwave (from Rowdy-Avocado) :white_check_mark:
* SuperStream (from Hexated) :white_check_mark:
* SoraStream (from Hexated) :white_check_mark:

* ZoroTV (TODO from scratch) :x:
* Onstream (TODO from scratch) :x:
* HDToday (TODO from scratch) :x:   (upcloud/vidcloud/upstream/mixdrop)

## Docs

Build and deployment: :ok: (https://shorturl.at/WbYNF)

* https://recloudstream.github.io/csdocs/devs/create-your-own-providers/#3-loading-the-show-page
* https://recloudstream.github.io/csdocs/devs/scraping/starting/


## Debug

```ps1
adb devices
adb connect 192.168.1.X:5555
adb logcat -s mnemo
```


## Notes

* vidcloud / upcloud uses https://rabbitstream.net/
* 9animetv is the same as Aniwave ?
* upstream.to is using hls2 but voe.sx hls2 is working