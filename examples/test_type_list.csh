# Listas com tipos diferentes
! "ls" ! [ "*.java", 42, true, "CompSh.g4" ] || stdout ;

# Guardar nome de ficheiro numa variável
stdin "ficheiro:" | store in nome : text ;

# Repetir comando com nome da variável
! "ls" ! [ nome ] || stdout ;

# Lista aninhada (não permitido)
! "ls" ! [ [ "*.java", "*.g4" ], "*.txt" ] || stdout ;

