// StreamPack libraries
include(":core")
project(":core").name = "streampack-core"
include(":services")
project(":services").name = "streampack-services"

// UI
include(":ui")
project(":ui").projectDir = File(rootDir, "ui/ui")
project(":ui").name = "streampack-ui"
include(":compose")
project(":compose").projectDir = File(rootDir, "ui/compose")
project(":compose").name = "streampack-compose"

// Extensions
include(":extension-flv")
project(":extension-flv").projectDir = File(rootDir, "extensions/flv")
project(":extension-flv").name = "streampack-flv"
include(":extension-rtmp")
project(":extension-rtmp").projectDir = File(rootDir, "extensions/rtmp")
project(":extension-rtmp").name = "streampack-rtmp"
include(":extension-srt")
project(":extension-srt").projectDir = File(rootDir, "extensions/srt")
project(":extension-srt").name = "streampack-srt"
