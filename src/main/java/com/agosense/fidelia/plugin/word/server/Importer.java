package com.agosense.fidelia.plugin.word.server;

import com.agosense.fidelia.rest.v1.ApiException;
import com.agosense.fidelia.rest.v1.FideliaApplicationApi;
import com.agosense.fidelia.rest.v1.NodeAttribute;
import com.agosense.fidelia.rest.v1.NodeCreate;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);
    private static final Importer INSTANCE = new Importer();

    public void runImport(
            String attributeId, String sheetId, String sheetVersion, String taskId, int totalCount,
            Path outputFolder, WebSocketChannel channel, FideliaApplicationApi api) throws ApiException, IOException {

        LOGGER.debug("Using output folder={}", outputFolder);

        NodeCreate node = new NodeCreate();
        NodeAttribute att = new NodeAttribute();
        att.setType(NodeAttribute.TypeEnum.RICHTEXT);
        att.setAttributeId(attributeId);

        for (int i = 0; i < totalCount; i++) {

            LOGGER.debug("Handling node={}", i);

            att.setValue(
                    readFile(
                            outputFolder + "/" + (i + 1) + ".content",
                            StandardCharsets.UTF_8
                    )
            );
            node.setReference("end");
            node.setLevel(checkLevel(i + 1, outputFolder));
            node.setTaskId(taskId);
            node.setNodeAttributes(Arrays.asList(att));

            LOGGER.debug("Adding node={} to fidelia", i);
            api.nodesAdd(sheetId, sheetVersion, "TENTATIVE", node);

            File attachmentFile = new File(outputFolder + "/" + (i + 1) + ".attachments");
            if (attachmentFile.exists() && !attachmentFile.isDirectory()) {
                String uri = api.getApiClient().getResponseHeaders().get(HttpHeaders.LOCATION).get(0);
                String nodeId = getIdFromUri(uri);
                BufferedReader br = new BufferedReader(new FileReader(attachmentFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("/");

                    LOGGER.debug("Adding attachment={} to node={} in fidelia", parts[parts.length - 1], i);
                    api.attachmentsPut(
                            sheetId,
                            sheetVersion,
                            "TENTATIVE",
                            nodeId,
                            parts[parts.length - 1],
                            taskId,
                            new File(line)
                    );
                }
            }
            if (channel != null) {
                float percentage = (float) (((i + 1) * 100) / totalCount);
                WebSockets.sendText(
                        "{" +
                                "\"state\":\"Importing\"," +
                                "\"completed\":" + Math.round(percentage) +
                                "}",
                        channel,
                        null
                );
            }
        }
    }

    public static Importer instance() {
        return INSTANCE;
    }

    private Long checkLevel(int i, Path outputFolder) throws IOException {

        String style = readFile(outputFolder + "/" + i + ".style", StandardCharsets.UTF_8);
        int styleNum = 0;

        // German documents
        if (style.contains("berschrift")) {
            styleNum = Integer.valueOf(style.substring(style.lastIndexOf("berschrift") + 10));
            LOGGER.debug("Found berschrift with level: {}", styleNum);
            // English documents
        } else if (style.contains("Heading")) {
            styleNum = Integer.valueOf(style.substring(style.lastIndexOf("Heading") + 7));
            LOGGER.debug("Found Heading with level: {}", styleNum);
        }

        switch (styleNum) {
            case 0:
                return null;
            case 1:
                return 0L;
            case 2:
                return 1L;
            case 3:
                return 2L;
            case 4:
                return 3L;
            case 5:
                return 4L;
            default:
                return 5L;
        }
    }

    private String readFile(String path, Charset encoding) throws IOException {

        LOGGER.debug("Reading file={}", path);

        byte[] encoded;
        encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private String getIdFromUri(String uri) {

        Integer idx = uri.lastIndexOf('/');
        return uri.substring(idx + 1);
    }

    private Importer() {
        // Static instance
    }
}
