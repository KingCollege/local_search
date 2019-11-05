cls
echo Making build folder...
if exist build RD /Q /S build
mkdir build
find "%cd%" -name "*.java" > build/sources.txt
cd build
javac -d . -Xlint:deprecation @sources.txt
if exist sources.txt del /Q sources.txt
