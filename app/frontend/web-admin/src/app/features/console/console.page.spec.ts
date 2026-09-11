import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { OpenApiCatalogService } from '../../core/openapi/openapi-catalog.service';
import { ConsolePage } from './console.page';

describe('ConsolePage', () => {
  let page: ConsolePage;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsolePage],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
    page = TestBed.createComponent(ConsolePage).componentInstance;
  });

  it('offers operations to call', () => {
    expect(page.operations().length).toBeGreaterThan(0);
  });

  // The gateway has no route for /internal/** — that is the point of the prefix. Listing those
  // here offers a button whose only outcome is a 404 that says nothing.
  it('leaves out the service-to-service endpoints the gateway does not route', () => {
    for (const service of page.services) {
      page.onServiceChange({ target: { value: service.id } } as unknown as Event);
      expect(page.operations().filter((op) => op.path.startsWith('/internal/'))).toEqual([]);
    }
  });

  it('still knows about them in the contracts, so this is a filter and not a gap', () => {
    const catalog = TestBed.inject(OpenApiCatalogService);
    const everything = catalog.services.flatMap((s) => catalog.operationsFor(s.id));
    expect(everything.some((op) => op.path.startsWith('/internal/'))).toBe(true);
  });
});
