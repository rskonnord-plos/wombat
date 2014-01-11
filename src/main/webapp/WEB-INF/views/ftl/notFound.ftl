<#include "common/htmlTag.ftl" />

<#assign title = "Page Not Found" />
<#include "common/head.ftl" />

<body>
<div id="container-main">
<#include "common/header.ftl" />
  <div class="error">

    <h1>Page Not Found</h1>

    <p>Sorry, the page that you've requested cannot be found; it may have been moved, changed or removed.</p>

    <p>Please use the search form above to locate an article.</p>
  </div>

<#include "common/footer/footer.ftl" />
</div><#-- end container-main -->

<#include "common/fullMenu/fullMenu.ftl" />
<#include "common/bodyJs.ftl" />
</body>
</html>