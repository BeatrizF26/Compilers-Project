# min01.csh

!"ls"! | stdout          # write ls results in stdout
NL | stdout              # write NewLine in stdout
prog: text               # declare variable prog of type text
"find" | store in prog   # assignment: store text in variable prog
res: program;            # ; is optative
!prog! | store in res    # execute find and store result of program in variable res 
res | stdout             # write all active channels (stdout, stderr, and exit) in variable res
