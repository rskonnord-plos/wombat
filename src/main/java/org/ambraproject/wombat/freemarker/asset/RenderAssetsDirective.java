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

package org.ambraproject.wombat.freemarker.asset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import freemarker.core.Environment;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import org.ambraproject.wombat.config.RuntimeConfiguration;
import org.ambraproject.wombat.config.site.Site;
import org.ambraproject.wombat.controller.SiteResolver;
import org.ambraproject.wombat.controller.StaticResourceController;
import org.ambraproject.wombat.freemarker.SitePageContext;
import org.ambraproject.wombat.service.AssetService;
import org.ambraproject.wombat.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract superclass of freemarker custom directives that render links to compiled assets. See {@link
 * AssetDirective}.
 */
abstract class RenderAssetsDirective implements TemplateDirectiveModel {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;
  @Autowired
  private AssetService assetService;
  @Autowired
  private SiteResolver siteResolver;

  /**
   * Renders queued asset links as HTML. If in dev mode, the rendered output will be a sequence of plain links to the
   * asset resources. Else, the queued assets will be compiled into a minified form, and the rendered output will be a
   * single link to the result.
   * <p/>
   * Either way, assets will be ordered according to their dependencies, defaulting to the order in which they were
   * enqueued. (That is, in dev mode the links appear in that order, and in production mode the assets are concatenated
   * in that order before they are minified.)
   * <p/>
   * This method pulls asset nodes from the named environment variable. Executing the method clears the queue.
   *
   * @param assetType           defines the type of asset (.js or .css)
   * @param requestVariableName the name of the request variable that uncompiled assets have been stored in by calls to
   *                            a subclass of {@link AssetDirective}
   * @param environment         freemarker execution environment
   * @throws TemplateException
   * @throws IOException
   */
  protected void renderAssets(AssetService.AssetType assetType, String requestVariableName, Environment environment)
      throws TemplateException, IOException {
    HttpServletRequest request = ((HttpRequestHashModel) environment.getDataModel().get("Request")).getRequest();
    Map<String, AssetNode> assetNodes = (Map<String, AssetNode>) request.getAttribute(requestVariableName);
    if (assetNodes == null) return;
    List<String> assetPaths = sortNodes(assetNodes.values());
    assetNodes.clear(); // Reset in case new assets get put in for a second render

    if (assetPaths != null && !assetPaths.isEmpty()) {
      if (runtimeConfiguration.devModeAssets()) {
        for (String assetPath : assetPaths) {
          String assetAddress = new SitePageContext(siteResolver, environment).buildLink(assetPath);
          environment.getOut().write(getHtml(assetAddress));
        }
      } else {
        Site site = new SitePageContext(siteResolver, environment).getSite();
        String assetLink = assetService.getCompiledAssetLink(assetType, assetPaths, site, request.getServletPath());
        String assetAddress = site.getRequestScheme().buildLink(request,
            PathUtil.JOINER.join(StaticResourceController.RESOURCE_NAMESPACE, assetLink));
        environment.getOut().write(getHtml(assetAddress));
      }
    }
  }

  /**
   * Sort assets by their dependencies and return their paths in order.
   * <p/>
   * The result is a topological sort that preserves the input order as much as possible. The algorithm repeatedly pulls
   * the first node from the sequence that does not depend on any nodes not yet pulled.
   * <p/>
   * This method clobbers the nodes' {@code dependencies} fields. Specifically, a successful run will empty all the
   * dependency sets, with the assumption that the node objects will be discarded immediately after this method
   * returns.
   *
   * @param assetNodes an ordered collection of assets
   * @return a list of asset paths, sorted by dependency
   */
  @VisibleForTesting
  static List<String> sortNodes(Collection<AssetNode> assetNodes) {
    List<String> simplePaths = extractPathsIfSimple(assetNodes);
    if (simplePaths != null) return simplePaths;

    // Topological sort by Kahn's algorithm
    Set<String> assetPaths = Sets.newLinkedHashSetWithExpectedSize(assetNodes.size());
    Deque<AssetNode> queue = new LinkedList<>(assetNodes);
    while (!queue.isEmpty()) {
      boolean foundAvailableNode = false;
      for (Iterator<AssetNode> queueIterator = queue.iterator(); queueIterator.hasNext(); ) {
        AssetNode candidate = queueIterator.next();

        // Check whether the candidate has any dependents not yet in assetPaths
        Collection<String> dependencies = candidate.getDependencies();
        for (Iterator<String> dependencyIterator = dependencies.iterator(); dependencyIterator.hasNext(); ) {
          String dependent = dependencyIterator.next();
          if (assetPaths.contains(dependent)) {
            dependencyIterator.remove();
          } else break;
        }
        if (dependencies.isEmpty()) {
          assetPaths.add(candidate.getPath());
          queueIterator.remove();
          foundAvailableNode = true;
          break;
        }
      }
      if (!foundAvailableNode) {
        String message = "Can't resolve asset dependencies. "
            + "(There is either a cycle or a reference to a nonexistent asset.) " + queue;
        throw new RuntimeException(message);
      }
    }
    return new ArrayList<>(assetPaths);
  }

  /**
   * If no asset nodes have any dependencies, return their paths in the same order. This is more efficient than stepping
   * through the topological sorting algorithm in {@link #sortNodes}.
   *
   * @param assetNodes a sequence of asset nodes
   * @return the nodes'
   */
  private static List<String> extractPathsIfSimple(Collection<AssetNode> assetNodes) {
    List<String> assetPaths = Lists.newArrayListWithCapacity(assetNodes.size());
    for (AssetNode assetNode : assetNodes) {
      if (!assetNode.getDependencies().isEmpty()) {
        return null;
      }
      assetPaths.add(assetNode.getPath());
    }
    return assetPaths;
  }

  /**
   * Returns the HTML that renders a link to an asset.  This will vary depending on the subclass (and the type of the
   * asset).
   *
   * @param assetPath path to the asset file
   * @return HTMl snippet linking to the asset file
   */
  protected abstract String getHtml(String assetPath);

}
