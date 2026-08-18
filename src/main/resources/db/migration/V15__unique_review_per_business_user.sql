-- One live review per (business, user) — Yelp-style: a user edits their existing
-- review instead of stacking new ones. Scoped to non-deleted rows only, mirroring
-- ux_business_slug_live, so a deleted review doesn't block writing a fresh one.
--
-- Pre-existing data already contains duplicates (submit() had no such guard
-- before this migration), so before the index can be created we soft-delete
-- every duplicate but the most recent per (business, user) pair, then
-- recompute each affected business's rating aggregate the same way
-- applyRatingAggregateDelta does, so averages/counts match what's actually
-- still visible.

WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY business_id, user_id ORDER BY created_at DESC) AS rn
    FROM review
    WHERE deleted_at IS NULL
)
UPDATE review SET deleted_at = now()
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);

UPDATE business b
SET rating_sum = agg.rating_sum,
    review_count = agg.review_count,
    average_rating = CASE WHEN agg.review_count = 0 THEN 0 ELSE ROUND(agg.rating_sum::numeric / agg.review_count, 2) END,
    updated_at = now()
FROM (
    SELECT r.business_id,
           COALESCE(SUM(r.rating) FILTER (WHERE r.visibility_status = 'RECOMMENDED'), 0)::int AS rating_sum,
           COUNT(*) FILTER (WHERE r.visibility_status = 'RECOMMENDED')::int AS review_count
    FROM review r
    WHERE r.deleted_at IS NULL
    GROUP BY r.business_id
) agg
WHERE b.id = agg.business_id
  AND (b.rating_sum, b.review_count) IS DISTINCT FROM (agg.rating_sum, agg.review_count);

CREATE UNIQUE INDEX ux_review_business_user_live ON review(business_id, user_id) WHERE deleted_at IS NULL;
