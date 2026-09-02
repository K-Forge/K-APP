import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { roleLabel } from '../../../core/auth/auth.model';

/** Small badge for a single role, colored consistently everywhere a role is shown. */
@Component({
  selector: 'app-role-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="badgeClass()">{{ roleLabel(role()) }}</span>`,
})
export class RoleBadgeComponent {
  readonly role = input.required<string>();
  readonly roleLabel = roleLabel;

  badgeClass(): string {
    switch (this.role()) {
      case 'ROLE_ADMIN':
        return 'badge-danger';
      case 'ROLE_PROFESSOR':
        return 'badge-primary';
      case 'ROLE_STUDENT':
        return 'badge-success';
      default:
        return 'badge-neutral';
    }
  }
}
