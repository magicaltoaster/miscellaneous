package com.fingy.micromedex.web.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fingy.micromedex.util.HtmlUnitUtil;
import com.fingy.scrape.util.JsoupParserUtil;
import com.gargoylesoftware.htmlunit.WebClient;

@Controller
public class MethodTwoController extends AbstractMicromedexController {

	private static final String INTERACTIONS_SEARCH_URL = "http://www.micromedexsolutions.com/micromedex2/librarian/PFDefaultActionId/evidencexpert.ShowDrugInteractionsResults";
	private static final String INTERACTIONS_QUERY_PAGE_URL = "http://www.micromedexsolutions.com/micromedex2/librarian/ND_T/evidencexpert/ND_PR/"
			+ "evidencexpert/CS/5C9FDD/ND_AppProduct/evidencexpert/DUPLICATIONSHIELDSYNC/53CD75/ND_PG/evidencexpert/ND_B/evidencexpert/ND_P/"
			+ "evidencexpert/PFActionId/evidencexpert.FindDrugInteractions";

	@ResponseBody
	@RequestMapping("/interactions")
	public Object interactions(@RequestParam(value = "drugs", required = true) final String drugsToSearch,
			@RequestParam(value = "allergies", required = true) final String allergiesToSearch) throws IOException {

		WebClient webClient = getWebClientHolder().get();

		Document page = Jsoup.connect(INTERACTIONS_QUERY_PAGE_URL).cookies(HtmlUnitUtil.getCookiesAsMap(webClient)).timeout(0).get();

		Map<String, String> params = new HashMap<>();
		Element form = page.getElementById("ivCompatibilityLookUp");
		Elements formParams = form.getElementsByTag("input");

		for (Element formParam : formParams) {
			String name = formParam.attr("name");
			String value = formParam.attr("value");

			if (StringUtils.isNotBlank(name)) {
				params.put(name, value);
			}
		}

		List<String[]> drugs = new AbstractDataSearcher() {
			@Override
			public String[][] getData(String searchTerm) throws IOException {
				return getDrugsWithNameLike(searchTerm);
			}
		}.doSearch(drugsToSearch);

		List<String[]> allergies = new AbstractDataSearcher() {
			@Override
			public String[][] getData(String searchTerm) throws IOException {
				return getAllergiesWithNameLike(searchTerm);
			}
		}.doSearch(allergiesToSearch);

		ObjectMapper mapper = new ObjectMapper();

		if (!drugs.isEmpty()) {
			params.put("selectedDrugs", mapper.writeValueAsString(drugs).replace("\"", "\'"));
			String[] lastDrug = drugs.get(drugs.size() - 1);
			params.put("WordWheel_ContentSetId_index_0", lastDrug[3]);
			params.put("WordWheel_ItemId_index_0", lastDrug[0]);
			params.put("WordWheel_MainSelected_index_0", lastDrug[1]);
		}

		if (!allergies.isEmpty()) {
			params.put("selectedAllergies_index_0", mapper.writeValueAsString(allergies).replace("\"", "\'"));
		}

		return doSearch(webClient, params);

	}

	private String doSearch(WebClient webClient, Map<String, String> params) throws IOException {
		return JsoupParserUtil.postDataToUrlWithCookies(INTERACTIONS_SEARCH_URL, HtmlUnitUtil.getCookiesAsMap(webClient), params).getElementById("resultsTable").html();
	}

	private abstract class AbstractDataSearcher {
		public List<String[]> doSearch(final String commaSeparatedTermsToSearch) throws IOException {
			List<String[]> searchResults = new ArrayList<>();

			StringTokenizer searchTermTokenizer = new StringTokenizer(commaSeparatedTermsToSearch, ",");
			while (searchTermTokenizer.hasMoreTokens()) {
				String term = searchTermTokenizer.nextToken();
				String[][] foundData = getData(term);

				for (String[] datum : foundData) {
					if (datum[NAME_INDEX].equalsIgnoreCase(term)) {
						reorderDataInExpectedFormat(searchResults, datum);
					}
				}
			}

			return searchResults;
		}

		private void reorderDataInExpectedFormat(List<String[]> searchResults, String[] datum) {
			searchResults.add(new String[] { datum[1], datum[0], datum[2], datum[3] });
		}

		public abstract String[][] getData(String searchTerm) throws JsonParseException, JsonMappingException, MalformedURLException, IOException;
	}
}
