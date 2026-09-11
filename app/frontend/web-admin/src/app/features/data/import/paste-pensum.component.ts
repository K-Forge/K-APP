import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppHttpError } from '../../../core/http/api-http-error';
import type { ApiError } from '../../../core/http/api-error.model';
import { ApiErrorBannerComponent } from '../../../shared/ui/api-error-banner/api-error-banner.component';
import type { CurriculumImportReport } from './import.model';
import { CurriculumImportService } from './import.service';
import {
  ITEM_FIELDS,
  PENSUM_STATUSES,
  PROGRAM_LEVELS,
  csvCell,
  looksLikeHeadingRow,
  looksTrue,
  mapFromHeadings,
  parsePaste,
  type ItemField,
  type PastedRow,
} from './paste-pensum.model';

/**
 * Build a pensum by pasting the table straight out of its PDF, correcting it in place, and
 * importing when it adds up.
 *
 * <h2>Why paste rather than parse the PDF</h2>
 * The pensums arrive as PDFs, laid out twenty-four different ways. A server-side parser that
 * reads twenty of them correctly is worse than none, because every one has to be checked anyway
 * and you would not know which four it got wrong. Copying a table out of a PDF preserves its
 * rows far better than any reconstruction of the page, and it costs nothing to build.
 *
 * <h2>Why the grid is the point</h2>
 * Whatever produces the rows, a person has to verify them: a mistyped prerequisite throws no
 * error, it silently breaks one student's semáforo months later. So the value is in the
 * correcting, not the parsing — which is why the totals are computed live, against what the
 * document declares, while you are still looking at the rows that produce them.
 *
 * <p>The output is the same CSV the API already accepts. Nothing new on the server: the same
 * validation runs, and the file that lands in `docs/templates/pensums/` is still the source
 * of truth — which matters, because the cluster has no backups.
 */
@Component({
  selector: 'app-paste-pensum',
  imports: [FormsModule, ApiErrorBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack">
      <!-- ── 1. The pensum itself ───────────────────────────────────────── -->
      <section class="card stack">
        <h3 class="step"><span class="step-n">1</span> The pensum</h3>
        <p class="text-muted" style="margin:0">
          Typed once. Every row of the CSV repeats it, which is how a spreadsheet works and how
          the import checks the rows agree with each other.
        </p>

        <div class="grid-3">
          <div class="field">
            <label for="pp-pcode">Program code</label>
            <input id="pp-pcode" [(ngModel)]="header.programCode" placeholder="506" />
          </div>
          <div class="field" style="grid-column: span 2">
            <label for="pp-pname">Program name</label>
            <input id="pp-pname" [(ngModel)]="header.programName" placeholder="Ingeniería de Sistemas" />
          </div>
          <div class="field" style="grid-column: span 2">
            <label for="pp-fac">Faculty</label>
            <input id="pp-fac" [(ngModel)]="header.faculty" placeholder="Facultad de Matemáticas e Ingenierías" />
          </div>
          <div class="field">
            <label for="pp-plevel">Level</label>
            <select id="pp-plevel" [(ngModel)]="header.programLevel">
              @for (l of programLevels; track l) {
                <option [value]="l">{{ l }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="pp-code">Pensum code</label>
            <input id="pp-code" [(ngModel)]="header.pensumCode" placeholder="1015" />
          </div>
          <div class="field">
            <label for="pp-reform">Reform</label>
            <input id="pp-reform" [(ngModel)]="header.reform" placeholder="Reforma 2018" />
          </div>
          <div class="field">
            <label for="pp-status">Status</label>
            <select id="pp-status" [(ngModel)]="header.pensumStatus">
              @for (st of statuses; track st) {
                <option [value]="st">{{ st }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label for="pp-dc">Declared credits</label>
            <input id="pp-dc" type="number" [(ngModel)]="header.declaredCredits" />
          </div>
          <div class="field">
            <label for="pp-dh">Declared weekly hours</label>
            <input id="pp-dh" type="number" [(ngModel)]="header.declaredHours" />
          </div>
          <div class="field">
            <label for="pp-lv">Levels</label>
            <input id="pp-lv" type="number" [(ngModel)]="header.levels" placeholder="9" />
          </div>
        </div>
        <p class="hint" style="margin:0">
          Declared credits and hours are what the official document says. They are checked
          against the rows below, not trusted — that check is how we found the seeded plan
          declaring 142 credits where its courses give 144.
        </p>
      </section>

      <!-- ── 2. The paste ───────────────────────────────────────────────── -->
      <section class="card stack">
        <h3 class="step"><span class="step-n">2</span> Paste the table</h3>
        <p class="text-muted" style="margin:0">
          Select the course table in the PDF, copy, and paste here. Tabs or runs of spaces both
          work — one row per line. Headers and page furniture can come along; you delete those
          rows in step 3.
        </p>
        <textarea
          rows="8"
          [value]="pasted()"
          (input)="onPaste($event)"
          placeholder="1001&#9;10011&#9;Cálculo Diferencial&#9;1&#9;3&#9;4&#9;CB&#9;"
        ></textarea>
        <div class="row">
          <button type="button" class="btn btn-primary" [disabled]="!pasted().trim()" (click)="parse()">
            Parse into rows
          </button>
          @if (rows().length) {
            <button type="button" class="btn" (click)="clear()">Clear</button>
          }
        </div>
      </section>

      <!-- ── 3. Correct it ──────────────────────────────────────────────── -->
      @if (rows().length) {
        <section class="card stack">
          <h3 class="step"><span class="step-n">3</span> Say what each column is, then fix what is wrong</h3>

          <!-- What the parse decided on its own. Both of these change what gets imported, so
               neither one happens silently. -->
          @if (headingRow(); as heading) {
            <p class="did">
              The first line looked like a heading and was <strong>not</strong> imported as a
              course: <em>{{ heading.join(' · ') }}</em>. The columns below are named after it.
              <button type="button" class="btn btn-sm" (click)="keepHeadingAsRow()">
                No — it is a course, put it back
              </button>
            </p>
          }
          @if (itemCodeFromCourseCode()) {
            <p class="did">
              No item-code column, so each course's <strong>own code is used as its item
              code</strong> — it only has to be unique inside this pensum. Elective slots have no
              course code, so those rows stay flagged until you type one.
            </p>
          }

          <div class="scroll-x">
            <table class="paste-grid">
              <thead>
                <tr>
                  <th class="rownum"></th>
                  @for (c of columnIndexes(); track c) {
                    <th>
                      <!-- [selected] on the option, not [value] on the select: Angular sets
                           the select's value before the @for has rendered its options, so the
                           guessed mapping existed but showed as blank - which reads as "it
                           detected nothing". -->
                      <select (change)="mapColumn(c, $event)">
                        <option value="" [selected]="!mapping()[c]">— ignore —</option>
                        @for (f of fields; track f.key) {
                          <option [value]="f.key" [selected]="mapping()[c] === f.key">
                            {{ f.label }}{{ f.required ? ' *' : '' }}
                          </option>
                        }
                      </select>
                    </th>
                  }
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <!-- "let r = $index" is not decoration. Inside the inner @for, $index is the
                     COLUMN's index, not the row's - so editCell($index, …) wrote to the wrong
                     row, and on a table with more columns than rows it indexed past the end and
                     threw, which stopped every row after the first from rendering at all. -->
                @for (row of rows(); track $index; let r = $index) {
                  <tr [class.row-bad]="rowProblems()[r].length > 0">
                    <td class="rownum" [title]="rowProblems()[r].join(' · ')">
                      {{ r + 1 }}
                      @if (rowProblems()[r].length) {
                        <span class="row-flag">!</span>
                      }
                    </td>
                    @for (c of columnIndexes(); track c) {
                      <td>
                        <input
                          [value]="row.cells[c] ?? ''"
                          (input)="editCell(r, c, $event)"
                          [class.cell-bad]="cellIsBad(r, c)"
                        />
                      </td>
                    }
                    <td>
                      <button type="button" class="btn btn-sm btn-danger" (click)="removeRow(r)">Drop</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- The arithmetic, while you are still looking at the rows that produce it. -->
          <div class="totals">
            <div class="total" [class.total-bad]="creditsMismatch()">
              <span class="total-label">Credits</span>
              <span class="total-value">{{ totals().credits }}</span>
              @if (header.declaredCredits !== null) {
                <span class="total-vs">declared {{ header.declaredCredits }}</span>
              }
            </div>
            <div class="total" [class.total-bad]="hoursMismatch()">
              <span class="total-label">Weekly hours</span>
              <span class="total-value">{{ totals().hours }}</span>
              @if (header.declaredHours !== null) {
                <span class="total-vs">declared {{ header.declaredHours }}</span>
              }
            </div>
            <div class="total">
              <span class="total-label">Rows</span>
              <span class="total-value">{{ rows().length }}</span>
              @if (badRowCount()) {
                <span class="total-vs total-bad">{{ badRowCount() }} with problems</span>
              }
            </div>
          </div>

          @if (raggedRows().length) {
            <p class="warn">
              <strong>Rows {{ raggedRows().join(', ') }} have a different number of columns</strong>
              than the rest. Copying from a PDF does this whenever a cell's own text contains
              what looked like a column break — a course name with two spaces in it, or a number
              sitting right after the name with only one space before it. Their values are
              shifted, so check them before anything else.
            </p>
          }
          @if (unknownPrerequisites().length) {
            <p class="warn">
              Prerequisites that name nothing in this paste:
              <strong>{{ unknownPrerequisites().join(', ') }}</strong>. The import refuses these,
              so fix the code or drop it.
            </p>
          }
          @if (missingRequired().length) {
            <p class="warn">
              Not assigned to any column yet: <strong>{{ missingRequired().join(', ') }}</strong>.
            </p>
          }
        </section>

        <!-- ── 4. Out ───────────────────────────────────────────────────── -->
        <section class="card stack">
          <h3 class="step"><span class="step-n">4</span> Check it, then import</h3>
          <div class="row">
            <button type="button" class="btn btn-primary" [disabled]="!ready() || busy()" (click)="send(true)">
              {{ busy() && dryRun() ? 'Checking…' : 'Check — writes nothing' }}
            </button>
            <button type="button" class="btn" [disabled]="!ready() || busy()" (click)="send(false)">
              {{ busy() && !dryRun() ? 'Importing…' : 'Import for real' }}
            </button>
            <button type="button" class="btn" [disabled]="!ready()" (click)="download()">
              Download the CSV
            </button>
          </div>
          <p class="hint" style="margin:0">
            Keep the CSV. It is what goes in <code>docs/templates/pensums/</code>, and with no
            backups on the cluster it is the only copy that survives a mistake.
          </p>

          <app-api-error-banner [error]="error()" />

          @if (report(); as r) {
            <div class="card stack" style="border-color: var(--success)">
              <strong>{{ r.dryRun ? 'Valid — nothing was written' : 'Imported' }}</strong>
              @for (c of r.curricula; track c.pensumCode) {
                <p style="margin:0" class="text-muted">
                  {{ c.pensumCode }} · {{ c.programName }} — {{ c.courses }} courses,
                  {{ c.computedCredits }} credits, {{ c.computedHours }} weekly hours.
                </p>
              }
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .step {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1rem;
    }
    .step-n {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 1.5rem;
      height: 1.5rem;
      border-radius: 50%;
      background: var(--primary-bg);
      color: var(--primary-strong);
      font-size: 0.8125rem;
      font-weight: 700;
      flex: 0 0 auto;
    }
    .grid-3 {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.5rem 0.75rem;
    }
    .grid-3 .field {
      margin-bottom: 0;
    }
    @media (max-width: 720px) {
      .grid-3 {
        grid-template-columns: 1fr;
      }
      .grid-3 .field[style] {
        grid-column: auto !important;
      }
    }

    .paste-grid {
      border-collapse: collapse;
      font-size: 0.8125rem;
    }
    .paste-grid th,
    .paste-grid td {
      padding: 0.15rem;
      border: 1px solid var(--border);
    }
    .paste-grid input,
    .paste-grid select {
      width: 100%;
      min-width: 7rem;
      padding: 0.25rem 0.35rem;
      font-size: 0.8125rem;
      border: 1px solid transparent;
      background: transparent;
    }
    .paste-grid input:focus {
      border-color: var(--focus-ring);
      background: var(--bg-elevated);
    }
    .paste-grid .rownum {
      width: 2.4rem;
      text-align: right;
      color: var(--text-faint);
      background: var(--bg-inset);
    }
    .row-bad .rownum {
      background: var(--danger-bg);
      color: var(--danger);
    }
    .row-flag {
      font-weight: 700;
    }
    .cell-bad {
      background: var(--danger-bg) !important;
    }

    .totals {
      display: flex;
      flex-wrap: wrap;
      gap: 1.5rem;
    }
    .total {
      display: flex;
      align-items: baseline;
      gap: 0.4rem;
    }
    .total-label {
      font-size: 0.6875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
    }
    .total-value {
      font-size: 1.25rem;
      font-weight: 700;
    }
    .total-vs {
      font-size: 0.75rem;
      color: var(--text-muted);
    }
    .total-bad .total-value,
    .total-vs.total-bad {
      color: var(--danger);
    }
    .did {
      margin: 0;
      padding: 0.5rem 0.75rem;
      border-left: 3px solid var(--brand-teal);
      background: color-mix(in srgb, var(--brand-teal) 10%, transparent);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      font-size: 0.8125rem;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
    }
    .did em {
      color: var(--text-muted);
      font-style: normal;
    }

    .warn {
      margin: 0;
      padding: 0.5rem 0.75rem;
      border-left: 3px solid var(--warning);
      background: var(--warning-bg);
      border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      font-size: 0.8125rem;
    }
  `,
})
export class PastePensumComponent {
  private readonly service = inject(CurriculumImportService);

  readonly fields = ITEM_FIELDS;
  readonly programLevels = PROGRAM_LEVELS;
  readonly statuses = PENSUM_STATUSES;

  header = {
    programCode: '',
    programName: '',
    faculty: '',
    programLevel: 'PREGRADO',
    pensumCode: '',
    reform: '',
    pensumStatus: 'DRAFT',
    declaredCredits: null as number | null,
    declaredHours: null as number | null,
    levels: null as number | null,
  };

  readonly pasted = signal('');
  readonly rows = signal<PastedRow[]>([]);
  /** The heading line, when the paste had one and it was left out of the courses. */
  readonly headingRow = signal<string[] | null>(null);
  /** Column index → the field it carries. Guessed on parse, corrected by hand. */
  readonly mapping = signal<Record<number, ItemField | undefined>>({});

  readonly busy = signal(false);
  readonly dryRun = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly report = signal<CurriculumImportReport | null>(null);

  readonly columnIndexes = computed(() => {
    const widest = this.rows().reduce((n, r) => Math.max(n, r.cells.length), 0);
    return Array.from({ length: widest }, (_, i) => i);
  });

  onPaste(event: Event): void {
    this.pasted.set((event.target as HTMLTextAreaElement).value);
  }

  parse(): void {
    const all = parsePaste(this.pasted());

    // A table copied out of a PDF brings its heading line with it. Left in place it becomes a
    // course called "Asignatura" with no credits, and - worse - it poisons the column guess,
    // because the guess reads the shape of the values and every column then looks like text.
    const hasHeading = all.length > 1 && looksLikeHeadingRow(all[0].cells);
    const rows = hasHeading ? all.slice(1) : all;
    const seed = hasHeading ? mapFromHeadings(all[0].cells) : {};

    this.headingRow.set(hasHeading ? all[0].cells : null);
    this.rows.set(rows);
    this.mapping.set(this.guessMapping(rows, seed));
    this.report.set(null);
    this.error.set(null);
  }

  /**
   * Puts the heading line back as a course row.
   *
   * <p>The detection can be wrong, and when it is it costs a whole course with no trace. One
   * button undoes it; the columns it already worked out stay.
   */
  keepHeadingAsRow(): void {
    const heading = this.headingRow();
    if (!heading) return;
    this.rows.set([{ cells: heading }, ...this.rows()]);
    this.headingRow.set(null);
  }

  clear(): void {
    this.rows.set([]);
    this.headingRow.set(null);
    this.mapping.set({});
    this.pasted.set('');
    this.report.set(null);
  }

  /**
   * A first guess at what each column holds, so the common case needs no dropdowns at all.
   *
   * <p>The headings, when the paste has them, are taken at their word - they are the author's
   * own statement of what the column is. Everything they do not cover falls back to the shape
   * of the values, which is deliberately shallow: a wrong guess costs one dropdown, but a
   * clever guess that is wrong in a way nobody notices costs a corrupted pensum.
   */
  private guessMapping(
    rows: PastedRow[],
    seed: Record<number, ItemField | undefined> = {},
  ): Record<number, ItemField | undefined> {
    const sample = rows.slice(0, 12);
    const out: Record<number, ItemField | undefined> = { ...seed };
    const used = new Set<ItemField>(Object.values(seed).filter((f): f is ItemField => !!f));

    const columnValues = (c: number) => sample.map((r) => (r.cells[c] ?? '').trim()).filter(Boolean);
    const allSmallInts = (vals: string[], max: number) =>
      vals.length > 0 && vals.every((v) => /^\d{1,2}$/.test(v) && Number(v) <= max);

    for (const c of Array.from({ length: Math.max(...rows.map((r) => r.cells.length), 0) }, (_, i) => i)) {
      if (out[c]) continue;
      const vals = columnValues(c);
      if (!vals.length) continue;

      const take = (f: ItemField) => {
        if (!used.has(f)) {
          out[c] = f;
          used.add(f);
        }
      };

      // The longest free text is the name; small integers are level, credits or hours in the
      // order they conventionally appear; short alphanumerics are codes.
      const avgLen = vals.reduce((n, v) => n + v.length, 0) / vals.length;
      if (avgLen > 12 && vals.some((v) => /\s/.test(v))) {
        take('courseName');
      } else if (allSmallInts(vals, 12) && !used.has('courseLevel')) {
        take('courseLevel');
      } else if (allSmallInts(vals, 12) && !used.has('credits')) {
        take('credits');
      } else if (allSmallInts(vals, 20) && !used.has('weeklyHours')) {
        take('weeklyHours');
      } else if (vals.every((v) => /^[A-Z]{2,5}$/.test(v)) && !used.has('areaCode')) {
        take('areaCode');
      } else if (!used.has('pensumItemCode')) {
        take('pensumItemCode');
      } else if (!used.has('courseCode')) {
        take('courseCode');
      }
    }
    return out;
  }

  mapColumn(index: number, event: Event): void {
    const value = (event.target as HTMLSelectElement).value as ItemField | '';
    const next = { ...this.mapping() };
    // A field can only come from one column; assigning it elsewhere frees the old one rather
    // than silently producing two values for the same thing.
    for (const key of Object.keys(next)) {
      if (next[Number(key)] === value) {
        next[Number(key)] = undefined;
      }
    }
    next[index] = value === '' ? undefined : value;
    this.mapping.set(next);
  }

  editCell(rowIndex: number, column: number, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    const rows = this.rows().map((r, i) => {
      if (i !== rowIndex) return r;
      const cells = [...r.cells];
      while (cells.length <= column) cells.push('');
      cells[column] = value;
      return { cells };
    });
    this.rows.set(rows);
  }

  removeRow(index: number): void {
    this.rows.set(this.rows().filter((_, i) => i !== index));
  }

  private columnFor(field: ItemField): number | null {
    const entry = Object.entries(this.mapping()).find(([, f]) => f === field);
    return entry ? Number(entry[0]) : null;
  }

  private valueOf(row: PastedRow, field: ItemField): string {
    const resolved =
      field === 'pensumItemCode' && this.itemCodeFromCourseCode() ? 'courseCode' : field;
    const c = this.columnFor(resolved);
    return c === null ? '' : (row.cells[c] ?? '').trim();
  }

  /**
   * Whether the item code is being taken from the course code.
   *
   * <p>A pensum PDF carries one code per course and no separate item number, so demanding a
   * column for it would mean typing 60 numbers by hand for every pensum. The item code only
   * has to be unique within the pensum, and the course code already is - except on an elective
   * slot, which has no course code, and there the admin does have to type one. That row is
   * flagged like any other empty required cell.
   */
  readonly itemCodeFromCourseCode = computed(
    () => this.columnFor('pensumItemCode') === null && this.columnFor('courseCode') !== null,
  );

  private hasSource(field: ItemField): boolean {
    if (field === 'pensumItemCode' && this.itemCodeFromCourseCode()) return true;
    return this.columnFor(field) !== null;
  }

  readonly missingRequired = computed(() =>
    ITEM_FIELDS.filter((f) => f.required && !this.hasSource(f.key)).map((f) => f.label),
  );

  readonly rowProblems = computed(() =>
    this.rows().map((row) => {
      const problems: string[] = [];
      for (const f of ITEM_FIELDS) {
        if (!f.required) continue;
        if (!this.hasSource(f.key)) continue;
        if (!this.valueOf(row, f.key)) problems.push(`${f.label} is empty`);
      }
      for (const numeric of ['courseLevel', 'credits', 'weeklyHours'] as const) {
        const v = this.valueOf(row, numeric);
        if (v && !/^\d+$/.test(v)) problems.push(`${numeric} is not a whole number`);
      }
      // An elective slot has no course code; anything else must have one.
      const elective = looksTrue(this.valueOf(row, 'isElectiveSlot'));
      const code = this.valueOf(row, 'courseCode');
      if (elective && code) problems.push('an elective slot must not carry a course code');
      return problems;
    }),
  );

  cellIsBad(rowIndex: number, column: number): boolean {
    const field = this.mapping()[column];
    if (!field) return false;
    const row = this.rows()[rowIndex];
    // A template expression that throws takes the whole table down with it, and the row that
    // failed is not the one that looks wrong. Cheap insurance.
    if (!row) return false;
    const spec = ITEM_FIELDS.find((f) => f.key === field);
    const value = (row.cells[column] ?? '').trim();
    if (spec?.required && !value) return true;
    if (['courseLevel', 'credits', 'weeklyHours'].includes(field) && value && !/^\d+$/.test(value)) return true;
    return false;
  }

  readonly badRowCount = computed(() => this.rowProblems().filter((p) => p.length > 0).length);

  /**
   * Rows whose cell count differs from the most common one.
   *
   * <p>This is the characteristic failure of pasting a PDF table, and the dangerous one: the row
   * still looks plausible, but every value after the break sits one column to the left, so a
   * level becomes a credit count and nothing about it is obviously wrong. Comparing against the
   * most common width rather than the widest means one stray long row does not flag all the
   * correct ones.
   */
  readonly raggedRows = computed(() => {
    const rows = this.rows();
    if (rows.length < 2) return [];
    const counts = new Map<number, number>();
    for (const r of rows) counts.set(r.cells.length, (counts.get(r.cells.length) ?? 0) + 1);
    const usual = [...counts.entries()].sort((a, b) => b[1] - a[1])[0][0];
    return rows
      .map((r, i) => (r.cells.length === usual ? null : i + 1))
      .filter((n): n is number => n !== null);
  });

  readonly totals = computed(() => {
    let credits = 0;
    let hours = 0;
    for (const row of this.rows()) {
      credits += Number(this.valueOf(row, 'credits')) || 0;
      hours += Number(this.valueOf(row, 'weeklyHours')) || 0;
    }
    return { credits, hours };
  });

  readonly creditsMismatch = computed(
    () => this.header.declaredCredits !== null && this.header.declaredCredits !== this.totals().credits,
  );
  readonly hoursMismatch = computed(
    () => this.header.declaredHours !== null && this.header.declaredHours !== this.totals().hours,
  );

  /** Prerequisites naming a course code that is not in this paste — the import refuses those. */
  readonly unknownPrerequisites = computed(() => {
    const known = new Set(this.rows().map((r) => this.valueOf(r, 'courseCode')).filter(Boolean));
    const unknown = new Set<string>();
    for (const row of this.rows()) {
      for (const p of this.valueOf(row, 'prerequisites').split(/[;,]/)) {
        const code = p.trim();
        if (code && !known.has(code)) unknown.add(code);
      }
    }
    return [...unknown];
  });

  readonly ready = computed(
    () =>
      this.rows().length > 0 &&
      this.missingRequired().length === 0 &&
      !!this.header.programCode.trim() &&
      !!this.header.pensumCode.trim(),
  );

  /** The same CSV the endpoint already accepts, so the server-side validation is unchanged. */
  private buildCsv(): string {
    const h = this.header;
    const head = [
      'programCode', 'programName', 'faculty', 'programLevel', 'pensumCode', 'reform',
      'pensumStatus', 'declaredCredits', 'declaredHours', 'levels', 'areaCode', 'areaName',
      'areaColor', 'pensumItemCode', 'courseCode', 'courseName', 'courseLevel', 'credits',
      'weeklyHours', 'isElectiveSlot', 'prerequisites', 'sinuCode',
    ].join(',');

    const lines = this.rows().map((row) => {
      const area = this.valueOf(row, 'areaCode');
      const elective = looksTrue(this.valueOf(row, 'isElectiveSlot'));
      const prereqs = this.valueOf(row, 'prerequisites')
        .split(/[;,]/)
        .map((p) => p.trim())
        .filter(Boolean)
        .join(';');
      return [
        h.programCode, h.programName, h.faculty, h.programLevel, h.pensumCode, h.reform,
        h.pensumStatus, String(h.declaredCredits ?? this.totals().credits),
        String(h.declaredHours ?? this.totals().hours), String(h.levels ?? ''),
        area,
        // Area name and colour are not asked for: the import derives the totals itself, and a
        // colour invented here would be one more thing to correct later.
        area, '#539392',
        this.valueOf(row, 'pensumItemCode'),
        elective ? '' : this.valueOf(row, 'courseCode'),
        this.valueOf(row, 'courseName'),
        this.valueOf(row, 'courseLevel'),
        this.valueOf(row, 'credits'),
        this.valueOf(row, 'weeklyHours'),
        String(elective),
        prereqs,
        '',
      ].map(csvCell).join(',');
    });

    return [head, ...lines].join('\n') + '\n';
  }

  private csvFile(): File {
    return new File([this.buildCsv()], `pensum-${this.header.pensumCode || 'draft'}.csv`, {
      type: 'text/csv',
    });
  }

  send(dryRun: boolean): void {
    if (!dryRun && !window.confirm(`Import pensum ${this.header.pensumCode} for real? An existing one with the same code is replaced.`)) {
      return;
    }
    this.busy.set(true);
    this.dryRun.set(dryRun);
    this.error.set(null);
    this.report.set(null);

    this.service.upload(this.csvFile(), dryRun).subscribe({
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

  download(): void {
    const url = URL.createObjectURL(this.csvFile());
    const a = document.createElement('a');
    a.href = url;
    a.download = this.csvFile().name;
    a.click();
    URL.revokeObjectURL(url);
  }
}
