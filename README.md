# archetype-languageserver
A language server for OpenEHR archetypes, to be used in your favorite IDE

# Build instructions
- clone github.com/openehr/archie, branch preserve_aom_file_positions
- add `includeBuild 'path-to-archie'` in your settings.gradle file
- download the mac, linux, and windows openJDK binaries, version 14 or newer
- use one of the version 14 openJDK vms to compile the project
- copy `gradle.example.properties` to `gradle.properties`
- cahnge the JDK locations in the gradle properties file
- `./gradlew clean build runtime buildExtension`

The output will be in the .vsix file in `./vscode-extension`. 

It contains a linked windows, linux and macos JRE from the OpenJDK project, so should run on whatever OS needed, but x86 only for now.
If you want ARM, very easy as well, just download the ARM openJDK and change the runtime config in build.gradle

This is not yet published to the VSCode marketplace, so instructions for that will follow later.

