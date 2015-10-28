package org.ambraproject.wombat.config.site;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;


/**
 * Handler mapping class that incorporates custom {@link SiteRequestCondition}
 */
public class SiteHandlerMapping extends RequestMappingHandlerMapping {

  @Autowired
  SiteResolver siteResolver;
  @Autowired
  RequestMappingContextDictionary requestMappingContextDictionary;
  @Autowired
  SiteSet siteSet;

  @Override
  protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
    return null; // class level site mapping is not supported since the configuration is supplied at the method level
  }

  @Override
  protected RequestCondition<?> getCustomMethodCondition(Method method) {
    RequestMapping methodAnnotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
    Preconditions.checkNotNull(methodAnnotation, "No @RequestMapping found on mapped method");
    return SiteRequestCondition.create(siteResolver, siteSet, method, requestMappingContextDictionary);
  }

  @Override
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    RequestMappingInfo info = null;
    RequestMappingContext mapping = RequestMappingContext.create(method);
    if (mapping != null) {
      RequestCondition<?> methodCondition = this.getCustomMethodCondition(method);
      info = this.createRequestMappingInfo(mapping, methodCondition);
      checkMappingsOnHandlerType(handlerType);
    }
    return info;
  }

  /**
   * Throw an exception if a handler class has a RequestMapping annotation on it. We force everything to be declared
   * directly on the controller methods, sometimes risking a little redundancy.
   * <p/>
   * The motivation here is to avoid complications with our extensions to RequestMapping semantics, especially the
   * Siteless annotation. This is simpler than relying on the behavior of {@link org.springframework.web.servlet.mvc.method.RequestMappingInfo#combine},
   * as {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping} does, to account for
   * the Siteless annotation correctly.
   */
  private static void checkMappingsOnHandlerType(Class<?> handlerType) {
    RequestMapping requestMapping = AnnotationUtils.findAnnotation(handlerType, RequestMapping.class);
    if (requestMapping != null) {
      throw new RuntimeException("Expected RequestMapping not to appear at class level");
    }
  }

  @Override
  protected RequestMappingInfo createRequestMappingInfo(RequestMapping annotation, RequestCondition<?> customCondition) {
    // Kludge alert: We expect our override of getMappingForMethod
    // to shield this method from all calls in the superclass
    throw new UnsupportedOperationException();
  }

  private RequestMappingInfo createRequestMappingInfo(RequestMappingContext mapping, RequestCondition<?> customCondition) {
    Set<String> allPatterns = SiteRequestCondition.getAllPatterns(siteSet, mapping);
    RequestMapping annotation = mapping.getAnnotation();
    String[] embeddedPatterns = resolveEmbeddedValuesInPatterns(allPatterns.toArray(new String[allPatterns.size()]));
    return new RequestMappingInfo(
        annotation.name(),
        new PatternsRequestCondition(embeddedPatterns, null, null, true, true, null),
        new RequestMethodsRequestCondition(annotation.method()),
        new ParamsRequestCondition(annotation.params()),
        new HeadersRequestCondition(annotation.headers()),
        new ConsumesRequestCondition(annotation.consumes(), annotation.headers()),
        new ProducesRequestCondition(annotation.produces(), annotation.headers(), null),
        customCondition);
  }

  @Override
  protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> requestMappingInfos, String lookupPath,
                                        HttpServletRequest request) throws ServletException {
    /* The base implementation of this method looks for reasons that a given request may not have matched any existing
    handler mapping despite matching a URL pattern correctly. This could be an incorrect HTTP method (e.g. a GET
    instead of a POST), incorrect or missing params, etc. It then throws an exception or returns null if no obvious
    reason was found.

    Because we are providing a custom RequestCondition object which may reject a given request for any number of
    reasons unrelated to any of these RequestMapping attributes, this method can no longer properly detect the
    reason for a mismatch based solely on these attributes and may throw an exception inappropriately.

    We may at some point want to implement our own handler code here to produce a log entry or other debugging
    information for rejected requests, but at this time, returning a null (passing control to the usual 404 status
    handling) seems sufficient. */
    return null;
  }
}
