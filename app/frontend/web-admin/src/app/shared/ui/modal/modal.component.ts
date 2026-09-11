import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Output, ViewChild, input } from '@angular/core';

/**
 * Thin wrapper around the native <dialog> element: free focus trap, Escape-to-close and a
 * backdrop, with none of the accessibility bugs a hand-rolled modal tends to ship. Used for every
 * create/edit form in the data-management screens, so there is exactly one modal implementation
 * to get right.
 */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialogEl (close)="closed.emit()" (cancel)="closed.emit()">
      <div class="modal-header">
        <h2>{{ title() }}</h2>
        <button type="button" class="btn btn-ghost btn-sm" (click)="close()" aria-label="Close">✕</button>
      </div>
      <div class="modal-body">
        <ng-content />
      </div>
    </dialog>
  `,
  styles: `
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.25rem;
      border-bottom: 1px solid var(--border);
    }
    .modal-header h2 {
      margin: 0;
      font-size: 1rem;
    }
    .modal-body {
      padding: 1.25rem;
      max-height: 70vh;
      overflow-y: auto;
    }
  `,
})
export class ModalComponent {
  readonly title = input('');
  @Output() readonly closed = new EventEmitter<void>();

  @ViewChild('dialogEl') private dialogRef?: ElementRef<HTMLDialogElement>;

  open(): void {
    this.dialogRef?.nativeElement.showModal();
  }

  close(): void {
    this.dialogRef?.nativeElement.close();
  }
}
