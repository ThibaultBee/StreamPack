/*
 * Copyright (C) 2023 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.streampack.core.internal.utils.av.descriptors

enum class Tags(val value: Byte) {
    ObjectDescr(0x01),
    InitialObjectDescr(0x02),
    ESDescr(0x03),
    DecoderConfigDescr(0x04),
    DecSpecificInfo(0x05),
    SLConfigDescr(0x06),
    ContentIdentDescr(0x07),
    SupplContentIdentDescr(0x08),
    IPIDescrPointer(0x09),
    IPMPDescrPointer(0x0A),
    IPMPDescr(0x0B),
    QoSDescr(0x0C),
    RegistrationDescr(0x0D),
    ES_ID_Inc(0x0E),
    ES_ID_Ref(0x0F),
    MP4_IOD(0x10),
    MP4_OD(0x11),
    IPL_DescrPointerRef(0x12),
    ExtensionProfileLevelDescr(0x13),
    ProfileLevelIndicationIndexDescr(0x14),
    ContentClassificationDescr(0x40),
    KeyWordDescr(0x41),
    RatingDescr(0x42),
    LanguageDescr(0x43),
    ShortTextualDescr(0x44),
    ExpandedTextualDescr(0x45),
    ContentCreatorNameDescr(0x46),
    ContentCreationDateDescr(0x47),
    OCICreatorNameDescr(0x48),
    OCICreationDateDescr(0x49),
    SmpteCameraPositionDescr(0x4A);

    companion object {
        fun from(tag: Byte): Tags {
            return entries.find { it.value == tag }
                ?: throw IllegalArgumentException("Unknown tag: $tag")
        }
    }
}