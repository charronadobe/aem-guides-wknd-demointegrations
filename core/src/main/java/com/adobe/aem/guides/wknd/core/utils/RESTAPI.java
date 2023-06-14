package com.adobe.aem.guides.wknd.core.utils;

import com.adobe.aem.guides.wknd.core.models.ServiceDoc;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;

import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class RESTAPI {

    private static final Logger log = LoggerFactory.getLogger(RESTAPI.class);

    public static String STRINGFIELDNAME = "_STRINGRESPONSE_";
    public static String AFFINITYCOOKIE = "_AFFINITYCOOKIE_";

    public static Map<String, Object> callGet(String url, Map<String, String> headers) {

        Map<String, Object> responseMap = null;
        CookieStore httpCookieStore = new BasicCookieStore();
        CloseableHttpClient httpclient = createAcceptSelfSignedCertificateClient(httpCookieStore);
        HttpGet httpGet = new HttpGet(url);
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                httpGet.addHeader(key, headers.get(key));
            }
        }

        try {
            CloseableHttpResponse response = httpclient.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int resp = statusLine.getStatusCode();
            log.info("GET response code: " + resp);

            HttpEntity respEntity = response.getEntity();
            if (respEntity != null && resp < 400) {
                responseMap = loadResponseMap(respEntity);
                // responseString = EntityUtils.toString(respEntity);
                // log.info("respString: " +responseString);
            }
            // logHeaders(response.getAllHeaders());
        } catch (UnsupportedEncodingException e) {
            log.error("Error!", e);
        } catch (IOException e) {
            log.error("Error!", e);
        }
        return responseMap;
    }

    public static int callPutBinary(String url, Map<String, String> headers, byte[] binary, Cookie affinity) {
        int respCode = -1;

        CookieStore httpCookieStore = new BasicCookieStore();
        if (affinity != null) {
            httpCookieStore.addCookie(affinity);
        }

        CloseableHttpClient httpClient = createAcceptSelfSignedCertificateClient(httpCookieStore);
        HttpPut httpPut = new HttpPut(url);
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                httpPut.addHeader(key, headers.get(key));
            }
        }
        ByteArrayEntity bae = new ByteArrayEntity(binary);
        httpPut.setEntity(bae);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpPut);
            log.info("PUT response: " + response.getStatusLine().getStatusCode());
            HttpEntity respEntity = response.getEntity();
            if (respEntity != null) {
                String responseString = EntityUtils.toString(respEntity);
                log.info(responseString);
            }
        } catch (IOException e) {
            log.error("Error!", e);
        }
        return response.getStatusLine().getStatusCode();
    }

    public static Map<String, Object> callPostBody(String url, Map<String, String> headers, String body) {
        Map<String, Object> responseMap = null;

        CookieStore httpCookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = createAcceptSelfSignedCertificateClient(httpCookieStore);
        HttpPost httpPost = getPost(url, headers);

        try {
            StringEntity stringEntity = new StringEntity(body);
            httpPost.setEntity(stringEntity);
            // log.info("*** calling exec Post Body");
            responseMap = execPost(httpPost, httpClient, httpCookieStore);

        } catch (UnsupportedEncodingException e) {
            log.error("Error!", e);
        } catch (IOException e) {
            log.error("Error!", e);
        }
        return responseMap;
    }

    public static Map<String, Object> callPostFields(String url, Map<String, String> headers,
            Map<String, Object> fields, Cookie affinity) {
        Map<String, Object> responseMap = null;

        CookieStore httpCookieStore = new BasicCookieStore();
        if (affinity != null) {
            httpCookieStore.addCookie(affinity);
        }
        CloseableHttpClient httpClient = createAcceptSelfSignedCertificateClient(httpCookieStore);
        HttpPost httpPost = getPost(url, headers);

        try {
            if (fields != null && fields.size() > 0) {
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                Set<String> keys = fields.keySet();
                for (String key : keys) {
                    Object obj = fields.get(key);
                    if (obj instanceof String) {
                        StringBody paramValue = new StringBody((String) obj, ContentType.TEXT_PLAIN);
                        entityBuilder.addPart(key, paramValue);
                    } else if (obj instanceof byte[]) {
                        ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) obj);
                        entityBuilder.addBinaryBody(key, bais);
                    } else if (obj instanceof InputStream) {
                        entityBuilder.addBinaryBody(key, (InputStream) obj);
                    }
                }
                httpPost.setEntity(entityBuilder.build());
            }

            responseMap = execPost(httpPost, httpClient, httpCookieStore);

        } catch (UnsupportedEncodingException e) {
            log.error("Error!", e);
        } catch (IOException e) {
            log.error("Error!", e);
        }
        return responseMap;
    }

    private static HttpPost getPost(String url, Map<String, String> headers) {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String key : keys) {
                httpPost.addHeader(key, headers.get(key));
            }
        }
        return httpPost;
    }

    private static Map<String, Object> execPost(HttpPost httpPost, CloseableHttpClient httpClient,
            CookieStore cookieStore) throws IOException {
        Map<String, Object> responseMap = null;
        // logHeaders(httpPost.getAllHeaders());
        CloseableHttpResponse response = httpClient.execute(httpPost);

        StatusLine statusLine = response.getStatusLine();
        int resp = statusLine.getStatusCode();
        log.info("*********  EXEC POST  ************");
        log.info("POST response code: " + resp);
        // logHeaders(response.getAllHeaders());
        HttpEntity respEntity = response.getEntity();
        if (respEntity != null) {
            responseMap = loadResponseMap(respEntity);

        }

        // logHeaders(response.getAllHeaders());
        log.info("*********  EXEC POST COMPLETE ************");
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            log.info("cookie: " + cookie.getName() + " = " + cookie.getValue());
            if (cookie.getName().equalsIgnoreCase("affinity")) {
                responseMap.put(AFFINITYCOOKIE, cookie);
                // break;
            }
        }

        return responseMap;
    }

    private static Map<String, Object> loadResponseMap(HttpEntity httpEntity) {
        HashMap<String, Object> responseMap = new HashMap<>();
        if (httpEntity == null) {
            log.info("entity null");
            return responseMap;
        }
        Header header = httpEntity.getContentType();

        String respContentType = header != null ? header.getValue() : "text/plain";
        if (respContentType.startsWith("application/json") || respContentType.startsWith("text/")) {
            try {
                String responseString = EntityUtils.toString(httpEntity);
                responseMap.put(STRINGFIELDNAME, responseString);
                log.info("POST ResponseString:\n" + responseString);
            } catch (IOException e) {
                log.error("Error!", e);
            }
        } else {
            try {
                ByteArrayDataSource bads = new ByteArrayDataSource(httpEntity.getContent(), "multipart/form-data");
                MimeMultipart multipart = new MimeMultipart(bads);

                int count = multipart.getCount();
                log.info("bodypart count " + count);
                for (int i = 0; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String[] dispHeaders = bodyPart.getHeader("Content-Disposition");
                    String partName = "part" + i;
                    if (dispHeaders != null) {
                        String headerVal = dispHeaders[0];
                        log.info("ContentDispositionHeader: " + headerVal);
                        partName = headerVal.substring(headerVal.indexOf("\"") + 1, headerVal.lastIndexOf("\""));
                    }
                    log.info("partName: " + partName);
                    String bodyPartContentType = bodyPart.getContentType();

                    log.info("contentType: " + bodyPart.getContentType());
                    log.info("disposition: " + bodyPart.getDisposition());
                    ServiceDoc serviceDoc = new ServiceDoc();
                    serviceDoc.setContentType(bodyPart.getContentType());
                    serviceDoc.setContent(IOUtils.toByteArray(bodyPart.getInputStream()));
                    serviceDoc.setLocation(partName);
                    responseMap.put(partName, serviceDoc);
                }
            } catch (IOException e) {
                log.error("Error!", e);
            } catch (MessagingException e) {
                log.error("Error!", e);
            }
        }
        return responseMap;
    }

    private static CloseableHttpClient createAcceptSelfSignedCertificateClient(CookieStore httpCookieStore) {

        // use the TrustSelfSignedStrategy to allow Self Signed Certificates
        SSLConnectionSocketFactory connectionFactory = null;
        try {
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new TrustSelfSignedStrategy())
                    .build();

            // we can optionally disable hostname verification.
            // if you don't want to further weaken the security, you don't have to include
            // this.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

            // create an SSL Socket Factory to use the SSLContext with the trust self signed
            // certificate strategy
            // and allow all hosts verifier.
            connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error!", e);
        } catch (KeyManagementException e) {
            log.error("Error!", e);
        } catch (KeyStoreException e) {
            log.error("Error!", e);
        }

        // finally create the HttpClient using HttpClient factory methods and assign the
        // ssl socket factory
        return HttpClients
                .custom()
                .setSSLSocketFactory(connectionFactory)
                .setDefaultCookieStore(httpCookieStore)
                .build();
    }

    public static String getAuthHeader(String uname, String pwd) {
        String auth = uname + ":" + pwd;
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        return authHeader;
    }

    private static void logHeaders(Header[] respHeaders) {

        if (respHeaders != null) {
            log.info("Response Headers");
            for (int i = 0; i < respHeaders.length; i++) {
                Header header = respHeaders[i];
                log.info(header.getName() + " : " + header.getValue());
            }
        }
    }

}
