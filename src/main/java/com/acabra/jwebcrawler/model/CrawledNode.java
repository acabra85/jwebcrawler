package com.acabra.jwebcrawler.model;


import java.util.Objects;

public class CrawledNode {
    public final String url;
    public final int level;
    public final long id;
    public final long parentId;

    public CrawledNode(String url, long id) {
        this.url = url;
        this.id = id;
        this.level = 0;
        this.parentId = -1L;
    }

    public CrawledNode(String url, long id, int level, long parentId) {
        this.url = url;
        this.level = level;
        this.id = id;
        this.parentId = parentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawledNode that = (CrawledNode) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
