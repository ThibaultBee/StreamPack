# MPEG transport stream mux

MPEG-TS mux is a minimalist MPEG-TS mux.
It manages packets encapsulation and section tables (PAT, PMT, SDT).

It supports the following operations:
* Add/remove streams
* Add/remove services
* Encode of AAC and H264 frames

## Quick start

1/ Instantiate a `TSMux()`
```
val tsMux = TSMux(listener, serviceInfo)
```

2/ Register the streams
```
 val streamPid = addStreams(serviceInfo, listOf(MediaFormat.MIMETYPE_VIDEO_AVC))[0]
```

2/ Encode frames with `encode`
```
tsMux.encode(frame, streamPid)
```

