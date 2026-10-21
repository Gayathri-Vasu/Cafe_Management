import { Injectable, computed, signal } from '@angular/core';

/**
 * Tracks how many HTTP requests are currently in flight so the shell can show a
 * slim progress bar at the top of the page.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = signal(0);
  private readonly visible = signal(false);

  readonly isLoading = computed(() => this.visible());

  start(): void {
    this.pending.update((value) => value + 1);
    this.visible.set(true);
  }

  stop(): void {
    this.pending.update((value) => Math.max(0, value - 1));
    if (this.pending() === 0) {
      this.visible.set(false);
    }
  }

  reset(): void {
    this.pending.set(0);
    this.visible.set(false);
  }
}
