package com.dominic.photosweep;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

/** Never delete a tracked photo that somebody has restored through their gallery. */
final class TrashPolicy {
    private TrashPolicy() { }
    static int state(ContentResolver resolver, Uri uri) {
        Bundle query = new Bundle();
        query.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE);
        try (Cursor cursor = resolver.query(uri, new String[]{MediaStore.Images.Media.IS_TRASHED}, query, null)) {
            if (cursor == null) return -1; // Unknown access: retry later.
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (RuntimeException error) { return -1; }
    }
}
