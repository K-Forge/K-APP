import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import type { Building } from '../buildings/building.model';
import { SPACE_TYPES, type Space, type SpaceRequest, type SpaceType } from './space.model';

type SpaceForm = FormGroup<{
  code: FormControl<string>;
  name: FormControl<string>;
  type: FormControl<SpaceType>;
  buildingCode: FormControl<string>;
  floorLevel: FormControl<number>;
  x: FormControl<number>;
  y: FormControl<number>;
  capacity: FormControl<number | null>;
  aliases: FormArray<FormControl<string>>;
}>;

function aliasControl(value = ''): FormControl<string> {
  return new FormControl(value, { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(120)] });
}

/**
 * Create/edit form for a space. `buildingCode` and `floorLevel` are selects driven by the actual
 * buildings list (not free text) so a developer can't point a pin at a building or floor that
 * doesn't exist - the API would 404 that anyway, this just surfaces it before the round trip.
 */
@Component({
  selector: 'app-space-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="stack">
      <div class="field" [class.invalid]="invalid('code')">
        <label for="s-code">Code</label>
        <input id="s-code" type="text" formControlName="code" placeholder="708" />
        @if (invalid('code')) {
          <span class="error">Required, 1-20 characters.</span>
        }
      </div>

      <div class="field" [class.invalid]="invalid('name')">
        <label for="s-name">Name</label>
        <input id="s-name" type="text" formControlName="name" />
        @if (invalid('name')) {
          <span class="error">Required, 1-120 characters.</span>
        }
      </div>

      <div class="field">
        <label for="s-type">Type</label>
        <select id="s-type" formControlName="type">
          @for (type of spaceTypes; track type) {
            <option [value]="type">{{ type }}</option>
          }
        </select>
      </div>

      <div class="field" [class.invalid]="invalid('buildingCode')">
        <label for="s-building">Building</label>
        <select id="s-building" formControlName="buildingCode">
          <option value="" disabled>choose a building…</option>
          @for (building of buildings(); track building.code) {
            <option [value]="building.code">{{ building.code }} — {{ building.name }}</option>
          }
        </select>
        @if (invalid('buildingCode')) {
          <span class="error">Required.</span>
        }
      </div>

      <div class="field">
        <label for="s-floor">Floor level</label>
        @if (floorOptions().length) {
          <select id="s-floor" formControlName="floorLevel">
            @for (floor of floorOptions(); track floor.level) {
              <option [value]="floor.level">{{ floor.level }} — {{ floor.name }}</option>
            }
          </select>
        } @else {
          <input id="s-floor" type="number" formControlName="floorLevel" min="0" max="99" />
          <span class="hint">Pick a building to choose from its actual floors.</span>
        }
      </div>

      <div class="row spread">
        <div class="field" style="margin-bottom:0; flex: 1 1 8rem">
          <label for="s-x">X (%)</label>
          <input id="s-x" type="number" formControlName="x" min="0" max="100" step="0.1" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 8rem">
          <label for="s-y">Y (%)</label>
          <input id="s-y" type="number" formControlName="y" min="0" max="100" step="0.1" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 8rem">
          <label for="s-capacity">Capacity</label>
          <input id="s-capacity" type="number" formControlName="capacity" min="0" />
        </div>
      </div>

      <div class="row-between">
        <label style="font-size:0.8125rem; font-weight:600; color:var(--text-muted)">
          Aliases <span class="text-faint">(what students actually search for)</span>
        </label>
        <button type="button" class="btn btn-sm" (click)="addAlias()">Add alias</button>
      </div>
      @for (alias of form.controls.aliases.controls; track $index) {
        <div class="row">
          <input type="text" [formControl]="alias" placeholder="sala de sistemas" />
          <button type="button" class="btn btn-sm btn-danger" (click)="removeAlias($index)">Remove</button>
        </div>
      }

      <div class="row">
        <button type="submit" class="btn btn-primary" [disabled]="submitting()">
          {{ submitting() ? 'Saving…' : editing() ? 'Save changes' : 'Create space' }}
        </button>
        <button type="button" class="btn" (click)="cancelled.emit()">Cancel</button>
      </div>
    </form>
  `,
})
export class SpaceFormComponent {
  readonly initial = input<Space | null>(null);
  readonly buildings = input<Building[]>([]);
  readonly submitting = input(false);
  readonly submitted = output<SpaceRequest>();
  readonly cancelled = output<void>();

  readonly spaceTypes = SPACE_TYPES;
  readonly editing = () => this.initial() !== null;

  readonly floorOptions = computed(() => {
    const code = this.selectedBuildingCode();
    return this.buildings().find((b) => b.code === code)?.floors ?? [];
  });

  private readonly selectedBuildingCode = () => this.form.controls.buildingCode.value;

  form: SpaceForm = this.buildForm(null);

  constructor() {
    effect(() => {
      this.form = this.buildForm(this.initial());
    });
  }

  private buildForm(space: Space | null): SpaceForm {
    return new FormGroup({
      code: new FormControl(space?.code ?? '', { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(20)] }),
      name: new FormControl(space?.name ?? '', { nonNullable: true, validators: [Validators.required, Validators.minLength(1), Validators.maxLength(120)] }),
      type: new FormControl(space?.type ?? SPACE_TYPES[0], { nonNullable: true, validators: [Validators.required] }),
      buildingCode: new FormControl(space?.buildingCode ?? '', { nonNullable: true, validators: [Validators.required] }),
      floorLevel: new FormControl(space?.floorLevel ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(99)] }),
      x: new FormControl(space?.x ?? 50, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(100)] }),
      y: new FormControl(space?.y ?? 50, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(100)] }),
      capacity: new FormControl<number | null>(space?.capacity ?? null, { validators: [Validators.min(0)] }),
      aliases: new FormArray((space?.aliases ?? []).map((a) => aliasControl(a))),
    });
  }

  invalid(name: 'code' | 'name' | 'buildingCode'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  addAlias(): void {
    this.form.controls.aliases.push(aliasControl());
  }

  removeAlias(index: number): void {
    this.form.controls.aliases.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.submitted.emit({ ...raw, type: raw.type as Space['type'], capacity: raw.capacity ?? undefined });
  }
}
