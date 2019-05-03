package de.cimt.talendcomp.dbtools.parser;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

public class TestParseSQL {

	@Test
	public void testSelectsStrict() throws Exception {
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
	}

	@Test
	public void testSelectsFromMethod() throws Exception {
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

}
