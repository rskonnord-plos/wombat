package org.ambraproject.wombat.controller;

import com.google.common.io.Closer;
import org.ambraproject.wombat.config.Theme;
import org.ambraproject.wombat.service.AssetService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Controller
public class StaticFileController extends WombatController {

  private static final String STATIC_NAMESPACE = "static/";

  /**
   * Path prefix for compiled assets (.js and .css).
   */
  private static final String COMPILED_NAMESPACE = STATIC_NAMESPACE + AssetService.COMPILED_PATH_PREFIX;

  @Autowired
  private AssetService assetService;

  @RequestMapping("/{site}/static/**")
  public void serveStaticContent(HttpServletRequest request, HttpServletResponse response,
                                 HttpSession session, @PathVariable("site") String site)
      throws IOException {
    Theme theme = siteSet.getSite(site).getTheme();

    // Kludge to get "static/**"
    String servletPath = request.getServletPath();
    String filePath = servletPath.substring(site.length() + 2);
    response.setContentType(session.getServletContext().getMimeType(servletPath));
    if (filePath.startsWith(COMPILED_NAMESPACE)) {
      serveCompiledAsset(filePath, response);
    } else {
      serveFile(filePath, response, theme);
    }
  }

  /**
   * Serves a file provided by a theme.
   *
   * @param filePath the path to the file (relative to the theme)
   * @param response response object
   * @param theme    specifies the theme from which we are loading the file
   * @throws IOException
   */
  private void serveFile(String filePath, HttpServletResponse response, Theme theme) throws IOException {
    Closer closer = Closer.create();
    try {
      InputStream inputStream = theme.getStaticResource(filePath);
      if (inputStream == null) {
        // TODO: Forward to user-friendly 404 page
        response.setStatus(HttpStatus.NOT_FOUND.value());

        // Just for debugging
        closer.register(response.getOutputStream()).write("Not found!".getBytes());
      } else {
        closer.register(inputStream);
        OutputStream outputStream = closer.register(response.getOutputStream());
        IOUtils.copy(inputStream, outputStream);
      }
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

  /**
   * Serves a .js or .css asset that has already been concatenated and minified. See {@link AssetService} for details on
   * this process.
   *
   * @param filePath the path to the file (relative to the theme)
   * @param response response object
   * @throws IOException
   */
  private void serveCompiledAsset(String filePath, HttpServletResponse response) throws IOException {
    assetService.serveCompiledAsset(filePath.substring(COMPILED_NAMESPACE.length()), response.getOutputStream());
  }
}
