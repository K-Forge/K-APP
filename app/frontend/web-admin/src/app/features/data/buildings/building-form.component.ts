import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import type { Building, BuildingRequest, Floor } from './building.model';

type FloorForm = FormGroup<{
  level: FormControl<number>;
  name: FormControl<string>;
  gridRows: FormControl<number>;
  gridColumns: FormControl<number>;
}>;

type BuildingForm = FormGroup<{
  code: FormControl<string>;
  name: FormControl<string>;
  campus: FormControl<string>;
  description: FormControl<string>;
  floors: FormArray<FloorForm>;
}>;

function floorGroup(floor?: Floor): FloorForm {
  return new FormGroup({
    // -5 rather than 0: the central building has a basement at level -1, and the rule that a
    // room's first digit is its floor stops applying down there.
    level: new FormControl(floor?.level ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(-5), Validators.max(99)] }),
    name: new FormControl(floor?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(60)] }),
    gridRows: new FormControl(floor?.gridRows ?? 11, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(60)] }),
    gridColumns: new FormControl(floor?.gridColumns ?? 16, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(60)] }),
  });
}

/**
 * Create/edit form for a building, including its floor list. Constraints (lengths, minItems on
 * floors) come straight from BuildingRequest in docs/api/map.openapi.yaml - the point is that this
 * form can never be stricter or looser than what the server actually enforces.
 */
@Component({
  selector: 'app-building-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
      <div class="field" [class.invalid]="invalid('code')">
        <label for="b-code">Code</label>
        <input id="b-code" type="text" formControlName="code" [readonly]="editing()" />
        @if (invalid('code')) {
          <span class="error">Required, 1-10 characters.</span>
        }
      </div>

      <div class="field" [class.invalid]="invalid('name')">
        <label for="b-name">Name</label>
        <input id="b-name" type="text" formControlName="name" />
        @if (invalid('name')) {
          <span class="error">Required, 1-120 characters.</span>
        }
      </div>

      <div class="field" [class.invalid]="invalid('campus')">
        <label for="b-campus">Campus</label>
        <input id="b-campus" type="text" formControlName="campus" />
        @if (invalid('campus')) {
          <span class="error">Required, 1-120 characters.</span>
        }
      </div>

      <div class="field">
        <label for="b-description">Description</label>
        <textarea id="b-description" rows="2" formControlName="description"></textarea>
        <span class="hint">Optional, up to 500 characters.</span>
      </div>

      <div class="row-between">
        <h3 style="margin:0">Floors</h3>
        <button type="button" class="btn btn-sm" (click)="addFloor()">Add floor</button>
      </div>
      @if (form.controls.floors.invalid && form.controls.floors.touched) {
        <span class="error">At least one floor is required.</span>
      }

      @for (floor of form.controls.floors.controls; track $index) {
        <div class="card floor-row" [formGroup]="floor">
          <div class="row-between">
            <strong>Floor {{ $index + 1 }}</strong>
            <button type="button" class="btn btn-sm btn-danger" (click)="removeFloor($index)" [disabled]="form.controls.floors.length <= 1">
              Remove
            </button>
          </div>
          <div class="floor-grid">
            <div class="field">
              <label [for]="'level-' + $index">Level</label>
              <input [id]="'level-' + $index" type="number" formControlName="level" min="-5" max="99" />
              <span class="hint">-1 is the basement.</span>
            </div>
            <div class="field">
              <label [for]="'name-' + $index">Name</label>
              <input [id]="'name-' + $index" type="text" formControlName="name" />
            </div>
            <div class="field">
              <label [for]="'rows-' + $index">Grid rows</label>
              <input [id]="'rows-' + $index" type="number" formControlName="gridRows" min="1" max="60" />
            </div>
            <div class="field">
              <label [for]="'cols-' + $index">Grid columns</label>
              <input [id]="'cols-' + $index" type="number" formControlName="gridColumns" min="1" max="60" />
            </div>
          </div>
          @if (corridorCount($index); as count) {
            <p class="hint" style="margin:0.5rem 0 0">
              {{ count }} corridor{{ count === 1 ? '' : 's' }} on this floor, kept as they are.
              Corridors are drawn in the offline grid editor
              (<code>map-service/src/main/resources/static/admin/grid-editor.html</code>), which
              is also how a floor is captured in the first place.
            </p>
          }
        </div>
      }

      <div class="row">
        <button type="submit" class="btn btn-primary" [disabled]="submitting()">
          {{ submitting() ? 'Saving…' : editing() ? 'Save changes' : 'Create building' }}
        </button>
        <button type="button" class="btn" (click)="cancelled.emit()">Cancel</button>
      </div>
    </form>
  `,
  styles: `
    .floor-row {
      padding: 0.75rem;
    }
    .floor-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
      gap: 0.75rem;
      margin-top: 0.5rem;
    }
  `,
})
export class BuildingFormComponent {
  readonly initial = input<Building | null>(null);
  readonly submitting = input(false);
  readonly submitted = output<BuildingRequest>();
  readonly cancelled = output<void>();

  readonly editing = () => this.initial() !== null;

  form: BuildingForm = this.buildForm(null);

  constructor() {
    effect(() => {
      this.form = this.buildForm(this.initial());
    });
  }

  private buildForm(building: Building | null): BuildingForm {
    return new FormGroup({
      code: new FormControl(building?.code ?? '', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1), Validators.maxLength(10)],
      }),
      name: new FormControl(building?.name ?? '', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1), Validators.maxLength(120)],
      }),
      campus: new FormControl(building?.campus ?? '', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1), Validators.maxLength(120)],
      }),
      description: new FormControl(building?.description ?? '', { nonNullable: true, validators: [Validators.maxLength(500)] }),
      floors: new FormArray(
        (building?.floors.length ? building.floors : [undefined]).map((f) => floorGroup(f)),
        [Validators.required, Validators.minLength(1)],
      ),
    });
  }

  invalid(name: 'code' | 'name' | 'campus'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  addFloor(): void {
    this.form.controls.floors.push(
      floorGroup({ level: this.form.controls.floors.length, name: '', gridRows: 11, gridColumns: 16 }),
    );
  }

  /** How many corridors the floor at this index already has, so the form can say it keeps them. */
  corridorCount(index: number): number {
    return this.initial()?.floors[index]?.corridors?.length ?? 0;
  }

  removeFloor(index: number): void {
    this.form.controls.floors.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Corridors are carried through untouched. This form edits a floor's shape; its corridors
    // are polylines through the grid, which is a drawing job and belongs in the grid editor.
    // Dropping them here because the form does not show them would silently erase somebody's
    // afternoon of walking a floor.
    const raw = this.form.getRawValue();
    const existing = this.initial()?.floors ?? [];
    this.submitted.emit({
      ...raw,
      floors: raw.floors.map((floor) => ({
        ...floor,
        corridors: existing.find((f) => f.level === floor.level)?.corridors ?? [],
      })),
    });
  }
}
