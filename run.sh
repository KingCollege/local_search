BASEDIR=$(dirname "$0")
ORIGINALDIR=$(pwd)

PDDLFILE="pddl/$1"

DOMAIN=$(ls $PDDLFILE | grep "domain")

cd $BASEDIR/build

for i in ../$PDDLFILE/instances/*.pddl;
do
    java javaff.JavaFF ../$PDDLFILE/$DOMAIN $i 0 "output.txt"
done

cd $ORIGINALDIR