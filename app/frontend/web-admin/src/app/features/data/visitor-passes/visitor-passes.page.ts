import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import { DataTableComponent } from '../../../shared/ui/data-table/data-table.component';
import type { VisitorPass } from './visitor-pass.model';
import { VisitorPassesService } from './visitor-passes.service';

/**
 * The reception counter: issuing day passes and reading the register of who used them.
 *
 * <p>Issuing is one button on purpose. Somebody is standing at the desk; anything more than a
 * click and a code to read out is friction in the wrong place.
 *
 * <p><strong>This screen shows personal data.</strong> The register carries visitors' names and
 * identity documents, which is the entire point of keeping it, and the reason it exists nowhere
 * else. Records delete themselves 30 days after redemption.
 */
@Component({
  selector: 'app-visitor-passes-page',
  imports: [DataTableComponent, ApiErrorBannerComponent, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <div class="row-between">
        <h1>Visitor passes</h1>
        <button type="button" class="btn btn-primary" [disabled]="issuing()" (click)="issue()">
          {{ issuing() ? 'Issuing…' : 'Issue a pass' }}
        </button>
      </div>

      <p class="text-muted">
        A pass is a token, not an account: no e-mail, no password, nothing left behind. Redeemed,
        it opens the campus map for 24 hours and nothing else. <strong>The register below holds
        identity documents</strong> and deletes itself 30 days after each visit.
      </p>

      @if (justIssued(); as pass) {
        <div class="card issued" role="status">
          <p style="margin:0 0 0.25rem">Read this out to the visitor:</p>
          <p class="issued-code mono">{{ pass.code }}</p>
          <p class="text-muted" style="margin:0">
            Redeemable until {{ pass.redeemableUntil | date: 'short' }}. They need an identity
            document to redeem it.
          </p>
        </div>
      }

      <div class="card stack">
        <div class="field" style="margin-bottom:0; max-width: 14rem">
          <label for="vp-filter">Show</label>
          <select id="vp-filter" (change)="onFilter($event)">
            <option value="">All passes</option>
            <option value="true">Redeemed (the register)</option>
            <option value="false">Issued, not yet used</option>
          </select>
        </div>

        <app-api-error-banner [error]="error()" />

        <app-data-table
          [loading]="loading()"
          [empty]="!loading() && !error() && passes().length === 0"
          emptyMessage="No passes. Issue one when somebody arrives at reception."
        >
          <thead>
            <tr>
              <th>Code</th>
              <th>Issued</th>
              <th>Visitor</th>
              <th>Document</th>
              <th>Access until</th>
              <th>Notes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (pass of passes(); track pass.code) {
              <tr>
                <td class="mono">{{ pass.code }}</td>
                <td class="text-muted">{{ pass.createdAt | date: 'short' }}</td>
                <td>
                  @if (pass.redeemed) {
                    {{ pass.visitorName }}
                  } @else {
                    <span class="badge badge-neutral">not used</span>
                  }
                </td>
                <td class="mono text-muted">
                  {{ pass.redeemed ? pass.documentType + ' ' + pass.documentNumber : '—' }}
                </td>
                <td class="text-muted">
                  {{ pass.redeemed ? (pass.accessExpiresAt | date: 'short') : ('expires ' + (pass.redeemableUntil | date: 'shortTime')) }}
                </td>
                <td class="text-muted">{{ pass.notes ?? '—' }}</td>
                <td>
                  @if (!pass.redeemed) {
                    <button
                      type="button"
                      class="btn btn-sm btn-danger"
                      [disabled]="busyCode() === pass.code"
                      (click)="revoke(pass)"
                    >
                      Revoke
                    </button>
                  } @else {
                    <span class="text-faint" title="A redeemed pass is a visit record. It is deleted automatically after 30 days.">
                      kept 30 days
                    </span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </app-data-table>
      </div>
    </div>
  `,
  styles: `
    .issued {
      border-color: var(--accent);
    }
    .issued-code {
      font-size: 1.75rem;
      letter-spacing: 0.08em;
      margin: 0 0 0.25rem;
      font-weight: 700;
    }
  `,
})
export class VisitorPassesPage {
  private readonly service = inject(VisitorPassesService);

  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly passes = signal<VisitorPass[]>([]);
  readonly issuing = signal(false);
  readonly justIssued = signal<VisitorPass | null>(null);
  readonly busyCode = signal<string | null>(null);

  private redeemedFilter?: boolean;

  constructor() {
    this.fetch();
  }

  private fetch(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list(this.redeemedFilter).subscribe({
      next: (passes) => {
        this.passes.set(passes);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }

  onFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.redeemedFilter = value === '' ? undefined : value === 'true';
    this.fetch();
  }

  issue(): void {
    this.issuing.set(true);
    this.error.set(null);
    this.service.issue().subscribe({
      next: (pass) => {
        this.issuing.set(false);
        this.justIssued.set(pass);
        this.fetch();
      },
      error: (err: unknown) => {
        this.issuing.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }

  revoke(pass: VisitorPass): void {
    if (!window.confirm(`Revoke pass ${pass.code}? Whoever is holding it will not be able to use it.`)) {
      return;
    }
    this.busyCode.set(pass.code);
    this.error.set(null);
    this.service.revoke(pass.code).subscribe({
      next: () => {
        this.busyCode.set(null);
        if (this.justIssued()?.code === pass.code) {
          this.justIssued.set(null);
        }
        this.fetch();
      },
      error: (err: unknown) => {
        this.busyCode.set(null);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }
}
