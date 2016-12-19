/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cimt.talendcomp.dbtools.parser;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * @author lolling.jan
 * kapselt eine Parameter eines Prepared statements
 */
public class SQLPSParam implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String PARAM_DESC_DELIMITER = "\u0002";
    private SQLStatement sqlPs;                          // das zugehörige Prepared Statement
    private int index;
    private String name;                           // sofern ein Name des Parameter ermittelbar ist
    private int basicType            = -1;      // der Basistyp des Statements
    private String value;                          // hier muss ggf. ein Wrappertyp referenziert werden
    private String valueCode;
    private boolean outParam = false;
    private boolean isNamedParam = false;
	static public final int ORACLE_ROWID = -100;
	static public final int BASICTYPE_CHAR = 0;
	static public final int BASICTYPE_DATE = 1;
	static public final int BASICTYPE_NUMERIC = 2;
	static public final int BASICTYPE_BINARY = 3;
	static public final int BASICTYPE_CLOB = 4;
	static public final int BASICTYPE_BLOB = 6;
	static public final int BASICTYPE_ROWID = -100;
	static public final int BASICTYPE_BOOLEAN = 8;
    
    /**
     * @return Basistyp (wird noch von Hand konfiguriert)
     */
    public int getBasicType() {
        return basicType;
    }

    /**
     * @return Index des Parameters im Statement
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return ggf vorhandener Name
     */
    public String getName() {
        return name;
    }

    /**
     * @return zugehöriges Prepared Statement
     */
    public SQLStatement getSqlPs() {
        return sqlPs;
    }

    /**
     * @return Wert der für den Parameter gesetzt wird
     */
    public String getValue() {
        return value;
    }

    /**
     * @param i
     */
    public void setBasicType(int i) {
        basicType = i;
    }

    /**
     * @param i
     */
    public void setIndex(int i) {
        index = i;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        if (string != null && string.length() > 0) {
            string = string.trim();
            // wenn der Name Leerzeichen enthält, dann diesen Namen parsen
            // als erstes kommt der Parameterindex gefolgt von einer Raute,
            // dann kommt entweder der Name selbst oder erst mit Leerzeichen gefolgt der Datentyp
            int firstSpaceIndex = string.indexOf(" ");
            int lastSpaceIndex = string.lastIndexOf(" ");
            // zwischen der Raute und dem letzten Leerzeichen sollte der Datentyp liegen und
            // ab dem letzten Leerzeichen kommt der eigentliche Name
            if (firstSpaceIndex != -1) {
                // das bedeutet 2 Leerzeichen.
                // OK ab dem ersten bis zum zweiten ist der Datentyp und ab dem zweiten der Name
                if (lastSpaceIndex == firstSpaceIndex) {
                    if (string.indexOf("#") == -1) {
                        // ein Parameterindex ist nicht vorhanden
                        firstSpaceIndex = 0; // wenn nur ein Leerzeichen
                    }
                }
                String dataType = string.substring(firstSpaceIndex, lastSpaceIndex);
                if (dataType.length() > 0) {
                    dataType = dataType.toLowerCase().trim();
                    if (dataType.equals("string") || dataType.indexOf("char") != -1 || dataType.equals("rowid")) {
                        // Textinhalt gefunden
                        basicType = BASICTYPE_CHAR;
                    } else if (dataType.indexOf("num") != -1 || dataType.indexOf("int") != -1 || dataType.indexOf("long") != -1) {
                        basicType = BASICTYPE_NUMERIC;
                    } else if (dataType.indexOf("date") != -1 || dataType.indexOf("time") != -1) {
                        basicType = BASICTYPE_DATE;
                    }
                }
                name = string.substring(lastSpaceIndex).trim();
            } else {
                name = string.trim();
            }
        }
    }

    /**
     * @param statement
     */
    public void setSqlPs(SQLStatement statement) {
        sqlPs = statement;
    }

    /**
     * @param object
     */
    public void setValue(String value_loc) {
        this.value = value_loc;
    }

    /**
     * prüft, ob der parameter gültig ist
     * bedeutet, er hat einen Wert und einen gültigen Index
     * @return true wenn gültig
     */
    public boolean isValid() {
        return (index > 0) && (value != null);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("index=");
        sb.append(index);
        if (outParam) {
            sb.append(" out");
        } else {
            sb.append(" in");
        }
        if (value != null) {
            sb.append(" name=");
            sb.append(name);
        }
        if (value != null) {
            sb.append(" value=");
            sb.append(value);
        }
        return sb.toString();
    }

    /**
     * parst eine Stringbeschreibung des Parameters
     * @param paramStr
     */
    public void parseParamStr(String paramStr) {
        final StringTokenizer st=new StringTokenizer(paramStr,PARAM_DESC_DELIMITER);
        index = Integer.parseInt(st.nextToken());
        name = st.nextToken();
        value = st.nextToken();
    }

    /**
     * @return erzeugt eine Stringbeschreibung des Parameters
     */
    public String getParamStr() {
        return index + PARAM_DESC_DELIMITER + name + PARAM_DESC_DELIMITER + value;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode_loc) {
        this.valueCode = valueCode_loc;
    }

    public final boolean isOutParam() {
        return outParam;
    }

    public final void setOutParam(boolean outParam) {
        this.outParam = outParam;
    }

	public void setNamedParam(boolean isNamedParam) {
		this.isNamedParam = isNamedParam;
	}

	public boolean isNamedParam() {
		return isNamedParam;
	}

}
