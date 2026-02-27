!"ls"! | !"sort"! | !"grep"![".java"] | stdout

!"echo"!["sqrt(10)"] | !"bc"! | stdout

stdin "Path: " | store in path: text;

!"ls"![path] |? store in exit: integer |& store in err: text || stdout

"exit: "+exit+NL | stdout
"stderr:"+NL+err+NL | stdout
