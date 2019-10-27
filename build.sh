BASEDIR=$(dirname "$0")
ORIGINALDIR=$(pwd)
cd $BASEDIR
rm -rf build
mkdir build
find "$PWD" -name "*.java" > build/sources.txt
cd build
javac -d . -Xlint:deprecation @sources.txt
rm sources.txt
cd $ORIGINALDIR