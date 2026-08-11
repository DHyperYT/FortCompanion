# Shop Image & Section Refinement Walkthrough

I have refined the Item Shop's image resolution logic and section ordering to be strictly data-driven and aligned with the Fortnite-API v2 structure.

## Changes Made

### Data Layer
- **[PublicModels.kt](file:///C:/Users/lol/Desktop/fortnite-companion%20(1)/app/src/main/java/com/dhyper/fncompanion/data/models/PublicModels.kt)**: Updated `NewDisplayAsset` to include `cosmeticId` and `renderImages`. Added `RenderImage` data class to support modern API fields.

### View Models
- **[ShopViewModel.kt](file:///C:/Users/lol/Desktop/fortnite-companion%20(1)/app/src/main/java/com/dhyper/fncompanion/ui/viewmodels/ShopViewModel.kt)**:
    - Refined `sortEntries` to ensure "Special Offers" are placed second-to-last and "Jam Tracks" are absolute last.
    - Updated `getItemsForEntryInternal` to be more robust and consistent with the UI layer.

### UI Layer
- **[ShopScreen.kt](file:///C:/Users/lol/Desktop/fortnite-companion%20(1)/app/src/main/java/com/dhyper/fncompanion/ui/screens/ShopScreen.kt)**:
    - **Refined `getShopEntryImage`**:
        - Prioritizes `bundle.image`.
        - Uses `newDisplayAsset.renderImages` for high-quality offer renders.
        - Uses `newDisplayAsset.cosmeticId` to resolve the specific featured item's image when a bundle image is missing.
    - **ID-Strict Lookups**: Integrated `resolveIncludedItemImage` to ensure images are fetched based on exact ID and type (Vehicle, Track, Lego, or BR).

## Verification Results

### Build
- Successfully ran `:app:assembleDebug`.

### Logic Verification
- **Image Priority**:
    1. Bundle Image
    2. Render Images List
    3. Material Instance fallbacks
    4. Referenced Cosmetic ID (data-driven)
    5. Item fallbacks
- **Section Order**:
    - [Normal Sections]
    - [Vehicles]
    - [Special Offers]
    - [Jam Tracks]

This ensures a clean, reliable, and modern shop presentation that handles bundles and single-item offers correctly even when metadata is sparse.
