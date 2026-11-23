package id.neotica.orpheum.source.domain;

import java.util.List;

public class Feed<T> {

    public final List<T> items;
    public final int page;
    public final boolean hasMore;

    public Feed(List<T> items, int page, boolean hasMore) {
        this.items = items;
        this.page = page;
        this.hasMore = hasMore;
    }
}
