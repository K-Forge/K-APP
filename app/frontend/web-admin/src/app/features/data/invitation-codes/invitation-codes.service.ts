import { inject, Injectable } from '@angular/core';
import type { Observable } from 'rxjs';
import { ApiClientService } from '../../../core/http/api-client.service';
import type { InvitationCode, InvitationCodeRequest } from './invitation-code.model';

@Injectable({ providedIn: 'root' })
export class InvitationCodesService {
  private readonly api = inject(ApiClientService);

  list(active?: boolean): Observable<InvitationCode[]> {
    return this.api.get<InvitationCode[]>('/auth/admin/invitation-codes', { active });
  }

  create(request: InvitationCodeRequest): Observable<InvitationCode> {
    return this.api.post<InvitationCode>('/auth/admin/invitation-codes', request);
  }

  /**
   * The only mutable field. A code's role and quota are fixed once minted - changing either
   * retroactively would rewrite what the people already holding it were promised.
   */
  setActive(code: string, active: boolean): Observable<InvitationCode> {
    return this.api.patch<InvitationCode>(
      `/auth/admin/invitation-codes/${encodeURIComponent(code)}`,
      { active },
    );
  }

  delete(code: string): Observable<void> {
    return this.api.delete(`/auth/admin/invitation-codes/${encodeURIComponent(code)}`);
  }
}
