import { ALL_ROLES, type Role } from '../auth/auth.model';
import type { ConsoleOperation } from './console-operation.model';

export type RoleRequirement =
  | { kind: 'public' }
  | { kind: 'roles'; roles: Role[] }
  | { kind: 'unknown' };

/**
 * None of the five OpenAPI specs carries a machine-readable role annotation (no `x-roles`
 * extension) - authorization is documented as prose in each operation's `description`, in three
 * different phrasings across the five files ("Allowed roles: ...", "**Access: ...**", "Roles:
 * ..."). This parses that prose rather than guessing blind, and says so plainly (`unknown`) when
 * an operation states nothing usable, instead of inventing an answer for a tool whose whole job
 * is verifying the real authorization matrix by hand.
 */
export function resolveRoleRequirement(op: ConsoleOperation): RoleRequirement {
  if (Array.isArray(op.security) && op.security.length === 0) {
    return { kind: 'public' };
  }

  // YAML block-literal descriptions soft-wrap prose across lines; flattening whitespace means a
  // sentence isn't accidentally cut off at a line break before its regex can see the whole thing.
  const text = `${op.summary} ${op.description}`.replace(/\s+/g, ' ');

  if (/`?ROLE_ADMIN`?\s*only/i.test(text)) {
    return { kind: 'roles', roles: ['ROLE_ADMIN'] };
  }
  if (/any authenticated role except\s*`?ROLE_GUEST`?/i.test(text)) {
    return { kind: 'roles', roles: ['ROLE_STUDENT', 'ROLE_PROFESSOR', 'ROLE_ADMIN'] };
  }
  if (/any authenticated role/i.test(text)) {
    return { kind: 'roles', roles: [...ALL_ROLES] };
  }

  const listMatch = text.match(/(?:Allowed roles|Access|Roles):\s*([^.]+)/i);
  if (listMatch) {
    const roles = extractRoles(listMatch[1]);
    if (roles.length) {
      return { kind: 'roles', roles };
    }
  }

  const fallback = fallbackByPath(op);
  if (fallback) {
    return fallback;
  }

  const anyRoles = extractRoles(text);
  if (anyRoles.length) {
    return { kind: 'roles', roles: anyRoles };
  }

  return { kind: 'unknown' };
}

function extractRoles(text: string): Role[] {
  return ALL_ROLES.filter((role) => text.includes(role));
}

/**
 * Only the semaphore catalog endpoints need this: their per-operation descriptions state none of
 * the "Allowed roles"/"Access"/"Roles" phrasings above, and the authorization rule instead lives
 * once, in the spec's top-of-file table (see docs/api/semaphore.openapi.yaml's Authorization
 * section). Every other service states its roles on every operation.
 */
function fallbackByPath(op: ConsoleOperation): RoleRequirement | null {
  if (op.serviceId === 'semaphore') {
    if (op.path.startsWith('/api/catalog')) {
      return { kind: 'roles', roles: op.method === 'get' ? ['ROLE_STUDENT', 'ROLE_PROFESSOR', 'ROLE_ADMIN'] : ['ROLE_ADMIN'] };
    }
    if (op.path === '/api/semaphore/{userId}') {
      return { kind: 'roles', roles: ['ROLE_ADMIN'] };
    }
    if (op.path.startsWith('/api/semaphore/me')) {
      return { kind: 'roles', roles: ['ROLE_STUDENT'] };
    }
  }
  return null;
}
