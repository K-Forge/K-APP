import { Injectable, signal } from '@angular/core';
import { environment } from '../../../environments/environment';

const STORAGE_KEY = 'kapp-admin:api-base-url';

/**
 * Single place that knows which gateway the portal is talking to. The environment file only
 * supplies the day-one default (the gateway on the developer's own machine); everything after
 * that reads and writes this service, because the whole point is letting someone point the
 * portal at a teammate's machine or a tunnel without a rebuild.
 */
@Injectable({ providedIn: 'root' })
export class ApiConfigService {
  private readonly stored = readStoredBaseUrl();
  private readonly baseUrlSignal = signal(this.stored ?? environment.apiBaseUrl);

  readonly baseUrl = this.baseUrlSignal.asReadonly();

  /** True default, false when a developer has overridden it from the UI. */
  readonly isDefault = signal(this.stored === null);

  setBaseUrl(url: string): void {
    const trimmed = url.trim().replace(/\/+$/, '');
    if (!trimmed) {
      return;
    }
    this.baseUrlSignal.set(trimmed);
    this.isDefault.set(false);
    localStorage.setItem(STORAGE_KEY, trimmed);
  }

  resetToDefault(): void {
    this.baseUrlSignal.set(environment.apiBaseUrl);
    this.isDefault.set(true);
    localStorage.removeItem(STORAGE_KEY);
  }
}

function readStoredBaseUrl(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    // localStorage can throw in locked-down browser contexts (private mode with storage
    // disabled). Falling back to the environment default is preferable to a crashed app.
    return null;
  }
}
