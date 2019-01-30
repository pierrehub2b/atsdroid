# atsdroid
Ats driver for Android system. This driver will execute Ats actions on a connected Android mobile.

##### List of available commands (Supposing the driver is responding to url '192.168.0.1:8080')

* http://192.168.0.1:8080/info
```
Return device capabilities and the list of installed applications
```
* http://192.168.0.1:8080/driver/start
```
Turn mobile screen on
```
* http://192.168.0.1:8080/driver/stop
```
Turn mobile screen off
```
* http://192.168.0.1:8080/driver/quit
```
Stop and quit the driver process (Ats driver could no more be reachable after this action)
```
* http://192.168.0.1:8080/app/start/[app-id]
```
Start the application identified by qualified id
```
* http://192.168.0.1:8080/app/stop/[app-id]
```
Stop the application identified by qualified id
```
* http://192.168.0.1:8080/app/switch/[app-id]
```
Switch and send application to forward
```
* http://192.168.0.1:8080/app/info/[app-id]
```
Return application infos and details
```
* http://192.168.0.1:8080/button/[button-id]
```
Execute button action on device (back, home, menu)
```
* http://192.168.0.1:8080/capture
```
Retrieve elements tree (with uniq generated id) from the root of the current application
```
* http://192.168.0.1:8080/element/[element-id]/input/[text-input]
```
Execute input key action on element identified by [element-id]
```
* http://192.168.0.1:8080/element/[element-id]/tap
```
Execute tap action on the center of the element
```
* http://192.168.0.1:8080/element/[element-id]/tap/[offsetX]/[offsetY]
```
Execute tap action on the element with relative offset position
```
* http://192.168.0.1:8080/element/[element-id]/swipe/[offsetX]/[offsetY]/[directionX]/[directionY]
```
Execute swipe action on the element from offset relative position to x and y direction
```


