import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import type { ApiError } from '../../../core/http/api-error.model';

/**
 * The one place an ApiError gets rendered. Every feature passes its caught error here instead of
 * writing its own "something went wrong" - the point of parsing the envelope centrally is that a
 * validation message and its field-level details always look the same, wherever they came from.
 */
@Component({
  selector: 'app-api-error-banner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as err) {
      <div class="card api-error" role="alert">
        <div class="row-between">
          <strong>{{ err.error }} ({{ err.status }})</strong>
          <span class="text-faint mono">{{ err.path }}</span>
        </div>
        <p class="api-error-message">{{ err.message }}</p>
        @if (err.details?.length) {
          <ul class="api-error-details">
            @for (detail of err.details; track $index) {
              <li><strong>{{ detail.field }}</strong>: {{ detail.issue }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
  styles: `
    .api-error {
      border-color: var(--danger);
      background: var(--danger-bg);
      color: var(--danger-strong);
    }
    .api-error-message {
      margin: 0.5rem 0 0;
    }
    .api-error-details {
      margin: 0.5rem 0 0;
      padding-left: 1.25rem;
    }
  `,
})
export class ApiErrorBannerComponent {
  readonly error = input<ApiError | null>(null);
}
