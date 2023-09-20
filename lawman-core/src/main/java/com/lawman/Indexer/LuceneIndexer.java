package com.lawman.Indexer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lawman.search.LuceneQueryTransformer;

public class LuceneIndexer {
	private static final Logger logger = Logger.getLogger(LuceneQueryTransformer.class.getName());
	private static final Tika tika = new Tika();
	private static Map<String, String[]> multiMap = new HashMap<>();

	// Static initializer block to populate the map with some initial values
	static {
		multiMap.put("title", new String[] { "div.doc_title", "case_title" });
		multiMap.put("jurisdiction", new String[] { "div.docsource_main", "case_jurisdiction" });
		multiMap.put("citations", new String[] { "div.cite_title", "case_citations" });
		multiMap.put("judgments", new String[] { "div.judgments", "case_judgment" });
		multiMap.put("bench", new String[] { "div.doc_bench", "case_judges" });
		multiMap.put("author", new String[] { "div.doc_author", "case_author" });
	}

	public static String getValue(String key, int index) {
		String[] values = multiMap.get(key);
		if (values == null || index < 0 || index >= values.length) {
			return null;
		}
		return values[index];
	}

	static {
		try {
			LogManager.getLogManager()
					.readConfiguration(LuceneIndexer.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			System.err.println("Could not load default logging.properties file");
			e.printStackTrace();
		}
	}

	public int index(String indexDir, File dataDir, String suffix) throws IOException {
		IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
		Path indexPath = Paths.get(indexDir);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(indexPath), config);
		int fileCtr = 0;
		try {
			// perform indexing
			indexDirectory(indexWriter, dataDir, suffix);
			indexWriter.commit();
		} catch (Exception e) {
			e.printStackTrace();
			// in case of an exception, rollback any changes
			indexWriter.rollback();
		} finally {
			// NOTE - Dont forget to close the index writer
			// fileCtr = indexWriter.numDocs();
			indexWriter.close();
		}
		return fileCtr;
	}
	
	void moveToCompleted(File sourceFile) {
		   String CompletedDir = "E:\\indexingDataCompleted";

		   File destinationDirectory = new File(CompletedDir);
           if (!destinationDirectory.exists()) {
               destinationDirectory.mkdirs();
           }

           // Move the file to the destination directory
           File destinationFile = new File(CompletedDir, sourceFile.getName());
           try {
			Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
	 * Recursively index files in a directory.
	 * 
	 * @param indexWriter
	 * @param dataDir
	 * @param suffix
	 * @throws IOException
	 */
	private void indexDirectory(IndexWriter indexWriter, File dataDir, String suffix) throws IOException {
		File[] files = dataDir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				indexDirectory(indexWriter, file, suffix);
			} else {
				indexFileWithIndexWriter(indexWriter, file, suffix);
				indexWriter.commit();
				moveToCompleted(file);
			}
		}
	}

	private void indexFileWithIndexWriter(IndexWriter indexWriter, File file, String suffix) throws IOException {
		if (file.isHidden() || file.isDirectory() || !file.canRead() || !file.exists()) {
			return;
		}
		if (suffix != null && !file.getName().endsWith(suffix)) {
			return;
		}
		System.out.println("Indexing file : " + file.getCanonicalPath());
		Document document = new Document();
		org.jsoup.nodes.Document doc = Jsoup.parse(file, "UTF-8");

		Optional<Element> optionalElement = Optional.ofNullable(doc.selectFirst(getValue("title", 0)));

		String caseTitle = (String) optionalElement.map(element -> {
			String elementContent = element.text();
			element.remove();
			return elementContent;
		}).orElse("");

		optionalElement = Optional.ofNullable(doc.selectFirst(getValue("jurisdiction", 0)));
		String caseJurisdiction = (String) optionalElement.map(element -> {
			String elementContent = element.text();
			element.remove();
			return elementContent;
		}).orElse("");

		optionalElement = Optional.ofNullable(doc.selectFirst(getValue("author", 0)));
		String caseAuthor = (String) optionalElement.map(element -> {
			String elementContent = element.text();
			element.remove();
			return elementContent;
		}).orElse("");

		optionalElement = Optional.ofNullable(doc.selectFirst(getValue("bench", 0)));
		String caseJudges = (String) optionalElement.map(element -> {
			String elementContent = element.text();
			element.remove();
			return elementContent;
		}).orElse("");

		optionalElement = Optional.ofNullable(doc.selectFirst(getValue("judgments", 0)));
		String caseJudgements = (String) optionalElement.map(element -> {
			String elementContent = element.text();
			element.remove();
			return elementContent;
		}).orElse("");

		// Get the div elements with the names "doc_title", "docsource_main", and
		// "cite_title"
		Elements citeTitleDivs = doc.select(getValue("citations", 0));
		// Find the <pre> element with ID "pre_1" inside the judgmentsDiv
		// Element preElement = judgmentsDiv.selectFirst("pre#pre_1");
		doc.select("div.ad_doc").remove();

		for (Element div : citeTitleDivs) {
			//System.out.println(div.text());
			//citeTitleDivs.remove();
		}

		class CustomAnalyzer extends Analyzer {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				Tokenizer tokenizer = new StandardTokenizer();
				TokenStream tokenStream = new LowerCaseFilter(tokenizer);
				return new TokenStreamComponents(tokenizer, tokenStream);
			}
		}
		System.out.println("naveen " + caseTitle);

		document.add(new StringField("fileName", file.getName(), Field.Store.YES));
		// create a new document and add a field with the custom analyzer
		Field jurisdictionField = new TextField("jurisdiction", caseJurisdiction, Field.Store.YES);
		jurisdictionField.setTokenStream(new CustomAnalyzer().tokenStream("jurisdiction", caseJurisdiction));
		document.add(jurisdictionField);
		document.add(new StringField("caseTitle", caseTitle, Field.Store.YES));
		document.add(new StringField("caseAuthor", caseAuthor, Field.Store.YES));
		document.add(new StringField("caseJurisdiction", caseJurisdiction, Field.Store.YES));
		document.add(new Field("caseJudgements", caseJudgements, TextField.TYPE_NOT_STORED));
		for (String name : caseJudges.split(";")) {
			document.add(new TextField("caseJudges", name, Field.Store.YES));
		}
		indexWriter.addDocument(document);

	}
	

	public static void main(String[] args) {
		try {
			Path crawlOutputDir = null;
			String dirPath = "caseLawsRegistry";
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("linux")) {
				System.out.println("Running on Linux");
				crawlOutputDir = Paths.get("mnt", "e", dirPath);
			} else if (os.contains("windows")) {
				System.out.println("Running on Windows");
				crawlOutputDir = Paths.get("E:" + File.separator, "caseLawsRegistry");
			} else {
				System.out.println("Unknown operating system");
				return;
			}

			logger.info("Indexing  started");
			// Your application code here
			LuceneIndexer indexer = new LuceneIndexer();
			String indexDir = "E:\\indexingData";

			File dataDirPath = new File(crawlOutputDir.toString());
			indexer.index(indexDir, dataDirPath, null);
			logger.info("Indexing ended successfully");
			/*
			QueryParser queryParser = new QueryParser("jurisdiction", new CustomAnalyzer());
			Query query = queryParser.parse("example jurisdiction");
			*/

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}