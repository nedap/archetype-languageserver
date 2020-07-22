package com.nedap.openehr.lsp.document;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * index that finds a matching code range for a given position. The set of code ranges must be non-overlapping, but can contain holes
 *
 * Uses a TreeMap to scan ranges
 *
 * @param <T>
 */
public class CodeRangeIndex<T> {

    private Comparator positionComparator = Comparator.comparingInt(Position::getLine)
                    .thenComparing(Comparator.nullsFirst(Comparator.comparingInt(Position::getCharacter)));

    private TreeMap<Position, RangeWithContent<T>> index = new TreeMap<>(positionComparator);

    public T getFromCodeRange(Position position) {
        Map.Entry<Position, RangeWithContent<T>> entry = index.floorEntry(position);

        if (entry == null) {
            // too small
            return null;
        } else if (positionComparator.compare(position, entry.getValue().getStop()) <= 0) {
            return entry.getValue().getContent();
        } else {
            // too large or in a hole
            return null;
        }
    }

    public T getFirstMatchAfter(Position position, Predicate<T> filter) {
        Map.Entry<Position, RangeWithContent<T>> entry = index.ceilingEntry(position);
        while(entry != null && !filter.test(entry.getValue().getContent())) {
            entry = index.higherEntry(entry.getKey());
        }
        if(entry == null) {
            return null;
        }
        return entry.getValue().getContent();
    }

    public void addRange(Range range, T content) {
        addRange(range.getStart(), range.getEnd(), content);
    }

    public void addRange(Position start, Position stop, T content) {
        index.put(start, new RangeWithContent<>(start, stop, content));
    }

    public List<T> values() {
        return index.values().stream().map(v -> v.getContent()).collect(Collectors.toList());
    }

    private static class RangeWithContent<T> {
        Position start;
        Position stop;
        T content;

        public RangeWithContent(Position start, Position stop, T content) {
            this.start = start;
            this.stop = stop;
            this.content = content;
        }

        public Position getStart() {
            return start;
        }

        public Position getStop() {
            return stop;
        }

        public T getContent() {
            return content;
        }
    }
}
