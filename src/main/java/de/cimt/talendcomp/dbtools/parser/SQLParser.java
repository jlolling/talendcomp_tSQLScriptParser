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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Klasse enthält Methoden zum Zerlegen von kompletten SQL-Scripten
 */
public final class SQLParser {

    private List<SQLStatement> parsedStatements;
    protected int           blockCount;             // zählt die Anzahl der veschachtelten Codeblöcke
    private boolean         parserDisabled  = false;
    static public final int PL_SQL          = 0;
    private boolean         isPLSQL         = false;
    private boolean         autoDetectPLSQL = true;
    private boolean         includeComments = true;
    private boolean 		detectBindVariables = true;
    static int maxTextStringLength = 0;
    private static final char STATEMENT_END = ';';
    private char statementEnd = STATEMENT_END;
    private static final char SCRIPT_END = '/';
    private char scriptEnd = SCRIPT_END;
    private boolean useScriptDetecting = true;
    static String[]         plsqlKeyWords   = {
            "DECLARE",
            "CREATE OR REPLACE PROCEDURE",
            "CREATE OR REPLACE FUNCTION",
            "CREATE OR REPLACE PACKAGE",
            "CREATE FUNCTION",
            "CREATE PROCEDURE",
            "CREATE PACKAGE",
            "CREATE OR REPLACE TRIGGER",
            "CREATE TRIGGER",
            "BEGIN"                        };
    
    static {
    	for (String s : plsqlKeyWords) {
    		int l = s.length();
    		if (l > maxTextStringLength) {
    			maxTextStringLength = l;
    		}
    	}
    }

    public SQLParser() {}

    public SQLParser(String text) {
        parseScript(text);
    }

    public SQLParser(File file, String charset) throws IOException {
    	String sql = readSQLFile(file, charset);
        parseScript(sql);
    }

    public SQLParser(String text, boolean disableParser) {
        this.parserDisabled = disableParser;
        parseScript(text);
    }

    public SQLParser(String text, boolean disableParser, boolean enableAutoDetectPLSQL) {
        this.parserDisabled = disableParser;
        this.autoDetectPLSQL = enableAutoDetectPLSQL;
        parseScript(text);
    }

    /**
     * setzt die internen Statusvariablen auf defaultwerte
     */
    private void initParser() {
        parsedStatements = new ArrayList<SQLStatement>();
        isPLSQL = false;
        blockCount = 0;
    }

    public void disableParser(boolean disable) {
    	this.parserDisabled = disable;
    }
    
    public boolean isParserDisabled() {
    	return parserDisabled;
    }
    
    /**
     * zerlegt ein Script in einzelne Statements
     * die Ergebnisse werden in parsedStatements gehalten
     * @param text enthält das Script
     * @return der geparste Script-Text zur Kontrolle
     */
    public void parseScript(String text) {
        initParser();
    	if (text == null) {
    		throw new IllegalArgumentException("sql code cannot be null");
    	}
        final StringBuilder temp = new StringBuilder();
        if (parserDisabled) {
            SQLStatement.resetIndex();
            final SQLStatement sql = new SQLStatement(text);
            sql.setTextRange(0, text.length());
            parsedStatements.add(sql);
        } else {
            // in text ist ein komplettes Script enthalten
            // der Text muss komplett analysiert werden um das korrekte Ende des Statements zu finden.
            // zuerst nach String-Konstanten suchen und diese in der Suche nach dem Semikolon überspringen.
            // das ganz hat nur Zweck wenn es den auch Text zum Parsen gibt !
            // er muss mindestens 2 Zeichen lang sein !
            if (text.length() > 1) {
                SQLStatement.resetIndex();
                boolean inStringConstant = false;
                boolean inLineComment = false;
                boolean inBlockComment = false;
                boolean endOfStatement = false;
                char c0 = '\n'; // last c
                char c;         // current char
                char c1 = '\n'; // char after current char
                int i = 0;      // Zeiger im Text
                int as = 0;     // Anfang des Statements im Text
                boolean isPreparedStatement = false;
                boolean hasNamedParams = false;
                boolean isNotEmpty = false;
                boolean plsql = false;
                boolean plsqlTested = false;
                for (; i < text.length(); i++) {
                	if (i > 0) {
                		c0 = text.charAt(i - 1);
                	}
                    c = text.charAt(i); // Zeichen lesen
                    if (i < text.length() - 1) {
                        c1 = text.charAt(i + 1);
                    } else {
                        c1 = ' ';
                    }
                    if (inStringConstant == false && inLineComment == false && inBlockComment == false) {
                        if (c == '\'') { // String-Konstante ??
                            inStringConstant = true; // es ist ein String-Beginn !
                            temp.append(c); // Zeichen merken
                        } else if ((c == '/') && (c1 == '*')) { // Möglicher Block-Kommentarbeginn ?
                            // ok es ist ein Block-Kommentar !
                            inBlockComment = true;
                            i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                            // Block-Kommentare in die SQL-Anweisung mit aufnehmen - Oracle-HINTS !
                            if (includeComments) {
                                temp.append(c);
                                temp.append(c1);
                            }
                        } else if ((c == '-') && (c1 == '-')) { // Inline-Kommentar ?
                            // Inline-Kommentar gefunden !
                            inLineComment = true;
                            i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                            if (includeComments) {
                                temp.append(c);
                                temp.append(c1);
                            }
                        } else if (c == statementEnd || (c0 == '\n' && c == scriptEnd && Character.isWhitespace(c1))) { // Ende des Statements gefunden ?
                        	if (c == statementEnd) {
                        		if (plsql) {
                                    temp.append(c);
                        		} else {
                            		endOfStatement = true;
                        		}
                            } else if (plsql && c0 == '\n' && c == scriptEnd && Character.isWhitespace(c1)) {
                        		endOfStatement = true;
                            }
                        	if (i == text.length() - 1) {
                        		endOfStatement = true;
                            }
                        	if (endOfStatement) {
                                if (isPreparedStatement && plsql == false) {
                                	String sqlStr = temp.toString().trim();
                                	if (sqlStr.isEmpty() == false) {
                                        final SQLStatement sql = new SQLStatement(sqlStr);
                                        sql.setPrepared(true);
                                        sql.setHasNamedParams(hasNamedParams);
                                        setupPreparedStatementParams(sql);
                                        sql.setTextRange(as, i);
                                        parsedStatements.add(sql);
                                	}
                                } else if (isNotEmpty) {
                                	String sqlStr = temp.toString().trim();
                                	if (sqlStr.isEmpty() == false) {
	                                    final SQLStatement sql = new SQLStatement(temp.toString().trim());
	                                    sql.setTextRange(as, i);
	                                    parsedStatements.add(sql);
                                	}
                                }
                                temp.setLength(0);
                                plsql = false;
                                isNotEmpty = false;
                        	}
                        } else { // nur normale Zeichen
                            if (endOfStatement) { // vorheriges Statement-Ende gefunden,
                                // nun sinnvollen Start des nächsten finden
                                if ((Character.isWhitespace(c) == false || c == '\n') && 
                                		((c0 == '\n' && c == scriptEnd && Character.isWhitespace(c1)) == false) &&
                                		c != statementEnd) {
                                    endOfStatement = false;
                                    temp.append(c); // merken
                                    as = i; // Start des Statements im text merken
                                    plsqlTested = false;
                                }
                                if (plsqlTested == false) {
                                    if (testForPLSQL(text, i)) {
                                    	plsql = true;
                                    }
                                    plsqlTested = true;
                                }
                            } else {
                                if (c == '?') {
                                    isPreparedStatement = true;
                                } else if (detectBindVariables && (c0 != ':' && c == ':' && Character.isLetter(c1))) {
                                	isPreparedStatement = true;
                                	hasNamedParams = true;
                                }
                                if (Character.isWhitespace(c) == false) {
                                    isNotEmpty = true;
                                    if (plsqlTested == false) {
                                        if (testForPLSQL(text, i)) {
                                        	plsql = true;
                                        }
                                        plsqlTested = true;
                                    }
                                }
                                temp.append(c); // merken
                            }
                        }
                    } else {
                        // das Ende der zu überspringenden Textpassagen finden
                        if (inLineComment) {
                            // Zeilenkommentare enden am Ende der Zeile
                            if (c == '\n') {
                                inLineComment = false;
                                temp.append(c);
                            } else {
                            	if (includeComments) {
                                    temp.append(c);
                            	}
                            }
                        } else if (inBlockComment) {
                            if ((c == '*') && (c1 == '/')) {
                                inBlockComment = false;
                                i++;
                                // Blockkommentare unbedingt mit nehmen, da Oracle dort HINTS versteckt !
                                if (includeComments) {
                                    temp.append(c);
                                    temp.append(c1);
                                }
                            } else if (includeComments) {
                                temp.append(c); // block comment content
                            }
                        } else if (inStringConstant) {
                            if (c == '\'') {
                                inStringConstant = false;
                            }
                            temp.append(c); // die Zeichen sind aber wichtig !
                        }
                    }
                } // Ende der ersten Schleife in a steht nun der Beginn der zu überspringenden Textstelle
                // ist die Schleife komplett durch den Text durch ?
                if (!endOfStatement && (i == text.length())) {
                    // in temp ist nun ein komplettes Statement enthalten und der text ist durch !
                    final String remainingText = temp.toString().trim();
                    if (autoDetectPLSQL && testForPLSQL(remainingText.toUpperCase(), 0)) {
                    	plsql = true;
                    } else {
                        if (remainingText.length() > 2) {
                            if (isPreparedStatement) {
                                final SQLStatement statement = new SQLStatement(remainingText);
                                statement.setPrepared(true);
                                setupPreparedStatementParams(statement);
                                statement.setTextRange(as, i - 1);
                                parsedStatements.add(statement);
                            } else if (isNotEmpty) {
                                final SQLStatement sql = new SQLStatement(remainingText);
                                sql.setTextRange(as, i - 1);
                                parsedStatements.add(sql);
                            }
                        }
                    }
                } // if (!endOfStatement && (i == text.length()))
                isPLSQL = plsql;
            } // if (text.length() > 1)
        } // if (parserDisabled)
    }
    
    /**
     * findet die passende Klammer (vorwärts oder rückwärts)
     * @param currPos Die Textposition vor oder nach der Referenz-Klammer
     * @param text der zu durchsuchende Text
     * @return Textposition an der die passende Klammer steht
     */
    public static int[] findOppositeParenthese(int currPos, String text) {
        int[] bracketPositions = new int[] {-1, -1};
        if (text.length() == 0) {
            return bracketPositions;
        }
        int bracketCounter = 0;
        char c = ' ';
        if (currPos < text.length()) {
            c = text.charAt(currPos);
        }
        char c0 = ' ';
        if (currPos > 0) {
            c0 = text.charAt(currPos - 1);
        }
        char c1;
        boolean searchForwards = false;
        if (c0 == ')') {
            searchForwards = false;
            currPos--;
            bracketPositions[1] = currPos;
        } else if (c0 == '(') {
            searchForwards = true;
            currPos--;
            bracketPositions[0] = currPos;
        } else if (c == '(') {
            searchForwards = true;
            bracketPositions[0] = currPos;
        } else if (c == ')') {
            searchForwards = false;
            bracketPositions[1] = currPos;
        } else {
            return bracketPositions;
        }
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;
        if (searchForwards) {
            for (int i = currPos + 1; i < text.length(); i++) {
                c = text.charAt(i);
                if (i < text.length() - 1) {
                    c1 = text.charAt(i + 1);
                } else {
                    c1 = ' ';
                }
                if ((!inString) && (c == '\'')) {
                    inString = true;
                } else if (!inBlockComment && (c == '/') && (c1 == '*')) {
                    inBlockComment = true;
                } else if (!inLineComment && (c == '-') && (c1 == '-')) {
                    inLineComment = true;
                } else {
                    if (!inLineComment && !inBlockComment && !inString) {
                        if (c == ')') {
                            // gefunden...
                            if (bracketCounter == 0) {
                                bracketPositions[1] = i;
                                break;
                            } else if (bracketCounter > 0) {
                                bracketCounter--;
                            }
                        } else if (c == '(') {
                            bracketCounter++;
                        }
                    } else {
                        if (c == '\n') {
                            inLineComment = false;
                            inString = false;
                        } else if (inBlockComment && (c == '*') && (c1 == '/')) {
                            inBlockComment = false;
                        } else if (inString && (c == '\'')) {
                            inString = false;
                        }
                    }
                }
            }
        } else {
            for (int i = currPos - 1; i >= 0; i--) {
                c = text.charAt(i);
                if (i < text.length() - 1) {
                    c1 = text.charAt(i + 1);
                } else {
                    c1 = ' ';
                }
                if ((!inString) && (c == '\'')) {
                    inString = true;
                } else if (c == '\n') {
                    inString = false;
                    final int lineCommentStart = findLineCommentStartBackwards(i - 1, text);
                    if (lineCommentStart != -1) {
                        inLineComment = true;
                    }
                } else if (!inBlockComment && (c == '*') && (c1 == '/')) {
                    inBlockComment = true;
                } else {
                    if (!inLineComment && !inBlockComment && !inString) {
                        if (c == '(') {
                            // gefunden...
                            if (bracketCounter == 0) {
                                bracketPositions[0] = i;
                                break;
                            } else if (bracketCounter > 0) {
                                bracketCounter--;
                            }
                        } else if (c == ')') {
                            bracketCounter++;
                        }
                    } else {
                        if (inLineComment && (c == '-') && (c1 == '-')) {
                            inLineComment = false;
                        } else if (inBlockComment && (c == '/') && (c1 == '*')) {
                            inBlockComment = false;
                        } else if (inString && (c == '\'')) {
                            inString = false;
                        }
                    }
                }
            }
        }
        return bracketPositions;
    }

    /**
     * findet die passende Klammer (vorwärts oder rückwärts)
     * @param currPos Die Textposition NACH(!) der Referenz-Klammer
     * @param text der zu durchsuchende Text
     * @return Textposition an der die passende Klammer steht
     */
    public static int findOppositeToken(int currPos, String text, String startToken, String endToken) {
        int oppositePos = -1;
        int bracketCounter = 0;
        // abhängig davon was wir VOR der aktuellen Textposition finden
        currPos--;
        if (currPos > 0) {
            char c = text.charAt(currPos);
            char c1;
            boolean inLineComment = false;
            boolean inBlockComment = false;
            boolean inString = false;
            if (c == '(') {
                for (int i = currPos + 1; i < text.length(); i++) {
                    c = text.charAt(i);
                    if (i < text.length() - 1) {
                        c1 = text.charAt(i + 1);
                    } else {
                        c1 = ' ';
                    }
                    if (!inString && (c == '\'')) {
                        inString = true;
                    } else if (!inBlockComment && (c == '/') && (c1 == '*')) {
                        inBlockComment = true;
                    } else if (!inLineComment && (c == '-') && (c1 == '-')) {
                        inLineComment = true;
                    } else {
                        if (!inLineComment && !inBlockComment && !inString) {
                            if (c == ')') {
                                // gefunden...
                                if (bracketCounter == 0) {
                                    oppositePos = i;
                                    break;
                                } else if (bracketCounter > 0) {
                                    bracketCounter--;
                                }
                            } else if (c == '(') {
                                bracketCounter++;
                            }
                        } else {
                            if (c == '\n') {
                                inLineComment = false;
                                inString = false;
                            } else if (inBlockComment && (c == '*') && (c1 == '/')) {
                                inBlockComment = false;
                            } else if (inString && (c == '\'')) {
                                inString = false;
                            }
                        }
                    }
                }
            } else if (c == ')') {
                for (int i = currPos - 1; i >= 0; i--) {
                    c = text.charAt(i);
                    if (i < text.length() - 1) {
                        c1 = text.charAt(i + 1);
                    } else {
                        c1 = ' ';
                    }
                    if (!inString && (c == '\'')) {
                        inString = true;
                    } else if (c == '\n') {
                        inString = false;
                        final int lineCommentStart = findLineCommentStartBackwards(i - 1, text);
                        if (lineCommentStart != -1) {
                            inLineComment = true;
                        }
                    } else if (!inBlockComment && (c == '*') && (c1 == '/')) {
                        inBlockComment = true;
                    } else {
                        if (!inLineComment && !inBlockComment && !inString) {
                            if (c == '(') {
                                // gefunden...
                                if (bracketCounter == 0) {
                                    oppositePos = i;
                                    break;
                                } else if (bracketCounter > 0) {
                                    bracketCounter--;
                                }
                            } else if (c == ')') {
                                bracketCounter++;
                            }
                        } else {
                            if (inLineComment && (c == '-') && (c1 == '-')) {
                                inLineComment = false;
                            } else if (inBlockComment && (c == '/') && (c1 == '*')) {
                                inBlockComment = false;
                            } else if (inString && (c == '\'')) {
                                inString = false;
                            }
                        }
                    }
                }
            }
        }
        return oppositePos;
    }

    /**
     * findet einen Beginn eines Zeilenkommentares rückwärts durch die Zeile gehend
     * @param currPos ab dieser Poition rückwärts suchen
     * @param text Der zu durchsuchende Text
     * @return position innerhalb des Textes (-1 falls innerhalb der Zeile kein Zeilenkommentar gegann)
     */
    public static int findLineCommentStartBackwards(int currPos, String text) {
        int commentStart = -1;
        char c;
        char c1;
        boolean inString = false;
        for (int i = currPos; i >= 0; i--) {
            c = text.charAt(i);
            if (i < text.length() - 1) {
                c1 = text.charAt(i + 1);
            } else {
                c1 = ' ';
            }
            if (c == '\n') {
                // Suche abbrechen, da Zeilenanfang erreicht
                break;
            } else if (!inString && (c == '\'')) {
                inString = true;
            } else if (inString && (c == '\'')) {
                inString = false;
            } else if (!inString && (c == '-') && (c1 == '-')) {
                commentStart = i + 1; // da erst beim darauffolgenden Durchlauf der Kommentar erkannt wurde
                break;
            }
        }
        return commentStart;
    }

    private boolean testForPLSQL(String text, int startPos) {
    	if (useScriptDetecting == false) {
    		return false;
    	} else {
        	String test = null;
        	if ((startPos + maxTextStringLength) <= text.length()) {
        		test = text.substring(startPos, startPos + maxTextStringLength).trim().toUpperCase();
        	} else {
        		test = text.substring(startPos).trim().toUpperCase();
        	}
            boolean testResult = false;
        	for (int i = 0; i < plsqlKeyWords.length; i++) {
                if (test.startsWith(plsqlKeyWords[i])) {
                	testResult = true;
                    break;
                }
            } // for (int i=0; i < plsqlKeyWords.length; i++)
            return testResult;
    	}
    }

    public boolean isPLSQL() {
        return isPLSQL;
    }

    public int getStatementCount() {
        return parsedStatements.size();
    }

    public List<SQLStatement> getStatements() {
        return parsedStatements;
    }

    public SQLStatement getStatementAt(int index) {
        return parsedStatements.get(index);
    }

    public String getStatementSQLAt(int index) {
    	return parsedStatements.get(index).getSQL();
    }
    
    public String getStatementTypeAt(int index) {
    	switch (parsedStatements.get(index).getType()) {
    	case SQLStatement.EXPLAIN: return "explain";
    	case SQLStatement.OTHER: return "other";
    	case SQLStatement.QUERY: return "query";
    	case SQLStatement.START: return "start";
    	case SQLStatement.UPDATE: return "update";
    	default: return "unknwon";
    	}
    }
    
    /**
     * returns the name of the table or view for querys
     * @param statement
     * @return name from table or view
     */
    public static String getTableForQuery(SQLStatement stat) {
        String sql = stat.getSQL();
        sql = sql.toLowerCase();
        sql = sql.replace('\n', ' ');
        sql = sql.replace('\r', ' ');
        sql = sql.replace('\t', ' ');
        sql = removeAllComments(sql);
        // unbedingt ggf selects in der Spaltenliste ausblenden!
        // die alte Methode (Suche von hinten) hat den Fehler, dass bei where-Bedingungen mit
        // Abfragen die falsche Tabelle gefunden wird !
        // die neue Methode ist folgende:
        // ab dem Anfang starten und Zeichen für Zeichen durchgehen
        // wird eine öffnende Klammer gefunden den zähler für Klammern um eines
        // erhöhen
        // wenn der zähler für Klammern auf 0 steht überprüfen, ob es sich um Stringkostanten handelt
        // wenn nicht in einer Stringkonstante, dann überprüfen ob Leerzeichen kommt, wenn ja dann danach
        // nach " from " suchen
        int bracketCounter = 0;
        boolean inString = false;
        boolean prevCharIsSpace = false;
        boolean inWord = false;
        final StringBuffer sb = new StringBuffer();
        // ab diesem Index mit dem nachfolgenden Schritten fortfahren
        // finde erstes Leerzeichen (davor ist die Suche nicht sinnvoll)
        int p0 = sql.indexOf(' ');
        char c;
        while (p0 < sql.length()) {
            // nun Zeichen für Zeichen durchgehen und analysieren, was es denn sein könnte
            c = sql.charAt(p0);
            // Klammern innerhalb Strings sind nicht relevant
            if (c == '(') {
                // Klammern können mehrfach verschachtelt sein, deshalb die
                // Klammertiefe zählen
                bracketCounter++;
                inWord = false;
                prevCharIsSpace = false;
            } else if (c == ')') {
                bracketCounter--;
                inWord = false;
                prevCharIsSpace = false;
            } else if (Character.isSpaceChar(c)) {
                if (bracketCounter == 0) {
                    // nur ausserhalb einer Klammer ist das interessant
                    prevCharIsSpace = true;
                }
                // wenn zuvor ein Word gefunden wurde, dann nachsehen
                // ob das Word "from" ist
                if (inWord) {
                    if (sb.toString().equals("from")) {
                        // wir haben den Anfang von "from" gefunden
                        break;
                    }
                }
                inWord = false;
            } else if (c == '\'') {
                if (inString) {
                    inString = false;
                } else {
                    inString = true;
                }
                inWord = false;
                prevCharIsSpace = false;
            } else if (Character.isLetter(c)) {
                // ein Buchstabe gefunden
                // innerhalb von Strings keine Wirkung
                if (!inString) {
                    // innerhalb Klammern keine Wirkung
                    if (bracketCounter == 0) {
                        if (prevCharIsSpace) {
                            sb.setLength(0); // neues Wort also neu beginnen
                        }
                        if (prevCharIsSpace || inWord) {
                            // Wortanfang gefunden
                            inWord = true;
                            sb.append(c);
                        }
                    }
                }
                prevCharIsSpace = false;
            }
            p0++;
        }
        if (p0 != -1) {
            sql = sql.substring(p0, sql.length()).trim();
            if (sql.length() > 0) {
                // testen ob es sich um eine View handelt oder um eine table
                if (sql.charAt(0) == '(') {
                    // View gefunden, dann Ende der View finden
                    p0 = sql.indexOf(')') + 1;
                } else {
                    // Tabelle gefunden, dann das Ende finden
                    p0 = sql.indexOf(' '); // wird durch Leerzeichen abgeschlossen
                    if (p0 == -1) {
                        // keine Leerzeichen , dann nach Semikolon testen
                        p0 = sql.indexOf(';');
                        if (p0 == -1) {
                            // auch kein semikolon, dann Ende des Textes nutzen
                            p0 = sql.length();
                        }
                    }
                } // if (sql.charAt(0) == '(')
                if (p0 != -1) {
                    sql = sql.substring(0, p0);
                } else {
                    sql = null;
                }
            }
            return sql;
        } else {
            return null;
        }
    }

    /**
     * formatiert Quelltext 
     * @param sql unformatierter SQL-Quelltext
     * @return formatierter SQL-Quelltext
     */
    public static String formatSQL(String sql) {
        // alle Zeilenumbrüche entfernen
        final StringReplacer sr = new StringReplacer(sql.trim());
        sr.replace("\n", " ");
        sr.replace("\r", "");
        sr.replace("\t", " ");
        sql = sr.getResultText();
        // die verschiedenen Formatierungsmodi unterscheiden
        if (sql.toLowerCase().startsWith("select")) {
            return formatSelectStatement(sql);
        } else if (sql.toLowerCase().startsWith("update")) {
            return formatUpdateStatement(sql);
        } else if (sql.toLowerCase().startsWith("delete")) {
            return formatDeleteStatement(sql);
        } else {
            return sql;
        }
    }

    public static String formatSelectStatement(String sql) {
        final StringBuffer formattedSelect = new StringBuffer();
        // über das Wort select springen
        formattedSelect.append("select ");
        // nun den ersten Bestandteil der Feldliste finden
        final int pos = sql.indexOf(' ');
        if (pos != -1) {
            // den sinnlosen Fall abdecken, dass nur select dasteht
            // ab dem Anfang starten und Zeichen für Zeichen durchgehen
            // wird eine öffnende Klammer gefunden den zähler für Klammern um eines
            // erhöhen
            // wenn der zähler für Klammern auf 0 steht überprüfen, ob es sich um Stringkostanten handelt
            // wenn nicht in einer Stringkonstante, dann überprüfen ob Leerzeichen kommt, wenn ja dann danach
            // nach " from " suchen
            int bracketCounter = 0;
            boolean inString = false;
            boolean prevCharIsSpace = false;
            boolean inWord = false;
            final StringBuffer sb = new StringBuffer();
            // ab diesem Index mit dem nachfolgenden Schritten fortfahren
            // finde erstes Leerzeichen (davor ist die Suche nicht sinnvoll)
            int p0 = sql.indexOf(' ');
            char c;
            while (p0 < sql.length()) {
                // nun Zeichen für Zeichen durchgehen und analysieren, was es denn sein könnte
                c = sql.charAt(p0);
                // Klammern innerhalb Strings sind nicht relevant
                if (c == '(') {
                    // Klammern können mehrfach verschachtelt sein, deshalb die
                    // Klammertiefe zählen
                    bracketCounter++;
                    inWord = false;
                    prevCharIsSpace = false;
                } else if (c == ')') {
                    bracketCounter--;
                    inWord = false;
                    prevCharIsSpace = false;
                } else if (Character.isSpaceChar(c)) {
                    if (bracketCounter == 0) {
                        // nur ausserhalb einer Klammer ist das interessant
                        prevCharIsSpace = true;
                    }
                    // wenn zuvor ein Word gefunden wurde, dann nachsehen
                    // ob das Word "from" ist
                    if (inWord) {
                        if (sb.toString().equals("from")) {
                            // wir haben den Anfang von "from" gefunden
                            break;
                        }
                    }
                    inWord = false;
                } else if (c == '\'') {
                    if (inString) {
                        inString = false;
                    } else {
                        inString = true;
                    }
                    inWord = false;
                    prevCharIsSpace = false;
                } else if (Character.isLetter(c)) {
                    // ein Buchstabe gefunden
                    // innerhalb von Strings keine Wirkung
                    if (!inString) {
                        // innerhalb Klammern keine Wirkung
                        if (bracketCounter == 0) {
                            if (prevCharIsSpace) {
                                sb.setLength(0); // neues Wort also neu beginnen
                            }
                            if (prevCharIsSpace || inWord) {
                                // Wortanfang gefunden
                                inWord = true;
                                sb.append(c);
                            }
                        }
                    }
                    prevCharIsSpace = false;
                }
                p0++;
            }
            if (p0 != -1) {
                sql = sql.substring(p0, sql.length()).trim();
                if (sql.length() > 0) {
                    // testen ob es sich um eine View handelt oder um eine table
                    if (sql.charAt(0) == '(') {
                        // View gefunden, dann Ende der View finden
                        p0 = sql.indexOf(')') + 1;
                    } else {
                        // Tabelle gefunden, dann das Ende finden
                        p0 = sql.indexOf(' '); // wird durch Leerzeichen abgeschlossen
                        if (p0 == -1) {
                            // keine Leerzeichen , dann nach Semikolon testen
                            p0 = sql.indexOf(';');
                            if (p0 == -1) {
                                // auch kein semikolon, dann Ende des Textes nutzen
                                p0 = sql.length();
                            }
                        }
                    } // if (sql.charAt(0) == '(')
                    if (p0 != -1) {
                        sql = sql.substring(0, p0);
                    } else {
                        sql = null;
                    }
                }
                return sql;
            } else {
                return null;
            }
        }
        return formattedSelect.toString();
    }

    public static String formatUpdateStatement(String updateSQL) {
        final StringBuffer formattedUpdate = new StringBuffer(updateSQL);

        return formattedUpdate.toString();
    }

    public static String formatDeleteStatement(String deleteSQL) {
        final StringBuffer formattedDelete = new StringBuffer(deleteSQL);

        return formattedDelete.toString();
    }

    public static String removeAllComments(String sqlcode) {
        final StringBuffer sb = new StringBuffer();
        char c;
        char c1;
        int i = 0;
        boolean inStringConstant = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (i = 0; i < sqlcode.length(); i++) {
            c = sqlcode.charAt(i); // Zeichen lesen
            if (i < sqlcode.length() - 1) {
                c1 = sqlcode.charAt(i + 1);
            } else {
                c1 = ' ';
            }
            if (!inStringConstant && !inLineComment && !inBlockComment) {
                if (c == '\'') { // String-Konstante ??
                    inStringConstant = true; // es ist ein String-Beginn !
                    sb.append(c); // Zeichen merken
                } else if ((c == '/') && (c1 == '*')) { // Möglicher Block-Kommentarbeginn ?
                    // ok es ist ein Block-Kommentar !
                    inBlockComment = true;
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                } else if ((c == '-') && (c1 == '-')) { // Inline-Kommentar ?
                    // Inline-Kommentar gefunden !
                    inLineComment = true;
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                    // Line-Kommentare nicht in die SQL-Anweisung mit aufnehmen - Performance !
                } else { // nur normale Zeichen
                    sb.append(c); // merken
                }
            } else {
                // das Ende der zu überspringenden Textpassagen finden
                if (inLineComment) {
                    // Zeilenkommentare enden am Ende der Zeile
                    if (c == '\n') {
                        inLineComment = false;
                        sb.append(c);
                    }
                } else if (inBlockComment) {
                    if ((c == '*') && (c1 == '/')) {
                        inBlockComment = false;
                        i++; // Zeiger korrigieren
                    }
                } else if (inStringConstant) {
                    if (c == '\'') {
                        inStringConstant = false;
                    }
                    sb.append(c); // die Zeichen sind aber wichtig !
                }
            }
        } // for...
        return sb.toString();
    }

    /**
     * fügt in Kommentaren den Index des Parameters ein
     * direkt hinter das ? wird in Blockkommentar der Index des Parameter eingefügt
     * @param sql Code des prepared statement
     * @return kommentierter code des prepared statement
     */
    public static String commentPSParameter(String sqlcode) {
        final StringBuffer sb = new StringBuffer();
        char c;
        char c1;
        char c2;
        char c3;
        int i = 0;
        int paramIndex = 0;
        boolean inStringConstant = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inParamIndexNumber = false;
        for (i = 0; i < sqlcode.length(); i++) {
            c = sqlcode.charAt(i); // Zeichen lesen
            if (i < sqlcode.length() - 1) {
                c1 = sqlcode.charAt(i + 1);
            } else {
                c1 = ' ';
            }
            if (i < sqlcode.length() - 2) {
                c2 = sqlcode.charAt(i + 2);
            } else {
                c2 = ' ';
            }
            if (i < sqlcode.length() - 3) {
                c3 = sqlcode.charAt(i + 3);
            } else {
                c3 = ' ';
            }
            if (!inStringConstant && !inLineComment && !inBlockComment) {
                if (c == '\'') { // String-Konstante ??
                    inStringConstant = true; // es ist ein String-Beginn !
                    sb.append(c); // Zeichen merken
                } else if ((c == '/') && (c1 == '*')) { // Möglicher Block-Kommentarbeginn ?
                    // ok es ist ein Block-Kommentar !
                    inBlockComment = true;
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                    // Block-Kommentare in die SQL-Anweisung mit aufnehmen - Oracle-HINTS !
                    sb.append(c);
                    sb.append(c1);
                } else if ((c == '-') && (c1 == '-')) { // Inline-Kommentar ?
                    // Inline-Kommentar gefunden !
                    inLineComment = true;
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                    // Line-Kommentare nicht in die SQL-Anweisung mit aufnehmen - Performance !
                } else { // nur normale Zeichen
                    sb.append(c); // merken
                    if (c == '?') {
                        // feststellen ob sich direkt danach ein 
                        // Kommentar befindet und diesen ergänzen
                        if (c1 == '/' && c2 == '*') {
                            // wenn Zeichen vorkommt welches keine Ziffer darstellt, dann
                            // den Index schreiben
                            inBlockComment = true;
                            sb.append(c1);
                            sb.append(c2);
                            i = i + 2; // wir haben zwei weitere Zeichen analysiert !
                            if (c3 == '#') {
                                // dann ist ein normaler Kommentar enthalten der erhalten bleiben soll !
                                // er muss nur ergänzt werden um den ParameterIndex ! 
                                inParamIndexNumber = true;
                                sb.append(c3);
                                sb.append(++paramIndex); //  die neue Nummer eintragen
                                // die alten Nummern überspringen (weiter oben)
                                i++; // auch die Raute brauchen wir nicht mehr neu lesen in der Schleife!
                            } else {
                                sb.append('#');
                                sb.append(++paramIndex);
                                sb.append(' ');
                            }
                        } else {
                            sb.append("/*#");
                            sb.append(++paramIndex);
                            sb.append("*/");
                        }
                    }
                }
            } else {
                // das Ende der zu überspringenden Textpassagen finden
                if (inLineComment) {
                    // Zeilenkommentare enden am Ende der Zeile
                    if (c == '\n') {
                        inLineComment = false;
                        sb.append(c);
                    }
                } else if (inBlockComment) {
                    if ((c == '*') && (c1 == '/')) {
                        inBlockComment = false;
                        inParamIndexNumber = false;
                        i++; // Zeiger korrigieren
                        sb.append(c); // Blockkommentare unbedingt mit nehmen, da Oracle dort HINTS versteckt !
                        sb.append(c1);
                    } else {
                        if (inParamIndexNumber) {
                            if (c >= '0' && c <= '9') {
                                // nichts übernehmen !
                            } else {
                                sb.append(c);
                                inParamIndexNumber = false;
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                } else if (inStringConstant) {
                    if (c == '\'') {
                        inStringConstant = false;
                    }
                    sb.append(c); // die Zeichen sind aber wichtig !
                }
            }
        } // for...
        return sb.toString();
    }

    /**
     * konfiguriert die Parameter eines prepared statements
     * @param ps
     */
    public static void setupPreparedStatementParams(SQLStatement ps) {
        SQLPSParam param = null;
        char c0 = ' ';
        char c;
        char c1;
        int lastParamPos = 0;
        int i = 0;
        int paramIndex = 0;
        boolean inStringConstant = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inParamComment = false;
        boolean inParamName = false;
        boolean hasNamedParameter = false;
        StringBuffer paramName = new StringBuffer();
        final String sqlcode = ps.getSQL();
        for (i = 0; i < sqlcode.length(); i++) {
            c = sqlcode.charAt(i); // Zeichen lesen
            if (i < sqlcode.length() - 1) {
                c1 = sqlcode.charAt(i + 1);
            } else {
                c1 = ' ';
            }
            if (!inStringConstant && !inLineComment && !inBlockComment) {
                if (c == '\'') { // String-Konstante ??
                    inStringConstant = true; // es ist ein String-Beginn !
                    inParamName = false;
                } else if ((c == '/') && (c1 == '*')) { // Möglicher Block-Kommentarbeginn ?
                    // ok es ist ein Block-Kommentar !
                    inBlockComment = true;
                    if ((lastParamPos == i - 1) && inParamName == false) {
                        inParamComment = true;
                    }
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                    paramName = new StringBuffer();
                    inParamName = false;
                } else if ((c == '-') && (c1 == '-')) { // Inline-Kommentar ?
                    // Inline-Kommentar gefunden !
                    inLineComment = true;
                    i++; // Zeiger ein weiter da zwei Zeichen analysiert wurden
                    // Line-Kommentare nicht in die SQL-Anweisung mit aufnehmen - Performance !
                    inParamName = false;
                } else if (inParamName) {
                	if (Character.isWhitespace(c) || c == ' ' || c == ',') {
                        param.setName(paramName.toString());
                        inParamName = false;
                	} else {
                    	paramName.append(c);
                	}
                } else { // nur normale Zeichen
                    if (c == '?') {
                        // Parameter gefunden
                        param = new SQLPSParam();
                        param.setIndex(++paramIndex);
                        ps.addParam(param);
                        lastParamPos = i;
                        inParamName = false;
                    } else if (c0 != ':' && c == ':' && Character.isLetter(c1)) {
                        param = new SQLPSParam();
                        param.setIndex(++paramIndex);
                        param.setNamedParam(true);
                        ps.addParam(param);
                        lastParamPos = i;
                        inParamName = true;
                        paramName = new StringBuffer();
                        hasNamedParameter = true;
                    }
                }
            } else {
                // das Ende der zu überspringenden Textpassagen finden
                if (inLineComment) {
                    // Zeilenkommentare enden am Ende der Zeile
                    if (c == '\n') {
                        inLineComment = false;
                    }
                } else if (inBlockComment) {
                    if ((c == '*') && (c1 == '/')) {
                        inBlockComment = false;
                        i++; // Zeiger korrigieren
                        if (inParamComment && param != null) {
                            param.setName(paramName.toString());
                        }
                        inParamComment = false;
                    } else {
                        // testen ob der Kommentar direkt nach dem Parameter kommt
                        // wenn ja, dann den Text des Kommentares als Name des Parameters
                        // interpretieren
                        if (inParamComment) {
                            paramName.append(c);
                        }
                    }
                } else if (inStringConstant) {
                    if (c == '\'') {
                        inStringConstant = false;
                    }
                }
            }
            c0 = c;
        }
        if (inParamName) {
        	param.setName(paramName.toString());
        }
        if (hasNamedParameter) {
    		String sql = ps.getSQL();
    		StringReplacer sr = new StringReplacer(sql);
    		for (SQLPSParam p : ps.getParams()) {
    			if (p.isNamedParam()) {
    				sr.replace(":" + p.getName(), "?");
    			}
    		}
    		ps.setSQL(sr.getResultText());
        }
    }

    public static String removePSComments(String sql) {
        final StringBuffer sb = new StringBuffer();
        char c;
        char c1;
        int currPos = 0;
        boolean inBlockComment = false;
        for (currPos = 0; currPos < sql.length(); currPos++) {
            c = sql.charAt(currPos);
            if (inBlockComment) {
                // das Ende finden
                if (c == '*') {
                    if (currPos < sql.length() - 1) {
                        c1 = sql.charAt(currPos + 1);
                        if (c1 == '/') {
                            inBlockComment = false;
                            currPos++;
                        }
                    }
                }
            } else {
                sb.append(c);
                if (c == '?') {
                    if (currPos < sql.length() - 5) {
                        // nachfolgend kann noch ein Kommentar kommen
                        c1 = sql.charAt(currPos + 1);
                        if (c1 == '/') {
                            c1 = sql.charAt(currPos + 2);
                            if (c1 == '*') {
                                inBlockComment = true;
                            }
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String extractFileName(String sqlStartCommand, String baseDir) {
        // das SQL script beginnt mit "start" und anschliessend mit beliebig vielen Leerzeichen dazwischen kommt
        // der Dateiname
        String fileName = null;
        if (sqlStartCommand.startsWith("start")) {
        	fileName = sqlStartCommand.substring("start".length() + 1);
        } else if (sqlStartCommand.startsWith("@")) {
        	fileName = sqlStartCommand.substring("@".length() + 1);
        } else {
        	throw new IllegalArgumentException(sqlStartCommand + " is not a start command");
        }
        if (fileName != null) {
            fileName = fileName.trim();
        }
        boolean absoluteFileName = false;
        // ist der Dateiname ein relativer Name ?
        if (fileName.startsWith("/") || fileName.startsWith("\\")) {
            absoluteFileName = true;
        }
        if (fileName.indexOf(":\\") != -1 || fileName.indexOf(":/") != -1) {
            // fileName ist absolut
            absoluteFileName = true;
        }
        String fileSeparator = System.getProperty("file.separator");
        if (absoluteFileName == false) {
            if (baseDir.endsWith("/") || baseDir.endsWith("\\")) {
                fileName = baseDir + fileName;
            } else {
                fileName = baseDir + fileSeparator + fileName;
            }
        }
        if (fileName.toLowerCase().endsWith(".sql") == false) {
            fileName = fileName + ".sql";
        }
        return fileName;
    }
    
    public static String readSQLFile(File f, String charset) throws IOException {
        final char CR = 0x000D;
        final char LF = 0x000A;
        final char LSEP = 0x2028;
        final char PSEP = 0x2029;
        final char NL = 0x0085;
        BufferedReader in = null;
        final char[] buffer = new char[8096];
        StringBuffer sb = new StringBuffer(buffer.length);
        try {
            if (charset != null) {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
            } else {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            }
            int i;
            int nch;
            char c;
            char c0 = ' ';
            while ((nch = in.read(buffer, 0, buffer.length)) != -1) {
                for (i = 0; i < nch; i++) {
                    // create UNIX-Fileformat
                    c = buffer[i];
                    if ((c == LSEP) || (c == PSEP) || (c == NL)) {
                        buffer[i] = '\n';
                    } else if (c == LF) {
                        if (c0 == CR) {
                        	// replace Windows line feed
                            buffer[i] = 0x0000;
                        } else {
                            buffer[i] = '\n';
                        }
                    } else if (c == CR) {
                        buffer[i] = '\n';
                    }
                    c0 = c;
                }
                copy(sb, buffer, 0, nch);
            }
        } finally {
        	if (in != null) {
        		try {
        			in.close();
        		} catch (Exception e) {}
        	}
        }
        return sb.toString();
    }

    private static void copy(StringBuffer sb, char[] buffer, int startPos, int length) {
        char c;
        for (int i = startPos; i < (startPos + length); i++) {
            c = buffer[i];
            if (c > 0x0000) {
                sb.append(c);
            }
        }
    }

	public boolean isIncludeComments() {
		return includeComments;
	}

	public void setIncludeComments(boolean includeComments) {
		this.includeComments = includeComments;
	}

	public void setUseScriptDetecting(boolean useScriptDetecting) {
		this.useScriptDetecting = useScriptDetecting;
	}

	public boolean isDetectBindVariables() {
		return detectBindVariables;
	}

	public void setDetectBindVariables(boolean detectBindVariables) {
		this.detectBindVariables = detectBindVariables;
	}

}
