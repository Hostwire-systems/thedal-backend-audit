package com.thedal.thedal_app.campaign.api;

import com.thedal.thedal_app.campaign.dto.*;
import com.thedal.thedal_app.campaign.service.CampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CampaignController {
    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @PostMapping("/campaigns")
    public ResponseEntity<CampaignResponse> create(@RequestBody CampaignCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> list(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.list(channel, status, q));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<CampaignResponse> get(@PathVariable String id) {
        CampaignResponse res = service.get(id);
        return res == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @PatchMapping("/campaigns/{id}")
    public ResponseEntity<CampaignResponse> update(@PathVariable String id, @RequestBody CampaignCreateRequest partial) {
        CampaignResponse res = service.update(id, partial);
        return res == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/campaigns/{id}/send")
    public ResponseEntity<CampaignResponse> send(@PathVariable String id) {
        CampaignResponse res = service.send(id);
        return res == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @PostMapping("/campaigns/estimate")
    public ResponseEntity<EstimateResponse> estimate(@RequestBody EstimateRequest req) {
        return ResponseEntity.ok(service.estimate(req));
    }

    @GetMapping("/comm/filters")
    public ResponseEntity<FilterOptionsResponse> filters(@RequestParam(required = false) Long electionId,
                                                        @RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(service.getFilterOptions(electionId, accountId));
    }

    @GetMapping("/whatsapp/senders")
    public ResponseEntity<List<WhatsAppSender>> senders() {
        return ResponseEntity.ok(service.listWhatsAppSenders());
    }

    @GetMapping("/sms/senders")
    public ResponseEntity<List<SmsSender>> smsSenders() {
        return ResponseEntity.ok(service.listSmsSenders());
    }
}
