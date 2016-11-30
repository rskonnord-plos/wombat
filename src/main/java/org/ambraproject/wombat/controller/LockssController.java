package org.ambraproject.wombat.controller;

import org.ambraproject.wombat.config.site.Site;
import org.ambraproject.wombat.config.site.SiteParam;
import org.ambraproject.wombat.config.site.Siteless;
import org.ambraproject.wombat.service.ArticleArchiveService;
import org.ambraproject.wombat.service.TopLevelLockssManifestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

/**
 * Responsible for providing the publication year range, months and article DOIs published in a given year and month
 */
@Controller
public class LockssController extends WombatController {

  @Autowired
  private ArticleArchiveService articleArchiveServiceImpl;
  @Autowired
  private TopLevelLockssManifestService topLevelLockssManifestService;

  @RequestMapping(name = "lockssPermission", value = "/lockss.txt", method = RequestMethod.GET)
  public String getLockssPermission(@SiteParam Site site) {
    return site + "/ftl/lockss/permission";
  }

  /*
   * Note that this mapping value ("/lockss-manifest") is the same as for the "lockssYears" handler, which causes weird
   * conflicts on certain sites. Specifically, "lockssYears" must be disabled for any site that is configured without a
   * PathTokenPredicate. This is fine for PLOS's use case but breaks the feature for the general case.
   *
   * TODO: Improve. See comments in TopLevelLockssManifestService for further details.
   */
  @Siteless
  @RequestMapping(value = "/lockss-manifest", method = RequestMethod.GET)
  public ResponseEntity<?> getSiteManifestLinks(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.TEXT_HTML)
        .body(topLevelLockssManifestService.getPageText(request));
  }

  @RequestMapping(name = "lockssYears", value = "/lockss-manifest", method = RequestMethod.GET)
  public String getYearsForJournal(@SiteParam Site site, Model model) throws IOException, ParseException {
    Map<String, String> yearRange = (Map<String, String>) articleArchiveServiceImpl.getYearsForJournal(site);
    model.addAttribute("yearRange", yearRange);
    return site + "/ftl/lockss/years";
  }

  @RequestMapping(name = "lockssMonths", value = "/lockss-manifest/vol_{year}", method = RequestMethod.GET)
  public String getMonthsForYear(@SiteParam Site site, @PathVariable String year, Model model) {
    String[] months = articleArchiveServiceImpl.getMonthsForYear(year);
    model.addAttribute("year", year);
    model.addAttribute("months", months);
    return site + "/ftl/lockss/months";
  }

  @RequestMapping(name = "lockssArticles", value = "/lockss-manifest/vol_{year}/{month}", method = RequestMethod.GET)
  public String getArticlesPerMonth(@SiteParam Site site, @PathVariable String year,
                                    @PathVariable String month, Model model) throws IOException {
    Map<String, Map> searchResult = (Map<String, Map>) articleArchiveServiceImpl.getArticleDoisPerMonth(site,
        year, month);
    model.addAttribute("month", month);
    model.addAttribute("year", year);
    model.addAttribute("searchResult", searchResult);

    return site + "/ftl/lockss/dois";
  }
}
