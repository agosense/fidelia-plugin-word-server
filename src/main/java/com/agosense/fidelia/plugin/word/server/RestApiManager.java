package com.agosense.fidelia.plugin.word.server;

import com.agosense.fidelia.rest.v1.ApiClient;
import com.agosense.fidelia.rest.v1.FideliaApplicationApi;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class RestApiManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiManager.class);
    private static final RestApiManager INSTANCE = new RestApiManager();

    public FideliaApplicationApi create(String protocol, String host, String port, String domain, String token) {

        String BASE_URL = protocol + "://" + host + ":" + port + "/fidelia/" + domain + "/api/rest";
        LOGGER.info("Setting up agosense.fidelia REST API with baseUrl={}", BASE_URL);

        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(BASE_URL);
        apiClient.addDefaultHeader("API-TOKEN", token);
        FideliaApplicationApi api = new FideliaApplicationApi(apiClient);

        api.getApiClient().getHttpClient().property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);

        HttpsURLConnection.setDefaultHostnameVerifier(
                (String hostname, SSLSession session) -> true
        );
        LOGGER.debug("SSL hostname verifier applied");
        try {
            TrustManager[] trustManagers = new TrustManager[]{new TrustEveryoneManager()};
            apiClient.getHttpClient().getSslContext().init(null, trustManagers, new SecureRandom());
            LOGGER.debug("SSL context applied");
        } catch (KeyManagementException e) {
            LOGGER.error("Cannot apply SSL context", e);
        }

        return api;
    }

    public static RestApiManager instance() {
        return INSTANCE;
    }

    private RestApiManager() {
        // Static instance
    }

    class TrustEveryoneManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Nothing to be done
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Nothing to be done
        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
