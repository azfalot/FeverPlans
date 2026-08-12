const { test, expect } = require("@playwright/test");

const strictStart = "2021-01-01T00:00:00Z";
const strictEnd = "2022-01-01T00:00:00Z";

function providerLocalDateTime(event, prefix) {
  return `${event[`${prefix}_date`]}T${event[`${prefix}_time`]}`;
}

function localPart(isoDateTime) {
  return isoDateTime.replace(/Z$/, "");
}

function expectStrictlyAfter(actual, bound) {
  expect(actual.localeCompare(localPart(bound))).toBeGreaterThan(0);
}

function expectStrictlyBefore(actual, bound) {
  expect(actual.localeCompare(localPart(bound))).toBeLessThan(0);
}

async function applicationStatus(request) {
  try {
    return (await request.get("/v3/api-docs")).status();
  } catch {
    return 0;
  }
}

test.beforeEach(async ({ request }) => {
  await expect
    .poll(() => applicationStatus(request), {
      message: "The application did not become ready. Start it with `docker compose up --build -d`."
    })
    .toBe(200);
});

test("Swagger UI document is available", async ({ request }) => {
  const response = await request.get("/swagger-ui/index.html");
  const document = await response.text();

  expect(response.status()).toBe(200);
  expect(document).toContain("Swagger UI");
});

test("S-01 returns locally stored plans without filters", async ({ request }) => {
  const response = await request.get("/search");
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body.error).toBeNull();
  expect(body.data.events).toBeInstanceOf(Array);
  expect(body.data.events.length).toBeGreaterThan(0);
});

test("S-02 applies both strict time bounds and preserves the response contract", async ({ request }) => {
  const response = await request.get("/search", {
    params: { starts_at: strictStart, ends_at: strictEnd }
  });
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body.error).toBeNull();
  expect(body.data.events.length).toBeGreaterThan(0);

  for (const event of body.data.events) {
    expectStrictlyAfter(providerLocalDateTime(event, "start"), strictStart);
    expectStrictlyBefore(providerLocalDateTime(event, "end"), strictEnd);
    expect(event.id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i);
    expect(event.title).toEqual(expect.any(String));
    expect(event.start_date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(event.start_time).toMatch(/^\d{2}:\d{2}:\d{2}$/);
    expect(event.end_date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(event.end_time).toMatch(/^\d{2}:\d{2}:\d{2}$/);
    expect(event.min_price === null || typeof event.min_price === "number").toBeTruthy();
    expect(event.max_price === null || typeof event.max_price === "number").toBeTruthy();
  }
});

test("S-03 applies only the lower strict bound", async ({ request }) => {
  const lowerBound = "2021-06-01T00:00:00Z";
  const response = await request.get("/search", { params: { starts_at: lowerBound } });
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body.error).toBeNull();
  expect(body.data.events.length).toBeGreaterThan(0);
  for (const event of body.data.events) {
    expectStrictlyAfter(providerLocalDateTime(event, "start"), lowerBound);
  }
});

test("S-04 applies only the upper strict bound", async ({ request }) => {
  const upperBound = "2021-07-01T00:00:00Z";
  const response = await request.get("/search", { params: { ends_at: upperBound } });
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body.error).toBeNull();
  expect(body.data.events.length).toBeGreaterThan(0);
  for (const event of body.data.events) {
    expectStrictlyBefore(providerLocalDateTime(event, "end"), upperBound);
  }
});

test("S-05 accepts equal bounds and returns no event", async ({ request }) => {
  const bound = "2021-07-21T17:32:28Z";
  const response = await request.get("/search", {
    params: { starts_at: bound, ends_at: bound }
  });
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body).toEqual({ data: { events: [] }, error: null });
});

test("S-06 accepts reverse bounds and returns no event", async ({ request }) => {
  const response = await request.get("/search", {
    params: { starts_at: "2022-01-01T00:00:00Z", ends_at: "2021-01-01T00:00:00Z" }
  });
  const body = await response.json();

  expect(response.status()).toBe(200);
  expect(body).toEqual({ data: { events: [] }, error: null });
});

test("S-07 returns the documented error envelope for an invalid date-time", async ({ request }) => {
  const response = await request.get("/search?starts_at=not-a-date");
  const body = await response.json();

  expect(response.status()).toBe(400);
  expect(body.data).toBeNull();
  expect(body.error).toMatchObject({ code: "BAD_REQUEST" });
  expect(body.error.message).toEqual(expect.any(String));
});
