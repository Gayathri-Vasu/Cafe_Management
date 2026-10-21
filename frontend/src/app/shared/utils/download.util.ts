/** Triggers a browser download for an in-memory blob (used for invoice PDFs). */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

/**
 * Opens a blob (typically a PDF) in a new browser tab. Used by the "Print"
 * action so the browser's PDF viewer can handle printing.
 */
export function openBlobInNewTab(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  const opened = window.open(url, '_blank');
  // Revoke once the new tab has had a chance to load the document.
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  if (!opened) {
    URL.revokeObjectURL(url);
  }
}
