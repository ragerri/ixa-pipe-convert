package eus.ixa.ixa.pipe.convert;

import java.util.ArrayList;
import java.util.List;

import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class NAFUtils {

  private NAFUtils() {
  }

  public static String getSentenceStringFromWFs(List<WF> sent) {
    StringBuilder sb = new StringBuilder();
    for (WF wf : sent) {
      sb.append(wf.getForm()).append(" ");
    }
    return sb.toString().trim();
  }

  /**
   * Get all the WF ids for the terms contained in the KAFDocument.
   * 
   * @param kaf
   *          the KAFDocument
   * @return the list of all WF ids in the terms layer
   */
  public static List<String> getWFIdsFromTerms(List<Term> terms) {
    List<String> wfTermIds = new ArrayList<>();
    for (int i = 0; i < terms.size(); i++) {
      List<WF> sentTerms = terms.get(i).getWFs();
      for (WF form : sentTerms) {
        wfTermIds.add(form.getId());
      }
    }
    return wfTermIds;
  }

  /**
   * Check that the references from the entity spans are actually contained in
   * the term ids.
   * 
   * @param wfIds
   *          the worform ids corresponding to the Term span
   * @param termWfIds
   *          all the terms in the document
   * @return true or false
   */
  public static boolean checkTermsRefsIntegrity(List<String> wfIds,
      List<String> termWfIds) {
    for (int i = 0; i < wfIds.size(); i++) {
      if (!termWfIds.contains(wfIds.get(i))) {
        return false;
      }
    }
    return true;
  }

}
