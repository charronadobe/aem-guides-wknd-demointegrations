package com.adobe.aem.guides.wknd.core.workflows;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;

@Component(service = WorkflowProcess.class, property = { "process.label = Create Banner Ad Renditions" })
public class CreateBannerAdRenditions implements WorkflowProcess {
	private static final Logger log = LoggerFactory.getLogger(CreateBannerAdRenditions.class);

	@Override
	public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
			throws WorkflowException {
		String PROXY_ENDPOINT = "http://ec2-54-67-86-214.us-west-1.compute.amazonaws.com/api/";
		// String PROXY_ENDPOINT = "http://localhost:81/api/";

		try {
			String args = metaDataMap.get("PROCESS_ARGS", "string").toString();

			final WorkflowData workflowData = workItem.getWorkflowData();
			final String payloadPath = workflowData.getPayload().toString();
			String aemFolderPath = payloadPath.substring(0, payloadPath.lastIndexOf("/"));
			CloseableHttpClient client = HttpClients.createDefault();
			try {
				HttpGet request = new HttpGet(
						PROXY_ENDPOINT + "createPSDsFromTemplate?aemFolderPath=" + aemFolderPath + "&" + args);
				client.execute(request);
				client.close();
			} finally {
				if (client != null)
					client.close();
			}

			// log.error("CreateBannerAdRenditions response=" +
			// response.getStatusLine().getStatusCode() + "");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
