# OpenEHR ADL 2 extension

An OpenEHR extension to VSCode for editing ADL 2 code

## Functionality

This language server works on ADL files, with the extension .adl, .adlf, .adlt, .adl2 or .adls. It provides:
- syntax highlighting
- syntax validation
- model validation
- code folding
- document outline
- hover info on complex objects and attributes

For the validation, it compiles all ADL files in the entire workspace, and does a recompile of those archetypes required if you change one. The problems tab will list all validation errors.

