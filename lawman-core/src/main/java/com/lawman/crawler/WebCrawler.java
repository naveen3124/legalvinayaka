package com.lawman.crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawler {

	private Set<String> visitedUrls;
	private List<String> urlsToVisit;
	private String outputDir;
	private Set<Integer> completedUrls;

	public static boolean containsDocWithNumber(String url) {
		Pattern pattern = Pattern.compile("/doc/\\d+");
		Matcher matcher = pattern.matcher(url);
		return matcher.find();
	}

	public WebCrawler(String outputDir) {
		visitedUrls = new HashSet<String>();
		urlsToVisit = new LinkedList<String>();
		this.outputDir = outputDir;
		completedUrls = getIndexCompletedFiles();
		
	}
    public boolean checkAlreadyDone(String startUrl) {
    	String[] splits = startUrl.split("/");
    	if (splits.length > 0) {
    		try {
    			int number = Integer.parseInt(splits[splits.length - 1]);
    			return completedUrls.contains(number);
    		} catch (NumberFormatException e) {
    		}
    	}
    	return true;
    }
	public void crawl(String startUrl) throws Exception {
		if (!checkAlreadyDone(startUrl)) {
			urlsToVisit.add(startUrl);
		}
		
		while (!urlsToVisit.isEmpty()) {
			String url = urlsToVisit.remove(0);
			if (!visitedUrls.contains(url)) {
				try {
					if (url.endsWith("#")) {
						continue;
					}
					System.out.println("Fetching " + url);
					Document doc = Jsoup.connect(url).get();
					visitedUrls.add(url);
					Elements elements = doc.select("div.judgments");
					if (elements.isEmpty()) {
						continue;
					}
					downloadHtml(url, doc);
					Elements links = doc.select("a[href]");
					for (Element link : links) {
						String linkUrl = link.absUrl("href");
						if (linkUrl.startsWith("https") && !visitedUrls.contains(linkUrl)
								&& containsDocWithNumber(linkUrl)) {
							if (!checkAlreadyDone(linkUrl)) {
								urlsToVisit.add(linkUrl);
							}
						}
					}
				} catch (IOException e) {
					System.out.println("Error fetching " + url + ": " + e.getMessage());
					throw e;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Error fetching " + url + ": " + e.getMessage());
					throw e;
				}
			} else {
				break;
			}
		}
	}

	private void downloadHtml(String url, Document doc) {
		try {
			String fileName = url.replaceAll("[^a-zA-Z0-9]", "_");
			String filePath = outputDir + File.separator + fileName;
			FileWriter writer = new FileWriter(filePath);
			writer.write(doc.html());
			writer.close();
			File input = new File(filePath);
			Document doc1 = Jsoup.parse(input, "UTF-8");
			System.out.println("Downloaded " + doc1.select("div.doc_title"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Set<Integer> getIndexCompletedFiles() {
		File directory = new File("E:" + File.separator, "indexingDataCompleted");
		Set<Integer> numbersSet = new HashSet<>();

		// Check if the specified path is a directory
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();

			if (files != null) {
				for (File file : files) {
					if (file.isFile()) {
						String filename = file.getName();
						String[] splits = filename.split("_");

						if (splits.length > 0) {
							try {
								
								int number = Integer.parseInt(splits[splits.length - 1]);
								numbersSet.add(number);
							} catch (NumberFormatException e) {
							}
						}
					}
				}
			}
		}
		System.out.println(numbersSet.size());
		return numbersSet;
	}

	public static void main(String[] args) {

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
		String lastDownloadedDocumentNo = "output.txt"; 
		File file = new File(lastDownloadedDocumentNo);
		int counter = 0;

		// check if file exists
		if (file.exists()) {
			try {
				// read number from file
				Scanner scanner = new Scanner(file);
				counter = scanner.nextInt();
				System.out.println("The number in the file is: " + counter);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("File does not exist.");
		}
		if (counter == 0) {
			counter = 1;
		}
		WebCrawler crawler = new WebCrawler(crawlOutputDir.toString());
		long startTime = System.currentTimeMillis();
		long timeLimit = 3 * 60 * 60 * 1000;
		String startUrl = "https://indiankanoon.org/doc/1/";
		String[] pathSegments;
		try {
			URI uri = new URI(startUrl);
			pathSegments = uri.getPath().split("/");
			pathSegments[pathSegments.length - 1] = Integer.toString(counter);
			startUrl = uri.resolve(String.join("/", pathSegments)).toString();
		} catch (URISyntaxException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		while (System.currentTimeMillis() - startTime < timeLimit) {
			try {
				crawler.crawl(startUrl);
				URI uri = new URI(startUrl);
				pathSegments = uri.getPath().split("/");
				pathSegments[pathSegments.length - 1] = Integer.toString(++counter);
				startUrl = uri.resolve(String.join("/", pathSegments)).toString();
			} catch (Exception e) {
				e.printStackTrace();
				long timestill = System.currentTimeMillis() - startTime;
				System.out.println("the time still is " + timestill );
				System.out.println("the failure is " + e.getMessage());
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(lastDownloadedDocumentNo))) {
			writer.write(String.valueOf(counter));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Crawling Done");
	}
}
