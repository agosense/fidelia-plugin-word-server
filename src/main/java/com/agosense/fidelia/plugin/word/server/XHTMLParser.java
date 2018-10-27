package com.agosense.fidelia.plugin.word.server;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class XHTMLParser extends DefaultHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);

    private static final String TAG_P = "p";
    private static final String TAG_TABLE = "table";
    private static final String TAG_IMG = "img";

    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_SRC = "src";

    private static final String[] HEADING_NAMES = {
            "berschrift",
            "Heading"
    };

    private static final Stack<SaxStackElement> stack = new Stack<SaxStackElement>();

    private final transient Path outputFolder;

    private transient StringBuffer buffer;
    private transient List<String> attachments;
    private transient Boolean active = false;
    private transient Integer objectCount = 0;
    private transient String styleName = null;

    public XHTMLParser(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void startElement(
            String uri, String localName, String qName, Attributes attributes) throws SAXException {

        LOGGER.debug("entering startElement qName={}, stackSize={}", qName, stack.size());

        // If we are in active P we add the tags
        if (active) {
            appendCurrentElement(qName, attributes, outputFolder);
        }

        // We have a top-level paragraph
        if (stack.size() == 3 && (qName.equalsIgnoreCase(TAG_P) || qName
                .equalsIgnoreCase(TAG_TABLE))) {

            buffer = new StringBuffer();
            attachments = new ArrayList<String>();
            active = true;

            objectCount++;

            LOGGER.debug("found top level paragraph qName={}, objectCount={}", objectCount);

            if (attributes.getValue(ATTRIBUTE_CLASS) != null) {
                styleName = attributes.getValue(ATTRIBUTE_CLASS);
            }

            appendCurrentElement(qName, attributes, outputFolder);
        }

        stack.push(new SaxStackElement(qName, attributes));

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (active) {
            buffer.append(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        LOGGER.debug("entering endElement qName={}, stackSize={}", qName, stack.size());

        stack.pop();

        if (active) {
            buffer.append("</");
            buffer.append(qName);
            buffer.append(">");
        }

        if (stack.size() == 3 && (qName.equalsIgnoreCase(TAG_P) || qName
                .equalsIgnoreCase(TAG_TABLE)) && active) {

            try {
                File outputFile = new File(outputFolder.toString() + "/" + objectCount + ".content");
                Files.write(outputFile.toPath(), buffer == null ? "".getBytes() : buffer.toString().getBytes(),
                        StandardOpenOption.CREATE);

                if (!attachments.isEmpty()) {
                    outputFile = new File(outputFolder.toString() + "/" + objectCount + ".attachments");
                    Files.write(outputFile.toPath(), "".getBytes(), StandardOpenOption.CREATE);
                    for (String attachment : attachments) {
                        Files.write(outputFile.toPath(), (attachment + "\n").getBytes(), StandardOpenOption.APPEND);
                    }
                }

                outputFile = new File(outputFolder.toString() + "/" + objectCount + ".style");
                Files.write(outputFile.toPath(), styleName == null ? "".getBytes() : styleName.getBytes(),
                        StandardOpenOption.CREATE);

            } catch (IOException e) {
                throw new SAXException(e);
            }

            active = false;
            buffer = null;
            attachments = null;
            styleName = "";
        }
    }

    public Integer getObjectCount() {
        return objectCount;
    }

    private void appendCurrentElement(String qName, Attributes attributes, Path outputFolder) throws SAXException {

        if (qName.equalsIgnoreCase(TAG_IMG)) {

            File attachment = new File(attributes.getValue(ATTRIBUTE_SRC));
            String name = attachment.getName();
            attachments.add(outputFolder.toString() + "/word/media/" + name);

            buffer.append("<");
            buffer.append(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                buffer.append(" ");
                buffer.append(attributes.getQName(i));
                buffer.append("=\"");
                if (qName.equalsIgnoreCase(TAG_IMG) && attributes.getQName(i)
                        .equalsIgnoreCase(ATTRIBUTE_SRC)) {
                    buffer.append("agosense://" + name);
                } else {
                    buffer.append(attributes.getValue(i));
                }
                buffer.append("\"");
            }
            buffer.append(">");

        } else {
            buffer.append("<");
            buffer.append(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                buffer.append(" ");
                buffer.append(attributes.getQName(i));
                buffer.append("=\"");
                buffer.append(attributes.getValue(i));
                buffer.append("\"");
            }
            buffer.append(">");
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {

        if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(publicId)) {
            LOGGER.info("loading XHTML transitional");
            return new InputSource(Resources.getResource("xhtml1-transitional.dtd").openStream());
        }
        if ("-//W3C//ENTITIES Latin 1 for XHTML//EN".equals(publicId)) {
            LOGGER.info("loading XHTML latin 1");
            return new InputSource(Resources.getResource("xhtml-lat1.ent").openStream());
        }
        if ("-//W3C//ENTITIES Special for XHTML//EN".equals(publicId)) {
            LOGGER.info("loading XHTML special");
            return new InputSource(Resources.getResource("xhtml-special.ent").openStream());
        }
        if ("-//W3C//ENTITIES Symbols for XHTML//EN".equals(publicId)) {
            LOGGER.info("loading XHTML symbol");
            return new InputSource(Resources.getResource("xhtml-symbol.ent").openStream());
        }
        // This will avoid complains about missing DTDs in the temporary location
        return new InputSource(new StringReader(""));
    }
}
