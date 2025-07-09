// StreamPack libraries
include(":core")
project(":core").name = "streampack-core"
include(":ui")
project(":ui").name = "streampack-ui"
include(":services")
project(":services").name = "streampack-services"

// Extensions
include(":extension-rtmp")
project(":extension-rtmp").projectDir = File(rootDir, "extensions/rtmp")
project(":extension-rtmp").name = "streampack-rtmp"
include(":extension-srt")
project(":extension-srt").projectDir = File(rootDir, "extensions/srt")
project(":extension-srt").name = "streampack-srt"
