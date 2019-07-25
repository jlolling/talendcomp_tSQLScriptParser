package de.cimt.talendcomp.dbtools.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

public class TestParseSQL {

	@Test
	public void testSelectsStrict() throws Exception {
		System.out.println("################# testSelectsStrict");
		String sql1 = "with ws1 as (\n"
				    + "    select schema_c.function1(x) as alias_x from schema_c.table_c\n"
				    + "), \n"
				    + "ws2 as (\n"
				    + "    select y from schema_d.table_d\n"
				    + ") \n"
				    + "select \n"
				    + "    a as alias_a, \n"
				    + "    b as alias_b, \n"
				    + "    (select c from schema_e.table_e) as alias_c \n"
				    + "from schema_a.table_1 ta \n"
				    + "join schema_b.table_b tb using(c) \n"
				    + "join ws1 using(c)";
		Statement stmt = CCJSqlParserUtil.parse(sql1);
		assertTrue(stmt != null);
	}

	@Test
	public void testSelectsFromMethod() throws Exception {
		System.out.println("################# testSelectsFromMethod");
		String sql1 = "with ws1 as (\n"
				    + "    select schema_c.function1(x) as alias_x from schema_c.table_c\n"
				    + "), \n"
				    + "ws2 as (\n"
				    + "    select y from schema_d.table_d\n"
				    + ") \n"
				    + "select \n"
				    + "    a as alias_a, \n"
				    + "    b as alias_b, \n"
				    + "    (select c from schema_e.table_e) as alias_c \n"
				    + "from schema_a.table_1 ta \n"
				    + "join schema_b.table_b tb using(c) \n"
				    + "join ws1 using(c)";
		SQLParser parser = new SQLParser();
		List<String> tableList = parser.findFromTables(sql1);
		for (String t : tableList) {
			System.out.println(t);
		}
		assertEquals(5, tableList.size());
	}
	
	@Test
	public void testFindWithNames() throws Exception {
		System.out.println("################# testFindWithNames");
		String sql1 = "with ws1 as (\n"
				    + "    select schema_c.function1(x) as alias_x from schema_c.table_c\n"
				    + "), \n"
				    + "ws2 AS(\n"
				    + "    select y from schema_d.table_d\n"
				    + "),ws3 as (select y from schema_d.table_d)\n"
				    + "select \n"
				    + "    a as alias_a, \n"
				    + "    b as alias_b, \n"
				    + "    (select c from schema_e.table_e) as alias_c \n"
				    + "from schema_a.table_1 ta \n"
				    + "join schema_b.table_b tb using(c) \n"
				    + "join ws1 using(c)";
		SQLParser parser = new SQLParser();
		List<String> tableList = parser.findWithNames(sql1);
		for (String t : tableList) {
			System.out.println(t);
		}
		assertEquals(3, tableList.size());
	}

	@Test
	public void testFindTableNames2() throws Exception {
		System.out.println("################# testFindTableNames2");
		String sql1 = "with \n"
			    + " mandate_verfied as"
			    + " (select b17_core.catalog_id_by_sysname('mandate_verified', 'process_status') as id)\n"
			    + "select \n"
			    + "   cnt.businessobject_id as contract_bo_id,\n"
			    + "   cnt.contracttype_id, \n"
			    + "   cnt.name as contract_name,\n"
			    + "   mnd.mandate_id,\n"
			    + "   mnd.contract_id as mandate_contract_id,\n"
			    + "   mnd.valid_from as mandate_valid_from,\n"
			    + "   mnd.valid_to as mandate_valid_to,\n"
			    + "   mnd.tu_id as mandate_tu_id,\n"
			    + "   mnd.status_id as mandate_status_id,\n"
			    + "   b17_core.catalog_sysname_by_id(mnd.status_id) as mandate_status_sysname,\n"
			    + "   b17_core.catalog_label_by_id(mnd.status_id) as mandate_status_label,\n"
			    + "   pn.businesspartner_id as business_partner_id,\n"
			    + "   cnt.consignatory_id as business_partner_id_contract,\n"
			    + "   pn.name as business_partner_name,\n"
			    + "   pn.firstname as business_partner_firstname,\n"
			    + "   cntpn.businesspartner_id as contract_partner_id,\n"
			    + "   cnt.affiliate_id as contract_partner_id_contract,\n"
			    + "   cntpn.name as contract_partner_name,\n"
			    + "   cntpn.firstname as contract_partner_firstname\n"
			    + "from \n"
			    + " b17_core.contract cnt\n"
			    + " inner join b17_core.mandate mnd\n"
			    + "  on \n"
			    + "   mnd.contract_id = cnt.businessobject_id\n"
			    + " inner join b17_core.partnername pn\n"
			    + "  on \n"
			    + "   pn.businesspartner_id = cnt.consignatory_id\n"
			    + " inner join b17_core.partnername cntpn\n"
			    + "  on \n"
			    + "   cntpn.businesspartner_id = cnt.affiliate_id\n"
			    + "where\n"
			    + " pn.default_selection = true\n"
			    + " and\n"
			    + " cnt.affiliate_id =";
		SQLParser parser = new SQLParser();
		List<String> tableList = parser.findFromTables(sql1);
		for (String t : tableList) {
			System.out.println(t);
		}
		assertEquals(3, tableList.size());
		assertEquals("table 1 incorrect", "b17_core.contract",  tableList.get(0));
		assertEquals("table 2 incorrect", "b17_core.mandate",  tableList.get(1));
		assertEquals("table 2 incorrect", "b17_core.partnername",  tableList.get(2));
	}

	@Test
	public void testOracleScript() {
		System.out.println("################# testOracleScript");
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
		//sql.append("/* block comment*/\n");
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
		assertEquals(3, p.getStatementCount());
	}
	
	@Test
	public void testBackslashIsNotEscape() {
		System.out.println("################# testBackslashIsNotEscape");
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
			System.out.println("-- *************");
			System.out.println(s.getSQL());
		}
		assertEquals(2, p.getStatementCount());
	}
	
	@Test
	public void testSemikolonInSQL() {
		System.out.println("################# testSemikolonInSQL");
		String actual = null; 
		String expected = "SELECT log:host::string FROM log_table";
		SQLParser p = new SQLParser();
		p.setIncludeComments(true);
		p.setUseScriptDetecting(true);
		p.setDetectBindVariables(false);
		p.parseScript(expected);
		for (SQLStatement s : p.getStatements()) {
			System.out.println("-- #############");
			System.out.println(s.getSQL());
			actual = s.getSQL();
		}
		assertEquals("Parser result wrong", expected, actual);
	}
	
	@Test
	public void testLineCommentParserIssue() {
		System.out.println("################# testLineCommentParserIssue");
		String sql1 = "BEGIN\n"
			    //+ "select 1;\n"
			    //+ "NULL;\n"
			    + "END;";
		String sql2 = "BEGIN\n"
			    + "select 2;\n"
			    + "END;";
		String script = sql1
			    + "\n/\n"
			    + "-- y\n"
			    //+ "/* my comment */\n"
			    + sql2;
		SQLParser p = new SQLParser();
		p.setUseScriptDetecting(true);
		p.parseScript(script);
		List<SQLStatement> stats = p.getStatements();
		for (SQLStatement s : stats) {
			System.out.println("*****************");
			System.out.println(s.getSQL());
		}
		assertEquals("Number statements wrong", 2, stats.size());
	}

}
