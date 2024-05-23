# MPEG transport stream mux

MPEG-TS muxer is a minimalist MPEG-TS muxer.
It manages packets encapsulation and section tables (PAT, PMT, SDT).

It supports the following operations:
* Add/remove streams
* Add/remove services
* Encode of AAC and H264 frames

## Quick start

1/ Instantiates a `TSMuxer()`:

```
val muxer = TSMuxer(listener = object : IMuxerListerner {
    override fun onOutputFrame(packet: Packet)
        // Use packet
    }
})
```

2/ Registers on or multiple services and streams:

```
val streamPid = muxer.addStreams(serviceInfo, listOf(audioConfig))[audioConfig]
```

2/ Encodes frames with `encode`:

```
muxer.encode(frame, streamPid)
```

