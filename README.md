# Talend Compopnent tSQLScriptParser
Talend component to parse a SQL script. 

You can define the SQL script inside the component SQL field or read the script from a file.

Use the Iterate flow from the component to trigger the run for the statements (statement by statement) in a tDBRow component.
The tDBRow component can take the current single statement from a return value of the component.

Very simple example:

![Demo job design](https://github.com/jlolling/talendcomp_tSQLScriptParser/blob/master/doc/mysql_example.png)

Example with prepared statements (setup the parameter value happen in the tPostgresRow component):

![Demo job design](https://github.com/jlolling/talendcomp_tSQLScriptParser/blob/master/doc/tSQLScriptParser_example_with_prepared_statements.png)

Better design with a separate connection component and a proper transaction handling:

![Demo job design](https://github.com/jlolling/talendcomp_tSQLScriptParser/blob/master/doc/scenario_row.png)

Here a design of a generic job running SQL scripts:

![Demo job design](https://github.com/jlolling/talendcomp_tSQLScriptParser/blob/master/doc/tSQLScriptParser_Generic_script_job.png)
