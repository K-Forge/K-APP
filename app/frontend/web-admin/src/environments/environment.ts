// Build-time default only. The portal is meant to be pointed at whichever gateway the
// developer is actually running against (their own machine, a teammate's, a tunnel), so this
// value is just the starting point - ApiConfigService reads it once and then lets the UI
// override it at runtime, persisted in localStorage. Never read this constant directly outside
// ApiConfigService.
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
};
