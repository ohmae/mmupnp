# mmupnp
Universal Plug and Play (UPnP) ControlPoint library for Java.

## Feature
- Pure Java implementation.
- Available in both Java application and Android apps
- Easy to use
- High response

## Requirements
- Java SE 7 or later

## Example of use
- DMS Explorer - [Google Play](https://play.google.com/store/apps/details?id=net.mm2d.dmsexplorer)

## How to use

I describe Javadoc comments. Please refer to it for more information.

### Initialize and Start
```
ControlPoint cp = new ControlPoint();
cp.initialize();
// adding listener if necessary.
cp.addDiscoveryListener(...);
cp.addNotifyEventListener(...);
cp.start();
...
```

If you want to specify the network interface, describe the following.

```
NetworkInterface ni = NetworkInterface.getByName("eth0");
ControlPoint cp = new ControlPoint(Arrays.asList(ni));
```


### M-SEARCH
Call ControlPoint#search() or ControlPoint#search(String) method.
```
cp.search();                   // default ST is ssdp:all
```
```
cp.search("upnp:rootdevice"); // to use specific ST
```
These methods send one M-SEARCH packet to all interface.


### Invoke Action
Example of invoke "Browse" (ContentDirectory)
```
...
Device mediaServer = cp.getDevice(UDN);           // get device by UDN
Action browse = mediaServer.findAction("Browse"); // find "Browse" action
Map<String, String> arg = new HashMap<>();        // setup arguments
arg.put("ObjectID", "0");
arg.put("BrowseFlag", "BrowseDirectChildren");
arg.put("Filter", "*");
arg.put("StartingIndex", "0");
arg.put("RequestedCount", "0");
arg.put("SortCriteria", "");
Map<String, String> result = browse.invoke(arg);  // invoke action
String resultXml = result.get("Result");          // get result
...
```

### Event Subscription
Example of subscribe CDS events.
```
...
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

### Stop and Terminate
```
...
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
