"""
Prepares a mascot image for use in the app.

Usage (run from repo root):
    python tools/prep_mascot.py <filename>

Resizes the image to 512x512 and optimises PNG compression in place.
If the image is already 512x512, nothing changes.

Example:
    python tools/prep_mascot.py dragon.png
"""

from PIL import Image
import os
import argparse

TARGET_SIZE = 512
DRAWABLE = r"app\src\main\res\drawable"


def main(base):
    src = base if os.path.isabs(base) else os.path.join(DRAWABLE, base)
    if not os.path.exists(src):
        print(f"ERROR: {src} not found.")
        return

    img = Image.open(src).convert("RGBA")
    original_size = img.size

    if original_size == (TARGET_SIZE, TARGET_SIZE):
        print(f"Already {TARGET_SIZE}x{TARGET_SIZE}, nothing to do: {src}")
        return

    before_kb = os.path.getsize(src) // 1024
    img = img.resize((TARGET_SIZE, TARGET_SIZE), Image.LANCZOS)
    img.save(src, "PNG", optimize=True, compress_level=9)
    after_kb = os.path.getsize(src) // 1024

    print(f"Resized {original_size[0]}px -> {TARGET_SIZE}px  ({before_kb} KB -> {after_kb} KB): {src}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Resize a mascot image to 512x512.")
    parser.add_argument("base", help="Filename in drawable/ (e.g. dragon.png)")
    args = parser.parse_args()
    main(args.base)
