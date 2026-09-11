import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TokenStore } from '../../../core/auth/token.store';
import { UsersPage } from './users.page';
import type { UserProfile } from './user.model';

function profile(id: string): UserProfile {
  return {
    id,
    email: `${id}@kforge.dev`,
    firstName: 'A',
    lastName: 'B',
    identification: null,
    phone: null,
    avatarUrl: null,
    role: 'ROLE_ADMIN',
    active: true,
    academic: null,
  };
}

describe('UsersPage', () => {
  let page: UsersPage;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: TokenStore,
          useValue: { decoded: () => ({ claims: { sub: 'me' } }) },
        },
      ],
    }).compileComponents();
    page = TestBed.createComponent(UsersPage).componentInstance;
  });

  // Deactivating now genuinely removes access, and there is no create-user path here, so
  // switching your own account off means editing MongoDB by hand to get back in.
  it('will not let you switch off your own account', () => {
    expect(page.isSelf(profile('me'))).toBe(true);
  });

  it('still lets you switch off somebody else', () => {
    expect(page.isSelf(profile('someone-else'))).toBe(false);
  });
});
