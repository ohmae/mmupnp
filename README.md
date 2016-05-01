# mmupnp

Universal Plug and Play (UPnP) ControlPoint library for Java.

## Feature

- Easy for use
- Pure Java implementation.
- Suitable for both Java application and Android Apps
- Lightweight
- High response

## How to use

### initialize and start
```
ControlPoint cp = new ControlPoint();
cp.initialize();
// adding listener if necessary.
cp.addDiscoveryListener(...);
cp.addNotifyEventListener(...);
cp.start();
```

### send M-SEARCH
```
cp.search();
```

### example of invoke "Browse" (ContentDirectory)
```
Device mediaServer = cp.getDevice(UDN);          // get device by UDN
Action browse = mediaServer.getAction("Browse"); // get Browse action
Map<String, String> arg = new HashMap<>();       // setup arguments
arg.put("ObjectID", "0");
arg.put("BrowseFlag", "BrowseDirectChildren");
arg.put("Filter", "*");
arg.put("StartingIndex", "0");
arg.put("RequestedCount", "0");
arg.put("SortCriteria", "");
Map<String, String> result = action.invoke(arg); // action invoke
String resultXml = result.get("Result");         // get result
...
```

### Subscription
```
// add listener to receive event
cp.addNotifyEventListener(new NotifyEventListener(){
  public void onNotifyEvent(Service service, long seq, String variable, String value) {
    ...
  }
});
Device mediaServer = cp.getDevice(UDN);          // get device by UDN
Service cds = mediaServer.findServiceById(
  "urn:upnp-org:serviceId:ContentDirectory");    // find Service by ID
cds.subscribe(); // Start subscribe
...
cds.unsubscribe(); // End subscribe
```


### stop and terminate
```
cp.stop();
cp.removeDiscoveryListener(...);
cp.removeNotifyEventListener(...);
cp.terminate();

```

## Author
大前 良介 (OHMAE Ryosuke)
http://www.mm2d.net/

## License
[MIT License](./LICENSE)
