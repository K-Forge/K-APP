import { Injectable, effect, signal } from '@angular/core';
import type { HistoryEntry } from './console.model';

const STORAGE_KEY = 'kapp-admin:console-history';
const MAX_ENTRIES = 25;

/**
 * Recent console requests, newest first, persisted across reloads. Capped at 25 so a long
 * session of exploratory calls doesn't grow localStorage without bound.
 */
@Injectable({ providedIn: 'root' })
export class RequestHistoryService {
  readonly entries = signal<HistoryEntry[]>(readStored());

  constructor() {
    effect(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(this.entries()));
      } catch {
        // History is a convenience, not critical state - a full or unavailable storage quota
        // just means it won't survive a reload this time.
      }
    });
  }

  add(entry: HistoryEntry): void {
    this.entries.update((current) => [entry, ...current].slice(0, MAX_ENTRIES));
  }

  clear(): void {
    this.entries.set([]);
  }
}

function readStored(): HistoryEntry[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as HistoryEntry[]) : [];
  } catch {
    return [];
  }
}
