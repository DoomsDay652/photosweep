package com.dominic.photosweep;

/** Snapshot before one successful review action; no gallery bytes are stored. */
final class ReviewUndo {
    final long id;
    final String month;
    final long timestamp;
    final boolean trashed, wasReviewed, wasKept, wasTrashed;
    int earnedXp;

    ReviewUndo(long id, String month, long timestamp, boolean trashed, boolean wasReviewed, boolean wasKept, boolean wasTrashed) {
        this.id = id; this.month = month; this.timestamp = timestamp; this.trashed = trashed;
        this.wasReviewed = wasReviewed; this.wasKept = wasKept; this.wasTrashed = wasTrashed;
    }
    void rollback(java.util.Set<String> reviewed, java.util.Set<String> kept, java.util.Set<String> trashedIds,
            java.util.Set<String> rewarded) {
        String key = Long.toString(id);
        if (wasReviewed) reviewed.add(key); else reviewed.remove(key);
        if (!wasKept) kept.remove(key);
        if (!wasTrashed) trashedIds.remove(key);
        if (earnedXp > 0) rewarded.remove(key);
    }
    int restoredXp(int current) { return Math.max(0, current - earnedXp); }
}
