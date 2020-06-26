package com.nedap.openehr.lsp.utils;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Positions;

public class RangeUtils {
    public static boolean rangesOverlap(Range range1, Range range2) {
        Position maxStart = max(range1.getStart(), range2.getStart());
        Position minEnd = min(range1.getEnd(),range2.getEnd());
        return Positions.isBefore(maxStart,minEnd) || maxStart.equals(minEnd);//TODO: or equals!!!!
    }

    public static Position max(Position start1, Position start2) {
        if(Positions.isBefore(start1, start2)) {
            return start2;
        }
        return start1;
    }

    public static Position min(Position start1, Position start2) {
        if(Positions.isBefore(start1, start2)) {
            return start1;
        }
        return start2;
    }
}
