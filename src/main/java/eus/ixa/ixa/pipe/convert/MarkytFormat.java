package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class MarkytFormat {

  private MarkytFormat() {
  }

  private static void barrToNAFNER(KAFDocument kaf, String docName,
      String entitiesFile, String language) throws IOException {
    // 100005->T#63#64#GLOBAL
    ListMultimap<String, String> entitiesMap = getEntitiesMap(entitiesFile);
    // reading the document file
    List<String> docs = Files.readAllLines(Paths.get(docName));
    // naf sentence counter
    int counter = 1;
    for (String doc : docs) {
      String[] docArray = doc.split("\t");
      List<Integer> wfFromOffsetsTitle = new ArrayList<>();
      List<Integer> wfToOffsetsTitle = new ArrayList<>();
      List<Integer> wfFromOffsetsAbstract = new ArrayList<>();
      List<Integer> wfToOffsetsAbstract = new ArrayList<>();
      List<WF> sentWFs = new ArrayList<>();
      List<Term> sentTerms = new ArrayList<>();
      // docId and original text
      String docId = docArray[0];
      String titleString = docArray[2];
      String abstractString = docArray[3];

      List<List<Token>> tokenizedTitle = StringUtils
          .tokenizeSentence(titleString, language);
      // adding tokens from Title
      for (List<Token> sentence : tokenizedTitle) {
        for (Token token : sentence) {
          WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
              counter);
          int toOffset = wf.getOffset() + wf.getLength();
          wf.setXpath(docId + "#" + "T");
          final List<WF> wfTarget = new ArrayList<WF>();
          wfTarget.add(wf);
          wfFromOffsetsTitle.add(wf.getOffset());
          wfToOffsetsTitle.add(toOffset);
          sentWFs.add(wf);
          Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
          term.setPos("O");
          term.setLemma(token.getTokenValue());
          sentTerms.add(term);
        }
      }
      // update the NAF sentence counter after each title
      counter++;
      List<List<Token>> tokenizedAbstract = StringUtils
          .tokenizeDocument(abstractString, language);
      // adding tokens from Abstract
      for (List<Token> sentence : tokenizedAbstract) {
        for (Token token : sentence) {
          WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
              counter);
          int toOffset = wf.getOffset() + wf.getLength();
          wf.setXpath(docId + "#" + "A");
          final List<WF> wfTarget = new ArrayList<WF>();
          wfTarget.add(wf);
          wfFromOffsetsAbstract.add(wf.getOffset());
          wfToOffsetsAbstract.add(toOffset);
          sentWFs.add(wf);
          Term term = kaf.newTerm(KAFDocument.newWFSpan(wfTarget));
          term.setPos("O");
          term.setLemma(token.getTokenValue());
          sentTerms.add(term);
        }
        // update the NAF sentence counter after each sentence in abstract
        counter++;
      }
      String[] tokenIds = new String[sentWFs.size()];
      for (int i = 0; i < sentWFs.size(); i++) {
        tokenIds[i] = sentWFs.get(i).getId();
      }
      List<String> entityValues = entitiesMap.get(docId);
      System.err.println("-> DocId: " + docId);
      // System.err.println(entityValues);
      for (String entity : entityValues) {
        // 100005->T#63#64#GLOBAL
        String[] entityAttributes = entity.split("#");
        // processing entities in Titles
        if (entityAttributes[0].equalsIgnoreCase("T")) {
          int fromOffset = Integer.parseInt(entityAttributes[1]);
          int toOffset = Integer.parseInt(entityAttributes[2]);
          // System.err.println("-> TitlefromOffset: " + fromOffset);
          // System.err.println("-> TitletoOffset: " + toOffset);
          int startIndex = -1;
          int endIndex = -1;
          for (int i = 0; i < wfFromOffsetsTitle.size(); i++) {
            if (wfFromOffsetsTitle.get(i) == fromOffset) {
              startIndex = i;
            }
          }
          for (int i = 0; i < wfToOffsetsTitle.size(); i++) {
            if (wfToOffsetsTitle.get(i) == toOffset) {
              // span is +1 with respect to the last token of the span
              endIndex = i + 1;
            }
          }
          // TODO remove this condition to correct manually offsets
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
              neEntity.setType(entityAttributes[3]);
            }
          }
        } // processing entities in Abstract
        else if (entityAttributes[0].equalsIgnoreCase("A")) {
          int fromOffset = Integer.parseInt(entityAttributes[1]);
          int toOffset = Integer.parseInt(entityAttributes[2]);
          // System.err.println("-> AbstractfromOffset: " + fromOffset);
          // System.err.println("-> AbstracttoOffset: " + toOffset);
          int startIndex = -1;
          int endIndex = -1;
          for (int i = 0; i < wfFromOffsetsAbstract.size(); i++) {
            if (wfFromOffsetsAbstract.get(i) == fromOffset) {
              startIndex = i + wfFromOffsetsTitle.size();
              // System.err.println("-> startIndex: " + startIndex);
            }
          }
          for (int i = 0; i < wfToOffsetsAbstract.size(); i++) {
            if (wfToOffsetsAbstract.get(i) == toOffset) {
              // span is +1 with respect to the last token of the span
              endIndex = (i + 1) + wfToOffsetsTitle.size();
              // System.err.println("-> endIndex: " + endIndex);
            }
          }
          // TODO remove this condition to correct offsets manually
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
              neEntity.setType(entityAttributes[3]);
            }
          }
        }
      }
    } // end of document
  }

  private static ListMultimap<String, String> getEntitiesMap(
      String entitiesFile) throws IOException {
    ListMultimap<String, String> entitiesMap = ArrayListMultimap.create();
    // going through every entity marked in the entities file
    List<String> entitiesAnnotations = Files
        .readAllLines(Paths.get(entitiesFile));
    for (String entity : entitiesAnnotations) {
      String[] entityArray = entity.split("\t");
      String entityDocId = entityArray[0];
      String docType = entityArray[2];
      String type = entityArray[5];
      String[] offsets = entityArray[3].split(":");
      // 100005->T#63#64#GLOBAL
      String valueMap = docType + "#" + offsets[0] + "#" + offsets[1] + "#"
          + type;
      entitiesMap.put(entityDocId, valueMap);
    }
    return entitiesMap;
  }

  public static String barrToCoNLL2002(String docName, String entitiesFile,
      String language) throws IOException {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    barrToNAFNER(kaf, docName, entitiesFile, language);
    // System.out.println(kaf.toString());
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }

  public static String barrToWFs(String docName, String language)
      throws IOException {
    KAFDocument kaf = new KAFDocument("es", "v1.naf");
    // reading the document file
    List<String> docs = Files.readAllLines(Paths.get(docName));
    // naf sentence counter
    int counter = 1;
    for (String doc : docs) {
      String[] docArray = doc.split("\t");
      // docId and original text
      String docId = docArray[0];
      String titleString = docArray[2];
      String abstractString = docArray[3];

      List<List<Token>> tokenizedTitle = StringUtils
          .tokenizeSentence(titleString, language);
      // adding tokens from Title
      for (List<Token> sentence : tokenizedTitle) {
        for (Token token : sentence) {
          WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
              counter);
          wf.setXpath(docId + "#" + "T");
        }
      }
      // update the NAF sentence counter after each title
      counter++;
      List<List<Token>> tokenizedAbstract = StringUtils
          .tokenizeDocument(abstractString, language);
      // adding tokens from Abstract
      for (List<Token> sentence : tokenizedAbstract) {
        for (Token token : sentence) {
          WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
              counter);
          wf.setXpath(docId + "#" + "A");
        }
        // update the NAF sentence counter after each sentence in abstract
        counter++;
      }
    }
    return kaf.toString();
  }

  public static String nafToBARREntities(String inputNAF) throws IOException {
    // DOCUMENT_ID SECTION INIT END ANNOTATED_TEXT TYPE
    // 72280 A 207 211 TDAH SHORT
    StringBuilder sb = new StringBuilder();
    Path kafPath = Paths.get(inputNAF);
    KAFDocument kaf = KAFDocument.createFromFile(kafPath.toFile());
    List<Entity> entities = kaf.getEntities();
    for (Entity entity : entities) {
      String type = entity.getType();
      String annotation = entity.getStr();
      int fromOffset = entity.getTerms().get(0).getWFs().get(0).getOffset();
      List<WF> targetWFs = entity.getTerms().get(entity.getTerms().size() - 1)
          .getWFs();
      int toOffset = targetWFs.get(targetWFs.size() - 1).getOffset()
          + targetWFs.get(targetWFs.size() - 1).getLength();
      // 100005#T
      String xpath = entity.getTerms().get(0).getWFs().get(0).getXpath();
      String[] xpathElems = xpath.split("#");
      String section = xpathElems[1];
      String document = xpathElems[0];
      sb.append(document).append("\t").append(section).append("\t")
          .append(fromOffset).append("\t").append(toOffset).append("\t")
          .append(annotation).append("\t").append(type).append("\n");
    }
    return sb.toString().trim();
  }
}
