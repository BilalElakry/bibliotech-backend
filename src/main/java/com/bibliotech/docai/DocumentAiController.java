package com.bibliotech.docai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents/{id}/ai")
@RequiredArgsConstructor
public class DocumentAiController {

    private final DocumentAiService documentAiService;

    @PostMapping("/summarize")
    public ResponseEntity<DocAiResponse> summarize(@PathVariable Long id) {
        return ResponseEntity.ok(new DocAiResponse(documentAiService.summarize(id), null));
    }

    @PostMapping("/extract-keys")
    public ResponseEntity<DocAiResponse> extractKeys(@PathVariable Long id) {
        return ResponseEntity.ok(new DocAiResponse(documentAiService.extractKeys(id), null));
    }

    @PostMapping("/explain")
    public ResponseEntity<DocAiResponse> explain(@PathVariable Long id, @RequestBody DocAiRequest req) {
        return ResponseEntity.ok(new DocAiResponse(documentAiService.explain(id, req.text()), null));
    }

    @PostMapping("/ask")
    public ResponseEntity<DocAiResponse> ask(@PathVariable Long id, @RequestBody DocAiRequest req) {
        return ResponseEntity.ok(documentAiService.ask(id, req.text(), req.sessionId()));
    }
}