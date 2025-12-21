package com.guidelinex.repository;

import com.guidelinex.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * DocumentRepository handles low-level data access for GuidelineX.
 * 
 * Implementation Details:
 * - Uses PostgreSQL Full-Text Search (FTS) with prefix matching
 * - Matches using 'search_vector' and 'to_tsquery' with ':*' operator
 * - Supports partial word matching (e.g., "bloo" matches "blood")
 * - Ranks results via 'ts_rank' using database-level weights (Title > Keywords)
 * - Applies multi-dimensional filtering (Type, Region, Field, Year)
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  @Query(value = """
      SELECT
        id,
        type,
        region,
        field,
        title,
        year,
        link,
        keywords,
        CASE
          WHEN CAST(:slug AS text) IS NOT NULL AND slug = CAST(:slug AS text) THEN 1000.0
          WHEN lower(title) = lower(CAST(:query AS text)) THEN 100.0
          WHEN (CAST(:query AS text) IS NULL OR :query = '') THEN 0
          ELSE (
            COALESCE(ts_rank(search_vector, websearch_to_tsquery('english', :query)), 0) * 2 +
            COALESCE(ts_rank(search_vector, to_tsquery('english', :prefixQuery)), 0)
          )
        END AS score
      FROM documents
      WHERE
        (
          (CAST(:slug AS text) IS NOT NULL AND slug = CAST(:slug AS text))
          OR
          (
            CAST(:query AS text) IS NULL OR :query = ''
            OR lower(title) = lower(CAST(:query AS text))
            OR search_vector @@ websearch_to_tsquery('english', :query)
            OR search_vector @@ to_tsquery('english', :prefixQuery)
          )
        )
        AND (CAST(:types AS text[]) IS NULL OR type = ANY(CAST(:types AS text[])))
        AND (CAST(:region AS text) IS NULL OR region = CAST(:region AS text))
        AND (CAST(:field AS text) IS NULL OR field = CAST(:field AS text))
        AND (CAST(:year_from AS integer) IS NULL OR year >= CAST(:year_from AS integer))
        AND (CAST(:year_to AS integer) IS NULL OR year <= CAST(:year_to AS integer))
      ORDER BY
        CASE WHEN (CAST(:query AS text) IS NULL OR :query = '') THEN 0 ELSE 1 END DESC,
        score DESC,
        year DESC
      """, countQuery = """
      SELECT COUNT(*) FROM documents
      WHERE
        (
          (CAST(:slug AS text) IS NOT NULL AND slug = CAST(:slug AS text))
          OR
          (
            CAST(:query AS text) IS NULL OR :query = ''
            OR lower(title) = lower(CAST(:query AS text))
            OR search_vector @@ websearch_to_tsquery('english', :query)
            OR search_vector @@ to_tsquery('english', :prefixQuery)
          )
        )
        AND (CAST(:types AS text[]) IS NULL OR type = ANY(CAST(:types AS text[])))
        AND (CAST(:region AS text) IS NULL OR region = CAST(:region AS text))
        AND (CAST(:field AS text) IS NULL OR field = CAST(:field AS text))
        AND (CAST(:year_from AS integer) IS NULL OR year >= CAST(:year_from AS integer))
        AND (CAST(:year_to AS integer) IS NULL OR year <= CAST(:year_to AS integer))
      """, nativeQuery = true)
  Page<Object[]> searchDocuments(
      @Param("query") String query,
      @Param("prefixQuery") String prefixQuery,
      @Param("slug") String slug,
      @Param("types") String[] types,
      @Param("region") String region,
      @Param("field") String field,
      @Param("year_from") Integer yearFrom,
      @Param("year_to") Integer yearTo,
      Pageable pageable);

  @Query(value = "SELECT DISTINCT type FROM documents WHERE type IS NOT NULL ORDER BY type", nativeQuery = true)
  java.util.List<String> findDistinctTypes();

  @Query(value = "SELECT DISTINCT region FROM documents WHERE region IS NOT NULL ORDER BY region", nativeQuery = true)
  java.util.List<String> findDistinctRegions();

  @Query(value = "SELECT DISTINCT field FROM documents WHERE field IS NOT NULL ORDER BY field", nativeQuery = true)
  java.util.List<String> findDistinctFields();

  @Query(value = "SELECT MIN(year) as minYear, MAX(year) as maxYear FROM documents", nativeQuery = true)
  java.util.List<Object[]> findYearRange();

  @Query(value = """
      SELECT title, slug FROM (
        SELECT DISTINCT title, slug,
               ts_rank(search_vector, to_tsquery('english', regexp_replace(:query, E'\\\\s+', ':* & ', 'g') || ':*')) as rank
        FROM documents
        WHERE search_vector @@ to_tsquery('english', regexp_replace(:query, E'\\\\s+', ':* & ', 'g') || ':*')
          AND title ILIKE '%' || :query || '%'
        ORDER BY rank DESC
        LIMIT 5
      ) ranked_titles
      """, nativeQuery = true)
  java.util.List<Object[]> findAutocompleteSuggestions(@Param("query") String query);
}
