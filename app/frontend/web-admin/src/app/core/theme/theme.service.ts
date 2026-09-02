import { Injectable, effect, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'kapp-admin:theme';

/**
 * Applies the [data-theme] attribute styles.css keys its dark overrides on. "system" removes the
 * attribute entirely so the prefers-color-scheme media query decides, matching how most
 * developers expect an unconfigured app to behave.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly preference = signal<ThemePreference>(readStoredPreference());

  constructor() {
    effect(() => {
      const value = this.preference();
      const root = document.documentElement;
      if (value === 'system') {
        root.removeAttribute('data-theme');
      } else {
        root.setAttribute('data-theme', value);
      }
      try {
        localStorage.setItem(STORAGE_KEY, value);
      } catch {
        // Non-fatal: the preference just won't survive a reload.
      }
    });
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
  }
}

function readStoredPreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored;
    }
  } catch {
    // Fall through to the default below.
  }
  return 'system';
}
