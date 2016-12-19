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

/**
 * Klasse beschreibt eine Element einer URL wie z.B. host oder SID
 * jedes Element hat einen Namen und einen Wert
 */
public class URLElement implements Serializable {

    private static final long serialVersionUID = 1L;
    private String name;
    private String value;
    private boolean valid         = true;
    public static final String PASSWORD_NAME = "PASSWORD";
    public static final String USER_NAME     = "USER";
    public static final String COMMENT       = "COMMENT";
    public static final String PROPERTIES    = "PROPERTIES";
    public static final String INIT_SQL      = "INIT_SQL";
    public static final String FETCHSIZE     = "FETCHSIZE";
    public static final String PRODUCTIVE    = "PRODUCTIVE";
    public static final String URL           = "URL";
    public static final String DRIVER_CLASS  = "DRIVER_CLASS";

    public URLElement() {}

    /**
     * Konstruktor
     * @param name - Name des Elements
     * @param value - Wert des Elements
     */
    public URLElement(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public URLElement(URLElement element) {
        this.name = element.getName();
        this.value = element.getValue();
    }

    public URLElement(String param) {
        parseParamStr(param);
    }

    public String getParamStr() {
        if (value == null) {
            return name + "=";
        } else {
            return name + "=" + value;
        }
    }

    /**
     * bringt den Namen des Elements
     * @return Name des Elements
     */
    public String getName() {
        return name;
    }

    /**
     * setzt den Namen des Elements
     * @param name - Name des Elements
     */
    public void setName(String name_loc) {
        this.name = name_loc;
    }

    /**
     * Wert des Elements
     * @return Wert des Elements
     */
    public String getValue() {
        return value;
    }

    /**
     * setzt den Elementewert
     * @param value - Wert des Elements
     */
    public void setValue(String value_loc) {
        this.value = value_loc;
    }

    /**
     * wenn Elementename fehlt dann ist es nicht gültig
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }
    
    public boolean isUserNameElement() {
        return USER_NAME.equalsIgnoreCase(name);
    }
    
    private void parseParamStr(String param) {
        final int pos = param.indexOf('=');
        if (pos != -1) {
            name = (param.substring(0, pos)).trim();
            value = (param.substring(pos + 1, param.length())).trim();
        } else {
            valid = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof URLElement) {
            return name.equals(((URLElement) o).getName());
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * String-Repräsentation des Elements
     * @return text
     */
    @Override
    public String toString() {
        return "URLElement: name=" + name + ", value=" + value;
    }

}
