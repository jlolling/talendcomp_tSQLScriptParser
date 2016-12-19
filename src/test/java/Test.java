import de.cimt.talendcomp.dbtools.parser.SQLParser;
import de.cimt.talendcomp.dbtools.parser.SQLStatement;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test test = new Test();
		test.testBackslashIsNotEscape();
	}

	public void testOracleScript() {
		StringBuilder sql = new StringBuilder();
		sql.append("-- line comment 1\n");
		sql.append("/* comment 2 */\n");
		sql.append("begin\n");
		sql.append("-- comment after begin\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_2_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_3_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_4_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_5_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_6_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_7_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.ADD_LOGFILE (LogFileName => '/u01/app/oracle/product/11.2.0/xe/dbs/arch1_8_820601368.dbf',options => DBMS_LOGMNR.NEW);\n");
		sql.append("DBMS_LOGMNR.START_LOGMNR (DictFileName => '/tmp/utl/dictionary.ora');\n");
		sql.append("end;\n");
		sql.append("/ \n");
		sql.append("/* block comment*/\n");
		sql.append("create table stage as SELECT * FROM  v$logmnr_contents;\n");
		sql.append("begin  \n");
		sql.append("DBMS_LOGMNR.END_LOGMNR();\n");
		sql.append("end;\n");
		sql.append("/");
		String script = sql.toString();
//		String script = "/* comment 1 */\n" +
//				"-- line comment 1a\n" +
//				"select * /* comment2 */ from table; \r\n" +
//				
//				"/* comment 3 */\n\r" +
//				"update table;  -- comment4\n" +
//				
//				"-- line comment 5\n" +
//				"statement 3;\n"+
//				
//				"-- line comment 5b\n" +
//				";\n"+
//
//				"\n-- comment 6\n";
//		String script = "begin -- comment\nstat1; stat2;end;\n/" ;
		SQLParser p = new SQLParser();
		p.setIncludeComments(true);
		p.parseScript(script);
		for (SQLStatement s : p.getStatements()) {
			System.out.println("-------------------");
			System.out.println(s.getSQL());
		}
	}
	
	public void testBackslashIsNotEscape() {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE b17_core.title\n");
		sb.append("SET title = tmp.titelBereinigt, job_instance_id = '-771480954966350'\n");
		sb.append("FROM ( VALUES\n");
		sb.append("	(52676106, 'By Your Side'),\n");
		sb.append("	(46241884, 'Englische Suite Nr. 3 g-Moll BWV 808: 2. Allemande'),\n");
		sb.append("	(46085165, 'Someone To Watch Over Me'),\n");
		sb.append("	(46101525, 'Eastern Seas'),\n");
		sb.append("	(122850003, 'Spring Mcmlxxiv'),\n");
		sb.append("	(46340114, 'Dope On A Rope'),\n");
		sb.append("	(45857405, 'Sister'),\n");
		sb.append("	(40094922, 'Can''t Stop'),\n");
		sb.append("	(52676006, 'Nur Das Beste'),\n");
		sb.append("	(46340424, 'The Fight Is On'),\n");
		sb.append("	(45857665, 'Lix'),\n");
		sb.append("	(52692406, 'To Come'),\n");
		sb.append("	(46340454, 'Soldier'),\n");
		sb.append("	(46225774, 'Egerlaender Musikanten Spielen'),\n");
		sb.append("	(46340474, 'Soldier At His Best'),\n");
		sb.append("	(46340354, 'On The Wings Of Rock And Roll'),\n");
		sb.append("	(46471444, 'Schluss per Sms'),\n");
		sb.append("	(46325444, 'Jumper, (Dancefloor Kingz Remix)'),\n");
		sb.append("	(46259944, 'Études-Tableaux op. 33 Nr. 1-8: Nr. 7 g-Moll: Moderato'),\n");
		sb.append("	(46276334, 'Trenchmore'),\n");
		sb.append("	(46276224, 'I Got Plenty O'' Nuttin'''),\n");
		sb.append("	(45973035, 'Fizzy'),\n");
		sb.append("	(45973005, 'Catapult 30'),\n");
		sb.append("	(52693766, 'Paths, Prints'),\n");
		sb.append("	(53151901, ' / \\ / \\ / \\ Y / \\')) AS tmp(productId, titelBereinigt)\n");
		sb.append("WHERE b17_core.title.product_id = tmp.productId AND b17_core.title.primary_selection=TRUE;");
		sb.append("select * from tabelle;");
		SQLParser p = new SQLParser();
		p.setIncludeComments(true);
		p.setUseScriptDetecting(true);
		p.parseScript(sb.toString());
		for (SQLStatement s : p.getStatements()) {
			System.out.println("-- #############");
			System.out.println(s.getSQL());
		}
		
	}
	
}
