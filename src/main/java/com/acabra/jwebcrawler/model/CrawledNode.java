package com.acabra.jwebcrawler.model;


import java.util.Objects;

public class CrawledNode {
    public static final long ROOT_NODE_PARENT_ID = -1L;

    public final String url;
    public final int level;
    public final long id;
    public final long parentId;

    public CrawledNode(String url, long id) {
        this.url = url;
        this.id = id;
        this.level = 0;
        this.parentId = ROOT_NODE_PARENT_ID;
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
        return id == that.id && url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url);
    }
}
