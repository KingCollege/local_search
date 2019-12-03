cd ../
cd Classical_Domains/classical/%1/instances
cls

set /a COUNTER=1

ls > problem_names.txt
setlocal enabledelayedexpansion
for /F "tokens=*" %%A in (problem_names.txt) do (
    rename %%A instance-!COUNTER!.pddl
    set /a "COUNTER+=1"
)
endlocal