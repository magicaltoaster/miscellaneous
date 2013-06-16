package com.fingy.adultwholesale;

import static com.fingy.scrape.jsoup.AbstractJsoupScraper.USER_AGENT;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.fingy.adultwholesale.scrape.AdultItemCategoryJsoupScraper;
import com.fingy.adultwholesale.scrape.AdultItemJsoupScraper;
import com.fingy.concurrent.ExecutorsUtil;
import com.fingy.scrape.JsoupImageDownloader;
import com.fingy.scrape.queue.ScraperLinksQueue;

public class AdultWholesaleScraperScheduler {

	private static final String[] LOGIN_CREDENTIALS = new String[] { "username", "47198", "pass", "hithlum" };
	private static final String LOGIN_PAGE = "https://www.adultwholesaledirect.com/login.php";
	private static final String STARTING_URL = "https://www.adultwholesaledirect.com/customer/bulk/ajax_getbulkcategories.php";
	private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

	private ExecutorService categoryScrapingThreadPool;
	private ExecutorService imageScrapingThreadPool;
	private ExecutorService itemScrapintThreadPool;
	private ExecutorCompletionService<AdultItem> itemScrapingCompletionService;
	private ScraperLinksQueue linksQueue;
	private Map<String, String> cookies;
	private ArrayList<AdultItem> items;
	private AtomicInteger scrapedProductsCount;

	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException,
			ExecutionException {
		new AdultWholesaleScraperScheduler().doScrape();
	}

	public AdultWholesaleScraperScheduler() {
		categoryScrapingThreadPool = createDefaultThreadPool();
		itemScrapintThreadPool = createDefaultThreadPool();// Executors.newCachedThreadPool();
		itemScrapingCompletionService = new ExecutorCompletionService<>(itemScrapintThreadPool);
		imageScrapingThreadPool = createDefaultThreadPool();
		linksQueue = new ScraperLinksQueue();
		scrapedProductsCount = new AtomicInteger();
		items = new ArrayList<>();
	}

	public void doScrape() throws ExecutionException, IOException {
		System.getProperties().setProperty("socksProxyHost", "127.0.0.1");
		System.getProperties().setProperty("socksProxyPort", "9150");

		doLogin();

		processEntryPage();
		submitScrapingTasksWhileThereIsEnoughWork();

		System.out.println("Awaiting termination of the services");

		ExecutorsUtil.shutDownExecutorServiceAndAwaitTermination(categoryScrapingThreadPool, 10, TimeUnit.SECONDS);
		ExecutorsUtil.shutDownExecutorServiceAndAwaitTermination(itemScrapintThreadPool, 10, TimeUnit.SECONDS);

		saveResultsToExcelAndDownloadImages();
	}

	private void doLogin() throws IOException {
		final Response response = Jsoup.connect(LOGIN_PAGE).data(LOGIN_CREDENTIALS).method(Method.POST)
				.userAgent(USER_AGENT).execute();
		cookies = response.cookies();
	}

	private void processEntryPage() throws ExecutionException {
		try {
			Callable<AdultItem> entryPageTask = new AdultItemCategoryJsoupScraper(cookies, STARTING_URL, linksQueue);
			categoryScrapingThreadPool.submit(entryPageTask).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void submitScrapingTasksWhileThereIsEnoughWork() {
		scrapedProductsCount.set(0);
		while (stillHaveLinksToBeScraped()) {
			String link;
			try {
				link = linksQueue.take();

				if (isItemDescriptionPage(link)) {
					submitItemScrapingTask(link);
					scrapedProductsCount.incrementAndGet();
				} else {
					submitCategoryScrapingTask(link);
				}

				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
	}

	private boolean stillHaveLinksToBeScraped() {
		return !linksQueue.delayedIsEmpty(15000);
	}

	private void saveResultsToExcelAndDownloadImages() throws FileNotFoundException, IOException {
		if (scrapedProductsCount.get() > 0) {
			items.clear();
			for (int i = 0; i < scrapedProductsCount.get(); i++) {
				try {
					final Future<AdultItem> future = itemScrapingCompletionService.poll(10, TimeUnit.SECONDS);
					final AdultItem item = future.get();

					items.add(item);
					submitImageDownloadingTaskForItem(item);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			createExcelSheetFromScrapedItems(items);

			ExecutorService executorService = imageScrapingThreadPool;
			ExecutorsUtil.shutDownExecutorServiceAndAwaitTermination(executorService, 5, TimeUnit.MINUTES);
		}
	}

	private void submitImageDownloadingTaskForItem(final AdultItem item) {
		String fileName = "C:/Users/Fingy/Desktop/wholesale/" + item.getId();
		imageScrapingThreadPool.execute(new JsoupImageDownloader(item.getImageUrl(), fileName, cookies));
	}

	private void createExcelSheetFromScrapedItems(final List<AdultItem> items) throws FileNotFoundException,
			IOException {
		new AdultItemToExcelBuilder().buildExcel(items).writeToFile(
			"C:/Users/Fingy/Desktop/wholesale" + System.currentTimeMillis() + ".xlsx");
	}

	private void submitCategoryScrapingTask(final String link) {
		try {
			categoryScrapingThreadPool.submit(new AdultItemCategoryJsoupScraper(cookies, link, linksQueue));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void submitItemScrapingTask(final String link) {
		try {
			itemScrapingCompletionService.submit(new AdultItemJsoupScraper(cookies, link, linksQueue));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ExecutorService createDefaultThreadPool() {
		return new ThreadPoolExecutor(AVAILABLE_PROCESSORS * 4, Integer.MAX_VALUE, 1, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>());
	}

	private boolean isItemDescriptionPage(String href) {
		return href.contains("ajax_getbulkproductdetails");
	}
}