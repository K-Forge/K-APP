import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TokenStore } from '../../core/auth/token.store';
import { ApiConfigService } from '../../core/config/api-config.service';
import { ThemeService } from '../../core/theme/theme.service';
import { TokenCountdownComponent } from '../../shared/ui/token-countdown/token-countdown.component';

interface NavLink {
  path: string;
  label: string;
}

const NAV_LINKS: NavLink[] = [
  { path: '/identity', label: 'Identity' },
  { path: '/console', label: 'API console' },
  { path: '/roles', label: 'Roles' },
  { path: '/data/users', label: 'Users' },
  { path: '/data/buildings', label: 'Buildings' },
  { path: '/data/spaces', label: 'Spaces' },
  { path: '/data/programs', label: 'Programs' },
  { path: '/data/curricula', label: 'Curricula' },
  { path: '/data/invitation-codes', label: 'Invitation codes' },
  { path: '/data/visitor-passes', label: 'Visitor passes' },
  { path: '/data/import', label: 'Import pensums' },
];

/** Nav + header shared by every authenticated screen. Login stays outside so it renders alone. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TokenCountdownComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="shell">
      <header class="shell-header">
        <div class="row">
          <strong>KApp admin</strong>
          <span class="text-faint">&amp; dev portal</span>
        </div>

        <div class="row spread">
          <app-token-countdown />
          <button type="button" class="btn btn-sm" (click)="cycleTheme()" [attr.aria-label]="'Theme: ' + theme.preference()">
            {{ themeIcon() }} {{ theme.preference() }}
          </button>
          <button type="button" class="btn btn-sm" (click)="editBaseUrl()">
            gateway: {{ baseUrl() }}
          </button>
          <button type="button" class="btn btn-sm btn-danger" (click)="logout()">Sign out</button>
        </div>
      </header>

      <div class="shell-body">
        <nav class="shell-nav" aria-label="Sections">
          @for (link of links; track link.path) {
            <a [routerLink]="link.path" routerLinkActive="active" class="nav-link">{{ link.label }}</a>
          }
        </nav>

        <main class="shell-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: `
    .shell {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
    }
    .shell-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.75rem 1.25rem;
      border-bottom: 1px solid var(--border);
      background: var(--bg-elevated);
      flex-wrap: wrap;
    }
    .shell-body {
      flex: 1;
      display: flex;
      min-height: 0;
    }
    .shell-nav {
      width: 13rem;
      flex-shrink: 0;
      border-right: 1px solid var(--border);
      background: var(--bg-elevated);
      padding: 0.75rem;
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }
    .nav-link {
      display: block;
      padding: 0.5rem 0.75rem;
      border-radius: var(--radius-sm);
      color: var(--text-muted);
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 500;
    }
    .nav-link:hover {
      background: var(--bg-inset);
      color: var(--text);
    }
    .nav-link.active {
      background: var(--primary-bg);
      color: var(--primary);
    }
    .shell-content {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
    }
    @media (max-width: 720px) {
      .shell-body {
        flex-direction: column;
      }
      .shell-nav {
        width: 100%;
        flex-direction: row;
        flex-wrap: wrap;
        border-right: none;
        border-bottom: 1px solid var(--border);
      }
    }
  `,
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly config = inject(ApiConfigService);
  protected readonly tokenStore = inject(TokenStore);
  protected readonly theme = inject(ThemeService);

  readonly links = NAV_LINKS;
  readonly baseUrl = this.config.baseUrl;

  readonly themeIcons: Record<string, string> = { system: '◐', light: '☀', dark: '☽' };
  themeIcon(): string {
    return this.themeIcons[this.theme.preference()] ?? '';
  }

  cycleTheme(): void {
    const order = ['system', 'light', 'dark'] as const;
    const next = order[(order.indexOf(this.theme.preference()) + 1) % order.length];
    this.theme.set(next);
  }

  editBaseUrl(): void {
    const next = window.prompt('Gateway base URL', this.baseUrl());
    if (next) {
      this.config.setBaseUrl(next);
    }
  }

  logout(): void {
    this.auth.logout();
  }
}
