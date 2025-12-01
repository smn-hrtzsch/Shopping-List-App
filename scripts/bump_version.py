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
    # Regex: versionName = project.findProperty("versionName") as String? ?: "2.0"
    # Escaped for standard string: \\ becomes \ in regex
    name_pattern = 'versionName\\s*=\\s*project\\.findProperty("versionName")\\s*as\\s*String\\?\\s*\\?:\\s*"([^"]+)"'
    
    match_name = re.search(name_pattern, content)
    if not match_name:
        print("Error: Could not find version name pattern in build.gradle.kts.")
        print("File snippet around expected area:")
        print(content[:500] + "...") 
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
    # We reconstruct the string: key + new_value + quote
    # But since we matched the whole group including quotes in the regex, we need to be careful.
    # The pattern '... "([^"]+)"' captures the version inside quotes in group 1.
    # So full match is:  ... "2.0"
    # We can replace ' "2.0"' with ' "2.0.1"'
    
    # Let's just replace the specific substring found in the file
    # To avoid replacing other occurrences of "2.0", we use the full matched string context
    new_line_name = full_match_name.replace(f'"{current_version}"', f'"{new_version}"')
    content = content.replace(full_match_name, new_line_name)

    # 2. Update Version Code
    # Regex: versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 2
    code_pattern = 'versionCode\\s*=\\s*project\\.findProperty("versionCode")\\?\\.toString(\\)?\\.toInt(\\)?\\s*\\?:\\s*(\\d+)'
    
    match_code = re.search(code_pattern, content)
    if match_code:
        full_match_code = match_code.group(0)
        current_code = int(match_code.group(1))
        new_code = current_code + 1
        print(f"Bumping versionCode from {current_code} to {new_code}")
        
        new_line_code = full_match_code.replace(str(current_code), str(new_code))
        content = content.replace(full_match_code, new_line_code)
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
