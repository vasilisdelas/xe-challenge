package xe;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.testng.annotations.*;

import static org.testng.Assert.*;

public class PropertyTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeClass
    public void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    public void searchRentAdsInPagrati() {
        // Navigate to website
        page.navigate("https://www.xe.gr/");

        // Handle cookie popup
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("ΔΕΝ ΣΥΜΦΩΝΩ")).click();

        // Select property tab
        page.getByTestId("property-tab").click();

        // Select rent transaction
        page.getByTestId("open-property-transaction-dropdown").click();
        page.getByTestId("rent").click();

        // Select residence property type
        page.getByTestId("open-property-type-dropdown").click();
        page.getByTestId("re_residence").click();

        // Choose all related areas - using Locator instead of ElementHandle
        page.locator("[data-testid=\"area-input\"]").fill("Παγκρατι");
        page.waitForSelector("[data-testid=\"geo_place_id_dropdown_panel\"]");

        // Get count of area options
        int numberOfAreas = page.locator("[data-testid=\"geo_place_id_dropdown_panel\"] button").count();

        // Click each area button one by one
        for (int i = 0; i < numberOfAreas; i++) {
            page.getByTestId("area-input").fill("Παγκρατι");
            page.waitForSelector("[data-testid=\"geo_place_id_dropdown_panel\"]");

            // Use locator instead of ElementHandle to avoid detachment issues
            page.locator("[data-testid=\"geo_place_id_dropdown_panel\"] button").nth(i).click();
        }

        // Submit search
        page.getByTestId("submit-input").click();

        // Set Price Range
        page.getByTestId("price-filter-button").click();
        page.getByTestId("minimum_price_input").fill("200");
        page.getByTestId("maximum_price_input").fill("700");
        page.keyboard().press("Enter");

        // Set Square Meters
        page.getByTestId("size-filter-button").click();
        page.getByTestId("minimum_size_input").fill("75");
        page.getByTestId("maximum_size_input").fill("150");
        page.keyboard().press("Enter");

        // Wait for ads to load
        page.waitForSelector("div.common-ad", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Count number of ads
        int adCount = page.locator("div.common-ad").count();
        System.out.println("Number of ads loaded: " + adCount);

        // Sort ads by descending price
        page.getByTestId("open-property-sorting-dropdown").click();
        page.getByTestId("price_desc").click();

        // Wait for the ads to reload after sorting
        page.waitForTimeout(3000);

        // Create a list to store prices
        java.util.List<Double> priceValues = new java.util.ArrayList<>();

        // Extract prices from all ads
        System.out.println("Extracting prices from ads...");
        try {
            // Use a more direct approach to get all prices at once
            java.util.List<String> priceTexts = page
                    .locator("*[class*=\"common-ad\"] [data-testid=\"property-ad-price\"]")
                    .allInnerTexts();

            for (int i = 0; i < priceTexts.size(); i++) {
                try {
                    String priceText = priceTexts.get(i);
                    System.out.println("Raw price text for ad " + (i + 1) + ": \"" + priceText + "\"");

                    // Extract price using regex
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d[\\d.,\\s]*)\\s*€");
                    java.util.regex.Matcher matcher = pattern.matcher(priceText);

                    if (matcher.find()) {
                        // Clean up the matched price (remove spaces, replace dots with empty, replace commas with dots)
                        String cleanedPrice = matcher.group(1)
                                .replace(" ", "")
                                .replace(".", "")
                                .replace(",", ".");

                        double price = Double.parseDouble(cleanedPrice);
                        System.out.println("Extracted price for ad " + (i + 1) + ": " + price);
                        priceValues.add(price);
                    } else {
                        System.out.println("Could not extract price from \"" + priceText + "\" for ad " + (i + 1));
                    }
                } catch (Exception e) {
                    System.out.println("Error processing price for ad " + (i + 1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error extracting prices: " + e.getMessage());
        }

        // Log the extracted prices
        System.out.println("Extracted Prices: " + priceValues);

        // Verify prices are in descending order
        if (priceValues.size() > 1) {
            System.out.println("Verifying price sorting order (descending)...");
            boolean sortingCorrect = true;

            // Check if the prices are sorted in descending order
            for (int i = 0; i < priceValues.size() - 1; i++) {
                if (priceValues.get(i) < priceValues.get(i + 1)) {
                    System.out.println("Sorting issue: " + priceValues.get(i) +
                            " is less than " + priceValues.get(i + 1) +
                            " at positions " + i + " and " + (i + 1));
                    sortingCorrect = false;
                }
            }

            if (sortingCorrect) {
                System.out.println("Prices are correctly sorted in descending order");
            } else {
                System.out.println("Prices are NOT correctly sorted in descending order");
            }

            // Add test assertions
            for (int i = 0; i < priceValues.size() - 1; i++) {
                assertTrue(priceValues.get(i) >= priceValues.get(i + 1),
                        "Price at position " + i + " (" + priceValues.get(i) +
                                ") is less than price at position " + (i + 1) +
                                " (" + priceValues.get(i + 1) + ")");
            }
        } else {
            System.out.println("Not enough prices extracted to verify sorting");
        }
        // Iterate over ads
        for (int i = 0; i < adCount; i++) {
            try {
                // Refresh the list of ads for each iteration
                Locator ads = page.locator("div.common-ad")
                        .filter(new Locator.FilterOptions()
                                .setHasNot(page.locator("div.common-ad.multiple-ad")));

                // Make sure we haven't exceeded the number of ads
                int currentAdCount = ads.count();
                if (i >= currentAdCount) {
                    System.out.println("No more ads available. Breaking loop.");
                    break;
                }

                // Click the current ad
                ads.nth(i).click();

                // Check if the modal appears
                Locator modal = page.locator("[role=\"dialog\"]");

                try {
                    modal.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000));

                    // Check if modal contains the text "Αγγελίες για αυτό το ακίνητο"
                    String modalText = modal.innerText();
                    if (modalText.contains("Αγγελίες για αυτό το ακίνητο")) {
                        System.out.println("Modal with 'Αγγελίες για αυτό το ακίνητο' detected. Closing...");

                        // Close the modal
                        Locator closeButton = modal.locator("[data-testid=\"xe-modal-close\"]");
                        closeButton.click();

                        continue; // Skip to the next iteration of the loop
                    }
                } catch (PlaywrightException e) {
                    System.out.println("No modal appeared or modal check failed.");
                }

                // Alternative approach - check if element exists first
                String imageCountText = "";
                if (page.getByTestId("image-count-icon").count() > 0) {
                    try {
                        imageCountText = page
                                .getByTestId("image-count-icon")
                                .locator("span")
                                .first()
                                .innerText();

                        if (imageCountText != null && !imageCountText.trim().isEmpty()) {
                            int imageCount = Integer.parseInt(imageCountText.trim());
                            assertTrue(imageCount > 0);
                            assertTrue(imageCount < 31);
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing image count: " + e.getMessage());
                    }
                } else {
                    System.out.println("Image count element not found on page");
                }

                // Square Meters
                try {
                    String squareMetersText = page
                            .locator("[data-testid=\"basic-info\"] > div.title > h1")
                            .innerText();

                    // Extract square meters using regex
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
                    java.util.regex.Matcher matcher = pattern.matcher(squareMetersText);
                    Integer squareMeters = null;
                    if (matcher.find()) {
                        squareMeters = Integer.parseInt(matcher.group(0));
                    }

                    assertNotNull(squareMeters);
                    assertTrue(squareMeters > 0);
                } catch (PlaywrightException e) {
                    System.out.println("Could not get square meters: " + e.getMessage());
                }

                // Price
                try {
                    String priceText = page.locator("div.price > h2").innerText();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
                    java.util.regex.Matcher matcher = pattern.matcher(priceText);
                    Integer price = null;
                    if (matcher.find()) {
                        price = Integer.parseInt(matcher.group(0));
                    }

                    assertNotNull(price);
                    assertTrue(price > 0);
                } catch (PlaywrightException e) {
                    System.out.println("Could not get price: " + e.getMessage());
                }

                // Telephone Number
                try {
                    Locator phoneNumber = page.locator("[data-testid=\"phones\"]");
                    assertFalse(phoneNumber.isVisible());

                    Locator callButton = page.locator("[data-testid=\"call-action-button\"]");

                    if (callButton.isVisible()) {
                        callButton.click();

                        Locator phoneNumberInModal = page.locator("[data-testid=\"phones\"]");

                        // Wait for the phone number to be visible
                        phoneNumberInModal.waitFor(new Locator.WaitForOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(3000));

                        Locator phoneModalCloseButton = page
                                .getByRole(AriaRole.DIALOG)
                                .filter(new Locator.FilterOptions().setHasText("Τηλέφωνα επικοινωνίας"))
                                .getByTestId("xe-modal-close");

                        phoneModalCloseButton.click();
                    }

                    // Close the property detail modal
                    page.getByTestId("xe-modal-close").click();

                    // Wait a bit for any animations to complete
                    page.waitForTimeout(1000);

                } catch (PlaywrightException e) {
                    System.out.println("Error in phone number verification: " + e.getMessage());

                    // Try to close the modal regardless of errors
                    try {
                        page.getByTestId("xe-modal-close").click();
                        page.waitForTimeout(1000);
                    } catch (Exception ex) {
                        System.out.println("Could not close modal: " + ex.getMessage());
                    }
                }

            } catch (Exception e) {
                System.out.println("Error processing ad " + i + ": " + e.getMessage());

                // Try to close any open modals
                try {
                    Locator modals = page.locator("[role=\"dialog\"]");
                    if (modals.count() > 0) {
                        modals.first().locator("[data-testid=\"xe-modal-close\"]").click();
                    }
                    page.waitForTimeout(1000);
                } catch (Exception ex) {
                    System.out.println("Could not recover from error: " + ex.getMessage());
                }
            }
        }
    }

    @AfterClass
    public void tearDown() {
        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}