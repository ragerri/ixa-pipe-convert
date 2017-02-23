/*
	 *Copyright 2016 Rodrigo Agerri

	   Licensed under the Apache License, Version 2.0 (the "License");
	   you may not use this file except in compliance with the License.
	   You may obtain a copy of the License at

	       http://www.apache.org/licenses/LICENSE-2.0

	   Unless required by applicable law or agreed to in writing, software
	   distributed under the License is distributed on an "AS IS" BASIS,
	   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	   See the License for the specific language governing permissions and
	   limitations under the License.
	 */

package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class DSRCCorpus {

  public static Pattern endOfSentence = Pattern.compile("[?|\\.]");

  // do not instantiate this class
  private DSRCCorpus() {
  }

    private static void DSRCToNAFNER(KAFDocument kaf, String wordsDoc,
      String markablesDoc) throws JDOMException, IOException {
    // reading the words xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    Document docWords = sax.build(wordsDoc);
    XPathExpression<Element> expr = xFactory.compile("//word",
        Filters.element());
    List<Element> words = expr.evaluate(docWords);

    // building the NAF containing the WFs and Terms
    // naf sentence counter
    int sentCounter = 1;
    for (Element word : words) {
      List<WF> sentWFs = new ArrayList<>();
      List<Term> sentTerms = new ArrayList<>();
      // sentence id and original text
      String wordId = word.getAttributeValue("id");
      String token = word.getText();
      // the list contains just one list of tokens
      WF wf = kaf.newWF(0, token, sentCounter);
      final List<WF> wfTarget = new ArrayList<WF>();
      wfTarget.add(wf);
      sentWFs.add(wf);
      Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
      term.setPos("O");
      term.setLemma(token);
      sentTerms.add(term);
      Matcher endMatcher = endOfSentence.matcher(token);
      if (endMatcher.matches()) {
        sentCounter++;
      }
      String[] tokenIds = new String[sentWFs.size()];
      for (int i = 0; i < sentWFs.size(); i++) {
        tokenIds[i] = sentWFs.get(i).getId();
      }
      // going through the markable elements in mmax opinion
      // expressions files
      SAXBuilder markSax = new SAXBuilder();
      XPathFactory markxFactory = XPathFactory.instance();
      Document markDoc = markSax.build(markablesDoc);
      XPathExpression<Element> markExpr = markxFactory.compile("//markable",
          Filters.element());
      List<Element> markables = markExpr.evaluate(markDoc);
      for (Element markable : markables) {
        if (markable.getAttributeValue("annotation_type")
            .equalsIgnoreCase("target")) {
          String markSpan = markable.getAttributeValue("span");
          String[] spanWords = markSpan.split("\\.\\.");
          int startIndex = Integer
              .parseInt(spanWords[0].replaceAll("word_", ""));
          int endIndex = Integer.parseInt(
              spanWords[spanWords.length - 1].replace("word_", "")) + 1;

          List<String> wfIds = Arrays
              .asList(Arrays.copyOfRange(tokenIds, startIndex, endIndex));
          List<String> wfTermIds = getWFIdsFromTerms(sentTerms);
          if (checkTermsRefsIntegrity(wfIds, wfTermIds)) {
            List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
            ixa.kaflib.Span<Term> neSpan = KAFDocument.newTermSpan(nameTerms);
            List<ixa.kaflib.Span<Term>> references = new ArrayList<ixa.kaflib.Span<Term>>();
            references.add(neSpan);
            Entity neEntity = kaf.newEntity(references);
            neEntity.setType("TARGET");
          }
        } // end of create entity
      }
    }
  }

  public static String DSRCToCoNLL2002(String wordsFile, String markFile)
      throws JDOMException, IOException {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    DSRCToNAFNER(kaf, wordsFile, markFile);
    String conllFile = Convert.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }

  /**
   * Get all the WF ids for the terms contained in the KAFDocument.
   * 
   * @param kaf
   *          the KAFDocument
   * @return the list of all WF ids in the terms layer
   */
  private static List<String> getWFIdsFromTerms(List<Term> terms) {
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
  private static boolean checkTermsRefsIntegrity(List<String> wfIds,
      List<String> termWfIds) {
    for (int i = 0; i < wfIds.size(); i++) {
      if (!termWfIds.contains(wfIds.get(i))) {
        return false;
      }
    }
    return true;
  }
}
