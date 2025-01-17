/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.xwpf.usermodel;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Sets alignment values allowed for Tables and Table Rows
 */
public enum TableRowAlign {
    
    LEFT(STJcTable.INT_START),
    CENTER(STJcTable.INT_CENTER),
    RIGHT(STJcTable.INT_END);

    private static final Map<Integer, TableRowAlign> imap;

    static {
        final Map<Integer, TableRowAlign> tempMap = new HashMap<>();
        for (TableRowAlign p : values()) {
            tempMap.put(p.getValue(), p);
        }
        imap = Collections.unmodifiableMap(tempMap);
    }

    private final int value;

    private TableRowAlign(int val) {
        value = val;
    }

    public static TableRowAlign valueOf(int type) {
        TableRowAlign err = imap.get(type);
        if (err == null) throw new IllegalArgumentException("Unknown table row alignment: " + type);
        return err;
    }

    public int getValue() {
        return value;
    }
}
