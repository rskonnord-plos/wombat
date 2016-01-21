
<#include "../common/legacyLink.ftl" />
<#assign issueUrl = legacyUrlPrefix + "article/browse/issue" />
<#if issueImage??>
  <a href="${issueUrl}">
  <#assign issueImageId = issueImage.figures[0].thumbnails.medium.file />
    <img src="<@siteLink path="/article/asset?id=" + issueImageId />" class="current-img" alt="Current Issue"/>
  </a>
</#if>
<#if currentIssue??>
  <p class="boxtitle">
    <a href="${issueUrl}">Current Issue</a>
    <span class="subhead">${currentIssue.displayName} ${currentIssue.parentVolume.displayName}</span>
  </p>
<#else>
  <p>Our system is having a bad day.</p>
</#if>
