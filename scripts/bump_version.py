import re
import os
import sys
import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("release_type", choices=["patch", "minor", "major"])
    args = parser.parse_args()
    
    file_path = "app/build.gradle.kts"
    release_type = args.release_type

    print(f"Processing {file_path} with release type: {release_type}")

    try:
        with open(file_path, "r") as f:
            content = f.read()
    except FileNotFoundError:
        print(f"Error: File not found at {file_path}")
        sys.exit(1)

    # 1. Update Version Name
    # Pattern to find the version string: ... ?: "2.0"
    # We look for the fallback string at the end of the line
    name_pattern = r'(versionName\s*=\s*project\.findProperty\("versionName"\)\s*as\s*String\?\s*\?:\s*")([^"]+)(