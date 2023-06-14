package com.adobe.aem.guides.wknd.core.services.impl;

/**
 * Created by Douglas Prevelige on 2/3/2022.
 * Non-production code for POC purposes only.
 */

import com.adobe.aem.guides.wknd.core.models.ServiceDoc;
import com.adobe.aem.guides.wknd.core.services.AssetAPIService;
import com.adobe.aem.guides.wknd.core.utils.RESTAPI;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Designate(ocd = AssetAPIServiceConfig.class)
@Component(service = AssetAPIService.class)
public class AssetAPIServiceImpl implements AssetAPIService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static String SEL_INITIATE = ".initiateUpload.json";

    private String _urlAEMCS;
    private String _assetUser;
    private String _assetPWD;

    public void uploadAsset(ServiceDoc asset, String folderPath) {
        if (!folderPath.startsWith("/"))
            folderPath = "/" + folderPath;

        Map<String, Object> respMap = initiateUpload(asset, folderPath);

        if (respMap != null && respMap.containsKey(RESTAPI.STRINGFIELDNAME)) {
            String respString = (String) respMap.get(RESTAPI.STRINGFIELDNAME);
            try {
                JSONObject joInitResp = new JSONObject(respString);
                Cookie affinityCookie = (Cookie) respMap.get(RESTAPI.AFFINITYCOOKIE);
                if (joInitResp.has("completeURI")) {
                    String completeURI = joInitResp.getString("completeURI");
                    JSONArray jaFiles = joInitResp.getJSONArray("files");
                    for (int i = 0; i < jaFiles.length(); i++) {
                        JSONArray jaURIs = jaFiles.getJSONObject(i).getJSONArray("uploadURIs");
                        String[] uris = new String[jaURIs.length()];
                        for (int j = 0; j < jaURIs.length(); j++) {
                            uris[j] = jaURIs.getString(j);
                        }
                        int maxPartSize = jaFiles.getJSONObject(i).has("maxPartSize")
                                ? jaFiles.getJSONObject(i).getInt("maxPartSize")
                                : -1;
                        uploadBinary(asset, uris, affinityCookie);
                        completeUpload(completeURI, jaFiles.getJSONObject(i), affinityCookie);

                    }
                }
            } catch (JSONException e) {
                log.error("Error!", e);
            }
        }
    }

    private Map<String, Object> initiateUpload(ServiceDoc asset, String folderPath) {

        Map<String, Object> fields = new HashMap<>();
        fields.put("fileName", asset.getLocation());
        fields.put("fileSize", Integer.toString(asset.getContentSizeBytes()));

        Map<String, Object> respMap = RESTAPI.callPostFields(_urlAEMCS + folderPath + SEL_INITIATE, getHeaders(),
                fields, null);
        return respMap;
    }

    private void uploadBinary(ServiceDoc asset, String[] uris, Cookie affinityCookie) {

        if (uris.length == 1) {
            log.info("Upload URI: " + uris[0]);
            RESTAPI.callPutBinary(uris[0], null, asset.getContent(), affinityCookie);
        } else {
            int batchSize = asset.getContentSizeBytes() / uris.length;
            int startVal = 0;

            for (int i = 0; i < uris.length - 1; i++) {
                int endVal = startVal + batchSize - 1;
                byte[] content = Arrays.copyOfRange(asset.getContent(), startVal, endVal);
                startVal += batchSize;
                RESTAPI.callPutBinary(uris[i], null, content, affinityCookie);
            }
            RESTAPI.callPutBinary(uris[uris.length - 1], null,
                    Arrays.copyOfRange(asset.getContent(), startVal, asset.getContentSizeBytes() - 1),
                    affinityCookie);
        }
    }

    private void completeUpload(String completeURI, JSONObject jsonObject, Cookie affinityCookie) {
        try {
            if (!completeURI.startsWith("http"))
                completeURI = _urlAEMCS + completeURI;
            Map<String, Object> fields = new HashMap<>();
            fields.put("fileName", jsonObject.getString("fileName"));
            fields.put("mimeType", jsonObject.getString("mimeType"));
            fields.put("uploadToken", jsonObject.getString("uploadToken"));
            fields.put("replace", "true");
            RESTAPI.callPostFields(completeURI, getHeaders(), fields, affinityCookie);
        } catch (JSONException e) {
            log.error("Error!", e);
        }
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, RESTAPI.getAuthHeader(_assetUser, _assetPWD));
        return headers;
    }

    @Activate
    @Modified
    protected final void activate(AssetAPIServiceConfig config) {
        _assetUser = config.apiUser();
        _assetPWD = config.apiPWD();
        _urlAEMCS = config.urlAEMCS();
        log.info(_urlAEMCS);
        if (_urlAEMCS != null && _urlAEMCS.length() > 0 && _urlAEMCS.endsWith("/")) {
            _urlAEMCS = _urlAEMCS.substring(0, _urlAEMCS.length() - 1);
            log.info(_urlAEMCS);
        }
    }
}