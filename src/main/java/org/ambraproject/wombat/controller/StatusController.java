/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.wombat.controller;

import org.ambraproject.rhombat.cache.Cache;
import org.ambraproject.wombat.config.Site;
import org.ambraproject.wombat.service.SearchService;
import org.ambraproject.wombat.service.SoaService;
import org.ambraproject.wombat.service.SolrSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Controller class that handles requests to "/status".  If all backends are operational, it returns "OK" as the
 * sole response.  Intended for automated monitoring.
 */
@Controller
public class StatusController extends WombatController {

  @Autowired
  private SearchService searchService;

  @Autowired
  private Cache cache;

  @Autowired
  private SoaService soaService;

  @RequestMapping("/status")
  public void renderStatus(HttpServletResponse response) throws IOException {

    // Just pick a valid site--we don't care which one.
    Site testSite = siteSet.getSites().iterator().next();

    // 1. solr
    searchService.simpleSearch("test", testSite, 0, 1, SolrSearchService.SolrSortOrder.RELEVANCE,
        SolrSearchService.SolrDateRange.ALL_TIME);

    // 2. memcache (or whatever cache implementation is in use).  We don't call check the result of get() because
    // if we're using NullCache, it will always return null.
    cache.put("test", "foo");
    cache.get("test");

    // 3. Rhino (aka SOA)
    String ok = soaService.requestObject("status", String.class);
    if (!"OK".equals(ok)) {
      throw new RuntimeException("Rhino returned response " + ok);
    }
    response.setContentType("text/plain");
    try (PrintWriter writer = response.getWriter()) {
      writer.print("OK");
    }
  }
}
