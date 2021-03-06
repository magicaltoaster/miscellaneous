package com.fingy.scrape.jsoup;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.jsoup.nodes.Document;

import com.fingy.scrape.AbstractScraper;
import com.fingy.scrape.exception.ScrapeException;
import com.fingy.scrape.util.JsoupParserUtil;

public abstract class AbstractJsoupScraper<T> extends AbstractScraper<T> {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0";

    private Map<String, String> cookies;

    public AbstractJsoupScraper(final String scrapeUrl) {
        this(Collections.<String, String> emptyMap(), scrapeUrl);
    }

    public AbstractJsoupScraper(final Map<String, String> cookies, final String scrapeUrl) {
        super(scrapeUrl);
        this.setCookies(cookies);
    }

    protected abstract T scrapePage(Document page);

    @Override
    protected T scrapeLink() {
        if (isScrapeCompromised()) {
            throw new ScrapeException("Scrape compromised");
        }
        try {
            final Document page = getPage();
            return scrapePage(page);
        } catch (Exception e) {
            processException(e);
            throw new ScrapeException("Exception parsing link " + getScrapeUrl(), e);
        }
    }

    protected void processException(final Exception e) {
        // do nothing
    }

    protected Document getPage() throws IOException {
        return JsoupParserUtil.getPageFromUrlWithCookies(getScrapeUrl(), getCookies());
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public void setCookies(final Map<String, String> cookies) {
        this.cookies = cookies;
    }

}
