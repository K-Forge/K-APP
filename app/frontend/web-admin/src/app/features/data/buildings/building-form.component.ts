import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import type { Building, BuildingRequest, Floor } from './building.model';

type FloorForm = FormGroup<{
  level: FormControl<number>;
  name: FormControl<string>;
  planImageUrl: FormControl<string>;
  imageWidth: FormControl<number>;
  imageHeight: FormControl<number>;
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
    level: new FormControl(floor?.level ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(99)] }),
    name: new FormControl(floor?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(60)] }),
    planImageUrl: new FormControl(floor?.planImageUrl ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(1), Validators.maxLength(255)],
    }),
    imageWidth: new FormControl(floor?.imageWidth ?? 1024, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
    imageHeight: new FormControl(floor?.imageHeight ?? 768, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
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
              <input [id]="'level-' + $index" type="number" formControlName="level" min="0" max="99" />
            </div>
            <div class="field">
              <label [for]="'name-' + $index">Name</label>
              <input [id]="'name-' + $index" type="text" formControlName="name" />
            </div>
            <div class="field">
              <label [for]="'url-' + $index">Plan image URL</label>
              <input [id]="'url-' + $index" type="text" formControlName="planImageUrl" />
            </div>
            <div class="field">
              <label [for]="'w-' + $index">Image width (px)</label>
              <input [id]="'w-' + $index" type="number" formControlName="imageWidth" min="1" />
            </div>
            <div class="field">
              <label [for]="'h-' + $index">Image height (px)</label>
              <input [id]="'h-' + $index" type="number" formControlName="imageHeight" min="1" />
            </div>
          </div>
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
    this.form.controls.floors.push(floorGroup({ level: this.form.controls.floors.length, name: '', planImageUrl: '', imageWidth: 1024, imageHeight: 768 }));
  }

  removeFloor(index: number): void {
    this.form.controls.floors.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.getRawValue());
  }
}
