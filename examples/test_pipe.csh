texto: text;
"hello" |? stdout; # erro, tipo text não pode usar canal '?'

a: text;
"olá mundo" | store in a: text;