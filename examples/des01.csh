# Quadratic Formula solver

stdin "a: " | store in a: real;
stdin "b: " | store in b: real;
stdin "c: " | store in c: real;

root1, root2: real

if b*b-4*a*c < 0 then
   "Complex roots! " | stderr
else
   # Note: in the absense of stdout, expression channel can be are redirected to stdin
   "(-"+text(b)+"+sqrt("+text(b)+"*"+text(b)+"-4*"+text(a)+"*"+text(c)+"))/(2*"+text(a)+")" | !"bc"!["-l"] | store in root1
   "(-"+text(b)+"-sqrt("+text(b)+"*"+text(b)+"-4*"+text(a)+"*"+text(c)+"))/(2*"+text(a)+")" | !"bc"!["-l"] | store in root2

   "root 1: "+root1 | stdout
   "root 2: "+root2 | stdout
end