# Talend Compopnent tSQLScriptParser
Talend component to parse a SQL script. 

You can define the SQL script inside the component SQL field or read the script from a file.

Use the Iterate flow from the component to trigger the run for the statements (statement by statement) in a tDBRow component.
The tDBRow component can take the current single statement from a return value of the component.

![Demo job design](https://github.com/jlolling/talendcomp_tSQLScriptParser/blob/master/doc/mysql_example.png)
