# OpenEHR ADL and AQL extension

An OpenEHR extension to VSCode for editing ADL 1.4, ADL 2 and AQL code. Just install and start working on your ADL and AQL files - it will add full highlighting, validation, example generation and hover info to your ADL files, while providing an easy way to write AQL queries that integrates fully with any present archetypes.

## Functionality

This language server works on ADL and AQL files, with the extension .aql, .adl, .adlf, .adlt, .adl2 or .adls. It provides IDE support for both ADL and AQL.

### ADL 2

- syntax highlighting
- syntax validation
- model validation
- code folding
- document outline
- example generation, in JSON, flat JSON or XML format
- OPT2 generation, in ADL, JSON or XML
- hover info on complex objects and attributes
- links
- adding missing terminology constraints automatically

### AQL

AQL files have syntax highlighting and validation. If the archetypes in the AQL query are known, paths are annotated with a human readable name. Code completion is available for these paths, making AQL editing a lot easier!

### ADL 1.4
This plugin supports basic support for ADL 1.4 archetypes, with syntax highlighting and document outlines. The main thing it can do with these archetypes is to convert them to ADL 2.


