package com.fingy.aprod.scrape;

import java.io.IOException;
import java.util.Map;

import org.jsoup.nodes.Document;

import com.fingy.scrape.jsoup.AbstractJsoupScraper;
import com.fingy.scrape.jsoup.HttpClientParserUtil;
import com.fingy.scrape.queue.ScraperLinksQueue;

public abstract class AbstractAprodJsoupScraper<T> extends AbstractJsoupScraper<T> {

	protected ScraperLinksQueue linksQueue;

	public AbstractAprodJsoupScraper(Map<String, String> cookies, String scrapeUrl, ScraperLinksQueue linksQueue) {
		super(cookies, scrapeUrl);
		this.linksQueue = linksQueue;
	}

	@Override
	protected Document getPage(String scrapeUrl) throws IOException {
		try {
			return HttpClientParserUtil.getPageFromUrl(scrapeUrl);
		} catch (IOException e) {
			AbstractAprodJsoupScraper.setSessionExpired(true);
			throw e;
		}
	}

}