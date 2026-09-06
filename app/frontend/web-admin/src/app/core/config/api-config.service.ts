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
  private readonly baseUrlSignal = signal(this.stored ?? defaultBaseUrl());

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
    this.baseUrlSignal.set(defaultBaseUrl());
    this.isDefault.set(true);
    localStorage.removeItem(STORAGE_KEY);
  }
}

/**
 * Where to look for the gateway when nobody has said otherwise.
 *
 * On localhost that is the build-time default. Opened from anywhere else — a phone on the
 * Tailscale network, a teammate's laptop — `localhost` would mean *that* device, which is not
 * running anything, so the portal would sit there failing to connect with no obvious reason.
 * Serving host plus the gateway's port is right whenever the two are served from the same
 * machine, which is every arrangement we actually have.
 *
 * Still overridable from the UI; this only changes the starting point.
 */
function defaultBaseUrl(): string {
  const host = typeof window === 'undefined' ? '' : window.location.hostname;
  if (!host || host === 'localhost' || host === '127.0.0.1' || host === '::1') {
    return environment.apiBaseUrl;
  }
  return `${window.location.protocol}//${host}:8080`;
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
