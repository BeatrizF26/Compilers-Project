# grep "java" no resultado do ls
!"ls"! || / "java" | stdout ;

res : program ;
!"ls"! | store in res ;

# indentação de 3 espaços no stdout
res || prefix "   " | stdout ;

# indentação com flecha no stderr
res |& prefix "--> " | stdout ;

# sufixar NL ao código de saída
res |? suffix NL | stdout ;

