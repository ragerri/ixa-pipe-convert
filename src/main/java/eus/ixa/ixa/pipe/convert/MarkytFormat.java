package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class MarkytFormat {

  private MarkytFormat() {
  }
  
  private static void markytToNAFNER(KAFDocument kaf, String docName, String entitiesFile, String language) throws IOException {
    //reading the document file
    List<String> docs = Files.readAllLines(Paths.get(docName));
      //naf sentence counter
      int counter = 1;
      for (String doc : docs) {
        String[] docArray = doc.split("\t");
        List<Integer> wfFromOffsets = new ArrayList<>();
        List<Integer> wfToOffsets = new ArrayList<>();
        List<WF> sentWFs = new ArrayList<>();
        List<Term> sentTerms = new ArrayList<>();
        //sentence id and original text
        String sentId = docArray[0];
        String lang = docArray[1];
        String titleString = docArray[2];
        String abstractString = docArray[3];
        //the list contains just one list of tokens
        List<List<Token>> tokenizedTitle = AbsaSemEval.tokenizeSentence(titleString, language);
        List<List<Token>> tokenizedAbstract = AbsaSemEval.tokenizeSentence(abstractString, language);
        for (List<Token> sentence : tokenizedTitle) {
          for (Token token : sentence) {
            WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(),
                counter);
            wf.setXpath(sentId + "#" + lang + "#" + "T");
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
        for (List<Token> sentence : tokenizedAbstract) {
          for (Token token : sentence) {
            WF wf = kaf.newWF(token.startOffset(), token.getTokenValue(), counter);
            wf.setXpath(sentId + "#" + lang + "#" + "A");
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
        //going through every entity marked in the entities file
        List<String> entitiesAnnotations = Files.readAllLines(Paths.get(entitiesFile));
        for (String entity : entitiesAnnotations) {
          String[] entityArray = entity.split("\t");
          String type = entityArray[5];
          String[] offsets = entityArray[3].split(":");
          int fromOffset = Integer.parseInt(offsets[0]);
          int toOffset = Integer.parseInt(offsets[1]);
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
          List<String> wfTermIds = AbsaSemEval.getWFIdsFromTerms(sentTerms);
          if (AbsaSemEval.checkTermsRefsIntegrity(wfIds, wfTermIds)) {
            List<Term> nameTerms = kaf.getTermsFromWFs(wfIds);
            ixa.kaflib.Span<Term> neSpan = KAFDocument.newTermSpan(nameTerms);
            List<ixa.kaflib.Span<Term>> references = new ArrayList<ixa.kaflib.Span<Term>>();
            references.add(neSpan);
            Entity neEntity = kaf.newEntity(references);
            neEntity.setType(type);
        }
      }
      }//end of sentence
  }
  
  public static String markytToCoNLL2002(String docName, String entitiesFile, String language) throws IOException {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    markytToNAFNER(kaf, docName, entitiesFile, language);
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
}
