import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { PastePensumComponent } from './paste-pensum.component';

// A pensum as it comes out of a PDF: tab separated, header row first.
const PASTED = [
  'Código\tAsignatura\tNivel\tCréditos\tHoras\tÁrea',
  '17001\tCálculo diferencial\t1\t3\t4\tBASICA',
  '17002\tProgramación I\t1\t3\t5\tPROFESIONAL',
  '17003\tCátedra Konrad\t1\t1\t2\tCOMPLEMENTARIA',
  '17004\tÁlgebra lineal\t2\t3\t4\tBASICA',
  '17005\tProgramación II\t2\t3\t5\tPROFESIONAL',
].join('\n');

describe('PastePensumComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<PastePensumComponent>>;
  let component: PastePensumComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PastePensumComponent],
      providers: [provideHttpClient()],
    }).compileComponents();
    fixture = TestBed.createComponent(PastePensumComponent);
    component = fixture.componentInstance;
    component.pasted.set(PASTED);
    component.parse();
    fixture.detectChanges();
  });

  // The regression this guards: inside the inner @for, $index is the column's index, not the
  // row's. Rows 2..n threw on a column index past the end of the row array, and Angular stopped
  // rendering the table after the first row - with no visible error, just four missing rows.
  it('renders an editable cell for every column of every row', () => {
    const bodyRows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(bodyRows.length).toBe(5);
    const perRow = Array.from(bodyRows).map((tr) => (tr as HTMLElement).querySelectorAll('input').length);
    expect(perRow).toEqual([6, 6, 6, 6, 6]);
  });

  it('numbers the rows in order rather than repeating the first', () => {
    const numbers = Array.from(fixture.nativeElement.querySelectorAll('tbody .rownum')).map((td) =>
      (td as HTMLElement).textContent!.trim().replace(/\D+$/, ''),
    );
    expect(numbers).toEqual(['1', '2', '3', '4', '5']);
  });

  // Editing had the same off-by-a-dimension bug: a keystroke in row 4 landed in row 0.
  it('writes an edit into the row it was typed in', () => {
    const bodyRows = fixture.nativeElement.querySelectorAll('tbody tr');
    const input = bodyRows[3].querySelectorAll('input')[1] as HTMLInputElement;
    input.value = 'Álgebra lineal I';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(component.rows()[3].cells[1]).toBe('Álgebra lineal I');
    expect(component.rows()[0].cells[1]).toBe('Cálculo diferencial');
  });

  it('guesses the columns from the pasted header and totals what it read', () => {
    expect(component.mapping()[0]).toBe('courseCode');
    expect(component.mapping()[1]).toBe('courseName');
    expect(component.mapping()[2]).toBe('courseLevel');
    expect(component.mapping()[3]).toBe('credits');
    expect(component.mapping()[4]).toBe('weeklyHours');
    expect(component.totals()).toEqual({ credits: 13, hours: 20 });
    expect(component.missingRequired()).toEqual([]);
  });
});

describe('PastePensumComponent · reading the paste', () => {
  async function make(paste: string) {
    await TestBed.configureTestingModule({
      imports: [PastePensumComponent],
      providers: [provideHttpClient()],
    }).compileComponents();
    const fixture = TestBed.createComponent(PastePensumComponent);
    fixture.componentInstance.pasted.set(paste);
    fixture.componentInstance.parse();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('drops the heading line and names the columns after it', async () => {
    const f = await make(PASTED);
    expect(f.componentInstance.headingRow()).toEqual([
      'Código', 'Asignatura', 'Nivel', 'Créditos', 'Horas', 'Área',
    ]);
    expect(f.componentInstance.rows().length).toBe(5);
    expect(f.componentInstance.rows()[0].cells[1]).toBe('Cálculo diferencial');
  });

  // Dropping a real course silently is the expensive mistake, so it has to be undoable.
  it('puts the heading back as a course when told to', async () => {
    const f = await make(PASTED);
    f.componentInstance.keepHeadingAsRow();
    f.detectChanges();
    expect(f.componentInstance.headingRow()).toBeNull();
    expect(f.componentInstance.rows().length).toBe(6);
    expect(f.componentInstance.rows()[0].cells[0]).toBe('Código');
    expect(f.nativeElement.querySelectorAll('tbody tr').length).toBe(6);
  });

  it('keeps every line when the paste has no heading', async () => {
    const f = await make('17001\tCálculo diferencial\t1\t3\t4\n17002\tProgramación I\t1\t3\t5');
    expect(f.componentInstance.headingRow()).toBeNull();
    expect(f.componentInstance.rows().length).toBe(2);
  });

  // A pensum PDF gives one code per course and no item numbering; requiring one would mean
  // typing sixty numbers by hand per pensum.
  it('uses the course code as the item code when there is no item-code column', async () => {
    const f = await make(PASTED);
    expect(f.componentInstance.itemCodeFromCourseCode()).toBe(true);
    expect(f.componentInstance.missingRequired()).toEqual([]);
    expect(f.nativeElement.textContent).toContain('own code is used as its item');
  });

  it('does not invent an item code when the paste already has one', async () => {
    const f = await make(
      'Ítem\tCódigo\tAsignatura\tNivel\tCréditos\tHoras\tÁrea\n' +
        '1\t17001\tCálculo diferencial\t1\t3\t4\tBASICA',
    );
    const m = f.componentInstance.mapping();
    expect(m[0]).toBe('pensumItemCode');
    expect(m[1]).toBe('courseCode');
    expect(f.componentInstance.itemCodeFromCourseCode()).toBe(false);
  });

  it('flags a row whose values shifted a column', async () => {
    const f = await make(PASTED.replace('17003\tCátedra Konrad\t1\t1\t2\tCOMPLEMENTARIA', '17003\tCátedra Konrad\t1\t1\t2'));
    expect(f.componentInstance.raggedRows()).toEqual([3]);
  });
});

describe('PastePensumComponent · what the import needs before it will run', () => {
  async function ready(fill: Record<string, string>) {
    await TestBed.configureTestingModule({
      imports: [PastePensumComponent],
      providers: [provideHttpClient()],
    }).compileComponents();
    const f = TestBed.createComponent(PastePensumComponent);
    f.componentInstance.pasted.set(PASTED);
    f.componentInstance.parse();
    for (const [k, v] of Object.entries(fill)) {
      f.componentInstance.patch(k as never, v as never);
    }
    f.detectChanges();
    return f;
  }

  afterEach(() => TestBed.resetTestingModule());

  const COMPLETE = {
    programCode: '999',
    programName: 'QA',
    faculty: 'Facultad de QA',
    pensumCode: '9999',
    reform: 'Reforma QA',
  };

  // header used to be a plain object, so nothing in the signal graph heard these writes and
  // the buttons stayed disabled however complete the form was.
  it('enables the import once the header is filled in', async () => {
    const f = await ready(COMPLETE);
    expect(f.componentInstance.missingHeaderFields()).toEqual([]);
    expect(f.componentInstance.ready()).toBe(true);
  });

  // Every one of these is required() in CurriculumCsvImporter.readHeader.
  it('names the header fields still missing rather than silently refusing', async () => {
    const f = await ready({ programCode: '999' });
    expect(f.componentInstance.missingHeaderFields()).toEqual([
      'Program name', 'Faculty', 'Pensum code', 'Reform',
    ]);
    expect(f.componentInstance.ready()).toBe(false);
    expect(f.nativeElement.textContent).toContain('Program name, Faculty, Pensum code, Reform');
  });

  it('takes levels from the highest level in the paste', async () => {
    const f = await ready(COMPLETE);
    expect(f.componentInstance.defaultLevels()).toBe(2);
  });
});
