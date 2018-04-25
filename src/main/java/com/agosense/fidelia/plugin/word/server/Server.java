package com.agosense.fidelia.plugin.word.server;

import com.agosense.fidelia.rest.v1.FideliaApplicationApi;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

import static io.undertow.Handlers.websocket;

public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static void main(final String[] args) {

        // Get the static path from FIDELIA_WORD_SERVER_STATIC_PATH or apply /static
        String path = System.getenv("FIDELIA_WORD_SERVER_STATIC_PATH") == null
                ? "/static"
                : System.getenv("FIDELIA_WORD_SERVER_STATIC_PATH");

        LOGGER.info("Using static path={}", path);

        // Get the bind address from FIDELIA_WORD_SERVER_ADDRESS or apply LOCALHOST
        String address = System.getenv("FIDELIA_WORD_SERVER_ADDRESS") == null
                ? "0.0.0.0"
                : System.getenv("FIDELIA_WORD_SERVER_ADDRESS");
        LOGGER.info("Using bind address={}", address);

        // Get the listen port from FIDELIA_WORD_SERVER_PORT or apply 8181
        Integer port;
        String portString = System.getenv("FIDELIA_WORD_SERVER_PORT");
        try {
            if (portString == null) {
                LOGGER.info("No port specified, setting default port 8181");
                port = 8181;
            } else {
                port = Integer.valueOf(portString);
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid port specified FIDELIA_WORD_SERVER_PORT={}", portString);
            LOGGER.info("Setting default port 8181");
            port = 8181;
        }
        LOGGER.info("Using listen port={}", port);

        // Create  a handler for serving a static index.html with the client Javascript embedded
        HttpHandler indexHandler = (exchange) -> {

            LOGGER.info("Handling request for index with token={}", exchange.getRequestHeaders().getFirst("API-TOKEN"));

            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "<meta charset=\"utf-8\"/>\n" +
                    "<title>WordImport</title>\n" +
                    "<script type=\"text/javascript\">var apiToken = \"" + exchange.getRequestHeaders().getFirst("API-TOKEN") + "\"</script>\n" +
                    "<script type=\"text/javascript\" src=\"vendors.bundle.js\"></script>\n" +
                    "<script type=\"text/javascript\" src=\"main.bundle.js\"></script>\n" +
                    "<link rel=\"stylesheet\" href=\"main.css\">\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<div id='viewport'></div>\n" +
                    "</body>\n" +
                    "</html>";

            exchange.getResponseSender().send(html);
        };

        // Now create the HTTP server
        Undertow.Builder builder = Undertow.builder();
        builder.addHttpListener(port, address);
        builder.setHandler(Handlers.path()
                // Handler for any resources and the index.html
                .addPrefixPath("/word-import",
                        new ResourceHandler(
                                new PathResourceManager(Paths.get(path)),
                                indexHandler)
                )
                // Handler for the file upload
                .addExactPath("/word-import/upload",
                        new EagerFormParsingHandler(
                                FormParserFactory.builder()
                                        .addParsers(new MultiPartParserDefinition())
                                        .build())
                                .setNext((exchange) -> {
                                    LOGGER.info("Handling upload for word import for connection {}.", exchange.getConnection());

                                    ServerUtils.instance().handleUpload(exchange);
                                })
                )
                // Handler for the web socket communication
                .addExactPath("/word-import/websocket",
                        websocket((exchange, channel) -> {
                            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                @Override
                                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                    LOGGER.info("Websocket receiving text message: {}.", message);

                                    ServerUtils.instance().handleImport(channel, message, exchange);
                                }
                            });
                            channel.resumeReceives();
                        }))
        );
        Undertow server = builder.build();

        server.start();
    }
}
