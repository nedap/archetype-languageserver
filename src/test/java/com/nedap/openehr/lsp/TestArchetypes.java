package com.nedap.openehr.lsp;

public enum TestArchetypes {
    ADL14_VALID_ADL("adl14_valid.adl"),
    ARCHETYPE_WITH_RULES_ADLS("archetype_with_rules.adls"),
    CORRECT_OPT_OPT2("correct_opt.opt2"),
    CORRECT_TEMPLATE_ADLT("correct_template.adlt"),
    JSON_ERROR_ADLS("json_error.adls"),
    SYNTAX_ERROR_ADLS("syntax_error.adls"),
    TEMPLATE_DEFINITION_ERROR_IN_OVL_ADLT("template_definition_error_in_ovl.adlt"),
    TEMPLATE_NON_DEFINITION_ERROR_ADLT("template_non_definition_error.adlt"),
    TEST_ARCHETYPE_ADLS("test_archetype.adls"),
    VALIDATION_ERROR_ADLS("validation_error.adls");

    private final String filename;

    TestArchetypes(final String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
