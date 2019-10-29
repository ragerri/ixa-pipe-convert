package eus.ixa.ixa.pipe.convert;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eus.ixa.ixa.pipe.ml.StatisticalDocumentClassifier;
import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;

public class HyperPartisan {
  
  private static final Pattern htmlPattern = Pattern.compile("<.*?>");
  private static boolean tokenize = False;
                                                                                                                                                                                                                                                                                                                                            
  private HyperPartisan() {
  }

  public static void hyperPartisanToTrainDoc(Path textXML, Path groundTruth) throws IOException {
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      //reading the articles content XML
      Document doc = sax.build(textXML.toFile());
      XPathExpression<Element> expr = xFactory.compile("//article",
          Filters.element());
      List<Element> articles = expr.evaluate(doc);
      //read the ground truth XML
      Document docTruth = sax.build(groundTruth.toFile());
      List<Element> articlesTruth = expr.evaluate(docTruth);
      //iterate over every article
      for (int i = 0; i < articles.size(); i++) {
        //obtain text using stringbuffer
      //obtain text Element with html tags inside
        XMLOutputter outp = new XMLOutputter();
        outp.setFormat(Format.getCompactFormat());
        StringWriter sw = new StringWriter();
        outp.output(articles.get(i).getContent(), sw);
        StringBuffer sb = sw.getBuffer();
        String documentText = htmlPattern.matcher(sb.toString()).replaceAll("");
        //obtain rest of elements
        String articleTruth = articlesTruth.get(i).getAttributeValue("hyperpartisan");
        String articleTitle = articles.get(i).getAttributeValue("title");
        String sourceUrl = articlesTruth.get(i).getAttributeValue("url");
        if (tokenize) {
          //tokenizing the document
          List<List<Token>> tokenizedContent = StringUtils
                  .tokenizeSentence(documentText + " " + articleTitle, "en");
          StringBuilder tokenizedText = new StringBuilder();
          for (List<Token> sentence : tokenizedContent) {
            for (Token token : sentence) {
              tokenizedText.append(token).append(" ");
            }
          }
          System.out.println(articleTruth + "\t" + tokenizedText + "\t" + sourceUrl);
        }
        else {
          System.out.println(articleTruth + "\t" + documentText + " " + articleTitle);
        }
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static String hyperPartisanToTest(Path textXML, String model) throws IOException {
    StringBuilder outputText = new StringBuilder();
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      //reading the articles content XML
      Document doc = sax.build(textXML.toFile());
      XPathExpression<Element> expr = xFactory.compile("//article",
          Filters.element());
      List<Element> articles = expr.evaluate(doc);
      //iterate over every article
      for (Element article : articles) {
        //obtain text Element with html tags inside
        XMLOutputter outp = new XMLOutputter();
        outp.setFormat(Format.getCompactFormat());
        StringWriter sw = new StringWriter();
        outp.output(article.getContent(), sw);
        StringBuffer sb = sw.getBuffer();
        String documentText = htmlPattern.matcher(sb.toString()).replaceAll("");
        //obtain rest of elements
        String articleId = article.getAttributeValue("id");
        String articleTitle = article.getAttributeValue("title");
        //tokenizing the document
        List<List<Token>> tokenizedContent = StringUtils
            .tokenizeSentence(documentText + " " + articleTitle, "en");
        StringBuilder tokenizedText = new StringBuilder();
        for (List<Token> sentence : tokenizedContent) {
          for (Token token : sentence) {
            tokenizedText.append(token).append(" ");
          }
        }
        outputText.append(articleId).append("\t").append(tokenizedText.toString()).append("\n");
      }
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    String predictionFile = annotateGeneralTest(outputText.toString(), model);
    return predictionFile;
  }
  
  private static String annotateGeneralTest(String inputText, String model)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    String[] documentLines = inputText.split("\n");
    Properties properties = setDocProperties(model, "en", "no");
    StatisticalDocumentClassifier docClassifier = new StatisticalDocumentClassifier(
        properties);
    for (String line : documentLines) {
      String[] lineArray = line.split("\t");
      String textId = lineArray[0];
      String[] document = lineArray[1].split(" ");
      String hyperClass = docClassifier.classify(document);
      sb.append(textId).append("\t").append(hyperClass).append("\n");
    }
    return sb.toString();
  }

  private static Properties setDocProperties(String model, String language,
      String clearFeatures) {
    Properties oteProperties = new Properties();
    oteProperties.setProperty("model", model);
    oteProperties.setProperty("language", language);
    oteProperties.setProperty("clearFeatures", clearFeatures);
    return oteProperties;
  }
}
