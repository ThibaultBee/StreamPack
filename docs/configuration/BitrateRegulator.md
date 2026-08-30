# 📉 Bitrate Regulator

A Bitrate Regulator is used to automatically adapt the video bitrate based on network conditions. This helps avoid frame drops or connection timeouts when the network bandwidth fluctuates.

The architecture is split into two main components:

1. **The Regulator (`IBitrateRegulator`)**: Contains the algorithm that analyzes network metrics (like bytes sent, packets lost) and calculates the new optimal video bitrate. For example, `SimpleBitrateRegulator` is a basic built-in algorithm.
2. **The Controller (`IBitrateRegulatorController`)**: Decides *when* to trigger the regulator to perform its analysis. For example, the `IntervalBitrateRegulatorController` will trigger the regulator at a fixed, recurring time interval.

## Enabling Bitrate Regulation

To enable bitrate regulation, set the `bitrateRegulatorControllerFactory` property on your streamer instance **before** starting the stream:

```kotlin
import io.github.thibaultbee.streampack.core.regulator.controllers.intervalBitrateRegulatorControllerFactory
import io.github.thibaultbee.streampack.core.regulator.SimpleBitrateRegulator
import kotlin.time.Duration.Companion.seconds

// 1. Choose your regulator algorithm
val regulatorFactory = SimpleBitrateRegulator.Factory()

// 2. Choose your controller (Interval triggers the regulator every X seconds)
streamer.bitrateRegulatorControllerFactory = intervalBitrateRegulatorControllerFactory(
    bitrateRegulatorFactory = regulatorFactory,
    pollingTime = 1.seconds // The controller will trigger the regulator every 1 second
)
```

*(Note: Calling `intervalBitrateRegulatorControllerFactory()` without arguments defaults to using the `SimpleBitrateRegulator` and a 1-second polling interval).*

!!! important "The provided `SimpleBitrateRegulator` is a very basic implementation meant primarily as an example. For production applications, we strongly recommend implementing your own custom `IBitrateRegulator` (and its `Factory`) to create a robust adaptation algorithm tailored to your specific use cases."

## Protocol-Specific Regulators

If you are using the SRT extension, it's highly recommended to use the specialized SRT regulator, which utilizes SRT-specific socket metrics (like RTT and packet loss) for much more accurate estimations:

```kotlin
import io.github.thibaultbee.streampack.ext.srt.regulator.controllers.srtBitrateRegulatorControllerFactory

streamer.bitrateRegulatorControllerFactory = srtBitrateRegulatorControllerFactory()
```