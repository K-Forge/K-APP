package co.edu.konradlorenz.kapp.auth.web;

import co.edu.konradlorenz.kapp.auth.domain.InvitationCode;
import co.edu.konradlorenz.kapp.auth.service.InvitationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Administration of the codes that gate institutional registration.
 *
 * <p>These are the only door into KApp that is not a university identity, so being able to
 * see which codes exist, how many uses each has left and which are still redeemable is the
 * difference between managing access and guessing at it.
 *
 * <p>{@code ROLE_ADMIN} throughout, enforced per method rather than on the class: the
 * annotation being visible at each handler is what stops a later addition to this
 * controller from inheriting an assumption nobody re-reads.
 */
@RestController
@RequestMapping("/auth/admin/invitation-codes")
@Tag(name = "Invitation codes")
public class AdminInvitationCodeController {

    private final InvitationCodeService codes;

    public AdminInvitationCodeController(InvitationCodeService codes) {
        this.codes = codes;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List invitation codes")
    public List<InvitationCodeResponse> listInvitationCodes(
            @RequestParam(required = false) Boolean active) {
        return codes.list(active).stream().map(InvitationCodeResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an invitation code")
    public ResponseEntity<InvitationCodeResponse> createInvitationCode(
            @Valid @RequestBody InvitationCodeRequest body) {
        InvitationCode created = codes.create(body);
        return ResponseEntity
                .created(URI.create("/auth/admin/invitation-codes/" + created.code()))
                .body(InvitationCodeResponse.from(created));
    }

    @PatchMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate an invitation code")
    public InvitationCodeResponse updateInvitationCode(
            @PathVariable String code,
            @Valid @RequestBody InvitationCodeStatusRequest body) {
        return InvitationCodeResponse.from(codes.setActive(code, body.active()));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an invitation code")
    public void deleteInvitationCode(@PathVariable String code) {
        codes.delete(code);
    }
}
