const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-atelierg-5f8fc63b-3589-401a-9107-e1c69f39fb34";
const ALICE_UID = "alice_chef_01";
const BOB_UID = "bob_chef_02";

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

function buildValidPostPayload(userId, visibility = "private", extra = {}) {
  const now = new Date();
  return {
    userId,
    authorName: "Chef Alice",
    visibility,
    weekNumber: 17,
    cycleNumber: 3,
    axisId: 4,
    materialType: "prato_final",
    recipeId: "gefluegelgalantine_v4",
    recipeTitle: "Geflügelgalantine",
    isSwissmilkClassic: false,
    isChefkochV4: true,
    cantonCode: "ZH",
    caption: "Centro térmico cravado em 65 °C, farce passada no tamis sobre gelo.",
    mepSummary: "FC 1.0 | PCC 65 °C | Resfriamento rápido <= 5 °C",
    aiFeedback: "",
    thumbnailBase64: "dGVzdF90aHVtYg==",
    reactedUserIds: [],
    reactionCount: 0,
    commentCount: 0,
    createdAt: now,
    updatedAt: now,
    ...extra,
  };
}

test("Unauthenticated user cannot read profiles or portfolio_posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("portfolio_posts").where("visibility", "==", "public").get());
});

test("Profile owner can create/read own profile, cross-user read blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Chef Alice",
      currentWeek: 10,
      cantonFocus: "BE",
      specialtyBadge: "Saucier",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("profiles").doc(ALICE_UID).get());

  // Shadow update test (ghost field injection)
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      isVerifiedAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Private portfolio_posts and portfolio_media block non-owner read; public posts allow Mural query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  // Create private post + media
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc("post_priv_17").set(buildValidPostPayload(ALICE_UID, "private"))
  );
  await assertSucceeds(
    aliceDb.collection("portfolio_media").doc("post_priv_17").set({
      userId: ALICE_UID,
      visibility: "private",
      imageBase64: "aW1hZ2VfZGF0YQ==",
      width: 1200,
      height: 900,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's private post or media
  await assertFails(bobDb.collection("portfolio_posts").doc("post_priv_17").get());
  await assertFails(bobDb.collection("portfolio_media").doc("post_priv_17").get());

  // Unfiltered query on portfolio_posts fails (rules are not filters)
  await assertFails(bobDb.collection("portfolio_posts").get());

  // Alice can query her own portfolio_posts
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").where("userId", "==", ALICE_UID).get()
  );

  // Create public post
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc("post_pub_17").set(buildValidPostPayload(ALICE_UID, "public"))
  );

  // Bob can query public mural posts and toggle reaction, but cannot edit caption
  await assertSucceeds(
    bobDb.collection("portfolio_posts").where("visibility", "==", "public").get()
  );
  await assertSucceeds(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      reactedUserIds: [BOB_UID],
      reactionCount: 1,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      caption: "Hacked caption",
      updatedAt: new Date(),
    })
  );
});

test("Comments require existing accessible post and enforce denormalized visibility", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc("post_pub_1").set(buildValidPostPayload(ALICE_UID, "public"))
  );
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc("post_priv_1").set(buildValidPostPayload(ALICE_UID, "private"))
  );

  // Bob can comment on public post
  await assertSucceeds(
    bobDb.collection("portfolio_posts").doc("post_pub_1").collection("comments").doc("c_1").set({
      postId: "post_pub_1",
      postOwnerId: ALICE_UID,
      postVisibility: "public",
      userId: BOB_UID,
      authorName: "Chef Bob",
      text: "Excelente nappé e brilho do fundo!",
      createdAt: new Date(),
    })
  );

  // Bob can list public comments with matching filter
  await assertSucceeds(
    bobDb.collection("portfolio_posts").doc("post_pub_1").collection("comments")
      .where("postVisibility", "==", "public").get()
  );

  // Bob cannot comment on Alice's private post
  await assertFails(
    bobDb.collection("portfolio_posts").doc("post_priv_1").collection("comments").doc("c_2").set({
      postId: "post_priv_1",
      postOwnerId: ALICE_UID,
      postVisibility: "private",
      userId: BOB_UID,
      authorName: "Chef Bob",
      text: "Tentativa não autorizada",
      createdAt: new Date(),
    })
  );
});
