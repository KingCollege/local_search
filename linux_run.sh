#Where the script is located
BASEDIR=$(dirname "$0")
#Where the shell user actually ran the script from
ORIGINALDIR=$(pwd)
cd $BASEDIR
rm -rf build
mkdir build
find "$PWD" -name "*.java" > build/sources.txt
cd build
javac -d . -Xlint:deprecation @sources.txt
rm sources.txt
cd $ORIGINALDIR
# Remove Later for submission purposes

BASEDIR=$(dirname "$0")
ORIGINALDIR=$(pwd)

PDDLFILE="pddl/$1"

DOMAIN=$(ls $PDDLFILE | grep "domain")
cd $BASEDIR/$PDDLFILE/instances
TOTAL=$(ls -l | wc -l)
TOTAL=$((TOTAL-1))
echo $TOTAL
START=1
cd $ORIGINALDIR
cd $BASEDIR/build

for (( i=$START; i<=$TOTAL; i++ ))
do
    java javaff.JavaFF ../$PDDLFILE/$DOMAIN ../$PDDLFILE/instances/instance-$i.pddl $2
done

cd $ORIGINALDIR