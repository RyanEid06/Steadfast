package com.steadfast.app.smoke;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

/** Minimal test-only document provider for the optimized UI backup flow. */
public final class SyntheticDocumentsProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) { return null; }
    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) return 0;
        return new File(new File(getContext().getCacheDir(), "synthetic-documents"), name).delete() ? 1 : 0;
    }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) throw new FileNotFoundException("Invalid synthetic document");
        if (name.startsWith("provider-error")) throw new FileNotFoundException("Synthetic provider read failure");
        File root = new File(getContext().getCacheDir(), "synthetic-documents");
        if (!root.exists() && !root.mkdirs()) throw new FileNotFoundException("Cannot create provider storage");
        File file = new File(root, name);
        if ("r".equals(mode) && !file.isFile()) throw new FileNotFoundException("Synthetic document is missing");
        int flags = "r".equals(mode) ? ParcelFileDescriptor.MODE_READ_ONLY
            : ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_TRUNCATE;
        return ParcelFileDescriptor.open(file, flags);
    }
}
