# archetype-languageserver
A language server for OpenEHR archetypes, to be used in your favorite IDE. Packaged as a vscode-extension. Can be found in the vscode marketplace.

# Build instructions
- clone github.com/nedap/aqlparser, 
- make sure `settings.gradle` points to where you checked out aql parser.
- download the mac, linux, and windows openJDK binaries, version 14 or newer
- copy `gradle.example.properties` to `gradle.properties`
- change the JDK locations in the gradle properties file
- use one of the version 14 openJDK vms to compile the project
- `./gradlew clean build runtime buildExtension`

The output will be in the .vsix file in `./vscode-extension`. 

It contains a linked windows, linux and macos JRE from the OpenJDK project, so should run on whatever OS needed, but x86 only for now.
If you want ARM, very easy as well, just download the ARM openJDK and change the runtime config in build.gradle

# Running in development

replace the contents of `vscode-extension/client/src/extension.ts` with `extension_network.ts`, located in the same directory. Then start the archetype-language server by running the main class, `com/nedap/openehr/lsp/App.java`, with the argument 'network', in your favorite development environment. Then start the extension using the vscode development environment. You may want to uninstall the extension from the marketplace first.

