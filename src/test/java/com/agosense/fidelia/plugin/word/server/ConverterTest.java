package com.agosense.fidelia.plugin.word.server;

import com.google.common.io.Resources;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConverterTest {

    @Test
    public void testConverter() throws ParserConfigurationException, SAXException, IOException {

        Path input = Paths.get(Resources.getResource("FideliaQuickStart.docx").getFile());
        Converter.instance().convert(input);
    }
}
