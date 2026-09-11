package net.godlycow.org.essc.util;

import java.util.Collections;
import java.util.List;

public final class PaginatedList<T> {

    private final List<T> items;
    private final int perPage;
    private final int totalPages;

    public PaginatedList(List<T> items, int perPage) {
        if (perPage < 1) throw new IllegalArgumentException("perPage must be >= 1");
        this.items      = Collections.unmodifiableList(items);
        this.perPage    = perPage;
        this.totalPages = items.isEmpty() ? 1 : (int) Math.ceil(items.size() / (double) perPage);
    }

    public List<T> getPage(int page) {
        if (!isValidPage(page)) return Collections.emptyList();
        int start = (page - 1) * perPage;
        int end   = Math.min(start + perPage, items.size());
        return items.subList(start, end);
    }

    public int startIndex(int page) {
        return (page - 1) * perPage + 1;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalItems() {
        return items.size();
    }

    public int getPerPage() {
        return perPage;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isValidPage(int page) {
        return page >= 1 && page <= totalPages;
    }

    public boolean hasNextPage(int page) {
        return page < totalPages;
    }

    public boolean hasPreviousPage(int page) {
        return page > 1;
    }

    public int clamp(int page) {
        return Math.max(1, Math.min(page, totalPages));
    }
}