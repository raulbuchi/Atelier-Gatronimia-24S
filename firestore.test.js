const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_chef_01";
const BOB_UID = "bob_chef_02";
const CLARA_UID = "clara_chef_03";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
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

function buildValidPostPayload(userId, visibility = "private", extra = {}) {
  const now = pastTimestamp();
  return {
    userId,
    authorName: "Chef Alice",
    authorAvatarBase64: "YXZhdGFyXzQwMHB4",
    visibility,
    weekNumber: 17,
    cycleNumber: 3,
    axisId: 4,
    materialType: "prato_final",
    recipeId: "rec-gefluegelgalantine",
    recipeTitle: "Galantina de Ave Desossada",
    isSwissmilkClassic: false,
    isChefkochV4: true,
    cantonCode: "ZH",
    caption: "Centro térmico cravado em 65 °C, farce passada no tamis sobre gelo.",
    mepSummary: "FC 1.0 | PCC 65 °C | Resfriamento rápido <= 4 °C",
    selbstkontrolleScore: 5,
    erkenntnisse: "Farce emulsionada abaixo de 4 °C manteve corte liso.",
    offenePunkte: "Apertar cilindragem nas extremidades.",
    aiFeedback: "",
    thumbnailBase64: "dGVzdF90aHVtYg==",
    imageLink: "firestore://portfolio_media/post_17",
    reactedUserIds: [],
    reactionCount: 0,
    commentCount: 0,
    createdAt: now,
    updatedAt: now,
    ...extra,
  };
}

test("Unauthenticated user cannot read profiles, portfolio_posts, or mep_sheets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("portfolio_posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("mep_sheets").where("userId", "==", ALICE_UID).get());
});

test("Profile owner can create/update own profile with avatarBase64 and defaultVisibility, cross-user read blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Chef Alice",
      avatarBase64: "YXZhdGFyXzQwMHB4",
      defaultVisibility: "public",
      currentWeek: 10,
      cantonFocus: "BE",
      specialtyBadge: "Saucier",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      defaultVisibility: "private",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("profiles").doc(ALICE_UID).get());

  // Reject invalid defaultVisibility enum
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      defaultVisibility: "friends_only",
      updatedAt: pastTimestamp(),
    })
  );

  // Shadow update test (ghost field injection)
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      isVerifiedAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Private posts block non-owner read; public Mural posts enforce strict caller-only reaction toggle (+1 / -1) and v6 reflection bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const claraDb = testEnv.authenticatedContext(CLARA_UID).firestore();
  const now = pastTimestamp();

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

  await assertFails(bobDb.collection("portfolio_posts").doc("post_priv_17").get());
  await assertFails(bobDb.collection("portfolio_media").doc("post_priv_17").get());
  await assertFails(bobDb.collection("portfolio_posts").get());
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").where("userId", "==", ALICE_UID).get()
  );

  // Reject out-of-range selbstkontrolleScore (> 5)
  await assertFails(
    aliceDb.collection("portfolio_posts").doc("post_invalid_score").set(
      buildValidPostPayload(ALICE_UID, "public", { selbstkontrolleScore: 9 })
    )
  );

  // Create public Mural post
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc("post_pub_17").set(buildValidPostPayload(ALICE_UID, "public"))
  );

  // Bob queries public Mural
  await assertSucceeds(
    bobDb.collection("portfolio_posts").where("visibility", "==", "public").get()
  );

  // Bob CANNOT spoof Clara's UID when reacting
  await assertFails(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      reactedUserIds: [CLARA_UID],
      reactionCount: 1,
      updatedAt: pastTimestamp(),
    })
  );

  // Bob adds his own reaction (+1) -> succeeds
  await assertSucceeds(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      reactedUserIds: [BOB_UID],
      reactionCount: 1,
      updatedAt: pastTimestamp(),
    })
  );

  // Clara adds her reaction while preserving Bob's (+1) -> succeeds
  await assertSucceeds(
    claraDb.collection("portfolio_posts").doc("post_pub_17").update({
      reactedUserIds: [BOB_UID, CLARA_UID],
      reactionCount: 2,
      updatedAt: pastTimestamp(),
    })
  );

  // Bob removes ONLY his own reaction (-1) -> succeeds
  await assertSucceeds(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      reactedUserIds: [CLARA_UID],
      reactionCount: 1,
      updatedAt: pastTimestamp(),
    })
  );

  // Non-owner cannot edit caption or erkenntnisse
  await assertFails(
    bobDb.collection("portfolio_posts").doc("post_pub_17").update({
      erkenntnisse: "Hacked reflection",
      updatedAt: pastTimestamp(),
    })
  );
});

test("MepSheetCloudLink enforces atomic portfolio_media ownership, imageLink format, and owner-only queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastTimestamp();
  const mediaId = "mep_media_zurcher_01";
  const sheetDocId = `${ALICE_UID}_zurcher_geschnetzeltes`;

  await assertFails(
    aliceDb.collection("mep_sheets").doc(sheetDocId).set({
      userId: ALICE_UID,
      recipeId: "rec-zurcher-geschnetzeltes",
      title: "Iscas de Vitela à Moda de Zurique",
      weekNumber: 8,
      stationBrigade: "Saucier",
      targetServings: 4,
      pccTargetTempC: 68,
      executionScore1To5: 5,
      mediaId,
      imageLink: `firestore://portfolio_media/${mediaId}`,
      thumbnailBase64: "dGh1bWI=",
      createdAt: now,
      updatedAt: now,
    })
  );

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("portfolio_media").doc(mediaId), {
    userId: ALICE_UID,
    visibility: "private",
    imageBase64: "Y2FtZXJheF9qcGVn",
    width: 1200,
    height: 900,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("mep_sheets").doc(sheetDocId), {
    userId: ALICE_UID,
    recipeId: "rec-zurcher-geschnetzeltes",
    title: "Iscas de Vitela à Moda de Zurique",
    weekNumber: 8,
    stationBrigade: "Saucier",
    targetServings: 4,
    pccTargetTempC: 68,
    executionScore1To5: 5,
    mediaId,
    imageLink: `firestore://portfolio_media/${mediaId}`,
    thumbnailBase64: "dGh1bWI=",
    localPhotoUri: "content://com.aistudio.ateliergastronomia.v4kxmp.fileprovider/mep_creations/test.jpg",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(
    aliceDb.collection("mep_sheets").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("mep_sheets").get());
  await assertFails(bobDb.collection("mep_sheets").doc(sheetDocId).get());
});

test("Comments support atomic creation (+1 commentCount), public list query, and cascade post-owner batch cleanup", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const claraDb = testEnv.authenticatedContext(CLARA_UID).firestore();

  const postId = "post_pub_1";
  await assertSucceeds(
    aliceDb.collection("portfolio_posts").doc(postId).set(buildValidPostPayload(ALICE_UID, "public"))
  );

  const createBatch = bobDb.batch();
  createBatch.set(
    bobDb.collection("portfolio_posts").doc(postId).collection("comments").doc("c_1"),
    {
      postId,
      postOwnerId: ALICE_UID,
      postVisibility: "public",
      userId: BOB_UID,
      authorName: "Chef Bob",
      text: "Excelente ponto nappé e brilho do fundo!",
      createdAt: pastTimestamp(),
    }
  );
  createBatch.update(bobDb.collection("portfolio_posts").doc(postId), {
    commentCount: 1,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(createBatch.commit());

  await assertSucceeds(
    claraDb
      .collection("portfolio_posts")
      .doc(postId)
      .collection("comments")
      .where("postVisibility", "==", "public")
      .get()
  );

  // Post owner Alice can query comments and batch-delete child comments + parent post (cascade delete v6)
  const ownerCommentsSnap = await assertSucceeds(
    aliceDb
      .collection("portfolio_posts")
      .doc(postId)
      .collection("comments")
      .where("postOwnerId", "==", ALICE_UID)
      .get()
  );
  const cascadeBatch = aliceDb.batch();
  ownerCommentsSnap.docs.forEach((docSnap) => cascadeBatch.delete(docSnap.ref));
  cascadeBatch.delete(aliceDb.collection("portfolio_posts").doc(postId));
  await assertSucceeds(cascadeBatch.commit());
});
