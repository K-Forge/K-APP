import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * `docs/api/auth.openapi.yaml` documents `invitationCode` as a required field on
 * `POST /auth/register` - the code decides ROLE_STUDENT vs ROLE_PROFESSOR - but the contract
 * defines no endpoint to create, list, or revoke a code. There is nothing here to build a CRUD
 * screen against without inventing an endpoint that doesn't exist, which is exactly what this
 * tool is supposed to prevent (see "Forms validated from the spec's own constraints" in the
 * project brief). This page says so plainly instead of silently omitting the nav entry, so a
 * developer looking for it finds an answer instead of a 404.
 */
@Component({
  selector: 'app-invitation-codes-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <h1>Invitation codes</h1>
      <div class="card empty-state">
        <p>
          <strong>Not available yet.</strong> <code>docs/api/auth.openapi.yaml</code> requires an
          <code>invitationCode</code> on institutional registration, but defines no admin endpoint
          to create, list or revoke one - only to consume one at sign-up.
        </p>
        <p class="text-muted">
          Once the contract adds that endpoint, this screen is where it belongs. Until then, codes
          have to be minted directly against the database.
        </p>
      </div>
    </div>
  `,
})
export class InvitationCodesPage {}
