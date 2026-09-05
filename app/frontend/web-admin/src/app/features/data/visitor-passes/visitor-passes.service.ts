import { inject, Injectable } from '@angular/core';
import type { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { VisitorPass } from './visitor-pass.model';

@Injectable({ providedIn: 'root' })
export class VisitorPassesService {
  private readonly api = inject(ApiClientService);

  list(redeemed?: boolean): Observable<VisitorPass[]> {
    return this.api.get<VisitorPass[]>('/auth/admin/visitor-passes', { redeemed });
  }

  issue(notes?: string): Observable<VisitorPass> {
    return this.api.post<VisitorPass>('/auth/admin/visitor-passes', { notes: notes || null });
  }

  /**
   * Only works on an unredeemed pass. A redeemed one comes back as `409`: it is the record of a
   * visit, not a pass, and it leaves on its own after 30 days.
   */
  revoke(code: string): Observable<void> {
    return this.api.delete(`/auth/admin/visitor-passes/${encodeURIComponent(code)}`);
  }
}
