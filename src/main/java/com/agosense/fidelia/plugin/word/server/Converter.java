package com.agosense.fidelia.plugin.word.server;

import org.apache.poi.xwpf.converter.core.FileImageExtractor;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);
    private static final String XHTML_DOCTYPE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"xhtml1-transitional.dtd\">";
    private static final Converter INSTANCE = new Converter();

    public Result convert(Path inputFile) throws IOException, ParserConfigurationException, SAXException {

        // Create a temporary folder
        Path outputFolder = Files.createTempDirectory("fideliaWordImport");
        Path outputFile = outputFolder.resolve("importResult.xhtml");

        // Load the DOCX
        LOGGER.debug("Loading DOCX document");
        XWPFDocument document;
        InputStream in = new FileInputStream(inputFile.toFile());
        document = new XWPFDocument(in);
        XHTMLOptions options = XHTMLOptions.create();
        options.setExtractor(new FileImageExtractor(outputFolder.toFile()));

        // Transform the DOCX to XHTML
        LOGGER.debug("Transforming DOCX to XHTML");
        OutputStream out;
        out = new FileOutputStream(outputFile.toFile());
        out.write(XHTML_DOCTYPE.getBytes(StandardCharsets.UTF_8));
        XHTMLConverter.getInstance().convert(document, out, options);
        out.close();
        LOGGER.info("XHTML output created, file={}, size={}", outputFile, outputFile.toFile().length());

        // Analyzing XHTML
        LOGGER.debug("Analyzing XHTML, extracting style and content");
        XHTMLParser parser = new XHTMLParser(outputFolder);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser p;
        p = spf.newSAXParser();
        p.parse(outputFile.toFile(), parser);

        return new Result(parser.getObjectCount(), outputFolder.toString());
    }

    public static Converter instance() {
        return INSTANCE;
    }

    private Converter() {
        // Static instance
    }

    class Result {
        private final Integer paragraphCount;
        private final String serverFolder;

        public Result(Integer paragraphCount, String serverFolder) {
            this.paragraphCount = paragraphCount;
            this.serverFolder = serverFolder;
        }

        public Integer getParagraphCount() {
            return paragraphCount;
        }

        public String getServerFolder() {
            return serverFolder;
        }
    }
}
