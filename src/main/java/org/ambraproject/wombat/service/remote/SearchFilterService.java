package org.ambraproject.wombat.service.remote;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for performing faceted search on different fields used for filtering on the search result
 */
public class SearchFilterService {

  @Autowired
  private SearchService searchService;

  private Map<String, Map<?, ?>> filters;

  private final String JOURNAL_FACET_FIELD = "cross_published_journal_name";

  public Map<?,?> getSimpleSearchFilters(String query, List<String> journalKeys, List<String> articleTypes,
      SearchService.SearchCriterion dateRange) throws IOException {
    filters = new HashMap<>();
    filters.put("journal", searchService.simpleSearch(JOURNAL_FACET_FIELD, query, new ArrayList<String>(), articleTypes,
        dateRange));
    // TODO: add other filters here
    return filters;
  }

  public Map<?, ?> getAdvancedSearchFilers(String query, List<String> journalKeys,
      List<String> articleTypes, List<String> subjectList, SearchService.SearchCriterion dateRange) throws
      IOException {
    filters = new HashMap<>();
    filters.put("journal", searchService.advancedSearch(JOURNAL_FACET_FIELD, query, new ArrayList<String>(), articleTypes,
        subjectList, dateRange));
    // TODO: add other filters here
    return filters;
  }

  public Map<?, ?> getSubjectSearchFilters(List<String> subjects, List<String> journalKeys,
      List<String> articleTypes, SearchService.SearchCriterion dateRange) throws IOException {
    filters = new HashMap<>();
    filters.put("journal", searchService.subjectSearch(JOURNAL_FACET_FIELD, subjects, new ArrayList<String>(), articleTypes,
        dateRange));
    // TODO: add other filters here
    return filters;
  }

  public Map<?, ?> getAuthorSearchFilters(String author, List<String> journalKeys,
      List<String> articleTypes, SearchService.SearchCriterion dateRange) throws IOException {
    filters = new HashMap<>();
    filters.put("journal", searchService.authorSearch(JOURNAL_FACET_FIELD, author, new ArrayList<String>(), articleTypes,
        dateRange));
    // TODO: add other filters here
    return filters;
  }

  public Map<?, ?> getVolumeSearchFilters(int volume, List<String> journalKeys, List<String> articleTypes,
      SearchService.SearchCriterion dateRange) throws IOException {
    filters = new HashMap<>();
    // TODO: add other filters here (filter by journal is not applicable here)
    return filters;
  }
}