cd build
for /l %%x in (1, 1, 20) do java javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl
