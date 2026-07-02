databaseChangeLog = {

    // ── Full-text search ──────────────────────────────────────────────────────────
    // A STORED generated `search_fts` tsvector column + GIN index on every searchable
    // table. Generated columns auto-maintain on insert/update — no trigger, function, or
    // backfill that can drift out of sync with the source columns. They're valid here
    // because every term uses the explicit 'simple' regconfig, which makes to_tsvector
    // immutable — and 'simple' (lowercase + tokenize, no stemming) matches the
    // to_tsquery('simple', 'term:*') prefix queries in UniversalDataService.ftsSearch,
    // so index and query share one dictionary. ('simple' also behaves predictably for
    // Korean text — no English stemmer mangling hangul tokens.)
    //
    // The column is always named `search_fts` — the conventional name the
    // `fts:Domain:search_fts:q` data instruction passes. Weight buckets:
    // A = most salient (titles, names), then B, C.
    //
    // Postgres-only (dbms guard): the test env is H2 and never runs migrations
    // (updateOnStart: false), but the guard keeps an accidental H2 run a no-op.
    //
    // CAUTION: each expression names its source columns. Rename a source column and
    // Postgres errors on the generated column (fails loud, not silently stale) —
    // update the expr here in lockstep. Later feature phases append their own
    // changeSets for new tables rather than editing this one.

    changeSet(author: "ksh", id: "add-fts-generated-columns", dbms: "postgresql") {
        comment("STORED generated tsvector (search_fts) + GIN index for FTS on searchable tables")
        grailsChange {
            change {
                // table -> ordered weight buckets (A highest priority). snake_case columns.
                Map<String, Map<String, List<String>>> fts = [
                    course  : [A: ['short_title', 'long_title'], B: ['short_description', 'tags']],
                    app_user: [A: ['username', 'name'],          B: ['email']],
                ]
                fts.each { String table, Map<String, List<String>> buckets ->
                    String expr = buckets.collect { String weight, List<String> cols ->
                        cols.collect { String c ->
                            "setweight(to_tsvector('simple', COALESCE(${c}, '')), '${weight}')"
                        }.join(' || ')
                    }.join(' || ')
                    sql.execute("ALTER TABLE ${table} ADD COLUMN IF NOT EXISTS search_fts tsvector GENERATED ALWAYS AS (${expr}) STORED".toString())
                    sql.execute("CREATE INDEX IF NOT EXISTS idx_${table}_search_fts ON ${table} USING gin (search_fts)".toString())
                }
            }
            rollback {
                ['course', 'app_user'].each { String table ->
                    sql.execute("DROP INDEX IF EXISTS idx_${table}_search_fts".toString())
                    sql.execute("ALTER TABLE ${table} DROP COLUMN IF EXISTS search_fts".toString())
                }
            }
        }
    }
}
