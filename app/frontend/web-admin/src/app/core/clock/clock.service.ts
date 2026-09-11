import { Injectable, signal } from '@angular/core';

/**
 * One shared ticking clock for every "expires in..." countdown in the app, instead of each
 * component running its own setInterval. Zoneless change detection reacts to the signal write
 * directly, so no component needs to poll or subscribe to anything beyond reading `now()`.
 */
@Injectable({ providedIn: 'root' })
export class ClockService {
  private readonly nowSignal = signal(Date.now());
  readonly now = this.nowSignal.asReadonly();

  constructor() {
    setInterval(() => this.nowSignal.set(Date.now()), 1000);
  }
}
