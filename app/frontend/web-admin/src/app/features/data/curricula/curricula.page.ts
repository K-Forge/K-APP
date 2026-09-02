import { ChangeDetectionStrategy, Component, ViewChild, inject, input, signal } from '@angular/core';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import { ModalComponent } from '../../../shared/ui/modal/modal.component';
import { CurriculaService } from './curricula.service';
import { CURRICULUM_SKELETON, type Curriculum } from './curriculum.model';

/**
 * Lookup-by-code rather than a table: the semaphore contract has no "list curricula" endpoint,
 * only get/create/replace by pensumCode (see docs/api/semaphore.openapi.yaml). A pensum document
 * nests ~50 course items, so create/replace edit the whole document as JSON rather than forcing
 * every field through bespoke inputs - the same trade-off the API console makes for request
 * bodies, and for the same reason: the shape is defined by the schema, not reinvented here.
 */
@Component({
  selector: 'app-curricula-page',
  imports: [ApiErrorBannerComponent, ModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <div class="row-between">
        <h1>Curricula</h1>
        <button type="button" class="btn btn-primary" (click)="openCreate()">New curriculum</button>
      </div>

      <div class="card stack">
        <div class="row" style="align-items: end">
          <div class="field" style="margin-bottom: 0; flex: 1 1 16rem">
            <label for="pensum">Pensum code</label>
            <input id="pensum" type="text" [value]="searchCode()" (input)="onSearchInput($event)" placeholder="1015" />
          </div>
          <button type="button" class="btn" (click)="load()" [disabled]="loading() || !searchCode().trim()">
            {{ loading() ? 'Loading…' : 'Load' }}
          </button>
        </div>

        <app-api-error-banner [error]="error()" />

        @if (!loading() && !error() && !curriculum()) {
          <div class="empty-state">
            <p>Enter a pensum code to load a curriculum - for example 1015 (Ingeniería de Sistemas).</p>
          </div>
        }

        @if (curriculum(); as c) {
          <div class="stack">
            <div class="row-between">
              <h2 style="margin:0">{{ c.programName }} · {{ c.pensumCode }}</h2>
              <button type="button" class="btn btn-sm" (click)="openEdit(c)">Edit this curriculum</button>
            </div>
            <dl class="curriculum-summary">
              <dt>Faculty</dt>
              <dd>{{ c.faculty }}</dd>
              <dt>Reform</dt>
              <dd>{{ c.reform }}</dd>
              <dt>Status</dt>
              <dd><span class="badge badge-neutral">{{ c.status }}</span></dd>
              <dt>Totals</dt>
              <dd>{{ c.totalCredits }} credits · {{ c.totalHours }} weekly hours · {{ c.levels }} levels</dd>
            </dl>

            <h3>Knowledge areas ({{ c.areas.length }})</h3>
            <div class="scroll-x">
              <table>
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Credits</th>
                    <th>Weekly hours</th>
                  </tr>
                </thead>
                <tbody>
                  @for (area of c.areas; track area.code) {
                    <tr>
                      <td class="mono">{{ area.code }}</td>
                      <td>
                        <span class="area-dot" [style.background]="area.color"></span>
                        {{ area.name }}
                      </td>
                      <td>{{ area.credits }}</td>
                      <td>{{ area.hours }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <h3>Pensum items ({{ c.courses.length }})</h3>
            <div class="scroll-x table-scroll-y">
              <table>
                <thead>
                  <tr>
                    <th>Level</th>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Area</th>
                    <th>Credits</th>
                    <th>Prerequisites</th>
                  </tr>
                </thead>
                <tbody>
                  @for (course of c.courses; track course.pensumItemCode) {
                    <tr>
                      <td>{{ course.level }}</td>
                      <td class="mono">{{ course.code ?? '(elective slot)' }}</td>
                      <td>{{ course.name }}</td>
                      <td>{{ course.area }}</td>
                      <td>{{ course.credits }}</td>
                      <td class="text-muted">{{ course.prerequisites.length ? course.prerequisites.join(', ') : '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      </div>
    </div>

    <app-modal #formModal [title]="formMode() === 'create' ? 'New curriculum' : 'Edit curriculum'" (closed)="formError.set(null)">
      <div class="stack">
        <app-api-error-banner [error]="formError()" />
        <p class="text-muted">
          Full curriculum document as JSON, matching the <code>Curriculum</code> schema in
          docs/api/semaphore.openapi.yaml.
        </p>
        <div class="field">
          <label for="curriculum-json">Curriculum document</label>
          <textarea id="curriculum-json" rows="16" [value]="formText()" (input)="onFormTextInput($event)"></textarea>
        </div>
        <div class="row">
          <button type="button" class="btn btn-primary" [disabled]="formSubmitting()" (click)="submit()">
            {{ formSubmitting() ? 'Saving…' : formMode() === 'create' ? 'Create curriculum' : 'Save changes' }}
          </button>
          <button type="button" class="btn" (click)="formModal.close()">Cancel</button>
        </div>
      </div>
    </app-modal>
  `,
  styles: `
    .curriculum-summary {
      display: grid;
      grid-template-columns: 8rem 1fr;
      row-gap: 0.5rem;
      margin: 0;
    }
    .curriculum-summary dt {
      color: var(--text-muted);
      font-size: 0.8125rem;
      font-weight: 600;
    }
    .curriculum-summary dd {
      margin: 0;
    }
    .area-dot {
      display: inline-block;
      width: 0.6rem;
      height: 0.6rem;
      border-radius: 999px;
      margin-right: 0.4rem;
    }
    .table-scroll-y {
      max-height: 22rem;
      overflow-y: auto;
    }
  `,
})
export class CurriculaPage {
  private readonly curriculaService = inject(CurriculaService);

  /** Bound automatically from ?pensum=... via withComponentInputBinding (see Programs "View curriculum"). */
  readonly pensum = input('');

  readonly searchCode = signal('');
  readonly loading = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly curriculum = signal<Curriculum | null>(null);

  readonly formMode = signal<'create' | 'edit'>('create');
  readonly formText = signal('');
  readonly formSubmitting = signal(false);
  readonly formError = signal<ApiError | null>(null);

  @ViewChild('formModal') private formModal?: ModalComponent;

  constructor() {
    const initial = this.pensum();
    if (initial) {
      this.searchCode.set(initial);
      this.load();
    }
  }

  onSearchInput(event: Event): void {
    this.searchCode.set((event.target as HTMLInputElement).value);
  }

  load(): void {
    const code = this.searchCode().trim();
    if (!code) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.curriculaService.getByCode(code).subscribe({
      next: (curriculum) => {
        this.curriculum.set(curriculum);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.curriculum.set(null);
        this.loading.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }

  openCreate(): void {
    this.formMode.set('create');
    this.formText.set(JSON.stringify(CURRICULUM_SKELETON, null, 2));
    this.formError.set(null);
    this.formModal?.open();
  }

  openEdit(curriculum: Curriculum): void {
    this.formMode.set('edit');
    this.formText.set(JSON.stringify(curriculum, null, 2));
    this.formError.set(null);
    this.formModal?.open();
  }

  onFormTextInput(event: Event): void {
    this.formText.set((event.target as HTMLTextAreaElement).value);
  }

  submit(): void {
    let parsed: Curriculum;
    try {
      parsed = JSON.parse(this.formText());
    } catch {
      this.formError.set({
        timestamp: new Date().toISOString(),
        status: 0,
        error: 'Invalid JSON',
        message: 'The curriculum document is not valid JSON.',
        path: '',
      });
      return;
    }

    this.formSubmitting.set(true);
    this.formError.set(null);
    const call =
      this.formMode() === 'create' ? this.curriculaService.create(parsed) : this.curriculaService.replace(parsed.pensumCode, parsed);

    call.subscribe({
      next: (saved) => {
        this.formSubmitting.set(false);
        this.formModal?.close();
        this.curriculum.set(saved);
        this.searchCode.set(saved.pensumCode);
      },
      error: (err: unknown) => {
        this.formSubmitting.set(false);
        this.formError.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }
}
