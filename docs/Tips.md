# 💡 Tips

### RTMP or SRT

RTMP and SRT are both live streaming protocols. SRT is a UDP-based modern protocol, it is
reliable and ultra low latency. RTMP is a TCP-based protocol, it is also reliable but it is only low
latency.
There are already a lot of comparison over the Internet, so here is a summary:

* SRT:
    - Ultra low latency(< 1 s)
* RTMP:
    - Low latency (2 - 3 s)

So, the main question is : "which protocol to use?"
It is easy: if your server has SRT support, use SRT otherwise use RTMP.



### Android versions

Even if StreamPack sdk supports a `minSdkVersion` 21. We strongly recommend to set the
`minSdkVersion` of your application to a higher version (the highest is the best!) for better
performance.

