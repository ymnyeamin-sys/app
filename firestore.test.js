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
const CHARLIE_UID = "charlie_789";

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

    await adminDb.collection("messes").doc("mess_1").collection("meals").doc("meal_1").set({
      id: "meal_1",
      messId: "mess_1",
      userId: ALICE_UID,
      userName: "Alice",
      memberUids: [ALICE_UID],
      date: "2026-10-24",
      month: "2026-10",
      breakfast: 1.0,
      lunch: 1.0,
      dinner: 1.0,
      totalMeals: 3.0,
      recordedBy: ALICE_UID,
      recordedByName: "Alice",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob cannot read mess document
  await assertFails(bobDb.collection("messes").doc("mess_1").get());
  // Bob cannot read meal document
  await assertFails(bobDb.collection("messes").doc("mess_1").collection("meals").doc("meal_1").get());
  // Bob cannot add meal to Alice's mess
  await assertFails(
    bobDb.collection("messes").doc("mess_1").collection("meals").doc("meal_bob").set({
      id: "meal_bob",
      messId: "mess_1",
      userId: BOB_UID,
      userName: "Bob",
      memberUids: [ALICE_UID],
      date: "2026-10-24",
      month: "2026-10",
      breakfast: 1.0,
      lunch: 1.0,
      dinner: 1.0,
      totalMeals: 3.0,
      recordedBy: BOB_UID,
      createdAt: new Date(),
    })
  );
});

test("Member can record meal and deposit in mess", async () => {
  // Seed mess with Alice and Bob as members
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

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob can add meal
  await assertSucceeds(
    bobDb.collection("messes").doc("mess_1").collection("meals").doc("meal_2").set({
      id: "meal_2",
      messId: "mess_1",
      userId: BOB_UID,
      userName: "Bob",
      memberUids: [ALICE_UID, BOB_UID],
      date: "2026-10-24",
      month: "2026-10",
      breakfast: 1.0,
      lunch: 1.0,
      dinner: 1.0,
      totalMeals: 3.0,
      recordedBy: BOB_UID,
      recordedByName: "Bob",
      createdAt: new Date(),
    })
  );

  // Bob can add deposit
  await assertSucceeds(
    bobDb.collection("messes").doc("mess_1").collection("deposits").doc("dep_1").set({
      id: "dep_1",
      messId: "mess_1",
      userId: BOB_UID,
      userName: "Bob",
      memberUids: [ALICE_UID, BOB_UID],
      amount: 2500.0,
      date: "2026-10-24",
      month: "2026-10",
      note: "Bkash deposit",
      recordedBy: BOB_UID,
      createdAt: new Date(),
    })
  );

  // Bob can add bazar expense
  await assertSucceeds(
    bobDb.collection("messes").doc("mess_1").collection("bazar_expenses").doc("baz_1").set({
      id: "baz_1",
      messId: "mess_1",
      title: "Fish, Vegetables, Rice",
      amount: 1200.0,
      date: "2026-10-24",
      month: "2026-10",
      shopperId: BOB_UID,
      shopperName: "Bob",
      memberUids: [ALICE_UID, BOB_UID],
      recordedBy: BOB_UID,
      createdAt: new Date(),
    })
  );
});
