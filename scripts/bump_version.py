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
    # Simplified Regex: matches 'versionName = project.findProperty("versionName") ... ?: "2.0"'
    # We use non-greedy match .*? to skip the casting logic
    name_pattern = 'versionName\s*=\s*project\.findProperty\("versionName"\).*?:\s*"([^"]+)"'
    
    match_name = re.search(name_pattern, content)
    if not match_name:
        print("Error: Could not find version name pattern in build.gradle.kts.")
        # Try to print the specific line if possible, to see what's wrong
        for line in content.splitlines():
            if "versionName" in line and "project.findProperty" in line:
                print(f"Found similar line: {line.strip()}")
        sys.exit(1)

    full_match_name = match_name.group(0)
    current_version = match_name.group(1)
    
    print(f"Current Version: {current_version}")

    parts = [int(x) for x in current_version.split(".")]
    if len(parts) < 3:
        parts.extend([0] * (3 - len(parts)))
    
    if release_type == "major":
        parts[0] += 1
        parts[1] = 0
        parts[2] = 0
    elif release_type == "minor":
        parts[1] += 1
        parts[2] = 0
    else: # patch
        parts[2] += 1
    
    new_version = ".".join(map(str, parts))
    print(f"New Version: {new_version}")
    
    # Replace just the version part in the found string
    # We use replace on the full matched string to be safe
    new_line_name = full_match_name.replace(f'"{current_version}"', f'"{new_version}"')
    content = content.replace(full_match_name, new_line_name)

    # 2. Update Version Code
    # Simplified Regex: matches 'versionCode = project.findProperty("versionCode") ... ?: 2'
    code_pattern = 'versionCode\s*=\s*project\.findProperty\("versionCode"\).*?:\s*(\d+)'
    
    match_code = re.search(code_pattern, content)
    if match_code:
        full_match_code = match_code.group(0)
        current_code = int(match_code.group(1))
        new_code = current_code + 1
        print(f"Bumping versionCode from {current_code} to {new_code}")
        
        # Be careful with integer replacement (e.g. replacing '2' in '20')
        # Since we match the end of the line structure " ?: 2", it should be unique enough
        # We reconstruct the string from the regex groups if we wanted to be super safe, 
        # but replace on the full match is usually fine if the match is specific enough.
        # However, to be safer:
        # full_match_code ends with the number. 
        # Let's split by the number and reassemble.
        
        # Find the last occurrence of the number in the match
        idx = full_match_code.rfind(str(current_code))
        if idx != -1:
            new_line_code = full_match_code[:idx] + str(new_code) + full_match_code[idx+len(str(current_code)):]
            content = content.replace(full_match_code, new_line_code)
        else:
             print("Warning: Could not safely replace version code.")
    else:
        print("Warning: Could not find version code pattern. Skipping code bump.")

    with open(file_path, "w") as f:
        f.write(content)

    # Set output for GitHub Actions
    if "GITHUB_OUTPUT" in os.environ:
        with open(os.environ["GITHUB_OUTPUT"], "a") as f:
            f.write(f"new_version={new_version}\n")
    else:
        print(f"GITHUB_OUTPUT not set. New version is: {new_version}")

if __name__ == "__main__":
    main()