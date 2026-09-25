#!/usr/bin/env python3
from pathlib import Path
import struct

path = Path("NOS-Pipe.exe")
image = bytearray(path.read_bytes())
pe_offset = struct.unpack_from("<I", image, 0x3c)[0]
optional_header = pe_offset + 24

# GNU ld places one empty import descriptor before the hand-written table.
import_directory = optional_header + 96 + 8
import_rva, import_size = struct.unpack_from("<II", image, import_directory)
struct.pack_into("<II", image, import_directory, import_rva + 20, import_size - 20)

# Recalculate the ordinary PE checksum after the directory adjustment.
checksum_offset = optional_header + 64
struct.pack_into("<I", image, checksum_offset, 0)
checksum = 0
for offset in range(0, len(image), 2):
    word = image[offset]
    if offset + 1 < len(image):
        word |= image[offset + 1] << 8
    checksum = (checksum + word) & 0xffffffff
    checksum = (checksum & 0xffff) + (checksum >> 16)
checksum = (checksum & 0xffff) + (checksum >> 16)
checksum = (checksum + len(image)) & 0xffffffff
struct.pack_into("<I", image, checksum_offset, checksum)
path.write_bytes(image)

