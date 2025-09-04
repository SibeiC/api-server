package com.chencraft.common.service.api;

import com.chencraft.model.DDNSRequest;
import com.chencraft.model.cloudflare.ARecordResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CloudflareApiService.class})
class CloudflareApiServiceTest {
    @MockitoBean
    private CloudflareWebClient cloudflareWebClient;

    @Autowired
    private CloudflareApiService cloudflareApiService;

    private DDNSRequest request;

    @BeforeEach
    public void setup() {
        ARecordResponse record = new ARecordResponse();
        record.setProxiable(true);
        record.setProxied(true);
        record.setName("www.chencraft.com");
        record.setContent("1.1.1.1");
        record.setType("A");

        request = new DDNSRequest();
        request.setMyIp("8.8.8.8");
        request.setDnsType("A");
        request.setHostname("www.chencraft.com");

        when(cloudflareWebClient.listDnsRecords(any(), any())).thenReturn(new ArrayList<>());
        when(cloudflareWebClient.listDnsRecords(eq("www.chencraft.com"), eq("A"))).thenReturn(Collections.singletonList(record));
    }

    @Test
    public void testCreateRecord() {
        request.setHostname("new.chencraft.com");
        cloudflareApiService.updateDNSRecord(request);
        verify(cloudflareWebClient, times(1)).createDnsRecordDetail(any());
    }

    @Test
    public void testUpdateRecord() {
        cloudflareApiService.updateDNSRecord(request);
        ArgumentCaptor<DDNSRequest> requestCaptor = ArgumentCaptor.forClass(DDNSRequest.class);
        verify(cloudflareWebClient, times(1)).overwriteDnsRecord(requestCaptor.capture(), any());

        Assertions.assertTrue(requestCaptor.getValue().isProxied());

    }
}