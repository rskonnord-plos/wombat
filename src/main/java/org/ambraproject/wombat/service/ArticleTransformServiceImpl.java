package org.ambraproject.wombat.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.Closer;
import net.sf.json.JSONArray;
import org.ambraproject.wombat.config.theme.Theme;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import net.sf.json.xml.XMLSerializer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ArticleTransformServiceImpl implements ArticleTransformService {
  private static final Logger log = LoggerFactory.getLogger(ArticleTransformServiceImpl.class);

  private static final String TEMPLATE_ROOT_PATH = "xform/";
  private static final String TRANSFORM_TEMPLATE_PATH = TEMPLATE_ROOT_PATH + "article-transform.xsl";

  @Autowired
  private Charset charset;
  @Autowired
  private ArticleService articleService;

  private static TransformerFactory newTransformerFactory() {
    // This implementation is required for XSLT features, so just hard-code it here
    // Preferred over TransformerFactory.newInstance because Java system properties can burn in hell
    return new net.sf.saxon.TransformerFactoryImpl();
  }


  private static class ThemeUriResolver implements URIResolver, Closeable {

    /*
     * Any stream opened while providing a Source gets stored here,
     * then is closed when the outer ThemeUriResolver object is closed.
     */
    private final Closer closer = Closer.create();

    private final Theme theme;

    private ThemeUriResolver(Theme theme) {
      this.theme = Preconditions.checkNotNull(theme);
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
      InputStream resourceStream;
      try {
        resourceStream = closer.register(theme.getStaticResource(TEMPLATE_ROOT_PATH + href));
      } catch (IOException e) {
        throw new TransformerException(e);
      }
      return new StreamSource(resourceStream);
    }

    @Override
    public void close() throws IOException {
      closer.close();
    }
  }



  /**
   * Build a new transformer and attach any required parameters for the given render context
   *
   * @param renderContext The render context contains information (such as the site object and articleId, and
   *                      potentially other variables related to client side capabilities or identifiers of specific
   *                      excerpts targeted for rendering) that may be used to determine which parameters to pass to
   *                      the transformer object.
   * @param xmlReader An XML reader instance which is provided to parse XML-formatted strings for the purpose of
   *                  creating any secondary SAX sources for the transform (passed to the transformer as parameters)
   * @return the transformer
   * @throws IOException
   */
  private Transformer buildTransformer(RenderContext renderContext, XMLReader xmlReader) throws IOException {
    Theme theme = renderContext.getSite().getTheme();
    log.debug("Building transformer for: {}", renderContext.getSite());
    TransformerFactory factory = newTransformerFactory();

    Transformer transformer;
    try (ThemeUriResolver resolver = new ThemeUriResolver(theme);
         InputStream transformFile = theme.getStaticResource(TRANSFORM_TEMPLATE_PATH)) {
      factory.setURIResolver(resolver);
      transformer = factory.newTransformer(new StreamSource(transformFile));
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }

    // Add cited articles metadata for inclusion of DOI links in reference list
    // TODO: abstract out into a strategy pattern when and if render options become more complex
    boolean showsCitedArticles = (boolean) theme.getConfigMap("article").get("showsCitedArticles");
    if (showsCitedArticles && renderContext.getArticleId() != null) {
      Map<?, ?> articleMetadata = articleService.requestArticleMetadata(renderContext.getArticleId(), false);
      Object citedArticles = articleMetadata.get("citedArticles");
      JSONArray jsonArr = JSONArray.fromObject(citedArticles);
      String metadataXml = new XMLSerializer().write(jsonArr);
      SAXSource saxSourceMeta = new SAXSource(xmlReader, new InputSource(IOUtils.toInputStream(metadataXml)));
      transformer.setParameter("citedArticles", saxSourceMeta);
    }
    return transformer;
  }


  @Override
  public void transform(RenderContext renderContext, InputStream xml, OutputStream html)
      throws IOException, TransformerException {
    Preconditions.checkNotNull(renderContext);
    Preconditions.checkNotNull(xml);
    Preconditions.checkNotNull(html);

    log.debug("Starting XML transformation");
    SAXParserFactory spf = SAXParserFactory.newInstance();
    XMLReader xmlr;
    try {
      SAXParser sp = spf.newSAXParser();
      xmlr = sp.getXMLReader();
    } catch (ParserConfigurationException | SAXException e) {
      throw new TransformerException(e);
    }

    DummyEntityResolver entityResolver = new DummyEntityResolver();
    xmlr.setEntityResolver(entityResolver);

    // build the transformer and add any context-dependent parameters required for the transform
    // NOTE: the XMLReader is passed here for use in creating any required secondary SAX sources
    Transformer transformer = buildTransformer(renderContext, xmlr);
    SAXSource saxSource = new SAXSource(xmlr, new InputSource(xml));
    try {
      transformer.transform(saxSource, new StreamResult(html));
    } catch (TransformerException | RuntimeException e) {
      log.error("EntityResolver requests: {}", entityResolver.systemIdsRequested);
      throw e;
    }

    log.debug("Finished XML transformation");
  }

  // This is a little unorthodox.  Without setting this custom EntityResolver, the transform will
  // make ~50 HTTP calls to nlm.nih.gov to retrieve the DTD and various entity files referenced
  // in the article XML.  By setting a custom EntityResolver that just returns an empty string
  // for each of these, we prevent that.  This seems to have no ill effects on the transformation
  // itself.  This is a roundabout way of turning off DTD validation, which is more
  // straightforward to do with a Document/DocumentBuilder, but the saxon library we're using
  // is much faster at XSLT if it uses its own XML parser instead of DocumentBuilder.  See
  // http://stackoverflow.com/questions/155101/make-documentbuilder-parse-ignore-dtd-references
  // for a discussion.
  private static class DummyEntityResolver implements EntityResolver {
    private final Collection<String> systemIdsRequested = new ArrayList<>();

    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
      systemIdsRequested.add(systemId);
      return new InputSource(new StringReader("")); // returning null here would cause the HTTP request to be made.
    }
  }

  @Override
  public void transformExcerpt(RenderContext renderContext, InputStream xmlExcerpt, OutputStream html, String enclosingTag)
      throws IOException, TransformerException {
    Preconditions.checkNotNull(renderContext);
    Preconditions.checkNotNull(xmlExcerpt);
    Preconditions.checkNotNull(html);

    InputStream streamToTransform;
    if (Strings.isNullOrEmpty(enclosingTag)) {
      streamToTransform = xmlExcerpt;
    } else {
      // Build a stream that contains the original stream surrounded by tags.
      // To avoid dumping the original stream into memory, append two mini-streams.
      InputStream prefix = IOUtils.toInputStream('<' + enclosingTag + '>', charset);
      InputStream suffix = IOUtils.toInputStream("</" + enclosingTag + '>', charset);
      List<InputStream> concatenation = ImmutableList.of(prefix, xmlExcerpt, suffix);
      streamToTransform = new SequenceInputStream(Iterators.asEnumeration(concatenation.iterator()));
    }

    transform(renderContext, streamToTransform, html);
  }

  @Override
  public String transformExcerpt(RenderContext renderContext, String xmlExcerpt, String enclosingTag) throws TransformerException {
    Preconditions.checkNotNull(renderContext);
    Preconditions.checkNotNull(xmlExcerpt);
    StringWriter html = new StringWriter();
    OutputStream outputStream = new WriterOutputStream(html, charset);
    InputStream inputStream = IOUtils.toInputStream(xmlExcerpt, charset);
    try {
      transformExcerpt(renderContext, inputStream, outputStream, enclosingTag);
      outputStream.close(); // to flush (StringWriter doesn't require a finally block)
    } catch (IOException e) {
      throw new RuntimeException(e); // unexpected, since both streams are in memory
    }
    return html.toString();
  }

}
