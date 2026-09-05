// Replica set bootstrap, user provisioning AND liveness probe for the `mongo` container.
//
// Run by the compose healthcheck (`mongosh --quiet --file /scripts/rs-init.js`), not by
// the image's /docker-entrypoint-initdb.d hook: that hook runs against a temporary
// mongod bound to 127.0.0.1, so an rs.initiate() there registers a member the rest of
// the stack cannot reach. Running it from the healthcheck means it retries on its own
// interval and the container is not reported healthy until the node is really PRIMARY.
//
// Why a replica set for one node: it is what Atlas runs and what Testcontainers starts,
// so local matches both. Multi-document transactions are also a replica set feature; no
// MVP service uses them today, but a standalone would fail the first one added.
//
// The script must stay idempotent - it runs every 10 seconds for the container's life.

const RS_NAME = "rs0";

// Member host must be the compose service name: it is what the other containers
// resolve, and the driver connects to whatever host the set advertises. Host-side
// tooling therefore needs mongodb://localhost:27017/?directConnection=true.
const MEMBER_HOST = "mongo:27017";

const ROOT_USER = process.env.MONGO_ROOT_USER || "kapp_root";
const ROOT_PASSWORD = process.env.MONGO_ROOT_PASSWORD;

// One user per service, created INSIDE the database it owns. Keeping the user in its own
// database makes that database its authSource, so a connection string carries no
// reference to `admin` and a leaked credential opens exactly one database.
const SERVICE_USERS = [
  { db: "kapp_auth", user: "kapp_auth_user", password: process.env.MONGO_AUTH_PASSWORD },
  { db: "kapp_user", user: "kapp_user_user", password: process.env.MONGO_USER_PASSWORD },
  { db: "kapp_semaphore", user: "kapp_semaphore_user", password: process.env.MONGO_SEMAPHORE_PASSWORD },
  { db: "kapp_schedule", user: "kapp_schedule_user", password: process.env.MONGO_SCHEDULE_PASSWORD },
  { db: "kapp_map", user: "kapp_map_user", password: process.env.MONGO_MAP_PASSWORD },
];

// ── 1. Replica set ──────────────────────────────────────────────────────────────────
// Must happen before any user is created: creating a user is a write, and a member that
// has not been initiated cannot accept writes.
try {
  rs.status();
} catch (err) {
  // NotYetInitialized (94) is the only error worth acting on. Anything else - the node
  // still loading, a transient network error, or Unauthorized once users exist - is left
  // to the next healthcheck attempt.
  if (err.code === 94) {
    rs.initiate({ _id: RS_NAME, members: [{ _id: 0, host: MEMBER_HOST }] });
  }
}

// Election takes a moment after initiate, so the first probes legitimately fail.
// `isWritablePrimary` is the check that matters: a node can be up, in the set, and still
// be unable to accept the writes that follow. db.hello() needs no authentication, which
// is what lets this probe keep working after the localhost exception closes.
if (!db.hello().isWritablePrimary) {
  quit(1);
}

// ── 2. Users ────────────────────────────────────────────────────────────────────────
// Without a root password configured the container runs unauthenticated, which is the
// old behaviour. Refusing to start would break anyone who has not updated their .env.
if (!ROOT_PASSWORD) {
  quit(0);
}

const admin = db.getSiblingDB("admin");

// Authenticate FIRST, then fall back to creating root. The reverse order looks natural but
// breaks on every probe after the first: once any user exists the localhost exception has
// closed, and an unauthenticated getUser() throws Unauthorized rather than returning null.
//
// MongoDB's localhost exception permits creating the FIRST user without credentials, and
// closes the moment that user exists — which is why root is created and immediately used.
function tryAuth() {
  try {
    return admin.auth(ROOT_USER, ROOT_PASSWORD);
  } catch (e) {
    return false;
  }
}

if (!tryAuth()) {
  try {
    admin.createUser({
      user: ROOT_USER,
      pwd: ROOT_PASSWORD,
      roles: [{ role: "root", db: "admin" }],
    });
    print("created root user " + ROOT_USER);
  } catch (e) {
    // Reached when root already exists and the password does not match: a stale .env
    // against a provisioned volume. Failing the probe is correct — the services would
    // fail to connect too, and a container reporting healthy with unusable credentials
    // hides the problem until something else breaks.
    print("FATAL: cannot authenticate as " + ROOT_USER +
          " and cannot create it. Is MONGO_ROOT_PASSWORD correct for this volume? " + e);
    quit(1);
  }
  if (!tryAuth()) {
    print("FATAL: created " + ROOT_USER + " but cannot authenticate as it");
    quit(1);
  }
}

// Each service user gets readWrite on ONE database and nothing else. This is what turns
// the separation between services from a convention the code respects into a rule the
// server enforces: map-service holding its own credentials cannot read kapp_auth even if
// someone writes the query by mistake.
for (const svc of SERVICE_USERS) {
  if (!svc.password) {
    print("skipping " + svc.user + ": no password in the environment");
    continue;
  }
  const target = db.getSiblingDB(svc.db);
  if (target.getUser(svc.user) === null) {
    target.createUser({
      user: svc.user,
      pwd: svc.password,
      roles: [{ role: "readWrite", db: svc.db }],
    });
    print("created " + svc.user + " with readWrite on " + svc.db);
  }
}

quit(0);
