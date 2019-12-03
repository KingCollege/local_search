cls
echo Making build folder...
if exist build RD /Q /S build
mkdir build
find "%cd%" -name "*.java" > build/sources.txt
cd build
javac -d . -Xlint:deprecation @sources.txt
if exist sources.txt del /Q sources.txt
cd build
for /l %%x in (1, 1, 3) do java javaff.JavaFF ..\pddl\%1\domain.pddl ..\pddl\%1\instances\instance-%%x.pddl
