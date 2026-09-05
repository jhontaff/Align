package com.jet.align.ai.credential;

import com.jet.align.ai.credential.dto.LlmCredentialRequest;
import com.jet.align.ai.credential.dto.LlmCredentialStatusResponse;
import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/credentials")
@RequiredArgsConstructor
public class LlmCredentialController {

    private final LlmCredentialService llmCredentialService;

    @GetMapping
    public ResponseEntity<ApiResponse<LlmCredentialStatusResponse>> getStatus(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Credential status retrieved successfully.",
                        llmCredentialService.getStatus(user))
        );
    }

    /**
     * PUT y no POST: el usuario tiene a lo sumo una credencial, así que guardar
     * es reemplazar el recurso completo, y repetir la llamada con la misma key
     * deja el sistema en el mismo estado.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<LlmCredentialStatusResponse>> save(
            @Valid @RequestBody LlmCredentialRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Credential saved successfully.",
                        llmCredentialService.save(user, request.apiKey()))
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user
    ) {
        llmCredentialService.delete(user);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Credential removed successfully.", null)
        );
    }
}
