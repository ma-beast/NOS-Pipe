#!/usr/bin/env python3
from pathlib import Path
import struct

source = Path("NOS-Pipe.ico").read_bytes()
reserved, image_type, count = struct.unpack_from("<HHH", source, 0)
if (reserved, image_type, count) != (0, 1, 7):
    raise SystemExit("Expected the verified seven-image NOS-Pipe.ico")

for index in range(count):
    size, offset = struct.unpack_from("<II", source, 6 + index * 16 + 8)
    Path("icon%d.bin" % index).write_bytes(source[offset:offset + size])

