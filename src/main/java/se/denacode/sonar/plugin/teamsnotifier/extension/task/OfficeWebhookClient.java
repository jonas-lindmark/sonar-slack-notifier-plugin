/*
 * FAVEEO SA
 * __________________
 *
 *  [2016] - [2019] Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Faveeo SA and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Faveeo SA
 * and its suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Faveeo SA.
 */
package se.denacode.sonar.plugin.teamsnotifier.extension.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Strings;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import se.denacode.sonar.plugin.teamsnotifier.common.TeamsNotifierProp;
import se.denacode.sonar.plugin.teamsnotifier.extension.task.payload.Payload;

public class OfficeWebhookClient {
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final Logger LOG = Loggers.get(OfficeWebhookClient.class);
    private final OkHttpClient httpClient;
    private final Configuration settings;

    /**
     * Initializes the Slack HTTP Client.
     *
     * @param settings
     */
    public OfficeWebhookClient(final Configuration settings) {
        this.settings = settings;
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS);
        this.declareProxyIfNecessary(builder);
        httpClient = builder.build();


    }

    private void declareProxyIfNecessary(final OkHttpClient.Builder builder) {
        final Proxy httpProxy = this.getHttpProxy();
        if (httpProxy == null) {
            builder.proxy(httpProxy);
        }
    }

    private Proxy getHttpProxy() {
        final Proxy.Type proxyProtocol = this.getProxyProtocol();
        if (proxyProtocol == null) return null;

        final String proxyIP = this.getProxyIP();
        if (Strings.isNullOrEmpty(proxyIP)) {
            return null;
        }

        return new Proxy(
            proxyProtocol,
            new InetSocketAddress(
                proxyIP,
                this.getProxyPort()
            )
        );
    }

    private Proxy.Type getProxyProtocol() {

        final Optional<String> proxyProtocol = this.settings.get(TeamsNotifierProp.PROXY_PROTOCOL.property());
        return proxyProtocol.map(Proxy.Type::valueOf).orElseThrow(() -> new IllegalStateException("Proxy type property not found"));

    }

    private String getProxyIP() {

        final Optional<String> proxyIp = this.settings.get(TeamsNotifierProp.PROXY_IP.property());
        return proxyIp.orElse(null);
    }

    private int getProxyPort() {
        final Optional<Integer> proxyPort = this.settings.getInt(TeamsNotifierProp.PROXY_PORT.property());
        return proxyPort.orElseThrow(() -> new IllegalStateException("Proxy port property not found"));
    }

    boolean invokeIncomingWebhook(final String projectCustomHook, final Payload payload) throws IOException {

        final Gson gson = new Gson();
        final String payloadJson = gson.toJson(payload);

        final String incomingWebhookUrl = StringUtils.isEmpty(projectCustomHook) ? this.getSlackIncomingWebhookUrl() :
            projectCustomHook;
        final Request request = this.buildRequest(payloadJson, incomingWebhookUrl);
        LOG.info("Request configuration: [uri={}, payload={}", request.url(), payloadJson);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Formatted payload:");
            LOG.debug(new GsonBuilder().setPrettyPrinting().create().toJson(payload));
        }

        try (final Response response = this.httpClient.newCall(request).execute()) {
            LOG.info("Webhook HTTP response status: {}", response.code());
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("The webhook call has failed");
            }
            final ResponseBody body = response.body();
            if (body != null) {
                LOG.info("Webhook HTTP response body: {}", body.string());
                return true;
            } else {
                LOG.error("Slack HTTP response body no body ");
            }
        }
        return false;
    }

    private Request buildRequest(final String payloadJson, final String incomingWebhookUrl) {
        final Request.Builder requestBuilder = new Request.Builder().url(incomingWebhookUrl);
        requestBuilder.addHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED);
        requestBuilder.post(RequestBody.create(payloadJson, MediaType.parse(APPLICATION_X_WWW_FORM_URLENCODED)));
        return requestBuilder.build();
    }

    protected String getSlackIncomingWebhookUrl() {

        final Optional<String> hook = this.settings.get(TeamsNotifierProp.HOOK.property());
        return hook.orElseThrow(() -> new IllegalStateException("Hook property not found"));
    }
}
