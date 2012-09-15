# Loggy

### What is it?
Loggy is a root developer tool that lets you view your logcat in a browser.


### How does it work?
Loggy comes packaged with a lightweight webserver that supports websockets. A logcat process is run and piped to the browser via a WebSocket.

### FAQ

> Q: Hey bro, why does Loggy need root?

> A: Well bra, it's because as of JellyBean, apps can only access their own logs, and not the system log or other app logs. So root is required to see the full log. https://plus.google.com/103583939320326217147/posts/2Yb5wgrx5ib



#### Dependencies

Loggy depends on the AndroidAsync project available here:

* AndroidAsync - https://github.com/koush/AndroidAsync


Loggy packages the following projects:

* Twitter Bootstrap - http://github.com/twitter/bootstrap
* Jade - https://github.com/visionmedia/jade
* jQuery - https://github.com/jquery/jquery
* web-socket-js - https://github.com/gimite/web-socket-js