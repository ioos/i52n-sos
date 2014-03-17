package org.n52.sos.web;

import org.joda.time.Minutes;
import org.joda.time.ReadableInstant;

public class IoosJstlFunctions {
    public static Integer minutesBetween(ReadableInstant ri1, ReadableInstant ri2) {
        if (ri1 == null || ri2 == null){
            return null;
        }
        return Minutes.minutesBetween(ri1, ri2).getMinutes();
    }
}
