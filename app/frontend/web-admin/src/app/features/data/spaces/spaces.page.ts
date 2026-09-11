import { ChangeDetectionStrategy, Component, ViewChild, effect, inject, signal } from '@angular/core';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import type { PageResponse } from '../../../core/http/page-response.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import { PageIntroComponent } from '../../../shared/ui/page-intro/page-intro.component';
import { DataTableComponent } from '../../../shared/ui/data-table/data-table.component';
import { ModalComponent } from '../../../shared/ui/modal/modal.component';
import type { Building } from '../buildings/building.model';
import { BuildingsService } from '../buildings/buildings.service';
import { SpaceFormComponent } from './space-form.component';
import { SPACE_TYPES, type Space, type SpaceRequest, type SpaceType } from './space.model';
import { SpacesService } from './spaces.service';

const PAGE_SIZE = 20;
const MIN_QUERY_LENGTH = 2;

/** Full-text search plus CRUD over /api/map/spaces - the "find a room" screen turned inside out. */
@Component({
  selector: 'app-spaces-page',
  imports: [DataTableComponent, ApiErrorBannerComponent, ModalComponent, SpaceFormComponent, PageIntroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <app-page-intro
        title="Spaces"
        what="Every room, lab, lift, stairwell and corridor the map can point at. Each one sits in a cell of the grid of its floor."
        [can]="['Create a space and place it on the grid', 'Edit where it sits, what it is called and what it is', 'Delete one', 'Search by code, name or alias']"
        note="A wing is a field, not a suffix: 301-N, 301-S and 301 are three different rooms. The access route names the lift or stairs that serves the space — it is what produces &quot;take the central lift to floor 4&quot; — and it has to name a real one in the same building, or the save is refused."
      >
        <button actions type="button" class="btn btn-primary" (click)="openCreate()">New space</button>
      </app-page-intro>

      <div class="card stack">
        <div class="row spread">
          <div class="field" style="flex: 1 1 14rem; margin-bottom: 0">
            <label for="q">Search</label>
            <input id="q" type="text" placeholder="name, code or alias (min 2 chars)" (input)="onQueryInput($event)" />
          </div>
          <div class="field" style="margin-bottom: 0">
            <label for="type">Type</label>
            <select id="type" (change)="onTypeChange($event)">
              <option value="">All types</option>
              @for (type of spaceTypes; track type) {
                <option [value]="type">{{ type }}</option>
              }
            </select>
          </div>
          <div class="field" style="margin-bottom: 0">
            <label for="building">Building</label>
            <select id="building" (change)="onBuildingChange($event)">
              <option value="">All buildings</option>
              @for (building of buildings(); track building.code) {
                <option [value]="building.code">{{ building.code }}</option>
              }
            </select>
          </div>
        </div>

        <app-api-error-banner [error]="error()" />

        @if (query().trim().length < minQueryLength) {
          <div class="empty-state">
            <p>Type at least {{ minQueryLength }} characters to search spaces.</p>
          </div>
        } @else {
          <app-data-table
            [loading]="loading()"
            [empty]="!loading() && !error() && (result()?.content?.length ?? 0) === 0"
            emptyMessage="No spaces match this search."
            [totalItems]="result()?.totalElements ?? null"
            [page]="page()"
            [totalPages]="result()?.totalPages ?? 0"
            (pageChange)="onPageChange($event)"
          >
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Type</th>
                <th>Building</th>
                <th>Floor</th>
                <th>Capacity</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (space of result()?.content ?? []; track space.id) {
                <tr>
                  <td class="mono">{{ space.code }}</td>
                  <td>{{ space.name }}</td>
                  <td><span class="badge badge-neutral">{{ space.type }}</span></td>
                  <td>{{ space.buildingCode }}</td>
                  <td>{{ space.floorLevel }}</td>
                  <td class="text-muted">{{ space.capacity ?? '—' }}</td>
                  <td class="row">
                    <button type="button" class="btn btn-sm" (click)="openEdit(space)">Edit</button>
                    <button type="button" class="btn btn-sm btn-danger" [disabled]="deletingId() === space.id" (click)="remove(space)">
                      Delete
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </app-data-table>
        }
      </div>
    </div>

    <app-modal #formModal [title]="editingSpace() ? 'Edit space' : 'New space'" (closed)="formError.set(null)">
      <app-api-error-banner [error]="formError()" />
      <app-space-form
        [initial]="editingSpace()"
        [buildings]="buildings()"
        [knownSpaces]="result()?.content ?? []"
        [submitting]="formSubmitting()"
        (submitted)="save($event)"
        (cancelled)="formModal.close()"
      />
    </app-modal>
  `,
})
export class SpacesPage {
  private readonly spacesService = inject(SpacesService);
  private readonly buildingsService = inject(BuildingsService);

  readonly spaceTypes = SPACE_TYPES;
  readonly minQueryLength = MIN_QUERY_LENGTH;

  readonly query = signal('');
  readonly type = signal<SpaceType | ''>('');
  readonly buildingCodeFilter = signal('');
  readonly page = signal(0);
  /** Bumped after a save/delete to re-run the search effect without changing any real filter. */
  private readonly refreshTick = signal(0);

  readonly loading = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly result = signal<PageResponse<Space> | null>(null);
  readonly buildings = signal<Building[]>([]);

  readonly editingSpace = signal<Space | null>(null);
  readonly formSubmitting = signal(false);
  readonly formError = signal<ApiError | null>(null);
  readonly deletingId = signal<string | null>(null);

  @ViewChild('formModal') private formModal?: ModalComponent;

  private debounceHandle?: ReturnType<typeof setTimeout>;

  private readonly searchEffect = effect(() => {
    const q = this.query().trim();
    const type = this.type();
    const buildingCode = this.buildingCodeFilter();
    const page = this.page();
    this.refreshTick();

    if (q.length < MIN_QUERY_LENGTH) {
      this.result.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.spacesService.search({ q, page, size: PAGE_SIZE, type: type || undefined, buildingCode: buildingCode || undefined }).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  });

  constructor() {
    this.buildingsService.list().subscribe({ next: (buildings) => this.buildings.set(buildings) });
  }

  onQueryInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    clearTimeout(this.debounceHandle);
    this.debounceHandle = setTimeout(() => {
      this.page.set(0);
      this.query.set(value);
    }, 300);
  }

  onTypeChange(event: Event): void {
    this.page.set(0);
    this.type.set((event.target as HTMLSelectElement).value as SpaceType | '');
  }

  onBuildingChange(event: Event): void {
    this.page.set(0);
    this.buildingCodeFilter.set((event.target as HTMLSelectElement).value);
  }

  onPageChange(page: number): void {
    this.page.set(page);
  }

  openCreate(): void {
    this.editingSpace.set(null);
    this.formError.set(null);
    this.formModal?.open();
  }

  openEdit(space: Space): void {
    this.editingSpace.set(space);
    this.formError.set(null);
    this.formModal?.open();
  }

  save(request: SpaceRequest): void {
    this.formSubmitting.set(true);
    this.formError.set(null);
    const editing = this.editingSpace();
    const call = editing ? this.spacesService.update(editing.code, editing.buildingCode, request) : this.spacesService.create(request);

    call.subscribe({
      next: () => {
        this.formSubmitting.set(false);
        this.formModal?.close();
        this.rerunSearch();
      },
      error: (err: unknown) => {
        this.formSubmitting.set(false);
        this.formError.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }

  remove(space: Space): void {
    if (!window.confirm(`Delete space ${space.code} — ${space.name}? This cannot be undone.`)) {
      return;
    }
    this.deletingId.set(space.id);
    this.spacesService.delete(space.code, space.buildingCode).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.rerunSearch();
      },
      error: (err: unknown) => {
        this.deletingId.set(null);
        this.error.set(err instanceof AppHttpError ? err.apiError : null);
      },
    });
  }

  private rerunSearch(): void {
    this.refreshTick.update((n) => n + 1);
  }
}
