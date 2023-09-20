package com.lawman.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.lawman.search.LuceneQueryTransformer;

import php.java.bridge.http.JavaBridgeRunner;

public class Lawman_Main {
	public static final String JAVABRIDGE_PORT="8087";
	static final JavaBridgeRunner runner = JavaBridgeRunner.getInstance(JAVABRIDGE_PORT);
	private LuceneQueryTransformer lqt;
	private Directory indexDirectory;
	
	public Lawman_Main() throws Exception {
		String os = System.getProperty("os.name").toLowerCase();
		Path INDEX_DIRECTORY = Paths.get("/");
		try {
			if (os.contains("linux")) {
				INDEX_DIRECTORY = Paths.get("indexingData");
			} else {
				throw new Exception("Lawman runs only in the linux");
			}
			indexDirectory = FSDirectory.open(INDEX_DIRECTORY);
			this.lqt = new LuceneQueryTransformer(indexDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	public static void main(String args[]) throws Exception {
		//Lawman_Main lm = new Lawman_Main();
		runner.waitFor();
		//lm.cleanUp();
		System.exit(0);
	}
	
	public String hello(String xmlString) throws Exception {
		System.out.println(xmlString);
		return lqt.executeQuery(xmlString);
	}
	
	public void cleanUp () {
		lqt.closeResources();
		try {
			indexDirectory.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
