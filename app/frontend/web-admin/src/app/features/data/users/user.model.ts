import type { Role } from '../../../core/auth/auth.model';

export interface Identification {
  type: 'CC' | 'TI' | 'CE' | 'PASAPORTE';
  number: string;
}

export interface AcademicInfo {
  studentCode: string;
  programCode: string;
  pensumCode: string;
  currentLevel: number;
}

/** Mirrors UserProfile in docs/api/user.openapi.yaml. */
export interface UserProfile {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  identification: Identification | null;
  phone: string | null;
  avatarUrl: string | null;
  role: Role;
  active: boolean;
  academic: AcademicInfo | null;
}

export interface UserListFilters {
  page: number;
  size: number;
  role?: Role;
  active?: boolean;
  q?: string;
}
