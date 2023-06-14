package com.adobe.aem.guides.wknd.core.services.impl;

/**
 * Created by Douglas Prevelige on 2/3/2022.
 * Non-production code for POC purposes only.
 */

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.AttributeDefinition;

@ObjectClassDefinition(name = "AssetAPIServce Config")
public @interface AssetAPIServiceConfig {

    @AttributeDefinition(name = "Sign Endpoint", description = "Adobe Sign API Endpoint")
    String urlAEMCS() default "https://author-p102861-e970602.adobeaemcloud.com";

    @AttributeDefinition(name = "Truststore User Name", description = "User who's truststore contains key")
    String apiUser() default "uofphoenix";

    @AttributeDefinition(name = "Truststore User Pwd", description = "User who's truststore contains key")
    String apiPWD() default "S@389Z24Pw@u";
}