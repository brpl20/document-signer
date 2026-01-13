# Bug Report: Visual Signature Appears on Wrong Page

## Summary
When signing a multi-page PDF with a visible signature, the signature always appears on the **first page** instead of the **selected page**.

## Location
**File:** `src/main/java/com/example/documentsigner/pades/PadesSignerService.java`
**Method:** `createVisualSignatureTemplate()` (lines 269-380)

## Root Cause
The `createVisualSignatureTemplate` method creates a new PDF document template with only **one page** (imported from the source):

```java
// Line 277-278
PDPage srcPage = srcDoc.getPage(pageIndex);
PDPage templatePage = template.importPage(srcPage);
```

The problem is that when PDFBox applies this visual signature template to the original document, it doesn't correctly map the template's single page to the target page index. The template document has only 1 page (index 0), so the signature widget ends up on page 0 of the original document.

## Technical Details

1. `signatureOptions.setPage(pageIndex)` is called correctly (line 207)
2. However, the visual signature template overrides this because:
   - Template has only 1 page (the imported page)
   - Widget is set on `templatePage` which is at index 0 in the template
   - When merged, PDFBox places widget on page 0 of original document

## Proposed Fix

The template document should have `pageIndex + 1` pages, with the signature widget placed on the correct page index:

```java
private byte[] createVisualSignatureTemplate(PDDocument srcDoc, int pageIndex,
                                              PDRectangle signatureRect,
                                              SignerDisplayInfo signerInfo) throws IOException {
    PDDocument template = new PDDocument();

    try {
        // Create empty pages up to the target page
        for (int i = 0; i <= pageIndex; i++) {
            PDPage srcPage = srcDoc.getPage(i);
            PDPage newPage = new PDPage(srcPage.getMediaBox());
            template.addPage(newPage);
        }

        // Get the target page (at correct index)
        PDPage targetPage = template.getPage(pageIndex);

        // Create AcroForm and signature field on targetPage
        // ... rest of the code with widget.setPage(targetPage) ...
        // ... and targetPage.getAnnotations().add(widget) ...
```

## Affected Features
- API endpoint: `POST /api/v1/sign/pdf` with `visible=true` and `page > 1`
- GUI: PAdES signing with visual signature on pages other than first

## Test Case
1. Use a multi-page PDF (3+ pages)
2. Sign with visible signature, selecting page 3
3. **Expected:** Signature appears on page 3
4. **Actual:** Signature appears on page 1

## Priority
Medium - Core signing functionality works, only visual signature placement is affected.

## Resolution

**Status: FIXED**

The fix was applied to `createVisualSignatureTemplate()` method in `PadesSignerService.java`:

- Changed from importing only the target page to creating empty pages for all pages up to and including the target page
- The template now has the correct page structure so the signature widget is placed on the correct page index when merged
- Edge cases (first page, last page, middle pages) all work correctly
- Page validation already exists to prevent out-of-bounds errors
