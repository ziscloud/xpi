# xpi
Extended Java extension, based on the source code of [dubbo](https://dubbo.apache.org), but remove the dependent of URL.

XPI is inherited from standard JDK extension(Service Provider Interface) and makes it more powerful.

XPI fixed below issues of the standard JDK extension:

* The standard JDK extension will load and instantize all the implementations at once. It will be a waste of resources if one implementation is timecosted, but never be used.
* We can’t accquire the extension name, if loading the extension implementation is failed.For example: standard JDK ScriptEngine, get script type by invoking method getName(). RubyScriptEngine class will load failed if the depenency jar jruby.jar is missing, and the real error info will be lost. When user executes ruby scripts, the program throws exception that doesn’t support ruby, but it is not the real cause.
* Enhance the extension functionality by supporting IoC and AOP, one extension can be easily injected by another extension simply using setter.

## Appointment:

In the jar file containing extension class 1, places a config file META-INF/xpi/<full interface name>, file content pattern: 

*extension name=the fully qualified name for the extension class*

use new line seperator for multiple implementation.

## Example:

To extend `Protocol`, place a text file in the extension jar file: `META-INF/xpi/org.neuronbit.xpi.rpc.Protocol`, content:

```text
xxx=org.neuronbit.xxx.XxxProtocol
```

content of the implementation 2:

```java
package org.neuronbit.xxx;

import org.neuronbit.xpi.rpc.Protocol;

public class XxxProtocol implements Protocol {
// ...
}
```

get the extensino instanc via:
```java
XxxProtocol protocolInstance = ExtensionFactory.getExtensionFactory(XxxProtocol.class).getExtension("xxx");
```

## extension Features

### extension Auto Wrap

Auto wrap the extension’s Wrapper class. ExtensionFactory loads the extension implementation, if the extension has a copy instructor, it will be regarded as the extension’s Wrapper class.

Wrapper class content:

```java
package org.neuronbit.xxx;

import org.neuronbit.xpi.rpc.Protocol;

public class XxxProtocolWrapper implements Protocol {
    private Protocol impl;

    public XxxProtocolWrapper(Protocol protocol) { impl = protocol; }
 
    //after interface method is executed, the method in extension will be executed
    public void refer() {
        //... some operation
        impl.refer();
        // ... some operation
    }
 
    // ...
}
```

Wrapper class also implements the same extension interface, but Wrapper is not the real implementation. It is used for wrap the real implementation returned from the ExtensionFactory. The real returned instance by ExtensionFactory is the Wrapper class instance, Wrapper holder the real extension implementation class.

There can be more than one Wrapper for an extension, just simply add if you need.

With Wrapper class, you will be able to add same logics into Wrapper for all extensions, like AOP, Wrapper acts as a proxy for extension.

### extension Auto Load

when loading the extension, Dubbo will auto load the depency extension. When one extension implementation contains attribute which is also an extension of another type,ExtensionFactory will automatically load the depency extension. ExtensionFactory knows all the members of the specific extension by scanning the setter method of all implementation class.

Demo: two extension CarMaker（car maker）、WheelMaker (wheel maker)

Interfaces are:

```java
public interface CarMaker {
Car makeCar();
}

public interface WheelMaker {
Wheel makeWheel();
}
```

CarMaker’s implementation:

```java
public class RaceCarMaker implements CarMaker {
WheelMaker wheelMaker;

    public void setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }
 
    public Car makeCar() {
        // ...
        Wheel wheel = wheelMaker.makeWheel();
        // ...
        return new RaceCar(wheel, ...);
    }
}
```

when ExtensionFactory loads RaceCarMaker, the method setWheelMaker needs a parameter of WheelMaker type which is also an extension, It will be automatically loaded.

This brings a new question: How ExtensionFactory determines which implementation to use when load the injected extension. 

As for this demo, when existing multi WheelMaker implementation, which one should the ExtensionFactory choose.

This problem is solved with: extension Auto Adaptive.

### extension Auto Adaptive

The extension that ExtensionFactory injects is an instance of Adaptive, the real extension implementation is known until the adaptive instance is executed.

Dubbo use URL (containing Key-Value) to pass the configuration.

The extension method invocation has the URL parameter（Or Entity that has URL attribute）

In this way depended extension can get configuration from URL, after config all extension key needed, configuration information will be passed from outer by URL. URL acts as a bus when passing the config information.

Demo: two extension CarMaker、WheelMaker

interface looks like:

```java
public interface CarMaker {
Car makeCar(URL url);
}

public interface WheelMaker {
Wheel makeWheel(URL url);
}
```

CarMaker’s implementation:

```java
public class RaceCarMaker implements CarMaker {
WheelMaker wheelMaker;

    public void setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }
 
    public Car makeCar(URL url) {
        // ...
        Wheel wheel = wheelMaker.makeWheel(url);
        // ...
        return new RaceCar(wheel, ...);
    }
}
```

when execute the code above

```java
// ...
Wheel wheel = wheelMaker.makeWheel(url);
// ...

```
, the injected Adaptive object determine which WheelMaker’s makeWheel method will be executed by predefined Key. Such as wheel.type, key url.get("wheel.type") will determine WheelMake implementation. The logic ofAdaptive instance of fixed, getting the predefined Key of the URL, dynamically creating the real implementation and execute it.

For Dubbo, the extension Adaptive implementation in ExtensionFactory is dynamically created when dubbo is loading the extension. Get the Key from URL, the Key will be provided through @Adaptive annotation for the interface method definition.

Below is Dubbo Transporter extension codes:

```java
public interface Transporter {
@Adaptive({"server", "transport"})
Server bind(URL url, ChannelHandler handler) throws RemotingException;

    @Adaptive({"client", "transport"})
    Client connect(URL url, ChannelHandler handler) throws RemotingException;
}
```

for the method bind(), Adaptive will firstly search server key, if no Key were founded then will search transport key, to determine the implementation that the proxy represent for.

## extension Auto Activation

As for Collections extension, such as: Filter, InvokerListener, ExportListener, TelnetHandler, StatusChecker etc, multi implementations can be loaded at one time. User can simplify configuration by using auto activation, Like:
```java
import org.neuronbit.xpi.common.extension.Activate;
import org.neuronbit.xpi.rpc.Filter;

@Activate // Active for any condition
public class XxxFilter implements Filter {
// ...
}
```
Or:
```java
import org.neuronbit.xpi.common.extension.Activate;
import org.neuronbit.xpi.rpc.Filter;

@Activate("xxx") // when configed xxx parameter and the parameter has a valid value,the extension is activated, for example configed cache="lru", auto acitivate CacheFilter.
public class XxxFilter implements Filter {
// ...
}
```                       
Or:
```java
import org.neuronbit.xpi.common.extension.Activate;
import org.neuronbit.xpi.rpc.Filter;

@Activate(group = "provider", value = "xxx") // only activate for provider, group can be "provider" or "consumer"
public class XxxFilter implements Filter {
// ...
}
```
Note: The config file here is in you own jar file, not in dubbo release jar file, Dubbo will scan all jar files with the same filename in classpath and then merge them together ↩︎

Note: extension will be loaded in singleton pattern(Please ensure thread safety), cached in ExtensionFactory ↩︎

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation
