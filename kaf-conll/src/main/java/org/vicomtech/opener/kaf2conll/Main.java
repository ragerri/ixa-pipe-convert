package org.vicomtech.opener.kaf2conll;

import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Span;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.List;


/**
 * This class translates KAF format to CONLL format
 * It has been designed for Bootstrapping NER in OpeNER, but
 * could be used in other tasks.
 * @author mcuadros
 *
 */
public class Main {
	
	private boolean printEntites = true;
	
	private File inputDir  = null;
	private File outputDir = null;
	
	private static final String PROPER_NOUN = "R";
	
	private static final String KAF_EXT   = ".kaf";
	private static final String CONLL_EXT = ".conll";
	
	private static class KAFFilter implements FilenameFilter {
		@Override
		public boolean accept(File arg0, String arg1) {
			return arg1.endsWith(KAF_EXT);
		}
	}
	
	private static enum ExitStatus {
		SUCCESS(0), ERROR(-1);
		int value;
		ExitStatus(int i) {
			value = i;
		}
	}
	
	/**
	 * Enumeration class for ConLL 2003 BIO format
	 */
	private static enum BIO {
		BEGIN("B-"), IN("I-"), OUT("O");
		String tag;
		BIO(String tag) {
			this.tag = tag;
		}
		public String toString() {
			return this.tag;
		}
	}
	
	/**
	 * Static main method,
	 * Input KAF file as stream, Output CONLL file.
	 * @param args		input arguments
	 */
	public static void main(String[] args) {
		Main prog = new Main();
		try {
			prog.execute(args);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * Processes application and executes
	 * @param args		application arguments
	 * @throws FileNotFoundException 
	 */
	private void execute(String[] args) throws FileNotFoundException {
		checkArguments(args);
		if (this.inputDir != null && this.outputDir != null) {
			execute(this.inputDir, this.outputDir);
		}
		else {
			execute(System.in, System.out);
		}
	}
	
	/**
	 * Processes KAF files at input directory and puts output CONLL files
	 * at output directory.
	 * @param inputDir		KAF files input directory
	 * @param outputDir		CONLL files output directory
	 * @throws FileNotFoundException 
	 */
	public void execute(File inputDir, File outputDir) throws FileNotFoundException {
		for (File file : inputDir.listFiles(new KAFFilter())) {
			File outFile = getOutFile(file, outputDir);
			InputStream inStream   = new FileInputStream(file);
			OutputStream outStream = new FileOutputStream(outFile);
			this.execute(inStream, outStream);
		}
	}
	
	private File getOutFile(File inFile, File outputDir) {
		String inFilename = inFile.getName();
		String outFilename = inFilename + CONLL_EXT;
		String outFile = outputDir.getAbsolutePath()+File.separatorChar+outFilename;
		return new File(outFile);
	}
	
	/**
	 * Input KAF file as stream, Output CONLL file.
	 * @param inStream		KAF input stream
	 * @param outStream		CONLL output stream
	 */
	public void execute(InputStream inStream, OutputStream outStream) {
		
		/**
		 * Creation of buffers for input/output
		 */
		BufferedWriter bwriter = null;

		try {
			/**
			 * Inicialization of buffers
			 */
			bwriter = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
			
			// Inicialization of kaf variable as KAFDocument

			KAFDocument kaf = KAFDocument.createFromStream(new InputStreamReader(inStream,
					"UTF-8"));
			
			//Create list with named entities

			List<Entity> entities = kaf.getEntities();
			
			//Creation of two hashes for quicky access entities
			
			Hashtable<String, Integer> entities_number = new Hashtable<String, Integer>();
			Hashtable<String, String> entities_type = new Hashtable<String, String>();

			for (Entity entity_var : entities) {
				List<Span<Term>> entitySpans = entity_var.getSpans();
				for (Span<Term> sterm : entitySpans) {
					Term t = sterm.getFirstTarget();				
					entities_number.put(t.getId(), sterm.size());
					entities_type.put(t.getId(), entity_var.getType());
				}
			}

			List<List<WF>> sentences = kaf.getSentences();
			
			for (List<WF> sentence : sentences) {
				int index = 1;
				
				int sentenceNumber = sentence.get(0).getSent();

				List<Term> sentenceTerms = kaf.getSentenceTerms(sentenceNumber);

				// Go through sentences and terms, to print out number of token, term, lemma, nerc, id (not-used), type of entity
				boolean lastIsEntity = false;
				for (int i = 0; i < sentenceTerms.size(); i++) {
					
					Term t = sentenceTerms.get(i);

					if (entities_number.get(t.getId()) != null) {
						int number = entities_number.get(t.getId());
						String type = entities_type.get(t.getId());

						if (number > 1) { // is entity with more than 1 term
							for (int j = 0; j < number; j++) {
								t = sentenceTerms.get(i + j);
								
								//bwriter.write(Integer.toString(index));
								//bwriter.write("\t");
								bwriter.write(t.getForm());
								bwriter.write(" ");
								bwriter.write(t.getLemma());
								bwriter.write(" ");
								bwriter.write(t.getPos());
								bwriter.write(" ");
								//bwriter.write(Integer.toString(1));
								//bwriter.write(" ");

								if (this.printEntites) {
									if (j == 0 && lastIsEntity)
										bwriter.write(BIO.BEGIN.toString());
									else
										bwriter.write(BIO.IN.toString());
									bwriter.write(type.toUpperCase());
								}
								
								bwriter.newLine();
								index++;
							}
						}
						else { // is entity with 1 term
							//bwriter.write(Integer.toString(index));
							//bwriter.write("\t");
							bwriter.write(t.getForm());
							bwriter.write(" ");
							bwriter.write(t.getLemma());
							bwriter.write(" ");
							bwriter.write(PROPER_NOUN);
							//bwriter.write(" ");
							//bwriter.write(Integer.toString(1));
							
							if (this.printEntites) {
								bwriter.write(" ");
								if (lastIsEntity)
									bwriter.write(BIO.BEGIN.toString());
								else
									bwriter.write(BIO.IN.toString());
								bwriter.write(type.toUpperCase());
							}
							
							bwriter.newLine();
							index++;
						}
						
						lastIsEntity = true;
						i += number - 1;	
						
					} else { // is not an entity
						//bwriter.write(Integer.toString(index));
						//bwriter.write("\t");
						bwriter.write(t.getForm());
						bwriter.write(" ");
						bwriter.write(t.getLemma());
						bwriter.write(" ");
						bwriter.write(t.getPos());
						//bwriter.write(" ");
						//bwriter.write(Integer.toString(1));
						
						if (this.printEntites) {
							bwriter.write(" ");
							bwriter.write(BIO.OUT.toString());
						}
						
						bwriter.newLine();
						index++;
						lastIsEntity = false;
					}
				} // end of sentence

				bwriter.newLine();
			} // all sentences processed

			bwriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	/**
//	 * Input KAF file as stream, Output CONLL file.
//	 * @param inStream		KAF input stream
//	 * @param outStream		CONLL output stream
//	 */
//	public void execute(InputStream inStream, OutputStream outStream) {
//		
//		/**
//		 * Creation of buffers for input/output
//		 */
//		BufferedWriter bwriter = null;
//
//		try {
//			/**
//			 * Inicialization of buffers
//			 */
//			bwriter = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
//			
//			// Inicialization of kaf variable as KAFDocument
//
//			KAFDocument kaf = KAFDocument.createFromStream(new InputStreamReader(inStream,
//					"UTF-8"));
//			
//			//Create list with named entities
//
//			List<Entity> entities = kaf.getEntities();
//			
//			//Creation of two hashes for quicky access entities
//			
//			Hashtable<String, Integer> entities_number = new Hashtable<String, Integer>();
//			Hashtable<String, String> entities_type = new Hashtable<String, String>();
//
//			for (Entity entity_var : entities) {
//				List<Span<Term>> entitySpans = entity_var.getSpans();
//				for (Span<Term> sterm : entitySpans) {
//					Term t = sterm.getFirstTarget();				
//					entities_number.put(t.getId(), sterm.size());
//					entities_type.put(t.getId(), entity_var.getType());
//				}
//			}
//
//			List<List<WF>> sentences = kaf.getSentences();
//			
//			for (List<WF> sentence : sentences) {
//				int index = 1;
//				
//				int sentenceNumber = sentence.get(0).getSent();
//
//				List<Term> sentenceTerms = kaf.getSentenceTerms(sentenceNumber);
//
//				// Go through sentences and terms, to print out number of token, term, lemma, nerc, id (not-used), type of entity
//				for (int i = 0; i < sentenceTerms.size(); i++) {
//					
//					bwriter.write(Integer.toString(index));
//					bwriter.write("\t");
//					Term t = sentenceTerms.get(i);
//					String new_term_form = t.getForm();
//					String new_term_lemma = t.getLemma();
//
//					if (entities_number.get(t.getId()) != null) {
//						int number = entities_number.get(t.getId());
//						String type = entities_type.get(t.getId());
//
//						if (number > 1) {
//							for (int j = 1; j < number; j++) {
//								// if (i + j < sentenceTerms.size()) {
//								t = sentenceTerms.get(i + j);
//								new_term_form = new_term_form.concat("_")
//										.concat(t.getForm());
//								new_term_lemma = new_term_lemma.concat("_")
//										.concat(t.getLemma());
//								// }
//								// else {
//								//
//								// }
//							}
//						}
//						i += number - 1;
//
//						bwriter.write(new_term_form);
//						bwriter.write("\t");
//						bwriter.write(new_term_lemma);
//						bwriter.write("\t");
//
//						bwriter.write(PROPER_NOUN);
//						bwriter.write("\t");
//						bwriter.write(Integer.toString(1));
//						bwriter.write("\t");
//
//						bwriter.write("ne=");
//						bwriter.write(type);
//					} else {
//						bwriter.write(new_term_form);
//						bwriter.write("\t");
//						bwriter.write(new_term_lemma);
//						bwriter.write("\t");
//						bwriter.write(t.getPos());
//						bwriter.write("\t");
//						bwriter.write(Integer.toString(1));
//						bwriter.write("\t");
//					}
//
//					bwriter.newLine();
//					index++;
//
//				}
//
//				bwriter.newLine();
//			}
//
//			bwriter.close();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Checks application arguments.
	 * @param args		input arguments
	 */
	private void checkArguments(String[] args) {
		boolean error = false;
		boolean help = false;
		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help")) {
				help = true;
				break;
			}
			else if (arg.equalsIgnoreCase("-ne")) {
				String ne = args[++i];
				if (ne.equalsIgnoreCase("0"))
					this.printEntites = false;
				else if (ne.equalsIgnoreCase("1"))
					this.printEntites = true;
				else
					this.printEntites = Boolean.parseBoolean(ne);
			}
			else if (arg.equalsIgnoreCase("-id")) {
				String id = args[++i];
				this.inputDir = new File(id);
				if (this.inputDir.exists()) {
					if (!this.inputDir.isDirectory()) {
						System.err.println(String.format("input file '%s' is not a directory", id));
						help = true;
						error = true;
						break;
					}
				}
				else {
					System.err.println(String.format("input file '%s' does not exists", id));
					help = true;
					error = true;
					break;
				}
			}
			else if (arg.equalsIgnoreCase("-od")) {
				String od = args[++i];
				this.outputDir = new File(od);
				if (this.outputDir.exists()) {
					if (!this.outputDir.isDirectory()) {
						System.err.println(String.format("output file '%s' is not a directory", od));
						help = true;
						error = true;
						break;
					}
				}
				else {
					System.err.println(String.format("output file '%s' does not exists", od));
					help = true;
					error = true;
					break;
				}
			}
			else {
				System.err.println(String.format("unknown argument: %s", arg));
				help = true;
				error = true;
				break;
			}
		}
		
		if (!help && !error) {
			if (this.inputDir != null && this.outputDir == null) {
				System.err.println(String.format("output file required"));
				help = true;
				error = true;
			}
			else if (this.inputDir == null && this.outputDir != null) {
				System.err.println(String.format("input file required"));
				help = true;
				error = true;
			}
		}
		
		if (help) {
			printUsage();
			if (error) {
				System.exit(ExitStatus.ERROR.value);
			}
			else {
				System.exit(ExitStatus.SUCCESS.value);
			}
		}
		
	}
	
	/**
	 * Prints application help message.
	 */
	private void printUsage() {
		System.err.println("Translates KAF format to CONLL format.");
		System.err.println();
		System.err.println("USAGE:   java -jar kaf-conll-0.0.1-SNAPSHOT.jar [OPTIONS...]");
		System.err.println("Reads text from standard input and writes result at standard output.");
		System.err.println();
		System.err.println("  OPTIONS:");
		System.err.println("    -h, --help       shows this help");
		System.err.println("    -ne boolean,     print entities column or not, default value is 'true'");
		System.err.println("    -id inputDir,    reads KAF documents from the input directory");
		System.err.println("    -od outputDir,   prints KAF documents into the output directory");
	}
}

