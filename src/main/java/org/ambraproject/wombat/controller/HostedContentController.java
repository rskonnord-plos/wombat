/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.wombat.controller;

import com.google.common.base.Optional;
import org.ambraproject.wombat.config.RuntimeConfigurationException;
import org.ambraproject.wombat.config.site.Site;
import org.ambraproject.wombat.config.theme.Theme;
import org.ambraproject.wombat.freemarker.HtmlElementSubstitution;
import org.ambraproject.wombat.freemarker.HtmlElementTransformation;
import org.ambraproject.wombat.service.EntityNotFoundException;
import org.ambraproject.wombat.service.remote.CacheDeserializer;
import org.ambraproject.wombat.service.remote.EditorialContentService;
import org.ambraproject.wombat.util.CacheParams;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mortbay.util.ajax.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Controller serves a container page for externally generated content
 */
@Controller
public class HostedContentController extends WombatController {

  @Autowired
  private EditorialContentService editorialContentService;

  @RequestMapping(value = {"/h/{pageName}", "/{site}/h/{pageName}"})
  public String renderSiteContent(Model model, @SiteParam Site site, @PathVariable String pageName)
          throws IOException {

//    Theme theme = site.getTheme();
//    Map<String, Object> pageConfig = theme.getConfigMap("siteContent");
//
//    String repoKeyPrefix = (String) pageConfig.get("contentRepoKeyPrefix");
    String repoKeyPrefix = "h";
//    if (repoKeyPrefix == null) {
//      throw new RuntimeConfigurationException("Content repo prefix not configured for theme " + theme.toString());
//    }
    String repoKey = repoKeyPrefix.concat(".").concat(pageName);

    String cacheKey = "hostedContent:" + repoKey;

    Optional<Integer> version = Optional.absent(); // versioning is not supported for site content
    try {

      String jsonString = editorialContentService.getJson("hostedContent", repoKey);
      model.addAttribute("hostedContentRepoKey", repoKey);
      model.addAttribute("hostedData", JSON.parse(jsonString));

    } catch (EntityNotFoundException e) {
      // Return a 404 if no object found.
      throw new NotFoundException(e);
    }

    return site + "/ftl/hostedContent/container";
  }

}
