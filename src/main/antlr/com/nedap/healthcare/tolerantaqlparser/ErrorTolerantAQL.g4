// Very roughly based on:
/// Author: Bostjan Lah
/// (c) Copyright, Marand, http://www.marand.com
/// Licensed under LGPL: http://www.gnu.org/copyleft/lesser.html
/// Based on AQL grammar by Ocean Informatics: http://www.openehr.org/wiki/download/attachments/2949295/EQL_v0.6.grm?version=1&modificationDate=1259650833000
// and
/// https://github.com/ehrbase/ehrbase/blob/develop/service/src/main/antlr4/org/ehrbase/AQL/parser/Aql.g4 (mostly for the lexer)

// not implement (yet?):
// - timewindow
// - arithmetic functions (they are in the examples, but not in the grammar specification)
// - functions
// - (not) in (they are in the examples, but not in the grammar specification)
// - nested queries (they are in the examples, but not in the grammar specification)

grammar ErrorTolerantAQL;

// root rule
queryClause :	selectClause fromClause whereClause? orderByClause? EOF ;

// SELECT
selectClause : SELECT topClause? selectOperand (',' selectOperand?)* ;

topClause : TOP INTEGER direction=(FORWARD|BACKWARD)? ;

selectOperand :	identifiedPath (AS IDENTIFIER)? ;

// FROM
fromClause : FROM containsExpr;

// WHERE
whereClause : WHERE identifiedExpr ;

// ORDER BY
orderByClause : ORDERBY orderByExpr (','  orderByExpr)* ;

orderByExpr : identifiedPath order=(ASC | ASCENDING | DESC | DESCENDING)? ;

// TIMEWINDOW
// TODO

// global
identifiedPath : IDENTIFIER nodePredicate? ('/' objectPath?)?;

pathPart: IDENTIFIER nodePredicate?;

objectPath: pathPart ('/' pathPart?)*;

predicateOperand : identifiedPath | primitiveOperand ;

primitiveOperand: STRING | INTEGER | FLOAT | DATE | BOOLEAN | PARAMETER;

standardPredicate : '[' standardPredicateExpr ']' ;

standardPredicateExpr
    : standardPredicateExprOperand
    | NOT standardPredicateExpr
    | standardPredicateExpr AND standardPredicateExpr
    | standardPredicateExpr OR standardPredicateExpr
    | '(' standardPredicateExpr ')'
    ;

standardPredicateExprOperand : objectPath COMPARABLEOPERATOR predicateOperand;

archetypePredicate : '[' archetypePredicateExpr ']' ;

archetypePredicateExpr : ARCHETYPEID|PARAMETER ;

nodePredicate : '[' nodePredicateExpr ']' ;

nodePredicateExpr
    : nodePredicateExprOperand
    | nodePredicateExpr AND nodePredicateExpr
    | nodePredicateExpr OR nodePredicateExpr
    | '(' nodePredicateExpr ')'
    ;

nodePredicateExprOperand
    : NODEID (',' (STRING|PARAMETER))?
    | ARCHETYPEID (',' (STRING|PARAMETER))?
    | PARAMETER (',' (STRING|PARAMETER))?
    | predicateOperand COMPARABLEOPERATOR predicateOperand
    ;

identifiedExpr
    : identifiedExprOperand
    | NOT identifiedExpr
    | EXISTS identifiedExpr //This should be PredicateOperand
    | identifiedExpr AND identifiedExpr
    | identifiedExpr OR identifiedExpr
    | '(' identifiedExpr ')'
    ;

identifiedExprOperand : predicateOperand ((COMPARABLEOPERATOR predicateOperand)|(MATCHES '{' matchesOperand '}'))?;//Should not be optional

matchesOperand : valueList | URIVALUE ;

valueList : primitiveOperand  (',' primitiveOperand )* ;

containsExpr
    : classExprOperand (CONTAINS containsExpr)?
    | containsExpr AND containsExpr
    | containsExpr OR containsExpr
    | '(' containsExpr ')'
    ;

classExprOperand: IDENTIFIER IDENTIFIER? (archetypePredicate | standardPredicate)?;

// LEXER (might need some cleaning up still)
WS : [ \t\r\n] -> channel(HIDDEN) ;
COMMENT: '-''-' .*? NL -> channel(HIDDEN) ;

fragment NL: '\r'? '\n' ;

SELECT : S E L E C T ;
TOP : T O P ;
FORWARD : F O R W A R D ;
BACKWARD : B A C K W A R D ;
AS : A S ;
CONTAINS : C O N T A I N S ;
WHERE : W H E R E ;
ORDERBY : O R D E R ' ' B Y ;
TIMEWINDOW : T I M E W I N D O W ;
FROM : F R O M ;
DESC : D E S C ;
DESCENDING : D E S C E N D I N G ;
ASC : A S C ;
ASCENDING : A S C E N D I N G ;
AND : A N D ;
OR : O R ;
NOT : N O T ;
MATCHES : M A T C H E S ;
EXISTS: E X I S T S ;
VERSION	: V E R S I O N ;
VERSIONED_OBJECT : V E R S I O N E D '_' O B J E C T ;
LATEST_VERSION : L A T E S T '_' V E R S I O N ;
ALL_VERSIONS : A L L '_' V E R S I O N S ;

fragment ESC_SEQ
    :   '\\' [btnfr"'\\]
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment OCTAL_ESC
    :   '\\' [0-3] OCTAL_DIGIT OCTAL_DIGIT
    |   '\\' OCTAL_DIGIT OCTAL_DIGIT
    |   '\\' OCTAL_DIGIT
    ;

fragment OCTAL_DIGIT : [0-8] ;

fragment UNICODE_ESC : '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT ;

fragment HEX_DIGIT : [0-9a-zA-Z] ;

fragment DIGIT : [0-9];

fragment HEXCHAR : DIGIT [a-fA-F];

fragment LETTER :	[a-zA-Z];

fragment ALPHANUM :	LETTER|DIGIT;

fragment LETTERMINUSA :	[b-zB-Z];

fragment LETTERMINUST :	[a-su-zA-SU-Z];

fragment IDCHAR	: ALPHANUM|'_';

fragment IDCHARMINUST :	LETTERMINUST|DIGIT|'_';

fragment URISTRING :	ALPHANUM|'_'|'-'|'/'|':'|'.'|'?'|'&'|'%'|'$'|'#'|'@'|'!'|'+'|'='|'*';

BOOLEAN	:	'true' | 'false' | 'TRUE' | 'FALSE' ;
NODEID	:	('at'|'id') DIGIT+ ('.' DIGIT+)*; // DIGIT DIGIT DIGIT DIGIT;
IDENTIFIER
	:	('a'|'A') (ALPHANUM|'_')*
	| 	LETTERMINUSA IDCHAR*
	;

INTEGER	: '-'? DIGIT+;

FLOAT :	'-'? DIGIT+ '.' DIGIT+;

DATE
    : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT'-' DIGIT DIGIT  //1909-12-19
    | DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT'-' DIGIT DIGIT (' ')* DIGIT DIGIT ':' DIGIT DIGIT //1909-12-19 19:09
    | DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT'-' DIGIT DIGIT (' ')* DIGIT DIGIT ':' DIGIT DIGIT ':' DIGIT DIGIT; //1909-12-19 19:09:00

PARAMETER :	'$' LETTER IDCHAR*;

UNIQUEID
    : DIGIT+ ('.' DIGIT+)+ '.' DIGIT+  // OID
    | HEXCHAR+ ('-' HEXCHAR+)+       // UUID
	;

ARCHETYPEID : LETTER+ '-' LETTER+ '-' (LETTER|'_')+ '.' (IDCHAR|'-')+ '.v' DIGIT ('.' DIGIT)? ('.' DIGIT)?;
COMPARABLEOPERATOR :	'=' | '!=' | '>' | '>=' | '<' | '<=' ;
URIVALUE: LETTER+ '://' (URISTRING|'['|']'|', \''|'\'')* ;
STRING
   	:  '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\''
   	|  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
   	;

QUOTE :	'\'';
SLASH :	'/';
COMMA :	',';

OPENBRACKET : '[';
CLOSEBRACKET : ']';

OPEN	:	'(';
CLOSE	:	')';

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];