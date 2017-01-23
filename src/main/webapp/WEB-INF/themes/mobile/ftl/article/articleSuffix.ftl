<#--
  ~ Copyright (c) 2017 Public Library of Science
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a
  ~ copy of this software and associated documentation files (the "Software"),
  ~ to deal in the Software without restriction, including without limitation
  ~ the rights to use, copy, modify, merge, publish, distribute, sublicense,
  ~ and/or sell copies of the Software, and to permit persons to whom the
  ~ Software is furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in
  ~ all copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
  ~ THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
  ~ FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  ~ DEALINGS IN THE SOFTWARE.
  -->

<nav>
  <ul class="article-buttons">
  <#if figures?has_content>
    <li class="menuitem">
      <a class="btn-lg" href="<@siteLink handlerName="figuresPage" queryParameters={"id": article.doi} />">
        <span class="arrow">View</span>
        Figures (${figures?size})
      </a>
    </li>
  </#if>
  <#if commentCount.root &gt; 0>
    <li class="menuitem">
      <a class="btn-lg" href="<@siteLink handlerName="articleComments" queryParameters={"id": article.doi} />">
        <span class="arrow">View</span>
        Reader Comments (${commentCount.root})
      </a>
    </li>
  </#if>
    <li class="menuitem">
      <a class="btn-lg" href="<@siteLink handlerName="articleAuthors" queryParameters={"id": article.doi} />">
        <span class="arrow">View</span>
        About the Authors
      </a>
    </li>
      <li class="menuitem">
        <a class="btn-lg" href="<@siteLink handlerName="articleMetrics" queryParameters={"id": article.doi} />">
          <span class="arrow">View</span>
          Metrics
        </a>
      </li>
  <li class="menuitem">
    <a class="btn-lg" href="<@siteLink handlerName="articleRelatedContent" queryParameters={"id": article.doi} />">
      <span class="arrow">View</span>
      Related Content
    </a>
  </li>
  </ul>
</nav><#--end article buttons-->

<nav class="article-save-options">

<#macro articleSaveButton address>
  <div class="button-row">
    <a class="rounded coloration-white-on-color full" href="${address}">
      <#nested/>
    </a>
  </div>
</#macro>

<#if articleItems[article.doi].files?keys?seq_contains("printable")>
  <@siteLink handlerName="assetFile" queryParameters=(articlePtr + {"type": "printable"}) ; href>
    <@articleSaveButton href>Download Article (PDF)</@articleSaveButton>
  </@siteLink>
</#if>

<@siteLink handlerName="citationDownloadPage" queryParameters={"id": article.doi} ; href>
  <@articleSaveButton href>Download Citation</@articleSaveButton>
</@siteLink>

<@siteLink handlerName="email" queryParameters={"id": article.doi} ; href>
  <@articleSaveButton href>Email this Article</@articleSaveButton>
</@siteLink>

</nav><#--end article-save-options-->
</article>
