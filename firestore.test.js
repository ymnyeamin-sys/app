const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read messes or meals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("messes").get());
  await assertFails(unauthDb.collection("messes").doc("mess_1").collection("meals").get());
});

test("Authenticated user: can create a mess and becomes manager and member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const messRef = aliceDb.collection("messes").doc("mess_1");
  await assertSucceeds(
    messRef.set({
      id: "mess_1",
      name: "Dream Mess",
      joinCode: "DREAM1",
      managerId: ALICE_UID,
      memberUids: [ALICE_UID],
      memberNames: ["Alice"],
      activeMonth: "2026-10",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Admin and members can manage and join rooms", async () => {
  // Seed mess
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("messes").doc("mess_1").set({
      id: "mess_1",
      name: "Dream Mess",
      joinCode: "DREAM1",
      managerId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: ["Alice", "Bob"],
      activeMonth: "2026-10",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Admin Alice creates a Double room
  await assertSucceeds(
    aliceDb.collection("messes").doc("mess_1").collection("rooms").doc("room_101").set({
      id: "room_101",
      messId: "mess_1",
      roomNumber: "101",
      type: "Double",
      capacity: 2,
      rent: 2500,
      assignedMemberUids: [ALICE_UID],
      assignedMemberNames: ["Alice"],
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );

  // Member Bob joins the available room
  await assertSucceeds(
    bobDb.collection("messes").doc("mess_1").collection("rooms").doc("room_101").update({
      assignedMemberUids: [ALICE_UID, BOB_UID],
      assignedMemberNames: ["Alice", "Bob"],
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Non-member Bob cannot read Alice's mess or its meals", async () => {
  // Seed Alice's mess via admin context
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("messes").doc("mess_1").set({
      id: "mess_1",
      name: "Dream Mess",
      joinCode: "DREAM1",
      managerId: ALICE_UID,
      memberUids: [ALICE_UID],
      memberNames: ["Alice"],
      activeMonth: "2026-10",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("messes").doc("mess_1").get());
  await assertFails(bobDb.collection("messes").doc("mess_1").collection("rooms").get());
});
