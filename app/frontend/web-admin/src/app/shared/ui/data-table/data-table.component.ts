import { ChangeDetectionStrategy, Component, EventEmitter, Output, input } from '@angular/core';

/**
 * The one table shell every entity list uses: a loading row, an empty-state row, and paging
 * controls that respect whatever page/size/totalPages the API actually returned - none of that
 * chrome should be reinvented per entity, only the columns differ. The caller supplies its own
 * `<thead>` and `<tbody>` as projected content, because column shape (badges, formatted dates,
 * row actions) genuinely differs per entity and forcing it through a generic cell-renderer would
 * be more machinery than the five tables in this app justify.
 */
@Component({
  selector: 'app-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="data-table">
      <div class="scroll-x">
        <table>
          <ng-content select="thead" />
          @if (loading()) {
            <tbody>
              <tr>
                <td [attr.colspan]="100">
                  <div class="empty-state">Loading…</div>
                </td>
              </tr>
            </tbody>
          } @else if (empty()) {
            <tbody>
              <tr>
                <td [attr.colspan]="100">
                  <div class="empty-state">{{ emptyMessage() }}</div>
                </td>
              </tr>
            </tbody>
          } @else {
            <ng-content select="tbody" />
          }
        </table>
      </div>

      @if (totalPages() > 1) {
        <div class="row-between pagination">
          <span class="text-muted">
            Page {{ page() + 1 }} of {{ totalPages() }}
            @if (totalItems() !== null) {
              · {{ totalItems() }} total
            }
          </span>
          <div class="row">
            <button type="button" class="btn btn-sm" [disabled]="page() === 0 || loading()" (click)="pageChange.emit(page() - 1)">
              Previous
            </button>
            <button
              type="button"
              class="btn btn-sm"
              [disabled]="page() >= totalPages() - 1 || loading()"
              (click)="pageChange.emit(page() + 1)"
            >
              Next
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .pagination {
      padding: 0.75rem 0 0.25rem;
      font-size: 0.8125rem;
    }
  `,
})
export class DataTableComponent {
  readonly loading = input(false);
  readonly empty = input(false);
  readonly emptyMessage = input('No results.');
  readonly totalItems = input<number | null>(null);
  readonly page = input(0);
  readonly totalPages = input(0);

  @Output() readonly pageChange = new EventEmitter<number>();
}
