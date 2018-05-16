package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.WF;

public class TimeMLToNAF {

  private static final Pattern eventPattern = Pattern.compile("(<EVENT.*?>)(.*?)(</EVENT>)");
  private static final Pattern timex3Pattern = Pattern.compile("<\tTIMEX3.*?type\t=\t\"\t(\\S+)\t\".*?>\t(.*?)\t<\t/TIMEX3\t>");
  private static final Pattern timexPattern = Pattern.compile("<TIMEX3.*?>(.*?)</TIMEX3>");
  private static final Pattern timexTokenizedPattern = Pattern.compile("<\\s+TIMEX3.*?>\\s+(.*?)\\s+<\\s+/TIMEX3\\s+>");
  private static final Pattern timexBeginPattern = Pattern.compile("(<\\s+TIMEX3.*?>)");
  private static final Pattern timexEndPattern = Pattern.compile("<\\s+/TIMEX3\\s+>");
  
  private TimeMLToNAF() {
  }
  
  private static void timeMLToNAFNER(KAFDocument kaf, String fileName, String language) {
    //reading the TimeML xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      Element rootElement = doc.getRootElement(); 
      //getting the Document Creation Time
      Element dctElement = rootElement.getChild("DCT");
      Element dctTimex = dctElement.getChild("TIMEX3");
      String dctTimexValue = dctTimex.getAttributeValue("value");
      kaf.createFileDesc().creationtime = dctTimexValue;
      //getting everything in the TEXT element
      Element textElement = rootElement.getChild("TEXT");
      List<Content> textElements = textElement.getContent();
      StringBuilder sb = new StringBuilder();
      StringWriter sw = new StringWriter();
      XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
      xmlOutput.output(textElements, sw);
      StringBuffer sbuffer = sw.getBuffer();
      String text = sbuffer.toString().trim();
      text = eventPattern.matcher(text).replaceAll("$2");
      //we do not use event elements
      List<List<Token>> tokens = StringUtils.tokenizeDocument(text, language);
      //iterate over the tokenized sentences
      for (List<Token> sentence : tokens) {
        String[] tokensArray = eus.ixa.ixa.pipe.ml.utils.StringUtils.convertListTokenToArrayStrings(sentence);
        String sentenceString = StringUtils.getStringFromTokens(tokensArray);
        sentenceString = convertSpaceToTabTimex(sentenceString);
        //System.err.println(sentenceString);
        String[] textArray = sentenceString.split(" ");
        for (int i = 0; i < textArray.length; i++) {
           System.err.println(textArray[i]);
          /*if (textArray[i].startsWith("<\tTIMEX3")) {
            String timexType = timex3Pattern.matcher(textArray[i]).replaceAll("$1").trim();
            String timexText = timex3Pattern.matcher(textArray[i]).replaceAll("$2").trim();
            //System.err.println(timexText);
            String[] timexExpression = timexText.split("\t");
            sb.append(timexExpression[0]).append("\t").append("B-").append(timexType).append("\n");
            //System.err.println(timexExpression[0] + "\t" + "B-" + timexType);
            if (timexExpression.length > 1) {
              for (int j = 1; i < timexExpression.length; j++) {
                sb.append(timexExpression[j]).append("\t").append("I-").append(timexType).append("\n");
              }
            }
          } else {
            sb.append(textArray[i]).append("\t").append("O").append("\n");
          }*/
      }
      } //System.out.print(sb.toString());
    } catch (JDOMException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public static String timeMLToCoNLL2002(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    timeMLToNAFNER(kaf, fileName, language);
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
  
  public static String timeMLToRawNAF(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    SAXBuilder sax = new SAXBuilder();
    try {
      Document doc = sax.build(fileName);
      Element rootElement = doc.getRootElement();
      //getting the Document Creation Time
      Element dctElement = rootElement.getChild("DCT");
      Element dctTimex = dctElement.getChild("TIMEX3");
      String dctTimexValue = dctTimex.getAttributeValue("value");
      kaf.createFileDesc().creationtime = dctTimexValue;
      //getting the TEXT
      Element textElement = rootElement.getChild("TEXT");
      String words = textElement.getValue(); 
      kaf.setRawText(words);
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    return kaf.toString();
  }
  
  private static String convertSpaceToTabTimex(String line) {
    final Matcher timexMatcher = timexTokenizedPattern.matcher(line);
    final StringBuffer sb = new StringBuffer();
    while (timexMatcher.find()) {
      timexMatcher.appendReplacement(sb,
          timexMatcher.group().replaceAll("\\s+", "\t"));
    }
    timexMatcher.appendTail(sb);
    line = sb.toString();
    return line;
  }
}
