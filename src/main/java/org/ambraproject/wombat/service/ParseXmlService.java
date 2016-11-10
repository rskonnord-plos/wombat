package org.ambraproject.wombat.service;

import org.ambraproject.wombat.model.Reference;
import org.ambraproject.wombat.model.RetractionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This class is used to parse the article xml
 */
public interface ParseXmlService {
  /**
   * Parses the references in the article xml
   *
   * @param xml article xml
   * @return list of Reference objects
   * @throws IOException
   */
  List<Reference> parseArticleReferences(InputStream xml,
                                         ParseReferenceService.DoiToJournalLinkService linkService) throws IOException;

  /**
   * Parses the retraction type for an article
   *
   * @param xml
   * @return {@link RetractionType}
   * @throws IOException
   */
  RetractionType parseArticleAmendmentRetractionType(InputStream xml,
                                                     ParseReferenceService.DoiToJournalLinkService linkService) throws IOException;
}
