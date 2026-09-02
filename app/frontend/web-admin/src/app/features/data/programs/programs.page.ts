import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import { DataTableComponent } from '../../../shared/ui/data-table/data-table.component';
import type { Program } from './program.model';
import { ProgramsService } from './programs.service';

/**
 * Read-only: docs/api/semaphore.openapi.yaml defines `GET /api/catalog/programs` and
 * `GET /api/catalog/programs/{programCode}` but no admin write endpoint for programs themselves -
 * only curricula (pensums) can be created or replaced. Showing an edit action here would be
 * inventing a capability the API doesn't have, which is exactly what this tool exists to avoid.
 */
@Component({
  selector: 'app-programs-page',
  imports: [DataTableComponent, ApiErrorBannerComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <h1>Programs</h1>
      <p class="text-muted">
        Read-only. The semaphore contract has no admin endpoint to create or edit a program -
        only its curricula (pensums) can be managed, on the Curricula screen.
      </p>

      <div class="card">
        <app-api-error-banner [error]="error()" />

        <app-data-table [loading]="loading()" [empty]="!loading() && !error() && programs().length === 0" emptyMessage="No programs found.">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Faculty</th>
              <th>Level</th>
              <th>Active pensum</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (program of programs(); track program.code) {
              <tr>
                <td class="mono">{{ program.code }}</td>
                <td>{{ program.name }}</td>
                <td class="text-muted">{{ program.faculty }}</td>
                <td><span class="badge badge-neutral">{{ program.level }}</span></td>
                <td class="mono">{{ program.activePensumCode ?? '—' }}</td>
                <td>
                  @if (program.activePensumCode) {
                    <a class="btn btn-sm" [routerLink]="['/data/curricula']" [queryParams]="{ pensum: program.activePensumCode }">
                      View curriculum
                    </a>
                  }
                </td>
              </tr>
            }
          </tbody>
        </app-data-table>
      </div>
    </div>
  `,
})
export class ProgramsPage {
  private readonly programsService = inject(ProgramsService);

  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly programs = signal<Program[]>([]);

  constructor() {
    this.programsService.list().subscribe({
      next: (programs) => {
        this.programs.set(programs);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }
}
