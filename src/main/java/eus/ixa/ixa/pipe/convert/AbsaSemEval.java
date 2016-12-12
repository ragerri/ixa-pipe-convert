package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
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

  public static void absaSemEval2014ToNER(String fileName) {
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

  /**
   * Convert ABSA 2015-2016 format to NAF.
   * @param fileName
   */
  public static void absaSemEval2015ToNAF(String fileName) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
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
            Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
            term.setPos("O");
            term.setLemma(token.getTokenValue());
          }
        }
        counter++;
        //going through every opinion element for each sentence
        //each opinion element can contain one or more opinions
        Element opinionsElement = sent.getChild("Opinions");
        if (opinionsElement != null) {
          //for each opinion we keep the offsets in a list
          List<List<Integer>> offsetList = new ArrayList<List<Integer>>();
          List<String> categoryList = new ArrayList<>();
          List<String> polarityList = new ArrayList<>();
          //iterating over every opinion in the opinions element
          List<Element> opinionList = opinionsElement.getChildren();
          for (Element opinion : opinionList) {
            List<Integer> offsets = new ArrayList<>();
            //OTE class, also valid for E-A
            categoryList.add(opinion.getAttributeValue("category"));
            //adding offsets
            offsets.add(Integer.parseInt(opinion
                  .getAttributeValue("from")));
            offsets.add(Integer.parseInt(opinion
                  .getAttributeValue("to")));
            offsetList.add(offsets);
            polarityList.add(opinion.getAttributeValue("polarity"));
          }
        }
      }//end of sentence
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }

  public static String absa15testToNAFWFs(String fileName) {
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
  

  public static void absaSemEvalText(Reader reader) {
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
