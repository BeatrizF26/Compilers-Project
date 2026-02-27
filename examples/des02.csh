ans: text
0 | store count: integer
loop
   count+1 | store count
   stdin | store ans
until ans = "no" end

"loop with tail condition executed "+count+" times" | stdout 

0 | store in count
loop while count < 10 do
   count+1 | store count
end

"loop with head condition executed "+count+" times" | stdout 

0 | store in count
loop
   stdin | store ans
while ans /= "no" do
   count+1 | store count
end

"loop with middle condition executed "+count+" times" | stdout 
