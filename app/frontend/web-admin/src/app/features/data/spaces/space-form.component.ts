import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import type { Building } from '../buildings/building.model';
import { CIRCULATION_TYPES, SPACE_TYPES, WINGS, type Space, type SpaceRequest, type SpaceType, type Wing } from './space.model';

type SpaceForm = FormGroup<{
  code: FormControl<string>;
  name: FormControl<string>;
  type: FormControl<SpaceType>;
  buildingCode: FormControl<string>;
  floorLevel: FormControl<number>;
  wing: FormControl<Wing | ''>;
  gridRow: FormControl<number>;
  gridColumn: FormControl<number>;
  rowSpan: FormControl<number>;
  colSpan: FormControl<number>;
  accessVia: FormControl<string>;
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
          <input id="s-floor" type="number" formControlName="floorLevel" min="-5" max="99" />
          <span class="hint">Pick a building to choose from its actual floors.</span>
        }
      </div>

      <div class="field">
        <label for="s-wing">Wing</label>
        <select id="s-wing" formControlName="wing">
          <option value="">Derive from the code (-N, -S, -C)</option>
          @for (w of wings; track w) {
            <option [value]="w">{{ w }}</option>
          }
        </select>
        <span class="hint">301, 301-N and 301-S are three different rooms on one floor.</span>
      </div>

      <div class="row spread">
        <div class="field" style="margin-bottom:0; flex: 1 1 6rem">
          <label for="s-row">Grid row</label>
          <input id="s-row" type="number" formControlName="gridRow" min="0" [max]="maxRow()" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 6rem">
          <label for="s-col">Grid column</label>
          <input id="s-col" type="number" formControlName="gridColumn" min="0" [max]="maxColumn()" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 5rem">
          <label for="s-rowspan">Rows</label>
          <input id="s-rowspan" type="number" formControlName="rowSpan" min="1" max="60" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 5rem">
          <label for="s-colspan">Columns</label>
          <input id="s-colspan" type="number" formControlName="colSpan" min="1" max="60" />
        </div>
        <div class="field" style="margin-bottom:0; flex: 1 1 6rem">
          <label for="s-capacity">Capacity</label>
          <input id="s-capacity" type="number" formControlName="capacity" min="0" />
        </div>
      </div>
      @if (selectedFloor(); as floor) {
        <span class="hint">
          {{ floor.name }} is {{ floor.gridRows }} x {{ floor.gridColumns }}. The server refuses a
          space that would not fit, or that would overlap another.
        </span>
      } @else {
        <span class="hint">Pick a building and floor to see the grid's size.</span>
      }

      <div class="field">
        <label for="s-access">Reached via</label>
        <input id="s-access" type="text" formControlName="accessVia" list="circulation-codes" placeholder="ASC-CENTRAL" />
        <datalist id="circulation-codes">
          @for (option of circulation(); track option.code) {
            <option [value]="option.code">{{ option.name }}</option>
          }
        </datalist>
        <span class="hint">
          The lift, staircase or entrance that serves this space — what produces "piso 4, sube por
          el ascensor central". The suggestions come from the spaces currently listed; the server
          checks the code against the whole building and refuses one that names nothing.
        </span>
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
  readonly wings = WINGS;
  readonly editing = () => this.initial() !== null;

  readonly floorOptions = computed(() => {
    const code = this.selectedBuildingCode();
    return this.buildings().find((b) => b.code === code)?.floors ?? [];
  });

  /** The floor this space claims, so the form can say how big its grid is. */
  readonly selectedFloor = () =>
    this.floorOptions().find((f) => f.level === this.form.controls.floorLevel.value) ?? null;

  readonly maxRow = () => Math.max(0, (this.selectedFloor()?.gridRows ?? 60) - 1);
  readonly maxColumn = () => Math.max(0, (this.selectedFloor()?.gridColumns ?? 60) - 1);

  /**
   * What `accessVia` may point at: the circulation elements already recorded in this building.
   *
   * <p>A select rather than free text. The value has to match another space's code exactly for
   * the client to resolve "sube por el ascensor central" into anything, and a typed code that
   * is almost right fails silently - the app simply says nothing about how to get there.
   */
  readonly circulation = () => {
    const code = this.selectedBuildingCode();
    return this.knownSpaces()
      .filter((s) => s.buildingCode === code && CIRCULATION_TYPES.includes(s.type))
      .sort((a, b) => a.code.localeCompare(b.code));
  };

  readonly knownSpaces = input<Space[]>([]);

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
      floorLevel: new FormControl(space?.floorLevel ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(-5), Validators.max(99)] }),
      wing: new FormControl<Wing | ''>(space?.wing ?? '', { nonNullable: true }),
      gridRow: new FormControl(space?.gridRow ?? 0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
      gridColumn: new FormControl(space?.gridColumn ?? 0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
      rowSpan: new FormControl(space?.rowSpan ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(60)] }),
      colSpan: new FormControl(space?.colSpan ?? 1, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(60)] }),
      accessVia: new FormControl(space?.accessVia ?? '', { nonNullable: true, validators: [Validators.maxLength(20)] }),
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
    // Empty strings mean "not set", not "set to empty": the server treats wing as derivable
    // from the code and accessVia as genuinely optional, and sending "" would fail validation
    // on one and store a meaningless value on the other.
    this.submitted.emit({
      ...raw,
      type: raw.type as Space['type'],
      wing: raw.wing === '' ? null : raw.wing,
      accessVia: raw.accessVia.trim() === '' ? null : raw.accessVia.trim(),
      capacity: raw.capacity ?? undefined,
    });
  }
}
