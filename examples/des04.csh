# redirection of ls stdout to grep stdin
!"ls"! | !"grep"! ["java"] | store in procList : program | stdout # should print only the files that contain java in their name

# some channel selection
!"ls"! | store in res : program
res || stdout # send ls stdout to stdout
res |& stdout # send ls stderr to stdout
res |? stdout # send ls exit value to stdout

# some channel redirection
!"ls"! &^| || store in res : program # should not print anything
!"ls"! |^& |& stdout # should print ls stdout as stderr