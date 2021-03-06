/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.wombat.service.remote;

import org.ambraproject.wombat.config.site.Site;
import org.ambraproject.wombat.identity.ArticlePointer;
import org.ambraproject.wombat.service.ArticleService;
import org.ambraproject.wombat.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

public class CorpusContentApi extends AbstractContentApi {

  @Autowired
  private ArticleService articleService;

  @Override
  protected String getRepoConfigKey() {
    return "corpus";
  }

  /**
   * Consume an article manuscript.
   *
   * @param articleId    the article whose manuscript to read
   * @param site         the site in which the callback will render the manuscript, for caching purposes
   * @param cachePrefix  the cache space that stores the operation output
   * @param htmlCallback the operation to perform on the manuscript
   * @param <T>          the result type
   * @return the result of the operation
   * @throws IOException
   */
  public <T> T readManuscript(ArticlePointer articleId, Site site, String cachePrefix,
                              CacheDeserializer<InputStream, T> htmlCallback)
      throws IOException {
    CacheKey cacheKey = CacheKey.create(cachePrefix, site.getKey(),
        articleId.getDoi(), Integer.toString(articleId.getIngestionNumber()));
    ContentKey manuscriptKey = articleService.getManuscriptKey(articleId);
    return requestCachedStream(cacheKey, manuscriptKey, htmlCallback);
  }

}
