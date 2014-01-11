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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller intended to serve "nice" 404 pages, using the styling of the site, if possible. An instance of this
 * controller needs to be set as the defaultHandler property in the spring config.
 */
@Controller
public class NotFoundController extends WombatController {

  @RequestMapping
  public String handle404(HttpServletRequest request, HttpServletResponse response, Model model) {
    response.setStatus(HttpStatus.NOT_FOUND.value());
    SitePageContext context = inspectPathForContext(request);
    if (context == null) {
      return "//notFound";
    } else {
      model.addAttribute("depth", context.getPageDepth());
      return context.getSite().getKey() + "/ftl/notFound";
    }
  }
}