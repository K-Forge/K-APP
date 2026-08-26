// Replica set bootstrap AND liveness probe for the `mongo` container.
//
// Run by the compose healthcheck (`mongosh --quiet --file /scripts/rs-init.js`), not by
// the image's /docker-entrypoint-initdb.d hook: that hook runs against a temporary
// mongod bound to 127.0.0.1, so an rs.initiate() there registers a member the rest of
// the stack cannot reach. Running it from the healthcheck means it retries on its own
// interval and the container is not reported healthy until the node is really PRIMARY.
//
// Why a replica set at all for one node: multi-document transactions are a replica set
// feature. Against a standalone mongod, every @Transactional method fails at runtime
// with "Transaction numbers are only allowed on a replica set member or mongos".
//
// The script must stay idempotent - it runs every 10 seconds for the container's life.

const RS_NAME = "rs0";

// Member host must be the compose service name: it is what the other containers
// resolve, and the driver connects to whatever host the set advertises. Host-side
// tooling therefore needs mongodb://localhost:27017/?directConnection=true.
const MEMBER_HOST = "mongo:27017";

try {
  rs.status();
} catch (err) {
  // NotYetInitialized (94) is the only error worth acting on. Anything else - the node
  // still loading, a transient network error - is left to the next healthcheck attempt.
  if (err.code === 94) {
    rs.initiate({ _id: RS_NAME, members: [{ _id: 0, host: MEMBER_HOST }] });
  }
}

// Election takes a moment after initiate, so the first probes legitimately fail.
// `isWritablePrimary` is the check that matters: a node can be up, in the set, and
// still be unable to accept the writes the services are about to make.
const hello = db.hello();
if (!hello.isWritablePrimary) {
  quit(1);
}

quit(0);
