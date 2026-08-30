# 🧪 Testing

## Testing with a FFmpeg server

FFmpeg (specially `ffplay`) has been used as a server, demuxer, and decoder for the tests.

### RTMP

Tells FFplay to listen on IP `0.0.0.0` and port `1935`.

```
ffplay -listen 1 -i 'rtmp://0.0.0.0:1935/s/streamKey'
```

!!! tip "App Configuration"
    On StreamPack sample app settings, set **Endpoint** -> **Type** to `Stream to a remote RTMP device`, then set the server **URL** to `rtmp://serverip:1935/s/streamKey`.
    
    At this point, the StreamPack sample app should successfully send audio and video frames. On the FFplay side, you should be able to watch this live stream.

### SRT

Tells FFplay to listen on IP `0.0.0.0` and port `9998`:

```
ffplay -fflags nobuffer 'srt://0.0.0.0:9998?mode=listener'
```

!!! tip "App Configuration"
    On StreamPack sample app settings, set the server **IP** to your server IP and server **Port** to `9998`.
    
    At this point, the StreamPack sample app should successfully send audio and video frames. On the FFplay side, you should be able to watch this live stream.
