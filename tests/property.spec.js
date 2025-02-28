import { test, expect } from "@playwright/test";

test("Search rent ads in Παγκράτι", async ({ page }) => {
  await page.goto("https://www.xe.gr/");
  await page.getByRole("button", { name: "ΔΕΝ ΣΥΜΦΩΝΩ" }).click();
  await page.getByTestId("property-tab").click();
  await page.getByTestId("open-property-transaction-dropdown").click();
  await page.getByTestId("rent").click();
  await page.getByTestId("open-property-type-dropdown").click();
  await page.getByTestId("re_residence").click();

  // Choose all the related areas
  await page.locator('[data-testid="area-input"]').fill("Παγκρατι");
  await page.waitForSelector('[data-testid="geo_place_id_dropdown_panel"]');
  const areaButtons = await page
    .locator('[data-testid="geo_place_id_dropdown_panel"] button')
    .all();
  const numberOfAreas = areaButtons.length;

  // Click each area button one by one based on index
  for (let i = 0; i < numberOfAreas; i++) {
    await page.getByTestId("area-input").fill("Παγκρατι");
    const areaButton = areaButtons[i];
    const areaText = await areaButton.innerText();

    await areaButton.click();
  }

  await page.getByTestId("submit-input").click();

  // Set price range
  await page.getByTestId("price-filter-button").click();
  await page.getByTestId("minimum_price_input").fill("200");
  await page.getByTestId("maximum_price_input").fill("700");
  await page.keyboard.press("Enter");

  // Set square meters
  await page.getByTestId("size-filter-button").click();
  await page.getByTestId("minimum_size_input").fill("75");
  await page.getByTestId("maximum_size_input").fill("150");
  await page.keyboard.press("Enter");

  // Sort ads by descending price
  await page.getByTestId("open-property-sorting-dropdown").click();
  await page.getByTestId("price_desc").click();

  // Wait for the ads to reload after sorting
  await page.waitForTimeout(3000);

  // Get the number of ads
  const adCount = await page.locator("div.common-ad").count();
  console.log(`Total ads found: ${adCount}`);

  // Wait for the ads to fully load after sorting
  await page.waitForTimeout(5000);

  let priceValues = [];

  for (let i = 0; i < adCount; i++) {
    try {
      // Get the price for each ad
      const priceElement = page
        .locator("div.common-ad")
        .nth(i)
        .getByTestId("property-ad-price");

      // Wait for this specific element to be visible
      await priceElement
        .waitFor({ state: "visible", timeout: 2000 })
        .catch(() => {
          console.log(`Price element not visible for ad ${i + 1}`);
        });

      // Get the full text content
      const fullPriceText = await priceElement.innerText().catch((e) => {
        console.log(`Error getting innerText: ${e.message}`);
        return "";
      });

      //console.log(`Raw price text for ad ${i + 1}: "${fullPriceText}"`);

      // Look for digits followed by € symbol
      let price = null;
      let priceRegex = /(\d[\d.,\s]*)\s*€/;
      let match = fullPriceText.match(priceRegex);

      if (match && match[1]) {
        // Clean up the matched price
        const cleanedPrice = match[1]
          .replace(/\s/g, "")
          .replace(/\./g, "")
          .replace(/,/g, ".");
        price = parseFloat(cleanedPrice);
      } else {
        // Extract any number
        const numericMatch = fullPriceText.match(/\d[\d.,\s]*/);
        if (numericMatch) {
          const cleanedPrice = numericMatch[0]
            .replace(/\s/g, "")
            .replace(/\./g, "")
            .replace(/,/g, ".");
          price = parseFloat(cleanedPrice);
        }
      }

      if (price !== null && !isNaN(price)) {
        //console.log(`Extracted price for ad ${i + 1}: ${price}`);
        priceValues.push(price);
      } else {
        console.log(
          `Could not extract valid price from "${fullPriceText}" for ad ${
            i + 1
          }`
        );
      }
    } catch (error) {
      console.log(`Error processing ad ${i + 1}: ${error.message}`);
    }
  }

  // Log the extracted prices
  //console.log("Extracted Prices:", priceValues);

  // Check sorting
  if (priceValues.length > 1) {
    console.log("Verifying price sorting order (descending)...");
    let sortingCorrect = true;

    // Check if the prices are sorted in descending order
    for (let i = 0; i < priceValues.length - 1; i++) {
      if (priceValues[i] < priceValues[i + 1]) {
        console.log(
          `Sorting issue: ${priceValues[i]} is less than ${
            priceValues[i + 1]
          } at positions ${i} and ${i + 1}`
        );
        sortingCorrect = false;
      }
    }

    if (sortingCorrect) {
      console.log("Prices descending");
    } else {
      console.log("Prices NOT descending");
    }

    // Test assertion
    for (let i = 0; i < priceValues.length - 1; i++) {
      expect(priceValues[i]).toBeGreaterThanOrEqual(priceValues[i + 1]);
    }
  } else {
    expect(priceValues.length).toBeGreaterThan(0, "No prices");
  }

  await page.waitForSelector("div.common-ad", { timeout: 5000 });

  for (let i = 0; i < adCount; i++) {
    // Get the filtered ad (excluding ads with the "multiple-ad" class)
    const ad = page
      .locator("div.common-ad")
      .filter({
        hasNot: page.locator("div.common-ad.multiple-ad"),
      })
      .nth(i);

    await ad.click();

    // Check if the modal appears
    const modal = page.locator('[role="dialog"]');

    try {
      await modal.waitFor({ state: "visible", timeout: 3000 });

      // Check if modal contains the text "Αγγελίες για αυτό το ακίνητο"
      const modalText = await modal.innerText();
      if (modalText.includes("Αγγελίες για αυτό το ακίνητο")) {
        console.log("Modal with 'Αγγελίες για αυτό το ακίνητο'");

        // Close the modal
        const closeButton = modal.getByTestId("xe-modal-close");
        await closeButton.click();

        continue;
      }
    } catch (error) {
      console.log("No modal appeared");
    }

    const imageCountLocator = page.getByTestId("image-count-icon");
    let imageCountText = "";
    // Check if the element exists and is visible
    if (await imageCountLocator.isVisible()) {
      const imageCountText = await imageCountLocator.innerText();
      console.log(`Image Count: ${imageCountText}`);
    } else {
      console.log("Image count icon not found");
    }

    expect(imageCountText).not.toBeNull();
    expect(imageCountText).not.toBeUndefined();
    const imageCount = parseInt(imageCountText.trim(), 10);
    if (imageCount) {
      expect(imageCount).toBeGreaterThanOrEqual(0);
      expect(imageCount).toBeLessThan(31);
    }

    // Square meters
    const squareMetersText = await page
      .locator('[data-testid="basic-info"] > div.title > h1')
      .innerText();
    const squareMetersMatch = squareMetersText.match(/\d+/);
    const squareMeters = squareMetersMatch
      ? parseInt(squareMetersMatch[0], 10)
      : null;
    expect(squareMeters).not.toBe("");
    expect(squareMeters).not.toBeNull();
    expect(squareMeters).not.toBeUndefined();
    expect(squareMeters).toBeGreaterThan(0);

    // Price
    const priceText = await page.locator("div.price > h2").innerText();
    const priceMatch = priceText.match(/\d+/);
    const price = priceMatch ? parseInt(priceMatch[0], 10) : null;
    expect(price).not.toBe("");
    expect(price).not.toBeNull();
    expect(price).not.toBeUndefined();
    expect(price).toBeGreaterThan(0);

    // Telephone number
    const phoneNumber = page.locator('[data-testid="phones"]');
    await expect(phoneNumber).not.toBeVisible();

    const callButton = page.locator('[data-testid="call-action-button"]');
    await expect(callButton).toBeVisible();
    await expect(callButton).toBeEnabled();

    await callButton.click();

    const phoneNumberInModal = page.getByTestId("phones");
    await expect(phoneNumberInModal).toBeVisible();

    const phoneModalCloseButton = page
      .getByRole("dialog")
      .filter({ hasText: "Τηλέφωνα επικοινωνίας" })
      .getByTestId("xe-modal-close");

    await phoneModalCloseButton.click();

    await page.getByTestId("xe-modal-close").click();
  }
});
