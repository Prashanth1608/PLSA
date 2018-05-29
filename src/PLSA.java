import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import cc.mallet.types.MatrixOps;

/**
 * Implementation of the PLSA/PLSI model, as described in:
 * 
 * Thomas Hofmann. 1999. Probabilistic Latent Semantic Indexing. In Proceedings
 * of the 22nd Annual International SIGIR Conference on Research and Development
 * in Information Retrieval.
 * 
 * @author Dat Quoc Nguyen
 */

public class PLSA {
	final String folderPath;
	// The id-based corpus converted from the original word-based corpus
	List<List<Integer>> corpus;
	// The list to store the topic associating to every word in the whole corpus
	List<List<Integer>> topicAssignments;
	// The number of document in the corpus
	int numDocuments;
	// The vocabulary to map a word to its id.
	HashMap<String, Integer> word2IdVocabulary;
	// The vocabulary to retrieve a word based on its id.
	HashMap<Integer, String> id2WordVocabulary;
	// The number of unique words in the vocabulary
	int vocaSize;

	int numTopics;

	HashMap<Integer, HashMap<Integer, Integer>> wordDocCounter;
	HashMap<Integer, HashMap<Integer, Integer>> docWordCounter;
	HashMap<Integer, HashMap<Integer, Double>> docWordPros;

	public double[][] topicWordPros;
	public double[][] docTopicPros;

	public double[] sumExpTopicWordParas;
	public double[] sumExpDocTopicParas;

	// public double[] topicPros;

	public String pathToTrainingCorpus;

	public int numTopWords = 50;

	public String suffix;

	public String corpusStrName;

	public int diffL1Index;

	int numWordsInCorpus = 0;

	public PLSA(String pathToCorpus, int inNumTopics, String inSuffix) throws Exception {
		numTopics = inNumTopics;

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();

		suffix = inSuffix;

		// Get the path to folder containing the input training corpus
		folderPath = pathToCorpus.substring(0,
				Math.max(pathToCorpus.lastIndexOf("/"), pathToCorpus.lastIndexOf("\\")) + 1);

		corpusStrName = pathToCorpus
				.substring(Math.max(pathToCorpus.lastIndexOf("/"), pathToCorpus.lastIndexOf("\\")) + 1);

		pathToTrainingCorpus = pathToCorpus;

		// Map words and Ids, create id-based
		System.out.println("Reading corpus: " + pathToCorpus);
		corpus = new ArrayList<List<Integer>>();

		numDocuments = 0;
		int indexWord = -1;

		try (BufferedReader br = new BufferedReader(new FileReader(pathToCorpus))) {
			for (String doc; (doc = br.readLine()) != null;) {
				doc = doc.trim();
				if (doc.length() == 0)
					continue;
				String[] words = doc.split("\\s+");

				// Id-based document
				List<Integer> document = new ArrayList<Integer>();

				for (String word : words) {
					if (word2IdVocabulary.containsKey(word)) {
						document.add(word2IdVocabulary.get(word));
					} else {
						indexWord += 1;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord, word);
						document.add(indexWord);
					}
				}
				numDocuments++;
				corpus.add(document);
				numWordsInCorpus += document.size();
			}
		}

		vocaSize = word2IdVocabulary.size();
		docWordCounter = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();

			HashMap<Integer, Integer> wordCounter = new HashMap<Integer, Integer>();
			for (int i = 0; i < docSize; i++) {
				// Get current wordID
				int word = corpus.get(dIndex).get(i);
				int times = 0;
				if (wordCounter.containsKey(word))
					times = wordCounter.get(word);
				times += 1;
				wordCounter.put(word, times);
			}
			docWordCounter.put(dIndex, wordCounter);
		}

		wordDocCounter = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (int d : docWordCounter.keySet()) {
			for (int w : docWordCounter.get(d).keySet()) {
				HashMap<Integer, Integer> hashW = new HashMap<Integer, Integer>();
				if (wordDocCounter.containsKey(w))
					hashW = wordDocCounter.get(w);
				hashW.put(d, docWordCounter.get(d).get(w));
				wordDocCounter.put(w, hashW);
			}
		}

		// if (wordDocCounter.get(100).get(101) != docWordCounter.get(101)
		// .get(100))
		// try {
		// throw new Exception();
		// }
		// catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		docWordPros = new HashMap<Integer, HashMap<Integer, Double>>();

		topicWordPros = new double[numTopics][vocaSize];
		docTopicPros = new double[numDocuments][numTopics];

		sumExpTopicWordParas = new double[numTopics];
		sumExpDocTopicParas = new double[numDocuments];

		System.out.println("Corpus size: " + numDocuments + " docs");
		System.out.println("Vocabuary size: " + vocaSize);
		System.out.println("Number of words per document: " + (numWordsInCorpus * 1.0 / numDocuments));
		System.out.println("Number of topics:  " + numTopics);

		// Initialize parameters...

		initialize();

		diffL1Index = numTopics * vocaSize;
	}

	private void initialize() throws Exception {
		System.out.println("Randomly initialzing parameters...");

		double[] multiPros = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

		int[][] docTopicCount = new int[numDocuments][numTopics];
		int[][] topicWordCount = new int[numTopics][vocaSize];

		for (int i = 0; i < numDocuments; i++) {
			int docSize = corpus.get(i).size();
			// docTopicSum[i] = docSize;
			for (int j = 0; j < docSize; j++) {
				// Sample topic
				int topic = FuncUtils.nextDiscrete(multiPros);

				// Increase counts
				docTopicCount[i][topic] += 1;
				topicWordCount[topic][corpus.get(i).get(j)] += 1;

			}
		}

		for (int d = 0; d < numDocuments; d++) {
			for (int t = 0; t < numTopics; t++) {
				docTopicPros[d][t] = (docTopicCount[d][t] + 0.1);

			}
			MatrixOps.normalize(docTopicPros[d]);
		}

		for (int t = 0; t < numTopics; t++) {
			for (int w = 0; w < vocaSize; w++) {
				topicWordPros[t][w] = (topicWordCount[t][w] + 0.01);
			}
			MatrixOps.normalize(topicWordPros[t]);
		}

		// System.out.println("Init log likelihood: " + getValue());
		if (new Double(getValue()).isNaN())
			throw new Exception();
	}

	public int getNumParameters() {
		// TODO Auto-generated method stub
		return numTopics * (vocaSize + numDocuments);
	}

	public double getValue() {
		double value = 0.0;

		for (int d = 0; d < numDocuments; d++)
			for (int w : docWordCounter.get(d).keySet()) {
				double temp = 0.0;
				for (int z = 0; z < numTopics; z++) {
					temp += topicWordPros[z][w] * docTopicPros[d][z];
				}
				value += docWordCounter.get(d).get(w) * Math.log(temp);
			}

		return value;
	}

	public void computePwd() throws Exception {

		for (int d = 0; d < numDocuments; d++) {
			HashMap<Integer, Double> wordPros = new HashMap<Integer, Double>();
			for (int w : docWordCounter.get(d).keySet()) {
				double temp = 0.0;
				for (int z = 0; z < numTopics; z++) {
					temp += topicWordPros[z][w] * docTopicPros[d][z];
				}
				wordPros.put(w, temp);
			}
			docWordPros.put(d, wordPros);
		}
	}

	public void train(int nIters) {
		System.out.println("Number of iterations: " + nIters);
		for (int iter = 0; iter < nIters; iter++) {
			try {
				computePwd();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>> nProZDW = new HashMap<Integer, HashMap<Integer, HashMap<Integer, Double>>>();

			for (int z = 0; z < numTopics; z++) {
				HashMap<Integer, HashMap<Integer, Double>> nProZDWz = new HashMap<Integer, HashMap<Integer, Double>>();
				for (int d = 0; d < numDocuments; d++) {
					HashMap<Integer, Double> nProZDWd = new HashMap<Integer, Double>();
					for (int w : docWordCounter.get(d).keySet())
						nProZDWd.put(w, docWordCounter.get(d).get(w) * topicWordPros[z][w] * docTopicPros[d][z]
								/ docWordPros.get(d).get(w));
					nProZDWz.put(d, nProZDWd);
				}
				nProZDW.put(z, nProZDWz);
			}

			for (int z = 0; z < numTopics; z++) {
				double temp = 0.0;
				for (int w = 0; w < vocaSize; w++)
					for (int d : wordDocCounter.get(w).keySet()) {
						temp += nProZDW.get(z).get(d).get(w);
					}

				for (int w = 0; w < vocaSize; w++) {
					double temp1 = 0.0;
					for (int d : wordDocCounter.get(w).keySet()) {
						temp1 += nProZDW.get(z).get(d).get(w);
					}
					topicWordPros[z][w] = temp1 / temp;
				}
			}

			for (int d = 0; d < numDocuments; d++)
				for (int z = 0; z < numTopics; z++) {
					double tempdz = 0.0;
					for (int w : docWordCounter.get(d).keySet()) {
						tempdz += nProZDW.get(z).get(d).get(w);
					}
					docTopicPros[d][z] = tempdz / corpus.get(d).size();
				}

			System.out.println("Iteration " + iter + ": " + getValue());
		}
	}

	public void writeDocTopicPro() throws Exception {
		DecimalFormat df = new DecimalFormat("#.#######");
		df.setRoundingMode(RoundingMode.CEILING);

		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + suffix + ".PLSA.theta"));

		for (int d = 0; d < numDocuments; d++) {
			for (int z = 0; z < numTopics; z++) {
				writer.write(df.format(docTopicPros[d][z]) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopLikelyWords() throws Exception {
		// System.out.println("Getting most likely words...");

		BufferedWriter writer = new BufferedWriter(
				new FileWriter(folderPath + suffix + ".PLSA.top" + numTopWords + "Words"));

		for (int z = 0; z < numTopics; z++) {
			writer.write("Topic" + new Integer(z) + ":");

			// Get counts of number of times a word assigned to current topic
			Map<Integer, Double> wordCount = new TreeMap<Integer, Double>();
			for (int w = 0; w < vocaSize; w++) {
				wordCount.put(w, topicWordPros[z][w]);
			}
			// Sort the counts by value descending
			wordCount = FuncUtils.sortByValueDescending(wordCount);

			// Get most likely words
			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < numTopWords) {
					// writer.write("\t" + id2WordVocabulary.get(index) + "\t"
					// + wordCount.get(index) + "/" + topicWordSumLDA[tIndex]
					// + "\n");
					writer.write(" " + id2WordVocabulary.get(index));
					count += 1;
				} else {
					writer.write("\n\n");
					break;
				}
			}
		}
		writer.close();
	}

	public void writeCorpus() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(pathToTrainingCorpus + ".IDcorpus"));

		for (int d = 0; d < numDocuments; d++) {
			int docSize = docWordCounter.get(d).keySet().size();
			writer.write(docSize + " ");
			for (int w : docWordCounter.get(d).keySet())
				writer.write(w + ":" + docWordCounter.get(d).get(w) + " ");
			writer.write("\n");
		}

		writer.close();
	}

	public void writeVoca() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(pathToTrainingCorpus + ".Voca"));

		for (int i = 0; i < vocaSize; i++) {
			writer.write(id2WordVocabulary.get(i) + "\n");
		}

		writer.close();
	}

	public static void main(String[] args) {
		CmdArgs cmdArgs = new CmdArgs();
		CmdLineParser parser = new CmdLineParser(cmdArgs);
		try {
			parser.parseArgument(args);
			PLSA plsatest = new PLSA(cmdArgs.corpus, cmdArgs.ntopics, cmdArgs.expModelName);
			// plsatest.writeCorpus();
			// plsatest.writeVoca();
			plsatest.train(cmdArgs.niters);
			plsatest.writeDocTopicPro();
			plsatest.writeTopLikelyWords();

		} catch (CmdLineException cle) {
			System.out.println("Error: " + cle.getMessage());
			help(parser);
			return;
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			help(parser);
			return;
		}
	}

	public static void help(CmdLineParser parser) {
		System.out.println("java -jar jar/PLSA.jar [options ...] [arguments...]");
		parser.printUsage(System.out);
	}
}
