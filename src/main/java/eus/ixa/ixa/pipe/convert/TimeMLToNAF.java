package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.util.List;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.FileDesc;
import ixa.kaflib.WF;

public class TimeMLToNAF {

  private TimeMLToNAF() {
  }
  
  private static void timeMLToNAFNER(KAFDocument kaf, String fileName, String language) {
    //reading the TimeML xml file
    SAXBuilder sax = new SAXBuilder();
    XPathFactory xFactory = XPathFactory.instance();
    try {
      Document doc = sax.build(fileName);
      Element rootElement = doc.getRootElement();
      XPathExpression<Element> timexExpr = xFactory.compile("//TIMEX3",
          Filters.element());
      List<Element> timexElems = timexExpr.evaluate(doc);
      XPathExpression<Element> eventExpr = xFactory.compile("//EVENT", Filters.element());
      
      //getting the Document Creation Time
      Element dctElement = rootElement.getChild("DCT");
      Element dctTimex = dctElement.getChild("TIMEX3");
      String dctTimexType = dctTimex.getAttributeValue("type");
      String dctTimexValue = dctTimex.getAttributeValue("value");
      String dctTimexText = dctTimex.getText();
      
      //getting the TEXT
      Element textElement = rootElement.getChild("TEXT");
      List<Content> textElements = textElement.getContent();
      //we need to iterate over single content of the text element
      //to get the text and the relevant attributes from TIMEX and
      //EVENT elements
      for (Content textElem : textElements) {
        
      }

      
        
      
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
}
