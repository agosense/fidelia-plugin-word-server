package com.agosense.fidelia.plugin.word.server;

import com.agosense.fidelia.rest.v1.ApiException;
import com.agosense.fidelia.rest.v1.FideliaApplicationApi;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

final class ServerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerUtils.class);
    private static final ServerUtils INSTANCE = new ServerUtils();

    // Handles the case in which the client requests the final import
    public void handleImport(WebSocketChannel channel, BufferedTextMessage message, WebSocketHttpExchange exchange) {

        LOGGER.debug("Receiving X-Forwarded-Proto={}", exchange.getRequestHeaders().get("X-Forwarded-Proto").get(0));
        LOGGER.debug("Receiving X-Forwarded-Server={}", exchange.getRequestHeaders().get("X-Forwarded-Server").get(0));
        LOGGER.debug("Receiving X-Forwarded-Port={}", exchange.getRequestHeaders().get("X-Forwarded-Port").get(0));
        LOGGER.debug("Receiving DOMAIN={}", exchange.getRequestHeaders().get("DOMAIN").get(0));

        String messageText = message.getData().toString();
        JSONParser parse = new JSONParser();
        try {

            LOGGER.info("Parsing websocket message: {}", message);
            JSONObject obj = (JSONObject) parse.parse(messageText);

            if (obj.get("reason") != null && obj.get("reason").equals("Import")) {

                FideliaApplicationApi api = RestApiManager.instance().create(
                        exchange.getRequestHeaders().get("X-Forwarded-Proto").get(0),
                        exchange.getRequestHeaders().get("X-Forwarded-Server").get(0),
                        exchange.getRequestHeaders().get("X-Forwarded-Port").get(0),
                        exchange.getRequestHeaders().get("DOMAIN").get(0),
                        exchange.getRequestHeaders().get("API-TOKEN").get(0));
                try {

                    LOGGER.info("Starting import!");
                    Importer.instance().runImport(
                            obj.get("attributeId").toString(),
                            obj.get("sheetId").toString(),
                            obj.get("sheetVersion").toString(),
                            obj.get("taskId").toString(),
                            Integer.valueOf(obj.get("paragraphCount").toString()),
                            Paths.get(obj.get("serverFolder").toString()),
                            channel,
                            api
                    );

                    LOGGER.info("Import finished!");
                    WebSockets.sendText(
                            "{\"state\":\"Import finished\"}",
                            channel,
                            null
                    );
                } catch (ApiException | IOException e) {

                    LOGGER.error("Error while importing into fidelia: {}", e);
                    WebSockets.sendText(
                            " {\"state\":\"Error\",\"message\":\"Error while importing to fidelia!\"}",
                            channel,
                            null
                    );
                }
            } else {

                LOGGER.error("Unexpected message received: {}", message);
                WebSockets.sendText(
                        "{\"state\":\"Error\", \"message\":\"Unexpected message received!\"}",
                        channel,
                        null
                );
            }

        } catch (ParseException e) {
            LOGGER.error("Error while parsing message: {}", e);
            WebSockets.sendText(
                    " {\"state\":\"Error\",\"message\":\"Cannot parse message!\"}",
                    channel,
                    null
            );
        }
    }

    // Handles the file upload and runs the XHTML conversion
    public void handleUpload(HttpServerExchange exchange) {

        FormData attachment = exchange.getAttachment(FormDataParser.FORM_DATA);
        FormData.FormValue fileValue = attachment.get("file").getFirst();
        File uploadedFile = attachment.get("file").getFirst().getPath().toFile();
        Path file = fileValue.getPath();

        LOGGER.info("Upload finished, converting file!");

        try {
            Converter.Result result = Converter.instance().convert(file);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(
                    "{" +
                            "\"paragraphCount\":" + result.getParagraphCount() + "," +
                            "\"serverFolder\":\"" + result.getServerFolder() + "\"," +
                            "\"fileSize\":" + uploadedFile.length() + "," +
                            "\"fileName\":\"" + uploadedFile.getName() + "\"" +
                            "}"
            );
            LOGGER.info("File conversion finished!");
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.error("Error while converting file={}", file, e);
            exchange.getResponseSender().send("");
        }
    }

    public static ServerUtils instance() {
        return INSTANCE;
    }

    private ServerUtils() {
        // Static instance
    }
}
