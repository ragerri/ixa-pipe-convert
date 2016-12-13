package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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

import eus.ixa.ixa.pipe.ml.tok.RuleBasedTokenizer;
import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Opinion;
import ixa.kaflib.Opinion.OpinionExpression;
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

  private static void absa2015ToNAFOpinion(KAFDocument kaf, String fileName) {
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
        List<List<Token>> segmentedSentence = tokenizeSentence(sentString);
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
            //polarity and category are always specified
            //TODO add polarity
            String polarity = opinion.getAttributeValue("polarity");
            String category = opinion.getAttributeValue("category");
            String targetString = opinion.getAttributeValue("target");
            
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
              List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
              ixa.kaflib.Span<Term> oteSpan = KAFDocument.newTermSpan(nameTerms);
              Opinion opinionLayer = kaf.newOpinion();
              opinionLayer.createOpinionTarget(oteSpan);
              //TODO expression span, perhaps heuristic around ote?
              OpinionExpression opExpression = opinionLayer.createOpinionExpression(oteSpan);
              opExpression.setPolarity(polarity);
              opExpression.setSentimentProductFeature(category);
            }
          }
        }
      }//end of sentence
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static String absa2015ToNAF(String fileName) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    absa2015ToNAFOpinion(kaf, fileName);
    return kaf.toString();
  }
  
  private static void absa2015ToNAFNER(KAFDocument kaf, String fileName) {
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
        List<List<Token>> segmentedSentence = tokenizeSentence(sentString);
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
              List<String> wfTermIds = getWFIdsFromTerms(sentTerms);
              if (checkTermsRefsIntegrity(wfIds, wfTermIds)) {
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
  
  public static String absa2015ToCoNLL2002(String fileName) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    absa2015ToNAFNER(kaf, fileName);
    String conllFile = Convert.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
  
  /**
   * Get all the WF ids for the terms contained in the KAFDocument.
   * @param kaf the KAFDocument
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
   * Check that the references from the entity spans are
   * actually contained in the term ids.
   * @param wfIds the worform ids corresponding to the Term span
   * @param termWfIds all the terms in the document
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

  public static String absa2015ToWFs(String fileName) {
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
        List<List<Token>> segmentedSentences = tokenizeSentence(sentString);
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
  
  public static void absa2015Text(Reader reader) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(reader);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {
        String sentString = sent.getChildText("text");
        System.out.println(sentString);
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }

  public static void absa2014ToNAF(String fileName) {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      XPathExpression<Element> expr = xFactory.compile("//sentence",
          Filters.element());
      List<Element> sentences = expr.evaluate(doc);
      for (Element sent : sentences) {

        StringBuilder sb = new StringBuilder();
        String sentString = sent.getChildText("text");
        sb = sb.append(sentString);
        Element aspectTerms = sent.getChild("aspectTerms");
        String tokenizedSentence = null;
        if (aspectTerms != null) {
          List<List<Integer>> offsetList = new ArrayList<List<Integer>>();
          List<Integer> offsets = new ArrayList<Integer>();
          List<Element> aspectTermList = aspectTerms.getChildren();
          if (!aspectTermList.isEmpty()) {
            for (Element aspectElem : aspectTermList) {
              Integer offsetFrom = Integer.parseInt(aspectElem
                  .getAttributeValue("from"));
              Integer offsetTo = Integer.parseInt(aspectElem
                  .getAttributeValue("to"));
              offsets.add(offsetFrom);
              offsets.add(offsetTo);
            }
          }
          Collections.sort(offsets);
          for (int i = 0; i < offsets.size(); i++) {
            List<Integer> offsetArray = new ArrayList<Integer>();
            offsetArray.add(offsets.get(i++));
            if (offsets.size() > i) {
              offsetArray.add(offsets.get(i));
            }
            offsetList.add(offsetArray);
          }
          int counter = 0;
          for (List<Integer> offsetSent : offsetList) {
            Integer offsetFrom = offsetSent.get(0);
            Integer offsetTo = offsetSent.get(1);
            String aspectString = sentString.substring(offsetFrom, offsetTo);
            sb.replace(offsetFrom + counter, offsetTo + counter,
                "<START:term> " + aspectString + " <END>");
            counter += 19;
          }
          tokenizedSentence = getStringFromTokens(sb.toString());
          tokenizedSentence = tokenizedSentence.replaceAll(
              "<\\s+START\\s+:\\s+term\\s+>", "<START:term>");
          tokenizedSentence = tokenizedSentence.replaceAll("<\\s+END\\s+>",
              "<END>");
          System.out.println(tokenizedSentence);
        }
        // TODO make public getTokens() method in RuleBasedTokenizer!!
        //String tokenizedSentence = getStringFromTokens(sb.toString());
        //tokenizedSentence = tokenizedSentence.replaceAll(
        //    "<\\s+START\\s+:\\s+term\\s+>", "<START:term>");
        //tokenizedSentence = tokenizedSentence.replaceAll("<\\s+END\\s+>",
        //    "<END>");
        //System.out.println(tokenizedSentence);
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static String nafToATE(String kafDocument) {

    KAFDocument kaf = null;
    try {
      kaf = KAFDocument.createFromFile(new File(kafDocument));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Element sentencesElem = new Element("sentences");
    Document doc = new Document(sentencesElem);

    for (List<WF> sent : kaf.getSentences()) {
      StringBuilder sb = new StringBuilder();
      String sentId = sent.get(0).getXpath();
      for (int i = 0; i < sent.size(); i++) {
        sb = sb.append(sent.get(i).getForm()).append(" ");
      }
      Element sentenceElem = new Element("sentence");
      sentenceElem.setAttribute("id", sentId);
      Element textElem = new Element("text");
      textElem.setText(sb.toString().trim());
      sentenceElem.addContent(textElem);
      List<Entity> sentEntities = kaf.getEntitiesBySent(sent.get(0).getSent());

      if (!sentEntities.isEmpty()) {
        Element aspectTerms = new Element("aspectTerms");
        for (Entity entity : sentEntities) {
          // create and add opinion to the structure
          String polarity = "";
          String targetString = entity.getStr();
          int offsetFrom = entity.getTerms().get(0).getWFs().get(0).getOffset();
          List<WF> entWFs = entity.getTerms().get(entity.getTerms().size() - 1)
              .getWFs();
          int offsetTo = entWFs.get(entWFs.size() - 1).getOffset()
              + entWFs.get(entWFs.size() - 1).getLength();
          Element aspectTerm = new Element("aspectTerm");
          aspectTerm.setAttribute("term", targetString);
          aspectTerm.setAttribute("polarity", polarity);
          aspectTerm.setAttribute("from", Integer.toString(offsetFrom));
          aspectTerm.setAttribute("to", Integer.toString(offsetTo));
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
    BufferedReader breader = new BufferedReader(new FileReader(fileName));
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
  
  private static List<List<Token>> tokenizeSentence(String sentString) {
    RuleBasedTokenizer tokenizer = new RuleBasedTokenizer(sentString,
        setTokenizeProperties());
    List<String> sentenceList = new ArrayList<>();
    sentenceList.add(sentString);
    String[] sentences = sentenceList.toArray(new String[sentenceList.size()]);
    List<List<Token>> tokens = tokenizer.tokenize(sentences);
    return tokens;
  }

  private static Properties setTokenizeProperties() {
    Properties annotateProperties = new Properties();
    annotateProperties.setProperty("language", "en");
    annotateProperties.setProperty("normalize", "default");
    annotateProperties.setProperty("untokenizable", "no");
    annotateProperties.setProperty("hardParagraph", "no");
    return annotateProperties;
  }
  
  
  private static String getStringFromTokens(String sentString) {

    StringBuilder sb = new StringBuilder();
    List<List<Token>> tokens = tokenizeSentence(sentString);
    for (List<Token> sentence : tokens) {
      for (Token tok : sentence) {
        sb.append(tok.getTokenValue()).append(" ");
      }
    }
    return sb.toString();
  }
  
  
}
