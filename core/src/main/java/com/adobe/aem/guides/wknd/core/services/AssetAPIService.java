package com.adobe.aem.guides.wknd.core.services;

import com.adobe.aem.guides.wknd.core.models.ServiceDoc;

/**
 * Created by Douglas Prevelige on 2/3/2022.
 * Non-production code for POC purposes only.
 */
public interface AssetAPIService {

    void uploadAsset(ServiceDoc asset, String folderPath);

}