# archetype-languageserver
A language server for OpenEHR archetypes, to be used in your favorite IDE. Packaged as a vscode-extension. Can be found in the vscode marketplace.

# Build instructions
- clone https://github.com/nedap/aqlparser, checkout the branch jdk17
- make sure `settings.gradle` points to where you checked out aql parser.
- download the mac, linux, and windows openJDK binaries, version 14 or newer
- copy `gradle.example.properties` to `gradle.properties`
- change the JDK locations in the gradle properties file
- use one of the version 17 openJDK vms to compile the project
- `./gradlew clean build runtime buildExtension`

The output will be in the .vsix file in `./vscode-extension`. 

It contains a linked windows, linux and macos x64 JRE from the OpenJDK project, plus an Arm Mac OS one. So it should run on whatever OS needed. If you want Arm support for Linux of Windows, this is very possible, but requires a bit of extra work, so create an issue if you need this.

# Running in development

replace the contents of `vscode-extension/client/src/extension.ts` with `extension_network.ts`, located in the same directory. Then start the archetype-language server by running the main class, `com/nedap/openehr/lsp/App.java`, with the argument 'network', in your favorite development environment. Then start the extension using the vscode development environment. You may want to uninstall the extension from the marketplace first.

