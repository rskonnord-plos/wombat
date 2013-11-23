<#include "../macro/pathUp.ftl" />
<#include "../common/htmlTag.ftl" />

<head>
  <title>Privacy Policy</title>

  <#assign target><@pathUp 3 "static/css/base.css" /></#assign>
  <@cssLink target=target />
  <@cssLink target="../static/css/interface.css" />
  <@cssLink target="../static/css/mobile.css" />
  <#include "../cssLinks.ftl" /><#-- TODO: Debug (this is at the wrong relative path level) -->

  <script src="../static/js/vendor/modernizr.custom.25437.js"></script>
  <script src="../static/js/vendor/respond.min.js"></script>

</head>
<body>
<#include "../common/header.ftl" />
<#include "privacyPolicyText.ftl" />
</body>
</html>
