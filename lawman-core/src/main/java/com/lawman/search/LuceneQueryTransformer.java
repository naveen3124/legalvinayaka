/**
 * 
 */
package com.lawman.search;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.xml.CoreParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.IndexSearcher;
import org.xml.sax.InputSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;
import java.io.StringReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * @author naveen
 *
 */
public class LuceneQueryTransformer {
	private static final Logger logger = Logger.getLogger(LuceneQueryTransformer.class.getName());
	private final Analyzer analyzer;
	private final SearcherManager searcherManager;
	private static Path INDEX_DIRECTORY;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final Transformer transformer;
	static {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("linux")) {
			INDEX_DIRECTORY = Paths.get("indexingData");
		} else if (os.contains("windows")) {
			System.out.println("Running on Windows");
			INDEX_DIRECTORY = Paths.get("E:" + File.separator, "indexingData");
		}
		try {

			LogManager.getLogManager().readConfiguration(
					LuceneQueryTransformer.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			System.err.println("Could not load default logging.properties file");
			e.printStackTrace();
		}
	}
	public LuceneQueryTransformer(Directory indexDir) throws Exception {
		this.searcherManager  = new SearcherManager(indexDir, null);
		this.analyzer = new StandardAnalyzer();

		// Load XSLT stylesheet for transforming user input
		TransformerFactory tf = TransformerFactory.newInstance();
		StreamSource xslt = new StreamSource(getClass().getResourceAsStream("query-transform.xsl"));
		this.transformer = tf.newTransformer(xslt);
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	}

	public SearcherManager getSearchManager() {
		return searcherManager;
	}
	public Query transformQuery(String userInput) throws IOException, ParseException {
		// Transform user input with XSLT stylesheet
		StringWriter result = new StringWriter();
		try {
			transformer.transform(new StreamSource(new StringReader(userInput)), new StreamResult(result));
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Parse transformed query using Lucene's query parser
		QueryParser parser = new QueryParser("caseJudgements", analyzer);
		System.out.println(result.toString());
		// Return the parsed query
		return parser.parse(result.toString());
	}

	public String executeQuery(String userInput) throws IOException, ParseException {
		// Transform user input into a Lucene query
		Query query = transformQuery(userInput);
		// Execute the query using the SearcherManager
		IndexSearcher searcher = searcherManager.acquire();
		StringBuilder stringBuilder = new StringBuilder();
		ArrayNode jsonArray = objectMapper.createArrayNode();

		try {
			TopDocs hits = searcher.search(query, 10);
			StoredFields storedFields = searcher.storedFields();
			for (ScoreDoc hit : hits.scoreDocs) {
				Document doc = storedFields.document(hit.doc);
				ObjectNode jsonDocument = objectMapper.createObjectNode();
				jsonDocument.put("Document ID", hit.doc);

				ObjectNode fieldMap = objectMapper.createObjectNode();
				for (IndexableField field : doc.getFields()) {
					fieldMap.put(field.name(), field.stringValue());
				}
				jsonDocument.set("Fields", fieldMap);
				jsonArray.add(jsonDocument);
			}
			try {
				// Convert documentMap to JSON string and append to the StringBuilder
				stringBuilder.append(objectMapper.writeValueAsString(jsonArray));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		} finally {
			searcherManager.release(searcher);
		}
		System.out.println("anveen " + stringBuilder.toString());
		return stringBuilder.toString();
	}

	public void closeResources() {
		try {
			searcherManager.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String searchKeywords(String xmlString) {
		try {
			return executeQuery(xmlString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Keyword Not Found!!";

	}

	public static void main(String[] args) {
		try {
			logger.info("Application started");

			// Create the DirectoryReader
			Directory indexDirectory = FSDirectory.open(INDEX_DIRECTORY);
			// Create the SearcherManager with a SearcherFactory
			LuceneQueryTransformer lqt = new LuceneQueryTransformer(indexDirectory);
			String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><query><userInput>cases related to murder</userInput></query>";
			lqt.executeQuery(xmlString);
			lqt.searcherManager.close();
			indexDirectory.close();
			logger.info("Application ended");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
