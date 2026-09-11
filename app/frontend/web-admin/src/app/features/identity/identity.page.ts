import { PageIntroComponent } from '../../shared/ui/page-intro/page-intro.component';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { TokenStore } from '../../core/auth/token.store';
import { ClockService } from '../../core/clock/clock.service';
import { JsonViewComponent } from '../../shared/ui/json-view/json-view.component';
import { RoleBadgeComponent } from '../../shared/ui/role-badge/role-badge.component';
import { TokenCountdownComponent } from '../../shared/ui/token-countdown/token-countdown.component';

/**
 * "Decode and display the JWT" - the reason this whole app exists. A mobile developer signing in
 * as a role they don't normally use needs to see, without guessing, exactly what the token says:
 * who it is, what it grants, and how long it has left.
 */
@Component({
  selector: 'app-identity-page',
  imports: [JsonViewComponent, TokenCountdownComponent, RoleBadgeComponent, PageIntroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <app-page-intro
        title="My token"
        what="What the access token in this browser actually says: who you are to the API, what it grants you, and how long it has left."
        [can]="['Read every claim, decoded', 'See the roles the API will act on', 'Watch the countdown to expiry', 'Copy the raw claims']"
        note="Decoded here in the browser, not verified. The signature is what the services check, and only they can — so this shows what the token CLAIMS. If a call is refused while this says you have the role, that gap is worth reporting."
      />

      @if (decoded(); as decoded) {
        <div class="card stack">
          <div class="row-between">
            <h2 style="margin:0">Access token</h2>
            <app-token-countdown />
          </div>

          @if (isExpired()) {
            <div class="badge badge-danger" role="alert" style="align-self: start">
              This token expired at {{ expiryLocal() }}. Sign in again to get a fresh one.
            </div>
          }

          <dl class="claims-grid">
            <dt>Subject (sub)</dt>
            <dd class="mono">{{ decoded.claims.sub }}</dd>

            <dt>E-mail</dt>
            <dd>{{ decoded.claims.email }}</dd>

            <dt>Roles</dt>
            <dd>
              <div class="row spread">
                @for (role of roles(); track role) {
                  <app-role-badge [role]="role" />
                } @empty {
                  <span class="text-faint">none</span>
                }
              </div>
            </dd>

            <dt>Issuer (iss)</dt>
            <dd class="mono">{{ decoded.claims.iss }}</dd>

            <dt>Issued at (iat)</dt>
            <dd>{{ issuedLocal() }}</dd>

            <dt>Expires at (exp)</dt>
            <dd>{{ expiryLocal() }}</dd>

            <dt>Header</dt>
            <dd class="mono">alg={{ decoded.header['alg'] }} kid={{ decoded.header['kid'] }}</dd>
          </dl>
        </div>

        <div class="card stack">
          <h2 style="margin:0">Raw claims</h2>
          <app-json-view [value]="decoded.claims" />
        </div>
      } @else {
        <div class="card empty-state">
          <p>No token loaded. Sign in to see what a KApp access token contains.</p>
        </div>
      }
    </div>
  `,
  styles: `
    .claims-grid {
      display: grid;
      grid-template-columns: 10rem 1fr;
      row-gap: 0.75rem;
      column-gap: 1rem;
      margin: 0;
    }
    .claims-grid dt {
      color: var(--text-muted);
      font-size: 0.8125rem;
      font-weight: 600;
    }
    .claims-grid dd {
      margin: 0;
    }
  `,
})
export class IdentityPage {
  private readonly tokenStore = inject(TokenStore);
  private readonly clock = inject(ClockService);

  readonly decoded = this.tokenStore.decoded;
  readonly roles = this.tokenStore.roles;

  readonly isExpired = computed(() => {
    this.clock.now();
    return this.tokenStore.isExpired();
  });

  readonly issuedLocal = computed(() => {
    const claims = this.decoded()?.claims;
    return claims ? new Date(claims.iat * 1000).toLocaleString() : '';
  });

  readonly expiryLocal = computed(() => {
    const claims = this.decoded()?.claims;
    return claims ? new Date(claims.exp * 1000).toLocaleString() : '';
  });
}
