# Quadratic Formula

stdin "a: " | store in a: text;
stdin "b: " | store in b: text;
stdin "c: " | store in c: text;

"(-"+b+"+sqrt("+b+"*"+b+"-4*"+a+"*"+c+"))/(2*"+a+")" | stdout
NL | stdout
"(-"+b+"-sqrt("+b+"*"+b+"-4*"+a+"*"+c+"))/(2*"+a+")" | stdout
NL | stdout
