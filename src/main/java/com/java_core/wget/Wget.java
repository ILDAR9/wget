package com.java_core.wget;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.validator.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Wget {
	private static final Logger logger = LoggerFactory.getLogger(Wget.class);
	private static final String regexFile = "^[^/\\:*?<>|]+$";
	private static final String storePath = "wget_downloads";
	private static final String fileSiteId = "last_site_ID";
	private String urlString;
	private File imgFolder;

	public Wget(String urlString) throws MalformedURLException {
		if (urlString != null) {
			if (!urlString.startsWith("h")) {
				urlString = "http://" + urlString;
			}
			if (isValidURL(urlString)) {
				this.urlString = urlString;
				logger.debug("Creating an instance of WGET utility");
				return;
			} else {
				throw new MalformedURLException();
			}
		}
		throw new NullPointerException();
	}

	private boolean isValidURL(String urlString) {
		String[] schemes = { "http", "https" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		return (urlValidator.isValid(urlString));
	}

	private boolean isValidFileName(String fileName) {
		return fileName.matches(regexFile);
	}

	private File createFolder(URL urlParser) throws MalformedURLException {
		String folderName = urlParser.getHost();
		File folder = new File(storePath, folderName);
		if (!folder.getParentFile().exists()) {
			folder.getParentFile().mkdir();
		}
		folder.mkdir();
		return folder;
	}

	private String getFileName(URL urlParser) {
		// returns filename with extension, if not exists then folder
		// and in the last case hostname
		String path = urlParser.getFile();
		if (path != null && !path.isEmpty() && !path.endsWith("/")) {
			path = path.replace("\\", "/");
			int idx = path.lastIndexOf("/");
			if (idx >= 0) {
				String fileName = path.substring(idx + 1);
				if (!isValidFileName(fileName)) {
					fileName = fileName.replaceAll("[/\\:*?<>|]+", "");
				}
				if (fileName.contains(".")) {
					return fileName;
				}
			}
		}
		int siteId = readLastImgId();
		saveLastImgID(siteId + 1);
		return siteId + ".html";
	}

	public void downloadPage() {
		logger.debug("{} is valid url address.", urlString);
		logger.debug("Downloading html response from {}", urlString);
		try {
			Document doc = Jsoup.connect(urlString).get();
			logger.debug("html is downloaded from {}", urlString);
			// ...creating folder
			URL urlParser = new URL(urlString);
			File folder = createFolder(urlParser);
			imgFolder = new File(folder, "img");
			if (!imgFolder.exists()) {
				imgFolder.mkdir();
			}
			for (Element imgSrc : doc.select("img[src]")) {
				logger.info(" * img: <{}> {}x{} ({})", imgSrc.attr("abs:src"), imgSrc.attr("width"),
						imgSrc.attr("height"), trim(imgSrc.attr("alt"), 20));
				saveImage(imgSrc);
			}
			// ...loading repaired page
			String htmlFileName = getFileName(urlParser);
			File output = new File(folder, htmlFileName);
			output.createNewFile();
			logger.debug("...saving html response from {} to {}", urlString, output.getAbsolutePath());
			try (PrintWriter writer = new PrintWriter(output, "UTF-8")) {
				writer.write(doc.html());
			}
			logger.info("{} is saved to folder {}", htmlFileName, folder);
		} catch (MalformedURLException ex) {
			logger.error("{} : no legal protocol could be found or the string could not be parsed", urlString);
		} catch (UnknownHostException ex) {
			logger.info("Probably you are not connected to the Internet, please connect and try again");
			logger.error("UnknownHostException caught while attempting to find host", ex);
		} catch (SocketException ex) {
			logger.error("SocketException exception caught while attempting to connect to host", ex);
		} catch (EOFException ex) {
			logger.info("Probably your firewall is blocking operation");
			logger.error("EOFException exception caught while attepting to download from host", ex);
		} catch (IOException ex) {
			logger.error("IOException exception caught while attempting to create file", ex);
		}
	}

	private String getImgName(String url) {
		int idx = (url = url.replace("\\", "/")).lastIndexOf("/");
		String imgName = idx >= 0 ? url.substring(idx + 1) : url;
		if (!isValidFileName(imgName)) {
			String imgNameTemp = imgName.replaceAll("[/\\:*?<>|]+", "");
			logger.debug("file {} was renamed to {}", imgName, imgNameTemp);
			return imgNameTemp;
		}
		return imgName;
	}

	private String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}

	private void saveImage(Element imgSrc) {
		// Extract the name of the image from the src attribute
		String src = imgSrc.attr("abs:src");
		String imgName = getImgName(src);
		File imgFile = new File(imgFolder, imgName);
		// Open a URL Stream
		try {
			URL url = new URL(src);
			try (InputStream in = url.openStream();
					OutputStream out = new BufferedOutputStream(new FileOutputStream(imgFile));) {
				for (int b; (b = in.read()) != -1;) {
					out.write(b);
				}
				imgSrc.attr("src", "img\\" + imgName);
			}
		} catch (MalformedURLException ex) {
			logger.error("{} is not valid URI address", src);
		} catch (IOException ex) {
			logger.error("IOException exception caught while attempting to download from {} to {}", src, imgFile, ex);
		}
	}

	private int readLastImgId() {
		File file = new File(storePath, fileSiteId);
		try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
			int siteID = Integer.parseInt(reader.readLine());
			return siteID;
		} catch (FileNotFoundException ex) {
			logger.debug("first nameless html page will be named as 1.html");
		} catch (IOException ex) {
			logger.error("IOException exception caught while attempting to open file '{}\\{}'", storePath, fileSiteId,
					ex);
		} catch (java.lang.NumberFormatException ex) {
			logger.error(
					"NumberFormatException exception caught while attempting to parse siteId value from file '{}\\{}'",
					storePath, fileSiteId, ex);
		}
		return 1;
	}

	private void saveLastImgID(int siteId) {
		File file = new File(storePath, fileSiteId);
		try {
			try (PrintWriter writer = new PrintWriter(file);) {
				writer.print(siteId);
			}
		} catch (IOException ex) {
			logger.error("IOException exception caught while attempting to open file '{}\\{}'", storePath, fileSiteId,
					ex);
		}

	}
}
