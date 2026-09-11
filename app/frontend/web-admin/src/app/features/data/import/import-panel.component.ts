import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import type { CurriculumImportReport } from './import.model';
import { CurriculumImportService } from './import.service';

/**
 * Bulk loading of pensums from a spreadsheet export, as a panel rather than a page.
 *
 * <p>It sits inside the Pensums screen because importing a CSV IS the create half of that
 * screen's CRUD: twenty-four programmes and roughly 1200 rows is not something anybody enters
 * one at a time, and a separate "Import" entry in the navigation made it look like a different
 * feature rather than the same one at scale.
 */
@Component({
  selector: 'app-import-panel',
  imports: [ApiErrorBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <div class="card stack">
        <div class="field">
          <label for="csv">CSV file</label>
          <input id="csv" type="file" accept=".csv,text/csv" (change)="onFile($event)" />
          @if (file(); as f) {
            <span class="hint">{{ f.name }} — {{ (f.size / 1024).toFixed(1) }} KB</span>
          } @else {
            <span class="hint">UTF-8, at most 5 MB.</span>
          }
        </div>

        <div class="row">
          <button type="button" class="btn btn-primary" [disabled]="!file() || busy()" (click)="run(true)">
            {{ busy() && lastWasDryRun() ? 'Checking…' : 'Check the file' }}
          </button>
          <button type="button" class="btn" [disabled]="!file() || busy()" (click)="run(false)">
            {{ busy() && !lastWasDryRun() ? 'Importing…' : 'Import for real' }}
          </button>
        </div>

        <p class="hint" style="margin:0">
          Checking validates the whole file and writes nothing. Importing writes only if every row
          passes — there is no partial import.
        </p>
      </div>

      <app-api-error-banner [error]="error()" />
      @if (error()?.status === 400) {
        <p class="text-muted">
          Nothing was written. Each entry above is labelled with the line of your file, or the
          pensum it belongs to. Fix them in the spreadsheet and check again.
        </p>
      }

      @if (report(); as r) {
        <div class="card stack">
          <div class="row-between">
            <h2 style="margin:0">
              {{ r.dryRun ? 'The file is valid' : 'Imported' }}
            </h2>
            <span class="badge" [class.badge-neutral]="r.dryRun" [class.badge-ok]="!r.dryRun">
              {{ r.dryRun ? 'nothing was written' : 'written' }}
            </span>
          </div>
          <p class="text-muted" style="margin:0">
            {{ r.rowsRead }} data rows, {{ r.curricula.length }} pensum(s).
          </p>

          <table class="table">
            <thead>
              <tr>
                <th>Pensum</th>
                <th>Program</th>
                <th>Courses</th>
                <th>Credits</th>
                <th>Weekly hours</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (c of r.curricula; track c.pensumCode) {
                <tr>
                  <td class="mono">{{ c.pensumCode }}</td>
                  <td>{{ c.programCode }} — {{ c.programName }}</td>
                  <td>{{ c.courses }}</td>
                  <td>{{ c.computedCredits }}</td>
                  <td>{{ c.computedHours }}</td>
                  <td class="text-muted">
                    {{ c.curriculumCreated ? 'new' : 'replaced' }}
                  </td>
                </tr>
              }
            </tbody>
          </table>

          <p class="hint" style="margin:0">
            Credits and hours shown are what the courses actually add up to. The import refuses a
            file whose declared totals disagree with them — that check already found the seeded
            Ingeniería de Sistemas plan declaring 142 credits where its 48 courses give 144.
          </p>
        </div>
      }
    </div>
  `,
})
export class ImportPanelComponent {
  private readonly service = inject(CurriculumImportService);

  readonly file = signal<File | null>(null);
  readonly busy = signal(false);
  readonly lastWasDryRun = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly report = signal<CurriculumImportReport | null>(null);

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.[0] ?? null);
    this.report.set(null);
    this.error.set(null);
  }

  run(dryRun: boolean): void {
    const file = this.file();
    if (!file) {
      return;
    }
    if (!dryRun && !window.confirm(`Import ${file.name} for real? Existing pensums with the same codes are replaced.`)) {
      return;
    }

    this.busy.set(true);
    this.lastWasDryRun.set(dryRun);
    this.error.set(null);
    this.report.set(null);

    this.service.upload(file, dryRun).subscribe({
      next: (report) => {
        this.busy.set(false);
        this.report.set(report);
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }
}
