cls
cd pddl\%1\instances
Set n=0
for %%x in (*.pddl) do Set /A n+=1
echo %n%
cd ../../../
echo %1: %2 >> result.csv
echo Making build folder...
if exist build RD /Q /S build
mkdir build
find "%cd%" -name "*.java" > build/sources.txt
cd build
javac -d . -Xlint:deprecation @sources.txt
if exist sources.txt del /Q sources.txt
cd build
REM for /l %%x in (39, 1, %n%) do java -Xmx4096M javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl %2 %3

for /l %%x in (43, 1, %n%) do java -Xmx4096M javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl 15 %3

for /l %%x in (1, 1, %n%) do java -Xmx4096M javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl 20 %3

for /l %%x in (1, 1, %n%) do java -Xmx4096M javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl 25 %3

for /l %%x in (1, 1, %n%) do java -Xmx4096M javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl 30 %3
cd ../
