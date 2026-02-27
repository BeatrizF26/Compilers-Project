# Erro 1: condição não booleana no loop head
loop while 10 do
  !"ls"! | stdout
end

# Erro 2: loop tail com condição inválida (string em vez de booleano)
loop
  !"echo"! | stdout
until "not boolean"
end

# Erro 3: loop middle com condição inválida (real em vez de booleano)
loop
  !"ls"! | stdout
while 3.14 do
  !"date"! | stdout
end

# Erro 4: loop head com bloco vazio
loop while true do
end

# Erro 5: loop tail com bloco vazio
loop
until false
end

# Erro 6: loop middle com ambos os blocos vazios
loop
while true do
end

# Erro 7: loop middle com apenas o bloco antes da condição
loop
  !"echo"! | stdout
while true do
end

# Erro 8: loop tail com comando com erro (variável não declarada)
loop
  undeclaredVar | stdout
until true
end