[![Maven Central](https://img.shields.io/maven-central/v/io.github.thibaultbee.streampack/streampack-core)](https://central.sonatype.com/artifact/io.github.thibaultbee.streampack/streampack-core)
[![License](https://img.shields.io/github/license/ThibaultBee/StreamPack)](LICENSE.md)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Stars](https://img.shields.io/github/stars/ThibaultBee/StreamPack?style=social)](https://github.com/ThibaultBee/StreamPack/stargazers)
[![Sponsor](https://img.shields.io/badge/Sponsor-StreamPack-ff69b4)](https://github.com/sponsors/ThibaultBee)

<!-- --8<-- [start:summary] -->
# StreamPack: Open-Source Android Live Streaming SDK (RTMP & SRT)

StreamPack is a powerful, open-source **Android live streaming SDK** built entirely in **Kotlin**.
Designed for high-performance and **low-latency video broadcasting**, StreamPack allows developers
to seamlessly integrate **RTMP, RTMPS, and SRT streaming protocols** into their mobile applications.

By leveraging modern **Android Camera2** and hardware-accelerated **MediaCodec APIs**, this library
delivers a highly efficient and battery-friendly broadcasting experience. Its highly modular and
extensible architecture makes it easy to plug in custom streaming protocols, custom audio/video
sources, or build specialized applications like screen recorders and professional mobile broadcast
tools.
<!-- --8<-- [end:summary] -->


## Table of Contents

- [5-minute Quick Start / Boilerplate](#-5-minute-quick-start--boilerplate)
- [Setup](#-setup)
- [Features](#-features)
- [Documentation](#-documentation)
- [Demos](#-demos)
- [Contributing & Support](#-contributing--support)
- [License](#-license)

<!-- --8<-- [start:boilerplate] -->
## 🏗️ 5-minute Quick Start / Boilerplate

If you want to create a new application, you should use the
template [StreamPack boilerplate](https://github.com/ThibaultBee/StreamPack-boilerplate). In 5
minutes, you will be able to stream live video to your server.
<!-- --8<-- [end:boilerplate] -->

<!-- --8<-- [start:setup] -->
## 📦 Setup

Get StreamPack core latest artifacts on Maven Central:

```groovy
dependencies {
    // Replace $latest_version with the version on the Maven Central badge
    implementation 'io.github.thibaultbee.streampack:streampack-core:$latest_version'
    // For xml UI (incl. PreviewView)
    implementation 'io.github.thibaultbee.streampack:streampack-ui:$latest_version'
    // Or compose UI (incl. SourcePreview)
    implementation 'io.github.thibaultbee.streampack:streampack-compose:$latest_version'
    // For services (incl. screen capture/media projection service)
    implementation 'io.github.thibaultbee.streampack:streampack-services:$latest_version'
    // For RTMP
    implementation 'io.github.thibaultbee.streampack:streampack-rtmp:$latest_version'
    // For SRT
    implementation 'io.github.thibaultbee.streampack:streampack-srt:$latest_version'
}
```

<!-- --8<-- [end:setup] -->

<!-- --8<-- [start:features] -->
## ✨ Features

### 🎥 Video
* **Sources:** Cameras, Screen Recorder, or build your own [custom video source](https://thibaultbee.github.io/StreamPack/use_cases/CustomSources/).
* **Codecs:** HEVC/H.265, AVC/H.264, VP9, and AV1.
* **Configuration:** Fully configurable bitrate, resolution, frame rate (up to 60fps), encoder level, and profile.
* **Camera Settings:** Auto-focus, exposure, white balance, zoom, flash, and experimental HDR support.
* **Dynamic:** Switch between video sources on the fly. Portrait, landscape, and Video-only modes are supported.

### 🎙️ Audio
* **Sources:** Microphone, internal device audio, or a [custom audio source](https://thibaultbee.github.io/StreamPack/use_cases/CustomSources/).
* **Codecs:** AAC (LC, HE, HEv2) and Opus.
* **Configuration:** Configurable bitrate, sample rate, stereo/mono, and data format. 
* **Processing:** Built-in noise suppression and echo cancellation. Audio-only mode is also supported.
* **Dynamic:** Switch between audio sources seamlessly.

### 📡 Streaming (RTMP & SRT)
* **SRT:** Ultra low-latency streaming based on [SRT](https://github.com/Haivision/srt) with a built-in network adaptive bitrate mechanism.
* **RTMP:** Standard and Enhanced RTMP broadcasting.

### 💾 File Recording
* **Formats:** TS, FLV, MP4, WebM, Fragmented MP4, or [custom output](https://thibaultbee.github.io/StreamPack/use_cases/CustomEndpoints/).
* **Flexibility:** Record to a single file, split into multiple chunk files, or [record and stream simultaneously](https://thibaultbee.github.io/StreamPack/use_cases/LiveAndRecordSimultaneously/).
<!-- --8<-- [end:features] -->

## 📖 Documentation

For full documentation, setup instructions, APIs, and guides, please visit our [Documentation Site](https://thibaultbee.github.io/StreamPack).

<!-- --8<-- [start:demos] -->
## 🎬 Demos

For source code examples on how to use camera, audio, and screen recorder streamers,
check the [demos](https://github.com/ThibaultBee/StreamPack/tree/main/demos) directory.

### 📷 Camera and audio demo

For source code example on how to use camera and audio streamers,
check [demos/camera](https://github.com/ThibaultBee/StreamPack/tree/main/demos/camera). On
first launch, you will have to set RTMP url or SRT server IP in the settings menu.

### 🖥️ Screen recorder demo

For source code example on how to use screen recorder streamer, check
the [demos/screenrecorder](https://github.com/ThibaultBee/StreamPack/tree/main/demos/screenrecorder).
On first launch, you will have to set RTMP url or SRT server IP in the settings menu.
<!-- --8<-- [end:demos] -->

<!-- --8<-- [start:contributing] -->
## 🤝 Contributing & Support

⭐ If you like this project, don’t forget to star it!

💖 Want to support its development? Consider becoming a sponsor.

🛠️ Contributions are welcome—feel free to open issues or submit pull requests!
<!-- --8<-- [end:contributing] -->

<!-- --8<-- [start:license] -->
## 📄 License

    Copyright 2021 Thibault B.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
<!-- --8<-- [end:license] -->
