package com.acabra.jwebcrawler.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class CrawledNodeTest {

    @Test
    public void should_be_different_by_url() {
        MatcherAssert.assertThat(new CrawledNode("", 5), Matchers.not(new CrawledNode("otherPage", 5)));

    }

    @Test
    public void should_be_different_by_id() {
        MatcherAssert.assertThat(new CrawledNode("", 5), Matchers.not(new CrawledNode("", 1)));

    }

    @Test
    public void should_be_equal_as_is_the_same_node() {
        MatcherAssert.assertThat(new CrawledNode("otherPage", 3),
                Matchers.is(new CrawledNode("otherPage", 3L, 1, 5)));
    }

    @Test
    public void should_be_different_as_not_same_class() {
        MatcherAssert.assertThat(new CrawledNode("otherPage", 3),
                Matchers.not(Integer.valueOf(1)));
    }

    @Test
    public void should_be_equal_as_redirection_same_id_url() {
        CrawledNode otherPage = new CrawledNode("otherPage", 3);
        CrawledNode redirection = otherPage.redirection("otherPage", otherPage.id);
        MatcherAssert.assertThat(otherPage, Matchers.is(redirection));
        MatcherAssert.assertThat(otherPage.hashCode(), Matchers.is(redirection.hashCode()));
    }

    @Test
    public void should_be_different_child_as_different_id() {
        CrawledNode otherPage = new CrawledNode("otherPage", 3);
        MatcherAssert.assertThat(otherPage,
                Matchers.not(otherPage.buildChild("otherPage", otherPage.id+1)));
    }

    @Test
    public void should_be_different_child_as_different_url() {
        CrawledNode otherPage = new CrawledNode("otherPage", 3);
        MatcherAssert.assertThat(otherPage,
                Matchers.not(otherPage.buildChild("different", otherPage.id)));
    }

    @Test
    public void should_be_equal_same_object() {
        CrawledNode otherPage = new CrawledNode("otherPage", 3);
        MatcherAssert.assertThat(otherPage,
                Matchers.is(otherPage));
    }

    @Test
    public void should_be_different_than_null() {
        CrawledNode other = null;
        MatcherAssert.assertThat(new CrawledNode("", 0L), Matchers.not(other));
    }

    @Test
    public void should_match_ids() {
        CrawledNode other = new CrawledNode("", 157L);
        MatcherAssert.assertThat(other.id, Matchers.is(other.buildChild("", 5L).parentId));
    }

}