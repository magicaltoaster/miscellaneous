package com.fingy.aprod.scrape;

import java.util.Collections;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.fingy.scrape.queue.ScraperLinksQueue;

public class FirstAdPageJsoupScraper extends AbstractAprodJsoupScraper<String> {

	public FirstAdPageJsoupScraper(String scrapeUrl, ScraperLinksQueue linksQueue) {
		super(Collections.<String, String> emptyMap(), scrapeUrl, linksQueue);
	}

	public FirstAdPageJsoupScraper(Map<String, String> cookies, String scrapeUrl, ScraperLinksQueue linksQueue) {
		super(cookies, scrapeUrl, linksQueue);
	}

	@Override
	protected String scrapePage(Document page) {
		String scrapeUrl = getScrapeUrl();

		linksQueue.addIfNotVisited(scrapeUrl);
		int lastPageNumber = getLastPageNumber(page);
		for(int i = 2; i <= lastPageNumber; i++) {
			String pageLink = scrapeUrl + (scrapeUrl.contains("?") ? "&" : "?") + "page=" + i;
			linksQueue.addIfNotVisited(pageLink);
		}
		
		return scrapeUrl;
	}

	private Integer getLastPageNumber(Document page) {
		String cssQuery = "div.pager span.item a";
		Element lastPageNumber = page.select(cssQuery).last();
		return Integer.parseInt(lastPageNumber.text());
	}

}