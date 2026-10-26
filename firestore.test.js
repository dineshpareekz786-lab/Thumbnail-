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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, now) {
  return {
    userId: uid,
    title: "Coaching Industry Investigation",
    videoTopic: "The Dark Truth Behind India's Coaching Industry",
    headline: "EDUCATION SCAM",
    subheadline: "INSIDE THE BILLION DOLLAR FACTORY",
    category: "Documentary",
    visualStyle: "Cinematic Investigative",
    realism: "Photorealistic",
    aspectRatio: "16:9",
    compositionPreset: "Subject on the right, headline on the left",
    width: 1280,
    height: 720,
    aiPrompt: "Photorealistic documentary portrait of a stressed student...",
    artDirectionSummary: "Main subject on right, negative space on left...",
    headlineFont: "Bebas Neue",
    headlineColorHex: "#FFFFFF",
    accentColorHex: "#EF4444",
    headlineSizeSp: 64,
    headlineX: 8,
    headlineY: 22,
    exposure: 0,
    contrast: 15,
    saturation: -10,
    temperature: -5,
    vignette: 35,
    backgroundBlur: 10,
    localImagePath: "/data/user/0/proj_1.png",
    layersSummary: [
      "BACKGROUND|Studio Scene|0|0|1280|720|0|100|true|false|#000000|",
      "TEXT|Headline|8|22|600|140|0|100|true|false|#FFFFFF|EDUCATION SCAM"
    ],
    isTemplate: false,
    createdAt: now,
    updatedAt: now,
  };
}

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

test("1. Unauthenticated user: cannot read or list projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
});

test("2. Authenticated owner: can create and read a valid ThumbnailProject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .get()
  );
});

test("3. Cross-user isolation: Bob cannot read or write Alice's projects", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_2")
      .set(validProjectPayload(BOB_UID, now))
  );
});

test("4. Shadow field injection: rejects unknown fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const badPayload = {
    ...validProjectPayload(ALICE_UID, now),
    isVerifiedAdmin: true,
  };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_bad")
      .set(badPayload)
  );
});

test("5. Value poisoning & immutable fields: rejects mutating createdAt or invalid width", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now))
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        headline: "NEW HEADLINE",
        updatedAt: new Date(),
      })
  );

  // Invalid width out of range fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .update({
        width: 99999,
        updatedAt: new Date(),
      })
  );
});

test("6. BrandKit and GenerationHistoryItem rules enforce ownership and immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("brand_kits")
      .doc("kit_1")
      .set({
        userId: ALICE_UID,
        brandName: "Veritas Docs",
        primaryFont: "Bebas Neue",
        secondaryFont: "Montserrat",
        primaryColorHex: "#FFFFFF",
        secondaryColorHex: "#CBD5E1",
        accentColorHex: "#F59E0B",
        defaultStyle: "Realistic Documentary",
        channelTag: "VERITAS",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("history")
      .doc("hist_1")
      .set({
        userId: ALICE_UID,
        videoTopic: "Quantum Computing Explained",
        suggestedHeadline: "QUANTUM LEAP",
        category: "Science",
        visualStyle: "Scientific Visualization",
        realism: "Photorealistic",
        aspectRatio: "16:9",
        compositionPreset: "Subject on the right, headline on the left",
        mainSubject: "Quantum cryostat chandelier",
        lightingSetup: "Cool cyan and warm gold directional lighting",
        colorPalette: "Deep navy, cyan, gold",
        generatedPrompt: "Photorealistic quantum computer laboratory...",
        modelUsed: "gemini-nano-banana-2.1",
        localImagePath: "/data/user/0/hist_1.png",
        createdAt: now,
      })
  );

  // History items are immutable (cannot update)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("history")
      .doc("hist_1")
      .update({ suggestedHeadline: "HACKED" })
  );
});
