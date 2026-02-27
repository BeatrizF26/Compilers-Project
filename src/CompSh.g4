grammar CompSh;

program: (stat ';'?)* EOF;

stat
  : declaration                     #StatDeclaration
  | expr                            #StatExpr
  | loop                            #StatLoop
  | if                              #StatIf
  ;

loop
  : 'loop' 'while' expr 'do' (stat ';'?)* 'end'                                               #LoopHead
  | 'loop' (stat ';'?)* 'until' expr 'end'                                                    #LoopTail
  | 'loop' preStats=statBlock 'while' expr 'do' postStats=statBlock 'end'                     #loopMiddle
  ;

// if statement
if: 'if' expr 'then' preStats=statBlock ('else' postStats=statBlock)? 'end';

statBlock: (stat ';'?)*;

declaration:
  ID (',' ID)* ':' TYPE;

// semantic expressions
expr returns[String varName, Value val]
    : sign=('+'|'-'|'not') expr                           #ExprUnary
    | '(' expr ')'                                        #ExprParens
    | expr op=('*'|'/') expr                              #ExprMulDiv
    | expr op=('+'|'-') expr                              #ExprPlusMinus
    | expr op=('>'|'<'|'>='|'<=') expr                    #ExprRelational
    | expr op=('='|'/=') expr                             #ExprRelational
    | expr 'and' expr                                     #ExprAndOr
    | expr 'or' expr                                      #ExprAndOr
    | 'prefix' expr                                       #ExprPrefix
    | 'suffix' expr                                       #ExprSuffix
    | '/' expr                                            #ExprFilter
    | '[' expr (',' expr)* ']'                            #ExprListLiteral
    | 'store' 'in'? ID (':' TYPE)?                        #ExprPipeAssignement
    |'stdout'                                             #ExprStdout
    | 'stderr'                                            #ExprStderr
    | STRING                                              #ExprStr
    | INT                                                 #ExprInt
    | FLOAT                                               #ExprFloat
    | 'true'					   	                                #ExprBoolTrue
    | 'false'						                                  #ExprBoolFalse
    | ID                                                  #ExprId
    | TYPE '(' expr ')'                                   #ExprTypeCast
    | '!' expr '!' ('[' expr (',' expr)* ']')?            #ExprExecute
    | '!!' expr '!!'                                      #ExprExecuteIsh
    | 'NL'                                                #ExprNL
    | expr channel '^' channel                            #ExprRedirection
    | expr '|' channel? expr                              #ExprPipe
    | 'stdin' prompt=(STRING | ID)?                       #ExprStdin
    ;

// pipe expressions
channel:
      '$'   #DollarChannel
    |'|'    #StdoutChannel  
    | '&'   #StderrChannel 
    | '?'   #ExitChannel
    | '*'   #AllChannels
    | '-'   #NoChannel
    ;

// data types
TYPE: TEXT | INTEGER | REAL | PROGRAM | BOOLEAN | LIST;
TEXT: 'text';
INTEGER: 'integer';
REAL: 'real';
PROGRAM: 'program';
BOOLEAN: 'boolean';
LIST: 'list';

// identifier
ID: LETTER (LETTER | DIGIT)*;

// numbers
INT: DIGIT+;
FLOAT: (DIGIT+ '.' DIGIT* | '.' DIGIT+) ([eE][+-]?[0-9]+)?; // C like floats (10. or .90 or 10e-9)

// string
STRING: '"' .*? '"'; // escape not handled

// building blocks
fragment DIGIT: [0-9];
fragment LETTER: [a-zA-Z_];

// ignores (add comments here)
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '#' .*? '\n' -> skip;            // only one line comment
COMMENT: '##' (COMMENT | .)*? '##' -> skip;    // multi-line comment