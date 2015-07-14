package org.ambraproject.wombat.config.site;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.ambraproject.wombat.config.HandlerMappingConfiguration;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;


public class SitePatternsRequestCondition implements RequestCondition <SitePatternsRequestCondition> {

  private SiteResolver siteResolver;
  private ImmutableMap<String, PatternsRequestCondition> requestConditionMap;

  private SitePatternsRequestCondition(SiteResolver siteResolver){
    this.siteResolver = Preconditions.checkNotNull(siteResolver);
  }

  private void setRequestConditionMap(HandlerMappingConfiguration handlerMappingConfiguration,
                                                          SiteSet siteSet, Method method, Set<String> siteKeys) {
    ImmutableMap.Builder<String, PatternsRequestCondition> mapBuilder = ImmutableMap.builder();
    for (String siteKey: siteKeys) {
      Set<String> patterns = handlerMappingConfiguration.getValidPatternsForSite(siteKey, siteSet, method);
      mapBuilder.put(siteKey,
          new PatternsRequestCondition(patterns.toArray(new String[patterns.size()]), null, null, true, true, null));
    }
    requestConditionMap = mapBuilder.build();
  }

  public static SitePatternsRequestCondition create(HandlerMappingConfiguration handlerMappingConfiguration,
                SiteSet siteSet, SiteResolver siteResolver, Method method) {
    SitePatternsRequestCondition sitePatternRC = new SitePatternsRequestCondition(siteResolver);
    sitePatternRC.setRequestConditionMap(Preconditions.checkNotNull(handlerMappingConfiguration),
            Preconditions.checkNotNull(siteSet), Preconditions.checkNotNull(method), siteSet.getSiteKeys());
    return sitePatternRC;
  }

  @Override
  public SitePatternsRequestCondition combine(SitePatternsRequestCondition other) {
    Set<String> siteKeys = Sets.union(requestConditionMap.keySet(),other.requestConditionMap.keySet());
    SitePatternsRequestCondition sitePatternsRC = new SitePatternsRequestCondition(siteResolver);
    ImmutableMap.Builder<String, PatternsRequestCondition> mapBuilder = ImmutableMap.builder();
    for (String siteKey: siteKeys) {
      PatternsRequestCondition thisRC = requestConditionMap.get(siteKey);
      PatternsRequestCondition otherRC = other.requestConditionMap.get(siteKey);
      if (otherRC == null) {
        mapBuilder.put(siteKey, thisRC);
      } else if (thisRC == null) {
        mapBuilder.put(siteKey, otherRC);
      } else {
        mapBuilder.put(siteKey, thisRC.combine(otherRC));
      }
    }
    sitePatternsRC.requestConditionMap = mapBuilder.build();
    return sitePatternsRC;
  }

  @Override
  public SitePatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
    Site site = siteResolver.resolveSite(request);
    if (site == null || !requestConditionMap.containsKey(site.getKey())) {
      return null;
    }
    PatternsRequestCondition patternsRC = requestConditionMap.get(site.getKey()).getMatchingCondition(request);
    SitePatternsRequestCondition sitePatternsRC = new SitePatternsRequestCondition(siteResolver);
    sitePatternsRC.requestConditionMap = ImmutableMap.of(site.getKey(), patternsRC);
    return sitePatternsRC;
  }

  @Override
  public int compareTo(SitePatternsRequestCondition other, HttpServletRequest request) {
    return 0;
  }
}
