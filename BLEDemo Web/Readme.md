# BLE-Demo as Web-App

The "traditional" way of writing an app for smartphone or tablets is to use Android Studio or Xcode and create a real app. However, today it is possible to pretty much do the same with just HTML / JavaScript as "web app" - an app running in the browser.

As example I'm providing here the client for the BLE server as plain web app. 
The functionality is the same as for the android app!

## Preconditions

Your browser needs to support `bluetooth.requestDevice` - checkout [caniuse.com](https://caniuse.com/?search=bluetooth.requestDevice) for supported browsers. Best option currently is Google Chrome.

As using bluetooth is to be considered a security topic, you cannot load the web app directly into your browser, you need to download it from a TLS enabled webserver!
For tests the simplest way is prop. to use [Caddy](https://caddyserver.com/download). The needed `Caddyfile` is part of the repo. 

Just download caddy and start it directly in the repo folder with
```
caddy run
```
Caddy will generate the needed certificates and serve the content of your folder. The web app can be accessed via `https:\\127.0.0.1`

## Important reading

The web app is based on the following material:

* [https://web.dev/bluetooth/](https://web.dev/bluetooth/)
* [https://beaufortfrancois.github.io/sandbox/web-bluetooth/generator/](https://beaufortfrancois.github.io/sandbox/web-bluetooth/generator/)
* [https://web.dev/promises/](https://web.dev/promises/)

## The code

n/a

