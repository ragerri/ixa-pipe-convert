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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

/**
 * Class for conversors of ABSA SemEval tasks datasets.
 * @author ragerri
 * @version 2016-12-12
 */
public class AbsaSemEval {
  
  //do not instantiate this class
  private AbsaSemEval() {
  }

  private static void absa2015ToNAFNER(KAFDocument kaf, String fileName, String language) {
    //reading the ABSA xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      
      //naf sentence counter
      int counter = 1;
      for (Element sent : sentences) {
        List<Integer> wfFromOffsets = new ArrayList<>();
        List<Integer> wfToOffsets = new ArrayList<>();
        List<WF> sentWFs = new ArrayList<>();
        List<Term> sentTerms = new ArrayList<>();
        //sentence id and original text
        String sentId = sent.getAttributeValue("id");
        String sentString = sent.getChildText("text");
        //the list contains just one list of tokens
        List<List<Token>> segmentedSentence = StringUtils.tokenizeSentence(sentString, language);
        for (List<Token> sentence : segmentedSentence) {
          for (Token token : sentence) {
            WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
                counter);
            wf.setXpath(sentId);
            final List<WF> wfTarget = new ArrayList<WF>();
            wfTarget.add(wf);
            wfFromOffsets.add(wf.getOffset());
            wfToOffsets.add(wf.getOffset() + wf.getLength());
            sentWFs.add(wf);
            Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
            term.setPos("O");
            term.setLemma(token.getTokenValue());
            sentTerms.add(term);
          }
        }
        counter++;
        String[] tokenIds = new String[sentWFs.size()];
        for (int i = 0; i < sentWFs.size(); i++) {
          tokenIds[i] = sentWFs.get(i).getId();
        }
        //going through every opinion element for each sentence
        //each opinion element can contain one or more opinions
        Element opinionsElement = sent.getChild("Opinions");
        if (opinionsElement != null) {
          //iterating over every opinion in the opinions element
          List<Element> opinionList = opinionsElement.getChildren();
          for (Element opinion : opinionList) {
            String category = opinion.getAttributeValue("category");
            String targetString = opinion.getAttributeValue("target");
            System.err.println("-> " + category + ", " + targetString);
            //adding OTE
            if (!targetString.equalsIgnoreCase("NULL")) {
              int fromOffset = Integer.parseInt(opinion
                    .getAttributeValue("from"));
              int toOffset = Integer.parseInt(opinion
                    .getAttributeValue("to"));
              int startIndex = -1;
              int endIndex = -1;
              for (int i = 0; i < wfFromOffsets.size(); i++) {
                if (wfFromOffsets.get(i) == fromOffset) {
                  startIndex = i;
                }
              }
              for (int i = 0; i < wfToOffsets.size(); i++) {
                if (wfToOffsets.get(i) == toOffset) {
                  //span is +1 with respect to the last token of the span
                  endIndex = i + 1;
                }
              }
              List<String> wfIds = Arrays
                  .asList(Arrays.copyOfRange(tokenIds, startIndex, endIndex));
              List<String> wfTermIds = NAFUtils.getWFIdsFromTerms(sentTerms);
              if (NAFUtils.checkTermsRefsIntegrity(wfIds, wfTermIds)) {
                List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
                ixa.kaflib.Span<Term> neSpan = KAFDocument.newTermSpan(nameTerms);
                List<ixa.kaflib.Span<Term>> references = new ArrayList<ixa.kaflib.Span<Term>>();
                references.add(neSpan);
                Entity neEntity = kaf.newEntity(references);
                neEntity.setType(category);
              }
            }
          }
        }
      }//end of sentence
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static String absa2015ToCoNLL2002(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    absa2015ToNAFNER(kaf, fileName, language);
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
 

  public static String absa2015ToWFs(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);

      int counter = 1;
      for (Element sent : sentences) {
        String sentId = sent.getAttributeValue("id");
        String sentString = sent.getChildText("text");
        List<List<Token>> segmentedSentences = StringUtils.tokenizeSentence(sentString, language);
        for (List<Token> sentence : segmentedSentences) {
          for (Token token : sentence) {
            WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
                counter);
            wf.setXpath(sentId);
          }
        }
        counter++;
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    return kaf.toString();
  }

  public static String nafToAbsa2015(String inputNAF) throws IOException {

    Path kafPath = Paths.get(inputNAF);
    KAFDocument kaf = KAFDocument.createFromFile(kafPath.toFile());
    Set<String> reviewIds = getReviewIdsFromXpathAttribute(kaf);
        
    //root element in ABSA 2015 and 2016 format
    Element reviewsElem = new Element("Reviews");
    Document doc = new Document(reviewsElem);
    
    //creating Reviews children of Review
    for (String reviewId : reviewIds) {
      Element reviewElem = new Element("Review");
      reviewElem.setAttribute("rid", reviewId);
      Element sentencesElem = new Element("sentences");
      //getting the sentences in the review
      List<List<WF>> sentencesByReview = getSentencesByReview(kaf, reviewId);
      for (List<WF> sent : sentencesByReview) {
        String sentId = sent.get(0).getXpath();
        Integer sentNumber = sent.get(0).getSent();
        
        //getting text element from word forms in NAF
        String textString = NAFUtils.getSentenceStringFromWFs(sent);
        Element sentenceElem = new Element("sentence");
        sentenceElem.setAttribute("id", sentId);
        Element textElem = new Element("text");
        textElem.setText(textString);
        sentenceElem.addContent(textElem);
        
        //creating opinions element for sentence
        List<Opinion> opinionsBySentence = getOpinionsBySentence(kaf, sentNumber);
        Element opinionsElem = new Element("Opinions");
        if (!opinionsBySentence.isEmpty()) {
          //getting opinion info from NAF Opinion layer
          for (Opinion opinion : opinionsBySentence) {
            Element opinionElem = new Element("Opinion");
            //String polarity = opinion.getOpinionExpression().getPolarity();
            String category = opinion.getOpinionExpression().getSentimentProductFeature();
            String targetString = opinion.getStr();
            int fromOffset = opinion.getOpinionTarget().getTerms().get(0).getWFs().get(0).getOffset();
            List<WF> targetWFs = opinion.getOpinionTarget().getTerms().get(opinion.getOpinionTarget().getTerms().size() -1).getWFs();
            int toOffset = targetWFs.get(targetWFs.size() -1).getOffset() + targetWFs.get(targetWFs.size() -1).getLength();
            opinionElem.setAttribute("target", targetString);
            opinionElem.setAttribute("category", category);
            //TODO we still do not have polarity here
            opinionElem.setAttribute("polarity", "na");
            opinionElem.setAttribute("from", Integer.toString(fromOffset));
            opinionElem.setAttribute("to", Integer.toString(toOffset));
            opinionsElem.addContent(opinionElem);
          }
        }
        sentenceElem.addContent(opinionsElem);
        sentencesElem.addContent(sentenceElem);
      }
      reviewElem.addContent(sentencesElem);
      reviewsElem.addContent(reviewElem);
    }//end of review
    
    XMLOutputter xmlOutput = new XMLOutputter();
    Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);
  }
  
  private static List<List<WF>> getSentencesByReview(KAFDocument kaf, String reviewId) {
    List<List<WF>> sentsByReview = new ArrayList<List<WF>>();
    for (List<WF> sent : kaf.getSentences()) {
      if (sent.get(0).getXpath().split(":")[0].equalsIgnoreCase(reviewId)) {
        sentsByReview.add(sent);
      }
    }
    return sentsByReview;
  }
  
  private static List<Opinion> getOpinionsBySentence(KAFDocument kaf, Integer sentNumber) {
    List<Opinion> opinionList = kaf.getOpinions();
    List<Opinion> opinionsBySentence = new ArrayList<>();
    for (Opinion opinion : opinionList) {
      if (sentNumber.equals(opinion.getOpinionTarget().getSpan().getFirstTarget().getSent())) {
        opinionsBySentence.add(opinion);
      }
    }
    return opinionsBySentence;
  }
  
  private static Set<String> getReviewIdsFromXpathAttribute(KAFDocument kaf) {
    Set<String> reviewIds = new LinkedHashSet<>();
    for (List<WF> sent : kaf.getSentences()) {
      String reviewId = sent.get(0).getXpath().split(":")[0];
      reviewIds.add(reviewId);
    }
    return reviewIds;
  }
  
 

  private static void absa2014ToNAFNER(KAFDocument kaf, String fileName, String language) {
    //reading the ABSA xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      
      //naf sentence counter
      int counter = 1;
      for (Element sent : sentences) {
        List<Integer> wfFromOffsets = new ArrayList<>();
        List<Integer> wfToOffsets = new ArrayList<>();
        List<WF> sentWFs = new ArrayList<>();
        List<Term> sentTerms = new ArrayList<>();
        //sentence id and original text
        String sentId = sent.getAttributeValue("id");
        String sentString = sent.getChildText("text");
        //the list contains just one list of tokens
        List<List<Token>> segmentedSentence = StringUtils.tokenizeSentence(sentString, language);
        for (List<Token> sentence : segmentedSentence) {
          for (Token token : sentence) {
            WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
                counter);
            wf.setXpath(sentId);
            final List<WF> wfTarget = new ArrayList<WF>();
            wfTarget.add(wf);
            wfFromOffsets.add(wf.getOffset());
            wfToOffsets.add(wf.getOffset() + wf.getLength());
            sentWFs.add(wf);
            Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
            term.setPos("O");
            term.setLemma(token.getTokenValue());
            sentTerms.add(term);
          }
        }
        counter++;
        String[] tokenIds = new String[sentWFs.size()];
        for (int i = 0; i < sentWFs.size(); i++) {
          tokenIds[i] = sentWFs.get(i).getId();
        }
        //going through every opinion element for each sentence
        //each opinion element can contain one or more opinions
        Element aspectTermsElem = sent.getChild("aspectTerms");
        
        if (aspectTermsElem != null) {
          
          List<Element> aspectTermsList = aspectTermsElem.getChildren();
          //iterating over every opinion in the opinions element
          if (!aspectTermsList.isEmpty()) {
          for (Element aspectTerm : aspectTermsList) {
            String targetString = aspectTerm.getAttributeValue("term");
            System.err.println("-> " + targetString);
            //adding OTE
              int fromOffset = Integer.parseInt(aspectTerm
                    .getAttributeValue("from"));
              int toOffset = Integer.parseInt(aspectTerm
                    .getAttributeValue("to"));
              int startIndex = -1;
              int endIndex = -1;
              for (int i = 0; i < wfFromOffsets.size(); i++) {
                if (wfFromOffsets.get(i) == fromOffset) {
                  startIndex = i;
                }
              }
              for (int i = 0; i < wfToOffsets.size(); i++) {
                if (wfToOffsets.get(i) == toOffset) {
                  //span is +1 with respect to the last token of the span
                  endIndex = i + 1;
                }
              }
              if (startIndex != -1 && endIndex != -1) {
              List<String> wfIds = Arrays
                  .asList(Arrays.copyOfRange(tokenIds, startIndex, endIndex));
              List<String> wfTermIds = NAFUtils.getWFIdsFromTerms(sentTerms);
              if (NAFUtils.checkTermsRefsIntegrity(wfIds, wfTermIds)) {
                List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
                ixa.kaflib.Span<Term> neSpan = KAFDocument.newTermSpan(nameTerms);
                List<ixa.kaflib.Span<Term>> references = new ArrayList<ixa.kaflib.Span<Term>>();
                references.add(neSpan);
                Entity neEntity = kaf.newEntity(references);
                neEntity.setType("term");
              }
              }
          }
          }
        }
      }//end of sentence
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static String absa2014ToCoNLL2002(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    absa2014ToNAFNER(kaf, fileName, language);
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
  
  public static String nafToAbsa2014(String kafDocument) {

    KAFDocument kaf = null;
    try {
      Path kafPath = Paths.get(kafDocument);
      kaf = KAFDocument.createFromFile(kafPath.toFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
    Element sentencesElem = new Element("sentences");
    Document doc = new Document(sentencesElem);

    for (List<WF> sent : kaf.getSentences()) {
      String sentId = sent.get(0).getXpath();
      Integer sentNumber = sent.get(0).getSent();
      
      //getting text element from WFs in NAF
      String textString = NAFUtils.getSentenceStringFromWFs(sent);
      Element sentenceElem = new Element("sentence");
      sentenceElem.setAttribute("id", sentId);
      Element textElem = new Element("text");
      textElem.setText(textString);
      sentenceElem.addContent(textElem);
      
      //creating opinions element for sentence
      List<Opinion> opinionsBySentence = getOpinionsBySentence(kaf, sentNumber);
      if (!opinionsBySentence.isEmpty()) {
        Element aspectTerms = new Element("aspectTerms");
        //getting opinion info from NAF Opinion layer
        for (Opinion opinion : opinionsBySentence) {
          String polarity = "";
          String targetString = opinion.getStr();
          int fromOffset = opinion.getOpinionTarget().getTerms().get(0).getWFs().get(0).getOffset();
          List<WF> targetWFs = opinion.getOpinionTarget().getTerms().get(opinion.getOpinionTarget().getTerms().size() -1).getWFs();
          int toOffset = targetWFs.get(targetWFs.size() -1).getOffset() + targetWFs.get(targetWFs.size() -1).getLength();
          
          Element aspectTerm = new Element("aspectTerm");
          aspectTerm.setAttribute("term", targetString);
          aspectTerm.setAttribute("polarity", polarity);
          aspectTerm.setAttribute("from", Integer.toString(fromOffset));
          aspectTerm.setAttribute("to", Integer.toString(toOffset));
          aspectTerms.addContent(aspectTerm);
        }
        sentenceElem.addContent(aspectTerms);
      }
      sentencesElem.addContent(sentenceElem);
    }
    XMLOutputter xmlOutput = new XMLOutputter();
    Format format = Format.getPrettyFormat();
    xmlOutput.setFormat(format);
    return xmlOutput.outputString(doc);
  }
  
  public static void getYelpText(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    BufferedReader breader = new BufferedReader(Files.newBufferedReader(filePath, StandardCharsets.UTF_8));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        String text = (String) jsonObject.get("text");
        System.out.println(text);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
  }
}
